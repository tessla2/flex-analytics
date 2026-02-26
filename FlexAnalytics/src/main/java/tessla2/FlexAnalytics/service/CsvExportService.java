package tessla2.FlexAnalytics.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tessla2.FlexAnalytics.model.SensitivityResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@Service
public class CsvExportService {

    public void export(String inputFile, String outputFile, List<SensitivityResult> result)
            throws IOException, CsvException {

        // Resolve input as a classpath resource (strip prefix if present)
        Resource resource = new ClassPathResource(inputFile.replace("classpath:", "").trim());

        // If resource doesn't exist, fail fast
        if (!resource.exists()) {
            throw new IllegalArgumentException("Input file not found: " + inputFile);
        }

        // Read original CSV into memory
        List<String[]> rows;
        try (CSVReader reader = new CSVReader(new InputStreamReader(resource.getInputStream()))) {
            rows = reader.readAll();
        }

        // Create a map of variable -> correlation for lookup (preserves insertion order)
        Map<String, Double> correlationMap = new LinkedHashMap<>();
        for (SensitivityResult r : result) {
            correlationMap.put(r.getVariable(), r.getCorrelation());
        }

        // Add new analysis columns to header
        String[] originalHeader = rows.get(0);
        String[] newHeader = Arrays.copyOf(originalHeader, originalHeader.length + 2);
        newHeader[originalHeader.length] = "higher_impact_variable";
        newHeader[originalHeader.length + 1] = "correlation";
        rows.set(0, newHeader);

        // Find variable with highest absolute correlation
        SensitivityResult top = result.stream()
                 .max(Comparator.comparingDouble(r -> Math.abs(r.getCorrelation())))
                .orElseThrow(() -> new IllegalStateException("No sensitivity results provided"));

        // Determine top variable
        String topVariable = top.getVariable();
        double topCorrelation = top.getCorrelation();

        // Append new columns to each data row
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            String[] newRow = Arrays.copyOf(row, row.length + 2);
            newRow[row.length] = topVariable;
            newRow[row.length + 1] = String.format("%+.3f", topCorrelation);
            rows.set(i, newRow);
        }

        // Ensure output directory exists (safe if parent is null)
        File outFile = new File(outputFile);
        File parent = outFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        // Write CSV
        try (CSVWriter writer = new CSVWriter(new FileWriter(outFile))) {
            writer.writeAll(rows);
        }
    }
}