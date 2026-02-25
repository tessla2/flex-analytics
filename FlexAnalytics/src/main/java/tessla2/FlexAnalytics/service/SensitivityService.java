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


    // Pearson para medir a correlação entre cada variável de entrada e a saída
    // Parte de apache commons math
    private final PearsonsCorrelation pearson = new PearsonsCorrelation();

    public List<SensitivityResult> analyze(DataSet dataSet) {
        List<SensitivityResult> results = new ArrayList<>();


        // Para cada variável de entrada, calcula a correlação com a saída e armazena o resultado
        for (int v = 0; v < dataSet.numVars(); v++) {
            double[] col = dataSet.extractColumn(v);
            double correlation = pearson.correlation(col, dataSet.output()); // Calcula a correlação entre a variável de entrada e a saída
            results.add(new SensitivityResult(dataSet.headers()[v], correlation));
        }

        results.sort(Comparator.comparingDouble(SensitivityResult::getAbsoluteImpact));
        return results;
    }
}
