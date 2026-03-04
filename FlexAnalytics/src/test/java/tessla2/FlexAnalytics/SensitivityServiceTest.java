package tessla2.FlexAnalytics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import tessla2.FlexAnalytics.model.DataSet;
import tessla2.FlexAnalytics.model.SensitivityResult;
import tessla2.FlexAnalytics.service.SensitivityService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class SensitivityServiceTest {

    @InjectMocks
    private SensitivityService sensitivityService;

    @Test
    void shouldReturnResultsByImpact() {

        String[] headers = {"temperature" , "pressure", "production"};
        List<double[]> inputs = List.of(
                new double[]{72.5, 101.3},
                new double[]{68.0, 99.8},
                new double[]{75.3, 103.1},
                new double[]{65.2, 98.5},
                new double[]{80.1, 105.7}
        );
        double[] output = {502, 498, 561, 470, 610};

        DataSet dataSet = new DataSet(headers, 2, inputs, output);

        List<SensitivityResult> results = sensitivityService.analyze(dataSet);


        // verifies absolut impact ordering
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getAbsoluteImpact())
        .isGreaterThanOrEqualTo(results.get(1).getAbsoluteImpact());
    }

    @Test
    void shouldThrowExceptionWithEmptyDataSet() {
        DataSet dataSet = new DataSet(
                new String[]{"temperature", "production"},
                1,
                Collections.emptyList(),
                new double[]{}
        );
        assertThrows(Exception.class, () -> sensitivityService.analyze(dataSet));
    }
}
