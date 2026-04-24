package tessla2.FlexAnalytics.application.dto;

import java.time.Instant;

public record ErrorResponse(String message, String details, Instant timestamp) {
}
