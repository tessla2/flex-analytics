package tessla2.FlexAnalytics.model;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SensitivityResult {

    private String variable;
    private double correlation;

    public double getAbsoluteImpact() {
        return Math.abs(correlation);
    }
}
