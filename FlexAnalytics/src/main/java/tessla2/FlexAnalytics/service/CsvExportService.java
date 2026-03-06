package tessla2.FlexAnalytics.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tessla2.FlexAnalytics.model.SensitivityResult;

import java.io.*;
import java.util.*;

@Service
public class CsvExportService {

    public void export(String inputFile, String outputFile, List<SensitivityResult> results)
            throws IOException, CsvException {

        List<String[]> rows;

        // supports classpath and absolute
        try (InputStream is = resolveInputStream(inputFile);
             CSVReader reader = new CSVReader(new InputStreamReader(is))) {
            rows = reader.readAll();
        }

        if (rows == null || rows.isEmpty())
            throw new IllegalArgumentException("CSV de entrada vazio.");

        // builds correlation map
        Map<String, Double> correlationMap = new LinkedHashMap<>();
        for (SensitivityResult r : results) {
            correlationMap.put(r.getVariable(), r.getCorrelation());
        }

        // strongest impact variable
        String topVariable    = results.get(0).getVariable();
        double topCorrelation = results.get(0).getCorrelation();

        // add columns on header
        String[] originalHeader = rows.get(0);
        String[] newHeader = Arrays.copyOf(originalHeader, originalHeader.length + 2);
        newHeader[originalHeader.length]     = "variavel_maior_impacto";
        newHeader[originalHeader.length + 1] = "correlacao";
        rows.set(0, newHeader);

        // add value on lines
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            String[] newRow = Arrays.copyOf(row, row.length + 2);
            newRow[row.length]     = topVariable;
            newRow[row.length + 1] = String.format("%.3f", topCorrelation);
            rows.set(i, newRow);
        }
        File outFile = new File(outputFile);
        // output
        if (outFile.getParentFile() != null) {
            outFile.getParentFile().mkdirs();
        }

        try (CSVWriter writer = new CSVWriter(new FileWriter(outFile))) {
            writer.writeAll(rows);
        }
    }

    private InputStream resolveInputStream(String path) throws IOException {
        File file = new File(path);

        // absolute path — real file on system
        if (file.isAbsolute() && file.exists()) {
            return new FileInputStream(file);
        }

        // classpath
        Resource resource = new ClassPathResource(
                path.replace("classpath:", "").trim());

        if (!resource.exists())
            throw new FileNotFoundException("File Not Found: " + path);

        return resource.getInputStream();
    }
}