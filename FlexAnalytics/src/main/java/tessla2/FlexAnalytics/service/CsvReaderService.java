package tessla2.FlexAnalytics.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tessla2.FlexAnalytics.model.DataSet;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for loading a CSV and converting rows into a DataSet.
 *
 * Behavior notes:
 * - Supports classpath resources when the path starts with "classpath:" or when a
 *   resource with the given relative path exists on the classpath.
 * - Falls back to reading a filesystem path if a classpath resource is not found.
 * - Validates header row and enforces consistent column counts.
 * - Converts values to doubles and throws an informative IllegalArgumentException on parse errors.
 */
@Service
public class CsvReaderService {

    @Value("${app.max-file-size-mb:10}")
    private int maxFileSizeMb;

    public DataSet load(String filePath) throws IOException, CsvException {
        String cleanPath = filePath.replace("classpath:", "").trim();

        try (InputStream is = resolveInputStream(cleanPath);
             CSVReader reader = new CSVReader(new InputStreamReader(is))) {

            List<String[]> rows = reader.readAll();

            if (rows == null || rows.size() < 2)
                throw new IllegalArgumentException("CSV vazio ou sem dados suficientes.");

            String[] headers = rows.get(0);
            int numVars = headers.length - 1;
            List<double[]> inputs = new ArrayList<>();
            double[] output = new double[rows.size() - 1];

            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length != headers.length)
                    throw new IllegalArgumentException(
                            "Linha " + i + " com número incorreto de colunas.");

                double[] x = new double[numVars];
                for (int j = 0; j < numVars; j++) {
                    x[j] = parseCell(row[j], i, headers[j]);
                }
                inputs.add(x);
                output[i - 1] = parseCell(row[numVars], i, headers[numVars]);
            }

            return new DataSet(headers, numVars, inputs, output);
        }
    }

    private InputStream resolveInputStream(String path) throws IOException {
        File file = new File(path);

        // caminho absoluto — temp file ou arquivo real no sistema
        if (file.isAbsolute() && file.exists()) {
            validateFile(file);
            return new FileInputStream(file);
        }

        // classpath — recurso interno do projeto
        Resource resource = new ClassPathResource(path);

        if (!resource.exists())
            throw new FileNotFoundException("File not found: " + path);

        validateFile(resource.getFile());
        return resource.getInputStream();
    }

    private void validateFile(File file) {
        if (!file.getName().endsWith(".csv"))
            throw new IllegalArgumentException("Apenas arquivos .csv são permitidos.");

        if (file.length() > (long) maxFileSizeMb * 1024 * 1024)
            throw new IllegalArgumentException(
                    "Arquivo excede o tamanho máximo de " + maxFileSizeMb + "MB.");
    }

    private double parseCell(String value, int line, String column) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Valor inválido na coluna '" + column + "', linha " + line
                            + ": '" + value + "'");
        }
    }
}