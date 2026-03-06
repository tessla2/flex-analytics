package tessla2.FlexAnalytics.model;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SensitivityResult {

    private String variable;
    private double correlation;
    private String analysisType;

    public double getAbsoluteImpact() {
        return Math.abs(correlation);
    }
}
