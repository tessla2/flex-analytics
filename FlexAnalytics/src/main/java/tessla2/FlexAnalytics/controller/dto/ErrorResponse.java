package tessla2.FlexAnalytics.controller.dto;

import org.springframework.http.HttpStatus;

public record ErrorResponse (int status, String message) {

    public static ErrorResponse standardError(String message) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message);
    }

    public static ErrorResponse conflictError(String message) {
        return new ErrorResponse(HttpStatus.CONFLICT.value(), message);
    }

}
