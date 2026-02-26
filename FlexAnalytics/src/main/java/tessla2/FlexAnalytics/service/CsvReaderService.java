package tessla2.FlexAnalytics.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tessla2.FlexAnalytics.model.DataSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
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

    /**
     * Load CSV from the given path.
     *
     * @param filePath either:
     *                 - "classpath:some/path.csv" to read from classpath resources, or
     *                 - an absolute or relative filesystem path.
     * @return DataSet constructed from CSV
     * @throws IOException  if IO errors occur
     * @throws CsvException if CSV parsing via OpenCSV fails
     */
    public DataSet load(String filePath) throws IOException, CsvException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("filePath must be provided");
        }

        String trimmed = filePath.trim();

        // If explicitly prefixed with "classpath:" use classpath lookup
        if (trimmed.startsWith("classpath:")) {
            String resourcePath = trimmed.substring("classpath:".length()).trim();
            return loadFromClassPath(resourcePath);
        }

        // First try classpath (convenience if callers passed a relative resource path)
        Resource cp = new ClassPathResource(trimmed);
        if (cp.exists()) {
            return loadFromReader(new InputStreamReader(cp.getInputStream()));
        }

        // Fallback to filesystem path
        Path fsPath = Path.of(trimmed);
        if (Files.exists(fsPath)) {
            try (Reader fr = new FileReader(fsPath.toFile())) {
                return loadFromReader(fr);
            }
        }

        // File not found in either place -> clear error for caller
        throw new FileNotFoundException("File not found as classpath resource or filesystem path: " + filePath);
    }

    // Helper: load from a classpath resource
    private DataSet loadFromClassPath(String resourcePath) throws IOException, CsvException {
        Resource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            throw new FileNotFoundException("Arquivo não encontrado no classpath: " + resourcePath);
        }
        try (InputStreamReader isr = new InputStreamReader(resource.getInputStream())) {
            return loadFromReader(isr);
        }
    }

    // Core CSV parsing logic extracted for reuse
    private DataSet loadFromReader(Reader reader) throws IOException, CsvException {
        try (CSVReader csv = new CSVReader(reader)) {
            List<String[]> rows = csv.readAll();

            // Basic validation
            if (rows == null || rows.size() < 2) {
                throw new IllegalArgumentException("CSV vazio ou sem dados suficientes.");
            }

            String[] headers = rows.get(0);
            int numVars = headers.length - 1; // last column is treated as output
            if (numVars < 1) {
                throw new IllegalArgumentException("CSV must contain at least one input column and one output column.");
            }

            List<double[]> inputs = new ArrayList<>();
            double[] output = new double[rows.size() - 1];

            // Iterate data rows (i=1 corresponds to the first data row; report human-friendly line numbers)
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length != headers.length) {
                    // report line number as i+1 since CSV lines are 1-based and header is line 1
                    throw new IllegalArgumentException("Linha " + (i + 1) + " com número incorreto de colunas.");
                }

                double[] x = new double[numVars];
                for (int j = 0; j < numVars; j++) {
                    x[j] = parseCell(row[j], i + 1, headers[j]); // pass human-readable line number
                }
                inputs.add(x);
                output[i - 1] = parseCell(row[numVars], i + 1, headers[numVars]);
            }

            return new DataSet(headers, inputs, output, numVars);
        }
    }

    /**
     * Parse a single CSV cell into a double.
     *
     * The method trims whitespace and throws an IllegalArgumentException with
     * a clear message including column name and line number on parse failure.
     */
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