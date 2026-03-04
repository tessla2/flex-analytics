package tessla2.FlexAnalytics.controller.dto;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;


public record ErrorResponse (int status, String message, LocalDateTime timestamp) {

    public static ErrorResponse standardError(String message) {
        return new ErrorResponse
                (HttpStatus.BAD_REQUEST.value(),
                message,
                LocalDateTime.now());
    }

    public static ErrorResponse conflictError(String message) {
        return new ErrorResponse
                (HttpStatus.CONFLICT.value(),
                        message,
                        LocalDateTime.now()
                );
    }

}
