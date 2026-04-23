package tessla2.FlexAnalytics.application.dto;

import java.util.Map;

public record ScenarioSummaryDTO(int scenarioId, double totalThroughput, Map<String, Double> throughputByObject) {
}
