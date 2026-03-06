package tessla2.FlexAnalytics.service;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tessla2.FlexAnalytics.model.DataSet;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Service responsible for loading a CSV and converting rows into a DataSet.
 *
 * Supports the following FlexSim export types:
 * - Time series          (Time + metric)
 * - Object state         (Object + State + Time + Utilization)
 * - Sensor / battery     (AMR + metric + Tempo)
 * - Aggregated production (numeric columns only)
 * - Multi-scenario       (ScenarioID + RepNum + columns)
 *
 * Auto-detection rules:
 * 1. FlexSim metadata columns (ScenarioID, RepNum) are always ignored.
 * 2. Constant columns (zero variance) are ignored.
 * 3. Text columns with variation are label-encoded (categorical).
 * 4. Text columns that are constant are ignored.
 * 5. Date/time columns are converted to epoch seconds (UTC).
 * 6. Output = last non-ignored numeric column with variance.
 * 7. Time series (date as first useful column) → aggregate by hour.
 */
@Service
public class CsvReaderService {

    @Value("${app.max-file-size-mb:10}")
    private int maxFileSizeMb;

    private static final double MIN_VARIANCE = 1e-4;

    // FlexSim metadata columns — always ignored
    private static final Set<String> FLEXSIM_METADATA = Set.of(
            "scenarioid", "repnum"
    );

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy")
    );

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    public DataSet load(String filePath) throws IOException, CsvException {
        String cleanPath = filePath.replace("classpath:", "").trim();

        String rawContent;
        try (InputStream is = resolveInputStream(cleanPath)) {
            rawContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        char separator = detectSeparator(rawContent);
        CSVParser parser = new CSVParserBuilder().withSeparator(separator).build();

        List<String[]> rows;
        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(
                        new ByteArrayInputStream(rawContent.getBytes(StandardCharsets.UTF_8))))
                .withCSVParser(parser)
                .build()) {
            rows = reader.readAll();
        }

        if (rows == null || rows.size() < 2)
            throw new IllegalArgumentException("Empty CSV or insufficient data.");

        return buildDataSet(rows);
    }

    // -------------------------------------------------------------------------
    // DataSet builder — core logic
    // -------------------------------------------------------------------------

    private DataSet buildDataSet(List<String[]> rows) {
        String[] headers = rows.get(0);
        int totalCols = headers.length;
        int numDataRows = rows.size() - 1;

        // Step 1 — classify each column
        ColumnType[] types = classifyColumns(headers, rows);

        // Step 2 — label-encode text columns
        Map<Integer, Map<String, Double>> encodings = buildLabelEncodings(headers, rows, types);

        // Step 3 — resolve numeric values per column (including encoded + dates)
        double[][] matrix = buildMatrix(rows, headers, types, encodings, numDataRows, totalCols);

        // Step 4 — compute variance per column to filter constants
        double[] variances = computeVariances(matrix, numDataRows, totalCols);

        // Step 5 — determine which columns are active (not metadata, not constant)
        List<Integer> activeIndices = new ArrayList<>();
        for (int c = 0; c < totalCols; c++) {
            if (types[c] == ColumnType.METADATA || types[c] == ColumnType.IGNORED) continue;
            if (variances[c] < MIN_VARIANCE) continue; // constante ou quase constante
            activeIndices.add(c);
        }

        if (activeIndices.isEmpty())
            throw new IllegalArgumentException("No usable columns found after filtering constants.");

        // Step 6 — detect if it's a time series (first active col is a date)
        boolean isTimeSeries = types[activeIndices.get(0)] == ColumnType.DATE
                && activeIndices.size() >= 2;

        if (isTimeSeries) {
            return buildTimeSeriesDataSet(rows, headers, activeIndices, types);
        }

        // Step 7 — output = last active column; inputs = all others
        int outputColIndex = activeIndices.get(activeIndices.size() - 1);
        List<Integer> inputIndices = activeIndices.subList(0, activeIndices.size() - 1);

        if (inputIndices.isEmpty())
            throw new IllegalArgumentException("CSV must have at least one input variable.");

        // Step 8 — build headers, inputs, output, categoricalIndices
        int numVars = inputIndices.size();
        String[] newHeaders = new String[numVars + 1];
        Set<Integer> categoricalIndices = new HashSet<>();

        for (int i = 0; i < numVars; i++) {
            int col = inputIndices.get(i);
            newHeaders[i] = headers[col];
            if (types[col] == ColumnType.CATEGORICAL) {
                categoricalIndices.add(i);
            }
        }
        newHeaders[numVars] = headers[outputColIndex];

        List<double[]> inputs = new ArrayList<>();
        double[] output = new double[numDataRows];

        for (int r = 0; r < numDataRows; r++) {
            double[] x = new double[numVars];
            for (int i = 0; i < numVars; i++) {
                x[i] = matrix[r][inputIndices.get(i)];
            }
            inputs.add(x);
            output[r] = matrix[r][outputColIndex];
        }

        return new DataSet(newHeaders, numVars, inputs, output, categoricalIndices);
    }

    // -------------------------------------------------------------------------
    // Time series — aggregate by hour
    // -------------------------------------------------------------------------

    private DataSet buildTimeSeriesDataSet(List<String[]> rows, String[] headers,
                                           List<Integer> activeIndices, ColumnType[] types) {
        int dateCol = activeIndices.get(0);
        int metricCol = activeIndices.get(activeIndices.size() - 1);

        Map<Integer, Double> sumByHour = new LinkedHashMap<>();
        Map<Integer, Integer> countByHour = new LinkedHashMap<>();

        for (int i = 1; i < rows.size(); i++) {
            String raw = rows.get(i)[dateCol].trim();
            int hour = extractHourOfDay(raw, i, headers[dateCol]);

            String metricRaw = rows.get(i)[metricCol].trim().replace(',', '.');
            double metricVal;
            try {
                metricVal = Double.parseDouble(metricRaw);
            } catch (NumberFormatException e) {
                metricVal = 1.0; // count-based (ex: Contentores)
            }

            sumByHour.merge(hour, metricVal, Double::sum);
            countByHour.merge(hour, 1, Integer::sum);
        }

        // If all values are integers, treat as count-based series
        boolean isCountBased = sumByHour.values().stream().allMatch(v -> v % 1.0 == 0);

        String[] newHeaders = {"Hour", headers[metricCol]};
        List<double[]> inputs = new ArrayList<>();
        double[] output = new double[sumByHour.size()];

        int i = 0;
        for (Map.Entry<Integer, Double> entry : sumByHour.entrySet()) {
            inputs.add(new double[]{entry.getKey()});
            output[i++] = isCountBased ? countByHour.get(entry.getKey()) : entry.getValue();
        }

        return new DataSet(newHeaders, 1, inputs, output, Set.of());
    }

    // -------------------------------------------------------------------------
    // Column classification
    // -------------------------------------------------------------------------

    private enum ColumnType {
        METADATA,     // ScenarioID, RepNum — always ignored
        NUMERIC,      // parseable as double
        DATE,         // parseable as date/time
        CATEGORICAL,  // text with variation → label-encoded
        IGNORED       // constant text or unrecognized
    }

    private ColumnType[] classifyColumns(String[] headers, List<String[]> rows) {
        ColumnType[] types = new ColumnType[headers.length];

        for (int c = 0; c < headers.length; c++) {
            // FlexSim metadata
            if (FLEXSIM_METADATA.contains(headers[c].trim().toLowerCase())) {
                types[c] = ColumnType.METADATA;
                continue;
            }

            // Sample first non-empty data cell
            String sample = "";
            for (int r = 1; r < rows.size(); r++) {
                if (rows.get(r).length > c && !rows.get(r)[c].trim().isEmpty()) {
                    sample = rows.get(r)[c].trim();
                    break;
                }
            }

            if (sample.isEmpty()) {
                types[c] = ColumnType.IGNORED;
                continue;
            }

            // Numeric?
            try {
                Double.parseDouble(sample.replace(',', '.'));
                types[c] = ColumnType.NUMERIC;
                continue;
            } catch (NumberFormatException ignored) {}

            // Date?
            if (isDateValue(sample)) {
                types[c] = ColumnType.DATE;
                continue;
            }

            // Text → categorical
            types[c] = ColumnType.CATEGORICAL;
        }

        return types;
    }

    // -------------------------------------------------------------------------
    // Label encoding
    // -------------------------------------------------------------------------

    private Map<Integer, Map<String, Double>> buildLabelEncodings(
            String[] headers, List<String[]> rows, ColumnType[] types) {

        Map<Integer, Map<String, Double>> encodings = new HashMap<>();

        for (int c = 0; c < headers.length; c++) {
            if (types[c] != ColumnType.CATEGORICAL) continue;

            Map<String, Double> encoding = new LinkedHashMap<>();
            double code = 1.0;
            for (int r = 1; r < rows.size(); r++) {
                if (rows.get(r).length <= c) continue;
                String val = rows.get(r)[c].trim();
                if (!encoding.containsKey(val)) {
                    encoding.put(val, code++);
                }
            }
            encodings.put(c, encoding);
        }

        return encodings;
    }

    // -------------------------------------------------------------------------
    // Matrix builder
    // -------------------------------------------------------------------------

    private double[][] buildMatrix(List<String[]> rows, String[] headers,
                                   ColumnType[] types,
                                   Map<Integer, Map<String, Double>> encodings,
                                   int numDataRows, int totalCols) {
        double[][] matrix = new double[numDataRows][totalCols];

        for (int r = 0; r < numDataRows; r++) {
            String[] row = rows.get(r + 1);
            for (int c = 0; c < totalCols; c++) {
                if (types[c] == ColumnType.METADATA || types[c] == ColumnType.IGNORED) {
                    matrix[r][c] = 0.0;
                    continue;
                }

                String raw = (row.length > c) ? row[c].trim() : "";

                switch (types[c]) {
                    case NUMERIC     -> matrix[r][c] = parseNumeric(raw, r + 1, headers[c]);
                    case DATE        -> matrix[r][c] = parseDate(raw, r + 1, headers[c]);
                    case CATEGORICAL -> matrix[r][c] = encodings
                            .getOrDefault(c, Map.of())
                            .getOrDefault(raw, 0.0);
                    default          -> matrix[r][c] = 0.0;
                }
            }
        }

        return matrix;
    }

    // -------------------------------------------------------------------------
    // Variance
    // -------------------------------------------------------------------------

    private double[] computeVariances(double[][] matrix, int numRows, int numCols) {
        double[] variances = new double[numCols];
        for (int c = 0; c < numCols; c++) {
            double sum = 0, sumSq = 0;
            for (int r = 0; r < numRows; r++) {
                sum += matrix[r][c];
                sumSq += matrix[r][c] * matrix[r][c];
            }
            double mean = sum / numRows;
            variances[c] = (sumSq / numRows) - (mean * mean);
        }
        return variances;
    }

    // -------------------------------------------------------------------------
    // Date utilities
    // -------------------------------------------------------------------------

    private boolean isDateValue(String value) {
        try { OffsetDateTime.parse(value); return true; } catch (DateTimeParseException ignored) {}
        for (DateTimeFormatter f : DATE_FORMATTERS) {
            try { LocalDateTime.parse(value, f); return true; } catch (DateTimeParseException ignored) {}
            try { LocalDate.parse(value, f); return true; } catch (DateTimeParseException ignored) {}
        }
        return false;
    }

    private double parseDate(String value, int line, String column) {
        try { return OffsetDateTime.parse(value).toEpochSecond(); } catch (DateTimeParseException ignored) {}
        for (DateTimeFormatter f : DATE_FORMATTERS) {
            try { return LocalDateTime.parse(value, f).toEpochSecond(ZoneOffset.UTC); } catch (DateTimeParseException ignored) {}
            try { return LocalDate.parse(value, f).atStartOfDay().toEpochSecond(ZoneOffset.UTC); } catch (DateTimeParseException ignored) {}
        }
        throw new IllegalArgumentException(
                "Invalid date on column '" + column + "', line " + line + ": '" + value + "'");
    }

    private int extractHourOfDay(String value, int line, String column) {
        try { return OffsetDateTime.parse(value).getHour(); } catch (DateTimeParseException ignored) {}
        for (DateTimeFormatter f : DATE_FORMATTERS) {
            try { return LocalDateTime.parse(value, f).getHour(); } catch (DateTimeParseException ignored) {}
            try { return LocalDate.parse(value, f).atStartOfDay().getHour(); } catch (DateTimeParseException ignored) {}
        }
        throw new IllegalArgumentException(
                "Invalid date on column '" + column + "', line " + line + ": '" + value + "'");
    }

    // -------------------------------------------------------------------------
    // Numeric parse
    // -------------------------------------------------------------------------

    private double parseNumeric(String value, int line, String column) {
        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid value on column '" + column + "', line " + line + ": '" + value + "'");
        }
    }

    // -------------------------------------------------------------------------
    // Separator detection
    // -------------------------------------------------------------------------

    private char detectSeparator(String content) {
        String firstLine = content.split("\\r?\\n")[0];
        for (char candidate : new char[]{';', ',', '\t', '|'}) {
            if (firstLine.indexOf(candidate) >= 0) return candidate;
        }
        return ',';
    }

    // -------------------------------------------------------------------------
    // File resolution & validation
    // -------------------------------------------------------------------------

    private InputStream resolveInputStream(String path) throws IOException {
        File file = new File(path);

        if (file.isAbsolute() && file.exists()) {
            validateFile(file);
            return new FileInputStream(file);
        }

        Resource resource = new ClassPathResource(path);
        if (!resource.exists())
            throw new FileNotFoundException("File not found: " + path);

        validateFile(resource.getFile());
        return resource.getInputStream();
    }

    private void validateFile(File file) {
        if (!file.getName().endsWith(".csv"))
            throw new IllegalArgumentException("Only .csv files are allowed.");

        if (file.length() > (long) maxFileSizeMb * 1024 * 1024)
            throw new IllegalArgumentException(
                    "File exceeds the maximum size of " + maxFileSizeMb + "MB.");
    }
}