package tessla2.FlexAnalytics.application.mapper;

import org.springframework.stereotype.Component;
import tessla2.FlexAnalytics.application.dto.ResultDTO;
import tessla2.FlexAnalytics.domain.model.SensitivityResult;

@Component
public class SensitivityMapper {

    public ResultDTO toDto(SensitivityResult result) {
        return new ResultDTO(result.getVariable(), result.getCorrelation(), result.getAbsoluteImpact());
    }
}
