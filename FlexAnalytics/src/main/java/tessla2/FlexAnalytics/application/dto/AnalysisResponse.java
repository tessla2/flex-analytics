package tessla2.FlexAnalytics.application.dto;

import java.util.List;

public record AnalysisResponse(String inputFile, String outputFile, int rowCount, int variableCount,
                               List<ResultDTO> results) {
}
