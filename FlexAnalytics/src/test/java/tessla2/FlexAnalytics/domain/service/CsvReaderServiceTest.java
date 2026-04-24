package tessla2.FlexAnalytics.domain.service;

import com.opencsv.exceptions.CsvException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tessla2.FlexAnalytics.domain.model.DataSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvReaderServiceTest {

    private final CsvReaderService service = new CsvReaderService();

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadValidCsvFromFilesystem() throws IOException, CsvException {
        Path csv = tempDir.resolve("valid.csv");
        Files.writeString(csv, "x1,x2,y\n1,2,3\n2,3,4\n");

        DataSet dataSet = service.load(csv.toString());

        assertEquals(2, dataSet.getRowCount());
        assertEquals(2, dataSet.getNumVars());
    }

    @Test
    void shouldFailWhenColumnsAreInconsistent() throws IOException {
        Path csv = tempDir.resolve("invalid.csv");
        Files.writeString(csv, "x1,x2,y\n1,2,3\n2,3\n");

        assertThrows(IllegalArgumentException.class, () -> service.load(csv.toString()));
    }
}
