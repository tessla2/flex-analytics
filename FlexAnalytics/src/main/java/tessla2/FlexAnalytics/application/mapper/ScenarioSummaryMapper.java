package tessla2.FlexAnalytics.application.mapper;

import org.springframework.stereotype.Component;
import tessla2.FlexAnalytics.application.dto.ScenarioSummaryDTO;
import tessla2.FlexAnalytics.domain.model.ScenarioSummary;

@Component
public class ScenarioSummaryMapper {

    public ScenarioSummaryDTO toDto(ScenarioSummary summary) {
        return new ScenarioSummaryDTO(summary.scenarioId(), summary.totalThroughput(), summary.throughputByObject());
    }
}
