package tessla2.FlexAnalytics.domain.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tessla2.FlexAnalytics.domain.model.SensitivityResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class CsvExportService {

    public void export(String inputFile, String outputFile, List<SensitivityResult> result)
            throws IOException, CsvException {

        Resource resource = new ClassPathResource(inputFile.replace("classpath:", "").trim());

        if (!resource.exists()) {
            throw new IllegalArgumentException("Input file not found: " + inputFile);
        }

        List<String[]> rows;
        try (CSVReader reader = new CSVReader(new InputStreamReader(resource.getInputStream()))) {
            rows = reader.readAll();
        }

        String[] originalHeader = rows.get(0);
        String[] newHeader = Arrays.copyOf(originalHeader, originalHeader.length + 2);
        newHeader[originalHeader.length] = "higher_impact_variable";
        newHeader[originalHeader.length + 1] = "correlation";
        rows.set(0, newHeader);

        SensitivityResult top = result.stream()
                .max(Comparator.comparingDouble(r -> Math.abs(r.getCorrelation())))
                .orElseThrow(() -> new IllegalStateException("No sensitivity results provided"));

        String topVariable = top.getVariable();
        double topCorrelation = top.getCorrelation();

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            String[] newRow = Arrays.copyOf(row, row.length + 2);
            newRow[row.length] = topVariable;
            newRow[row.length + 1] = String.format("%+.3f", topCorrelation);
            rows.set(i, newRow);
        }

        File outFile = new File(outputFile);
        File parent = outFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        try (CSVWriter writer = new CSVWriter(new FileWriter(outFile))) {
            writer.writeAll(rows);
        }
    }
}
