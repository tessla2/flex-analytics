package tessla2.FlexAnalytics.domain.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tessla2.FlexAnalytics.domain.model.DataSet;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class CsvReaderService {

    public static final char DEFAULT_SEPARATOR = ',';
    public static final char SEMICOLON_SEPARATOR = ';';
    public static final char COMMA_DECIMAL = ',';
    public static final char DOT_DECIMAL = '.';

    public DataSet load(String filePath) throws IOException, CsvException {
        return load(filePath, null, true);
    }

    public DataSet load(String filePath, Character separator, Boolean convertDecimals) throws IOException, CsvException {
        return loadWithConfig(filePath, separator, convertDecimals, null, null);
    }

    public DataSet loadWithConfig(String filePath, Character separator, Boolean convertDecimals,
                           List<Integer> inputColumns, List<Integer> outputColumns) throws IOException, CsvException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("filePath must be provided");
        }

        String trimmed = filePath.trim();
        char resolvedSeparator = separator != null ? separator : detectDelimiter(trimmed);
        boolean resolvedConvertDecimals = convertDecimals != null ? convertDecimals : true;

        if (trimmed.startsWith("classpath:")) {
            String resourcePath = trimmed.substring("classpath:".length()).trim();
            return loadFromClassPath(resourcePath, resolvedSeparator, resolvedConvertDecimals,
                    inputColumns, outputColumns);
        }

        Resource cp = new ClassPathResource(trimmed);
        if (cp.exists()) {
            return loadFromReader(new InputStreamReader(cp.getInputStream()), resolvedSeparator,
                    resolvedConvertDecimals, inputColumns, outputColumns);
        }

        Path fsPath = Path.of(trimmed);
        if (Files.exists(fsPath)) {
            try (Reader fr = new FileReader(fsPath.toFile())) {
                return loadFromReader(fr, resolvedSeparator, resolvedConvertDecimals,
                        inputColumns, outputColumns);
            }
        }

        throw new FileNotFoundException("File not found as classpath resource or filesystem path: " + filePath);
    }

    public List<String[]> loadRaw(String filePath) throws IOException, CsvException {
        return loadRaw(filePath, null, true);
    }

    public List<String[]> loadRaw(String filePath, Character separator, Boolean convertDecimals) throws IOException, CsvException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("filePath must be provided");
        }

        String trimmed = filePath.trim();
        char resolvedSeparator = separator != null ? separator : detectDelimiter(trimmed);
        boolean resolvedConvertDecimals = convertDecimals != null ? convertDecimals : true;

        if (trimmed.startsWith("classpath:")) {
            String resourcePath = trimmed.substring("classpath:".length()).trim();
            return loadRawFromClassPath(resourcePath, resolvedSeparator, resolvedConvertDecimals);
        }

        Resource cp = new ClassPathResource(trimmed);
        if (cp.exists()) {
            return loadRawFromReader(new InputStreamReader(cp.getInputStream()),
                    resolvedSeparator, resolvedConvertDecimals);
        }

        Path fsPath = Path.of(trimmed);
        if (Files.exists(fsPath)) {
            try (Reader fr = new FileReader(fsPath.toFile())) {
                return loadRawFromReader(fr, resolvedSeparator, resolvedConvertDecimals);
            }
        }

        throw new FileNotFoundException("File not found: " + filePath);
    }

    public String[] getHeaders(String filePath) throws IOException, CsvException {
        List<String[]> data = loadRaw(filePath);
        return data.isEmpty() ? new String[0] : data.get(0);
    }

    private char detectDelimiter(String filePath) throws IOException {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            return DEFAULT_SEPARATOR;
        }
        try (Reader reader = new FileReader(path.toFile())) {
            char[] buffer = new char[1024];
            int read = reader.read(buffer);
            String sample = new String(buffer, 0, read);

            long semicolonCount = sample.chars().filter(c -> c == ';').count();
            long commaCount = sample.chars().filter(c -> c == ',').count();

            if (semicolonCount > commaCount) {
                return SEMICOLON_SEPARATOR;
            }
            return DEFAULT_SEPARATOR;
        } catch (IOException e) {
            return DEFAULT_SEPARATOR;
        }
    }

    private DataSet loadFromClassPath(String resourcePath, char separator, boolean convertDecimals) throws IOException, CsvException {
        return loadFromClassPath(resourcePath, separator, convertDecimals, null, null);
    }

    private DataSet loadFromReader(Reader reader, char separator, boolean convertDecimals) throws IOException, CsvException {
        return loadFromReader(reader, separator, convertDecimals, null, null);
    }

    private DataSet loadFromReader(Reader reader, char separator, boolean convertDecimals,
                                  List<Integer> inputColumns, List<Integer> outputColumns) throws IOException, CsvException {
        List<String[]> rows = new ArrayList<>();
        StringBuilder lineBuilder = new StringBuilder();
        int ch;
        boolean inQuotes = false;
        List<String> currentRow = new ArrayList<>();
        StringBuilder fieldBuilder = new StringBuilder();

        while ((ch = reader.read()) != -1) {
            char c = (char) ch;
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if ((c == separator || c == '\n' || c == '\r') && !inQuotes) {
                if (c == separator) {
                    currentRow.add(fieldBuilder.toString().trim());
                    fieldBuilder = new StringBuilder();
                } else if (c == '\n') {
                    currentRow.add(fieldBuilder.toString().trim());
                    if (!currentRow.isEmpty() || !fieldBuilder.toString().isEmpty()) {
                        rows.add(currentRow.toArray(new String[0]));
                    }
                    currentRow = new ArrayList<>();
                    fieldBuilder = new StringBuilder();
                }
            } else {
                fieldBuilder.append(c);
            }
        }

        if (fieldBuilder.length() > 0 || !currentRow.isEmpty()) {
            currentRow.add(fieldBuilder.toString().trim());
            rows.add(currentRow.toArray(new String[0]));
        }

        reader.close();

        if (rows == null || rows.size() < 2) {
            throw new IllegalArgumentException("CSV vazio ou sem dados suficientes.");
        }

        String[] headers = rows.get(0);
        int numVars = headers.length - 1;
        if (numVars < 1) {
            throw new IllegalArgumentException("CSV must contain at least one input column and one output column.");
        }

        List<double[]> inputs = new ArrayList<>();
        double[] output = new double[rows.size() - 1];

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length != headers.length) {
                throw new IllegalArgumentException("Linha " + (i + 1) + " com número incorreto de colunas.");
            }

            double[] x = new double[numVars];
            for (int j = 0; j < numVars; j++) {
                x[j] = parseCell(row[j], i + 1, headers[j], convertDecimals);
            }
            inputs.add(x);
            output[i - 1] = parseCell(row[numVars], i + 1, headers[numVars], convertDecimals);
        }

        return new DataSet(headers, inputs, output, numVars);
    }

    private double parseCell(String value, int line, String column, boolean convertDecimals) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Valor vazio ou nulo na coluna '" + column + "', linha " + line);
        }

        String trimmed = value.trim();

        if (convertDecimals && trimmed.contains(String.valueOf(COMMA_DECIMAL))) {
            trimmed = trimmed.replace(COMMA_DECIMAL, DOT_DECIMAL);
        }

        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Valor inválido na coluna '" + column + "', linha " + line + ": '" + value + "'");
        }
    }

    private DataSet loadFromClassPath(String resourcePath, char separator, boolean convertDecimals,
                                List<Integer> inputColumns, List<Integer> outputColumns) throws IOException, CsvException {
        Resource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            throw new FileNotFoundException("Arquivo não encontrado no classpath: " + resourcePath);
        }
        try (InputStreamReader isr = new InputStreamReader(resource.getInputStream())) {
            return loadFromReader(isr, separator, convertDecimals, inputColumns, outputColumns);
        }
    }

    private List<String[]> loadRawFromClassPath(String resourcePath, char separator, boolean convertDecimals)
            throws IOException {
        Resource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            throw new FileNotFoundException("Arquivo não encontrado no classpath: " + resourcePath);
        }
        try (InputStreamReader isr = new InputStreamReader(resource.getInputStream())) {
            return loadRawFromReader(isr, separator, convertDecimals);
        }
    }

    private List<String[]> loadRawFromReader(Reader reader, char separator, boolean convertDecimals)
            throws IOException {
        return parseCsv(reader, separator);
    }

    private List<String[]> parseCsv(Reader reader, char separator) throws IOException {
        List<String[]> rows = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        StringBuilder fieldBuilder = new StringBuilder();
        boolean inQuotes = false;
        int ch;

        while ((ch = reader.read()) != -1) {
            char c = (char) ch;
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if ((c == separator || c == '\n' || c == '\r') && !inQuotes) {
                if (c == separator) {
                    currentRow.add(fieldBuilder.toString().trim());
                    fieldBuilder = new StringBuilder();
                } else if (c == '\n') {
                    currentRow.add(fieldBuilder.toString().trim());
                    if (!currentRow.isEmpty() || !fieldBuilder.toString().isEmpty()) {
                        rows.add(currentRow.toArray(new String[0]));
                    }
                    currentRow = new ArrayList<>();
                    fieldBuilder = new StringBuilder();
                }
            } else {
                fieldBuilder.append(c);
            }
        }


        if (fieldBuilder.length() > 0 || !currentRow.isEmpty()) {
            currentRow.add(fieldBuilder.toString().trim());
            rows.add(currentRow.toArray(new String[0]));
        }

        reader.close();
        return rows;
    }
}
