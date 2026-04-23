package tessla2.FlexAnalytics.domain.service;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.springframework.stereotype.Service;
import tessla2.FlexAnalytics.domain.model.DataSet;
import tessla2.FlexAnalytics.domain.model.SensitivityResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SensitivityService {

    private final PearsonsCorrelation pearson = new PearsonsCorrelation();

    public List<SensitivityResult> analyze(DataSet dataSet) {
        List<SensitivityResult> results = new ArrayList<>();

        for (int v = 0; v < dataSet.numVars(); v++) {
            double[] col = dataSet.extractColumn(v);
            double correlation = pearson.correlation(col, dataSet.output());
            results.add(new SensitivityResult(dataSet.headers()[v], correlation));
        }

        results.sort(Comparator.comparingDouble(SensitivityResult::getAbsoluteImpact).reversed());
        return results;
    }
}
