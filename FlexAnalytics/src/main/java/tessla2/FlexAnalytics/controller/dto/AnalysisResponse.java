package tessla2.FlexAnalytics.controller.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {
    private String fileName;
    private int totalRows;
    private int totalVariables;
    private String outputVariable;
    private List<ResultDTO> results;
}
