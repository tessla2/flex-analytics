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

        // suporta tanto classpath quanto caminho absoluto
        try (InputStream is = resolveInputStream(inputFile);
             CSVReader reader = new CSVReader(new InputStreamReader(is))) {
            rows = reader.readAll();
        }

        if (rows == null || rows.isEmpty())
            throw new IllegalArgumentException("CSV de entrada vazio.");

        // monta mapa de correlação
        Map<String, Double> correlationMap = new LinkedHashMap<>();
        for (SensitivityResult r : results) {
            correlationMap.put(r.getVariable(), r.getCorrelation());
        }

        // variável de maior impacto
        String topVariable    = results.get(0).getVariable();
        double topCorrelation = results.get(0).getCorrelation();

        // adiciona colunas no header
        String[] originalHeader = rows.get(0);
        String[] newHeader = Arrays.copyOf(originalHeader, originalHeader.length + 2);
        newHeader[originalHeader.length]     = "variavel_maior_impacto";
        newHeader[originalHeader.length + 1] = "correlacao";
        rows.set(0, newHeader);

        // adiciona valores nas linhas
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            String[] newRow = Arrays.copyOf(row, row.length + 2);
            newRow[row.length]     = topVariable;
            newRow[row.length + 1] = String.format("%.3f", topCorrelation);
            rows.set(i, newRow);
        }
        File outFile = new File(outputFile);
        // escreve output
        if (outFile.getParentFile() != null) {
            outFile.getParentFile().mkdirs();
        }

        try (CSVWriter writer = new CSVWriter(new FileWriter(outFile))) {
            writer.writeAll(rows);
        }
    }

    private InputStream resolveInputStream(String path) throws IOException {
        File file = new File(path);

        // caminho absoluto — arquivo real no sistema
        if (file.isAbsolute() && file.exists()) {
            return new FileInputStream(file);
        }

        // classpath — recurso interno do projeto
        Resource resource = new ClassPathResource(
                path.replace("classpath:", "").trim());

        if (!resource.exists())
            throw new FileNotFoundException("Arquivo não encontrado: " + path);

        return resource.getInputStream();
    }
}