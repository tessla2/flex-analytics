package tessla2.FlexAnalytics.domain.model;

import java.util.Map;

public record ScenarioSummary(int scenarioId, double totalThroughput, Map<String, Double> throughputByObject) {
}
