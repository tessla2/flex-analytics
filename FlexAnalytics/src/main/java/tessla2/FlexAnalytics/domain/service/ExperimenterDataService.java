package tessla2.FlexAnalytics.domain.service;

import com.opencsv.exceptions.CsvException;
import org.springframework.stereotype.Service;
import tessla2.FlexAnalytics.domain.model.DataSet;

import java.io.IOException;
import java.util.*;

@Service
public class ExperimenterDataService {

    private final CsvReaderService csvReaderService;

    public ExperimenterDataService(CsvReaderService csvReaderService) {
        this.csvReaderService = csvReaderService;
    }

    public Map<String, Map<String, Double>> loadAndMergeAllNumeric(List<String> filePaths,
                                                       List<String> keyColumns) throws IOException, CsvException {
        Map<String, Map<String, Double>> mergedData = new LinkedHashMap<>();

        for (String filePath : filePaths) {
            List<String[]> rawData = csvReaderService.loadRaw(filePath);
            if (rawData.isEmpty()) {
                continue;
            }

            String[] headers = rawData.get(0);
            List<Integer> keyIndices = findColumnIndices(headers, keyColumns);

            if (keyIndices.isEmpty()) {
                continue;
            }

            Set<String> numericColumns = new LinkedHashSet<>();
            for (int col = 0; col < headers.length; col++) {
                if (!keyIndices.contains(col)) {
                    numericColumns.add(headers[col].trim());
                }
            }

            for (int i = 1; i < rawData.size(); i++) {
                String[] row = rawData.get(i);
                String key = buildKey(row, keyIndices);

                if (!mergedData.containsKey(key)) {
                    mergedData.put(key, new LinkedHashMap<>());
                }
                Map<String, Double> record = mergedData.get(key);

                for (String colName : numericColumns) {
                    int colIdx = findColumnIndex(headers, colName);
                    if (colIdx >= 0 && colIdx < row.length) {
                        double value = parseDouble(row[colIdx]);
                        record.merge(colName, value, (a, b) -> a + b);
                    }
                }
            }
        }

        return mergedData;
    }

    public Map<String, List<Map<String, Object>>> loadAndMerge(List<String> filePaths,
                                                     List<String> keyColumns,
                                                     String valueColumn) throws IOException, CsvException {
        Map<String, List<Map<String, Object>>> mergedData = new LinkedHashMap<>();

        for (String filePath : filePaths) {
            List<String[]> rawData = csvReaderService.loadRaw(filePath);
            if (rawData.isEmpty()) {
                continue;
            }

            String[] headers = rawData.get(0);
            List<Integer> keyIndices = findColumnIndices(headers, keyColumns);
            int valueIndex = findColumnIndex(headers, valueColumn);

            if (keyIndices.isEmpty() || valueIndex < 0) {
                continue;
            }

            for (int i = 1; i < rawData.size(); i++) {
                String[] row = rawData.get(i);
                if (row.length <= valueIndex) {
                    continue;
                }

                String key = buildKey(row, keyIndices);
                Map<String, Object> record = new HashMap<>();
                record.put(valueColumn, parseDouble(row[valueIndex]));
                record.put("_source", filePath);

                mergedData.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
            }
        }

        return mergedData;
    }

    public Map<String, Map<String, Double>> aggregateByKeys(Map<String, List<Map<String, Object>>> rawData) {
        Map<String, Map<String, Double>> aggregated = new LinkedHashMap<>();

        for (Map.Entry<String, List<Map<String, Object>>> entry : rawData.entrySet()) {
            Map<String, Double> summary = new LinkedHashMap<>();
            List<Map<String, Object>> records = entry.getValue();

            if (!records.isEmpty()) {
                Set<String> valueColumns = records.get(0).keySet();
                valueColumns.remove("_source");

                for (String col : valueColumns) {
                    double sum = 0;
                    int count = 0;
                    for (Map<String, Object> record : records) {
                        Object val = record.get(col);
                        if (val instanceof Number) {
                            sum += ((Number) val).doubleValue();
                            count++;
                        }
                    }
                    if (count > 0) {
                        summary.put(col, sum / count);
                    }
                }
            }

            aggregated.put(entry.getKey(), summary);
        }

        return aggregated;
    }

