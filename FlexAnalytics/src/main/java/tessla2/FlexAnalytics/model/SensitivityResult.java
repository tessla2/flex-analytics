package tessla2.FlexAnalytics.model;


import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class SensitivityResult {
    private final String variable;
    private final double correlation;

    public SensitivityResult(String variable, double correlation) {
        this.variable = variable;
        this.correlation = correlation;
    }
    public double getAbsoluteImpact()
    {return Math.abs(correlation);}
}
