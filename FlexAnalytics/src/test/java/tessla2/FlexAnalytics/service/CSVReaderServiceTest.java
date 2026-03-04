package tessla2.FlexAnalytics.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tessla2.FlexAnalytics.exception.FileNotFoundException;
import tessla2.FlexAnalytics.model.DataSet;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CSVReaderServiceTest {

    @InjectMocks
    private CsvReaderService csvReaderService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(csvReaderService, "maxFileSizeMb", 10);
    }

    @Test
    void shouldReadCsvCorrectly() throws Exception {
        DataSet dataSet = csvReaderService.load("test_data.csv");

        assertThat(dataSet).isNotNull();
        assertThat(dataSet.getRowCount()).isGreaterThan(0);
        assertThat(dataSet.getNumVars()).isGreaterThan(0);
        System.out.println("Funfou");
    }

    @Test
    void shouldThrowExceptionForAbsentFile() {
        assertThrows(FileNotFoundException.class,
        () -> csvReaderService.load("Absent CSV."));
    }

    @Test
    void shouldThrowExceptionForInvalidExtension() {
        assertThrows(IllegalArgumentException.class,
        () -> csvReaderService.load("Invalid document type."));
    }
}
