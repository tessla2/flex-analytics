package tessla2.FlexAnalytics.domain.service;

import org.junit.jupiter.api.Test;
import tessla2.FlexAnalytics.domain.model.CorrelationMethod;
import tessla2.FlexAnalytics.domain.model.DataSet;
import tessla2.FlexAnalytics.domain.model.SensitivityResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitivityServiceTest {

    private final SensitivityService service = new SensitivityService();

    @Test
    void shouldSortByAbsoluteImpactDesc() {
        DataSet dataSet = sampleData();

        List<SensitivityResult> results = service.analyze(dataSet, CorrelationMethod.PEARSON);

        assertEquals(2, results.size());
        assertTrue(results.get(0).getAbsoluteImpact() >= results.get(1).getAbsoluteImpact());
    }

    @Test
    void shouldSupportSpearmanAndPearson() {
        DataSet dataSet = sampleData();

        List<SensitivityResult> pearson = service.analyze(dataSet, CorrelationMethod.PEARSON);
        List<SensitivityResult> spearman = service.analyze(dataSet, CorrelationMethod.SPEARMAN);

        assertEquals(2, pearson.size());
        assertEquals(2, spearman.size());
    }

    private DataSet sampleData() {
        String[] headers = {"x1", "x2", "y"};
        List<double[]> inputs = List.of(
                new double[]{1, 5},
                new double[]{2, 4},
                new double[]{3, 3},
                new double[]{4, 2},
                new double[]{5, 1}
        );
        double[] output = {2, 4, 6, 8, 10};
        return new DataSet(headers, inputs, output, 2);
    }
}
