package tessla2.FlexAnalytics.controller.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultDTO {

    private String variable;
    private double correlation;
    private double absoluteImpact;
}



