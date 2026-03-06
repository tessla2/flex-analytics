package tessla2.FlexAnalytics.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@Schema(description = "Standardized error response")
public class ErrorResponse {

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Descriptive error message", example = "Only .csv files are allowed.")
    private String message;

    //@Schema(description = "Timestamp when the error occurred", example = "2026-03-03T17:00:00")
    //private LocalDateTime timestamp;

    public static ErrorResponse standardError(String message) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message);
    }

    public static ErrorResponse conflictError(String message) {
        return new ErrorResponse(HttpStatus.CONFLICT.value(), message);
    }
}
