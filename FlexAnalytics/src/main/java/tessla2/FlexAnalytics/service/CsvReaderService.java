package tessla2.FlexAnalytics.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import tessla2.FlexAnalytics.model.DataSet;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvReaderService {

    public DataSet load(String filePath) throws IOException, CsvException {

        // Remove prefixo "classpath:" se vier do yml
        String cleanPath = filePath.replace("classpath:", "").trim();

        Resource resource = new ClassPathResource(cleanPath);

        if (!resource.exists())
            throw new FileNotFoundException("Arquivo não encontrado no classpath: " + cleanPath);

        try (CSVReader reader = new CSVReader(new InputStreamReader(resource.getInputStream()))) {
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
                    throw new IllegalArgumentException("Linha " + i + " com número incorreto de colunas.");

                double[] x = new double[numVars];
                for (int j = 0; j < numVars; j++) {
                    x[j] = parseCell(row[j], i, headers[j]);
                }
                inputs.add(x);
                output[i - 1] = parseCell(row[numVars], i, headers[numVars]);
            }

            return new DataSet(headers, inputs, output, numVars);
        }
    }

    private double parseCell(String value, int line, String column) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Valor inválido na coluna '" + column + "', linha " + line + ": '" + value + "'");
        }
    }
}