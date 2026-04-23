package tessla2.FlexAnalytics.domain.service;

import com.opencsv.CSVReader;
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
import java.util.List;

@Service
public class CsvReaderService {

    public DataSet load(String filePath) throws IOException, CsvException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("filePath must be provided");
        }

        String trimmed = filePath.trim();

        if (trimmed.startsWith("classpath:")) {
            String resourcePath = trimmed.substring("classpath:".length()).trim();
            return loadFromClassPath(resourcePath);
        }

        Resource cp = new ClassPathResource(trimmed);
        if (cp.exists()) {
            return loadFromReader(new InputStreamReader(cp.getInputStream()));
        }

        Path fsPath = Path.of(trimmed);
        if (Files.exists(fsPath)) {
            try (Reader fr = new FileReader(fsPath.toFile())) {
                return loadFromReader(fr);
            }
        }

        throw new FileNotFoundException("File not found as classpath resource or filesystem path: " + filePath);
    }

    private DataSet loadFromClassPath(String resourcePath) throws IOException, CsvException {
        Resource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            throw new FileNotFoundException("Arquivo não encontrado no classpath: " + resourcePath);
        }
        try (InputStreamReader isr = new InputStreamReader(resource.getInputStream())) {
            return loadFromReader(isr);
        }
    }

    private DataSet loadFromReader(Reader reader) throws IOException, CsvException {
        try (CSVReader csv = new CSVReader(reader)) {
            List<String[]> rows = csv.readAll();

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
                    x[j] = parseCell(row[j], i + 1, headers[j]);
                }
                inputs.add(x);
                output[i - 1] = parseCell(row[numVars], i + 1, headers[numVars]);
            }

            return new DataSet(headers, inputs, output, numVars);
        }
    }

    private double parseCell(String value, int line, String column) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Valor vazio ou nulo na coluna '" + column + "', linha " + line);
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Valor inválido na coluna '" + column + "', linha " + line + ": '" + value + "'");
        }
    }
}
