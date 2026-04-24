package tessla2.FlexAnalytics.domain.model;

public class SensitivityResult {
    private final String variable;
    private final double correlation;

    public SensitivityResult(String variable, double correlation) {
        this.variable = variable;
        this.correlation = correlation;
    }

    public String getVariable() {
        return variable;
    }

    public double getCorrelation() {
        return correlation;
    }

    public double getAbsoluteImpact() {
        return Math.abs(correlation);
    }
}
