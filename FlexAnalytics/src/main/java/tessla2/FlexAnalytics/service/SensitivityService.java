package tessla2.FlexAnalytics.service;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.springframework.stereotype.Service;
import tessla2.FlexAnalytics.model.DataSet;
import tessla2.FlexAnalytics.model.SensitivityResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SensitivityService {

    // Parte de apache commons math
    private final PearsonsCorrelation pearson = new PearsonsCorrelation();

    public List<SensitivityResult> analyze(DataSet dataSet) {
        List<SensitivityResult> results = new ArrayList<>();


        // For each input variable, calculate the correlation with the output and store the result.
        for (int v = 0; v < dataSet.getNumVars(); v++) {
            double[] col = dataSet.extractColumn(v);
            double correlation = pearson.correlation(col, dataSet.getOutput()); // Calcula a correlação entre a variável de entrada e a saída
            results.add(new SensitivityResult(dataSet.getHeaders()[v], correlation));
        }

        results.sort(Comparator.comparingDouble(SensitivityResult::getAbsoluteImpact).reversed());
        return results;
    }
}
