package tessla2.FlexAnalytics.service;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.inference.OneWayAnova;
import org.springframework.stereotype.Service;
import tessla2.FlexAnalytics.model.DataSet;
import tessla2.FlexAnalytics.model.SensitivityResult;

import java.util.*;

@Service
public class SensitivityService {

    // Parte de apache commons math
    private final PearsonsCorrelation pearson = new PearsonsCorrelation();
    private final OneWayAnova anova = new OneWayAnova();

    public List<SensitivityResult> analyze(DataSet dataSet) {
        List<SensitivityResult> results = new ArrayList<>();

        // For each input variable, calculate the correlation with the output and store the result.

        for (int v = 0; v < dataSet.getNumVars(); v++) {
            double[] col = dataSet.extractColumn(v);
            String varName = dataSet.getHeaders()[v];
            SensitivityResult result;

            if (dataSet.isCategorical(v)) {
                result = analyzeWithAnova(varName, col, dataSet.getOutput());
            } else {
                result = analyzeWithPearson(varName, col, dataSet.getOutput());
            }

            results.add(result);
        }

        results.sort(Comparator.comparingDouble(SensitivityResult::getAbsoluteImpact).reversed());
        return results;

    }

    // -------------------------------------------------------------------------
    // Pearson — numeric variables
    // -------------------------------------------------------------------------

    private SensitivityResult analyzeWithPearson(String variable, double[] col, double[] output) {
        double correlation = pearson.correlation(col, output);
        // NaN can occur if column has zero variance (should be filtered upstream, but guard here)
        if (Double.isNaN(correlation)) correlation = 0.0;
        return new SensitivityResult(variable, correlation, "Pearson");
    }

    // -------------------------------------------------------------------------
    // ANOVA — categorical variables (label-encoded)
    // -------------------------------------------------------------------------

    private SensitivityResult analyzeWithAnova(String variable, double[] col, double[] output) {
        // Group output values by category
        Map<Double, List<Double>> groups = new LinkedHashMap<>();
        for (int i = 0; i < col.length; i++) {
            groups.computeIfAbsent(col[i], k -> new ArrayList<>()).add(output[i]);
        }

        // Need at least 2 groups with at least 2 observations each for ANOVA
        List<double[]> groupArrays = groups.values().stream()
                .filter(g -> g.size() >= 2)
                .map(g -> g.stream().mapToDouble(Double::doubleValue).toArray())
                .toList();

        if (groupArrays.size() < 2) {
            return new SensitivityResult(variable, 0.0, "ANOVA");
        }

        // Eta-squared = SS_between / SS_total (effect size, range [0, 1])
        double etaSquared = computeEtaSquared(groupArrays, output);

        // Use positive eta-squared as "correlation" for display purposes
        return new SensitivityResult(variable, etaSquared, "ANOVA");
    }

    private double computeEtaSquared(List<double[]> groups, double[] allValues) {
        double grandMean = Arrays.stream(allValues).average().orElse(0.0);

        double ssBetween = 0.0;
        for (double[] group : groups) {
            double groupMean = Arrays.stream(group).average().orElse(0.0);
            ssBetween += group.length * Math.pow(groupMean - grandMean, 2);
        }

        double ssTotal = Arrays.stream(allValues)
                .map(v -> Math.pow(v - grandMean, 2))
                .sum();

        if (ssTotal == 0.0) return 0.0;
        return ssBetween / ssTotal;
    }
}

