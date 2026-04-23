package tessla2.FlexAnalytics.domain.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tessla2.FlexAnalytics.domain.model.ScenarioSummary;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ExperimenterMergeService {

    public List<ScenarioSummary> mergeAndSummarize(List<String> inputFiles, String outputFile) throws IOException, CsvException {
        return mergeAndSummarize(inputFiles, outputFile, "ScenarioID", "Object", "Throughput");
    }

    public List<ScenarioSummary> mergeAndSummarize(List<String> inputFiles,
                                                   String outputFile,
                                                   String scenarioColumn,
                                                   String objectColumn,
                                                   String valueColumn) throws IOException, CsvException {
        if (inputFiles == null || inputFiles.isEmpty()) {
            throw new IllegalArgumentException("At least one input file is required");
        }

        Map<Integer, Map<String, DoubleSummaryStatistics>> grouped = new LinkedHashMap<>();
        Set<String> objects = new LinkedHashSet<>();

        for (String filePath : inputFiles) {
            loadSingleFile(filePath, grouped, objects, scenarioColumn, objectColumn, valueColumn);
        }

        List<ScenarioSummary> summaries = buildSummaries(grouped);
        exportMergedCsv(outputFile, summaries, objects);
        return summaries;
    }

    private void loadSingleFile(String filePath,
                                Map<Integer, Map<String, DoubleSummaryStatistics>> grouped,
                                Set<String> objects,
                                String scenarioColumn,
                                String objectColumn,
                                String valueColumn) throws IOException, CsvException {

        try (CSVReader csv = new CSVReader(resolveReader(filePath))) {
            List<String[]> rows = csv.readAll();
            if (rows.size() < 2) {
                return;
            }

            String[] header = rows.get(0);
            int scenarioIndex = indexOf(header, scenarioColumn);
            int objectIndex = indexOf(header, objectColumn);
            int throughputIndex = indexOf(header, valueColumn);

            if (scenarioIndex < 0 || objectIndex < 0 || throughputIndex < 0) {
                throw new IllegalArgumentException(
                        "CSV must contain columns " + scenarioColumn + ", " + objectColumn + ", " + valueColumn + ": " + filePath);
            }

            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length <= throughputIndex) {
                    continue;
                }

                int scenarioId = Integer.parseInt(row[scenarioIndex].trim());
                String object = row[objectIndex].trim();
                double throughput = Double.parseDouble(row[throughputIndex].trim());

                objects.add(object);
                grouped.computeIfAbsent(scenarioId, id -> new LinkedHashMap<>())
                        .computeIfAbsent(object, key -> new DoubleSummaryStatistics())
                        .accept(throughput);
            }
        }
    }

    private Reader resolveReader(String filePath) throws IOException {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("input file path is empty");
        }

        String trimmed = filePath.trim();
        if (trimmed.startsWith("classpath:")) {
            String resourcePath = trimmed.substring("classpath:".length()).trim();
            Resource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                throw new FileNotFoundException("Classpath file not found: " + resourcePath);
            }
            return new InputStreamReader(resource.getInputStream());
        }

        Resource cp = new ClassPathResource(trimmed);
        if (cp.exists()) {
            return new InputStreamReader(cp.getInputStream());
        }

        Path fsPath = Path.of(trimmed);
        if (Files.exists(fsPath)) {
            return new FileReader(fsPath.toFile());
        }

        throw new FileNotFoundException("File not found as classpath resource or filesystem path: " + filePath);
    }

    private List<ScenarioSummary> buildSummaries(Map<Integer, Map<String, DoubleSummaryStatistics>> grouped) {
        List<ScenarioSummary> summaries = new ArrayList<>();

        for (Map.Entry<Integer, Map<String, DoubleSummaryStatistics>> scenarioEntry : grouped.entrySet()) {
            Map<String, Double> byObject = new LinkedHashMap<>();
            double total = 0.0;

            for (Map.Entry<String, DoubleSummaryStatistics> objectEntry : scenarioEntry.getValue().entrySet()) {
                double mean = objectEntry.getValue().getAverage();
                byObject.put(objectEntry.getKey(), mean);
                total += mean;
            }

            summaries.add(new ScenarioSummary(scenarioEntry.getKey(), total, byObject));
        }

        summaries.sort(Comparator.comparingDouble(ScenarioSummary::totalThroughput).reversed());
        return summaries;
    }

    private void exportMergedCsv(String outputFile, List<ScenarioSummary> summaries, Set<String> objects) throws IOException {
        if (outputFile == null || outputFile.isBlank()) {
            return;
        }

        File outFile = new File(outputFile);
        File parent = outFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        try (CSVWriter writer = new CSVWriter(new FileWriter(outFile))) {
            List<String> header = new ArrayList<>();
            header.add("scenario_id");
            header.add("total_throughput");
            for (String object : objects) {
                header.add(normalizeColumnName(object));
            }
            writer.writeNext(header.toArray(new String[0]));

            for (ScenarioSummary summary : summaries) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(summary.scenarioId()));
                row.add(String.format(Locale.US, "%.4f", summary.totalThroughput()));
                for (String object : objects) {
                    double value = summary.throughputByObject().getOrDefault(object, 0.0);
                    row.add(String.format(Locale.US, "%.4f", value));
                }
                writer.writeNext(row.toArray(new String[0]));
            }
        }
    }

    private int indexOf(String[] header, String expected) {
        for (int i = 0; i < header.length; i++) {
            if (expected.equalsIgnoreCase(header[i].trim())) {
                return i;
            }
        }
        return -1;
    }

    private String normalizeColumnName(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("(^_+|_+$)", "");
    }
}