    public DataSet prepareForSensitivity(Map<String, Map<String, Double>> aggregatedData,
                                    List<String> inputColumns,
                                    List<String> outputColumns) {
        List<String> headers = new ArrayList<>();
        headers.addAll(inputColumns);
        headers.addAll(outputColumns);

        List<double[]> inputs = new ArrayList<>();
        double[] outputs = new double[aggregatedData.size()];

        int rowIndex = 0;
        for (Map<String, Double> data : aggregatedData.values()) {
            double[] row = new double[inputColumns.size() + outputColumns.size()];

            for (int i = 0; i < inputColumns.size(); i++) {
                row[i] = data.getOrDefault(inputColumns.get(i), 0.0);
            }
            for (int i = 0; i < outputColumns.size(); i++) {
                row[inputColumns.size() + i] = data.getOrDefault(outputColumns.get(i), 0.0);
            }

            inputs.add(row);
            rowIndex++;
        }

        return new DataSet(headers.toArray(new String[0]), inputs, outputs, inputColumns.size());
    }

    public List<String[]> pivotWide(List<String[]> data, String keyColumn, String valueColumn) {
        if (data.size() < 2) {
            return data;
        }

        String[] headers = data.get(0);
        int keyIdx = findColumnIndex(headers, keyColumn);
        int valueIdx = findColumnIndex(headers, valueColumn);

        if (keyIdx < 0 || valueIdx < 0) {
            return data;
        }

        Set<String> values = new LinkedHashSet<>();
        for (int i = 1; i < data.size(); i++) {
            if (data.get(i).length > valueIdx) {
                values.add(data.get(i)[valueIdx]);
            }
        }

        String[] newHeaders = new String[headers.length + values.size()];
        System.arraycopy(headers, 0, newHeaders, 0, headers.length);
        int idx = headers.length;
        for (String v : values) {
            newHeaders[idx++] = valueColumn + "_" + v;
        }

        List<String[]> pivoted = new ArrayList<>();
        pivoted.add(newHeaders);

        Map<String, Map<String, String>> grouped = new LinkedHashMap<>();
        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);
            if (row.length <= valueIdx) {
                continue;
            }
            String key = row[keyIdx];
            String valueName = row[valueIdx];
            String value = row[valueIdx];

            grouped.computeIfAbsent(key, k -> new LinkedHashMap<>()).put(valueName, value);
        }

        for (Map.Entry<String, Map<String, String>> entry : grouped.entrySet()) {
            String[] newRow = new String[newHeaders.length];
            System.arraycopy(entry.getValue().keySet().toArray(), 0, newRow, 0, Math.min(headers.length, newRow.length));

            idx = headers.length;
            for (String v : values) {
                newRow[idx++] = entry.getValue().getOrDefault(v, "0");
            }
            pivoted.add(newRow);
        }

        return pivoted;
    }

    private String buildKey(String[] row, List<Integer> indices) {
        StringBuilder key = new StringBuilder();
        for (int i : indices) {
            if (i < row.length) {
                if (key.length() > 0) {
                    key.append("_");
                }
                key.append(row[i].trim());
            }
        }
        return key.toString();
    }

    private List<Integer> findColumnIndices(String[] headers, List<String> columns) {
        List<Integer> indices = new ArrayList<>();
        for (String col : columns) {
            indices.add(findColumnIndex(headers, col));
        }
        return indices;
    }

    private int findColumnIndex(String[] headers, String column) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(column)) {
                return i;
            }
        }
        return -1;
    }

    private double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        try {
            String cleaned = value.trim().replace(',', '.');
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public void exportToCsv(Map<String, Map<String, Double>> data, String outputPath) throws IOException {
        java.io.File outFile = new java.io.File(outputPath);
        if (outFile.getParentFile() != null) {
            outFile.getParentFile().mkdirs();
        }

        try (java.io.FileWriter writer = new java.io.FileWriter(outFile);
             java.io.BufferedWriter bw = new java.io.BufferedWriter(writer)) {

            Set<String> columns = new LinkedHashSet<>();
            for (Map<String, Double> row : data.values()) {
                columns.addAll(row.keySet());
            }

            bw.write("ScenarioID");
            for (String col : columns) {
                bw.write(";" + col);
            }
            bw.newLine();

            for (Map.Entry<String, Map<String, Double>> entry : data.entrySet()) {
                bw.write(entry.getKey());
                for (String col : columns) {
                    Double val = entry.getValue().getOrDefault(col, 0.0);
                    bw.write(";" + String.format(java.util.Locale.US, "%.6f", val));
                }
                bw.newLine();
            }
        }
    }
}