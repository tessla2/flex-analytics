package tessla2.FlexAnalytics.domain.service;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.springframework.stereotype.Service;
import tessla2.FlexAnalytics.domain.model.CorrelationMethod;
import tessla2.FlexAnalytics.domain.model.DataSet;
import tessla2.FlexAnalytics.domain.model.SensitivityResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SensitivityService {

    private final PearsonsCorrelation pearson = new PearsonsCorrelation();
    private final SpearmansCorrelation spearman = new SpearmansCorrelation();

    public List<SensitivityResult> analyze(DataSet dataSet) {
        return analyze(dataSet, CorrelationMethod.PEARSON);
    }

    public List<SensitivityResult> analyze(DataSet dataSet, CorrelationMethod method) {
        List<SensitivityResult> results = new ArrayList<>();

        for (int v = 0; v < dataSet.numVars(); v++) {
            double[] col = dataSet.extractColumn(v);
            double correlation = correlation(col, dataSet.output(), method);
            results.add(new SensitivityResult(dataSet.headers()[v], correlation));
        }

        results.sort(Comparator.comparingDouble(SensitivityResult::getAbsoluteImpact).reversed());
        return results;
    }

    private double correlation(double[] x, double[] y, CorrelationMethod method) {
        return switch (method) {
            case SPEARMAN -> spearman.correlation(x, y);
            case PEARSON -> pearson.correlation(x, y);
        };
    }
}
