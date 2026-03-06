package tessla2.FlexAnalytics.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Correlation result for a single input variable")
public class ResultDTO {

    @Schema(description = "Input variable name", example = "operators")
    private String variable;

    @Schema(
            description = "Pearson correlation with the output variable. Positive = direct relationship, Negative = inverse relationship",
            example = "0.996",
            minimum = "-1",
            maximum = "1"
    )
    private double correlation;

    @Schema(
            description = "Absolute impact — correlation magnitude regardless of sign",
            example = "0.996",
            minimum = "0",
            maximum = "1"
    )
    private double absoluteImpact;

    @Schema(
            description = "Statistical method used for this variable",
            example = "Pearson",
            allowableValues = {"Pearson", "ANOVA"}
    )
    private String analysisType;
}