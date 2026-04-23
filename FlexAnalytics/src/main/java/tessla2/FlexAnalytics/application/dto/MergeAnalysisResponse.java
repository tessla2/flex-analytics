package tessla2.FlexAnalytics.application.dto;

import java.util.List;

public record MergeAnalysisResponse(List<String> inputFiles, String outputFile,
                                    String scenarioColumn, String objectColumn, String valueColumn,
                                    int scenarioCount,
                                    List<ScenarioSummaryDTO> scenarios) {
}
