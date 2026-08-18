package composable.domain.platform.http;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.http.eventregistration.generated.model.EventRegistrationErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class EventRegistrationHttpErrorHandler {

    @ExceptionHandler(EventRegistrationHttpException.class)
    public ResponseEntity<EventRegistrationErrorResponse> handleEventRegistrationFailure(
            EventRegistrationHttpException exception) {
        return response(
                exception.status(),
                exception.code(),
                exception.getMessage(),
                exception.context());
    }

    private static ResponseEntity<EventRegistrationErrorResponse> response(
            HttpStatus status,
            EventRegistrationErrorResponse.CodeEnum code,
            String message,
            ExecutionContext context) {
        return ResponseEntity.status(status)
                .header(EventRegistrationHttpCorrelation.HEADER_NAME, EventRegistrationHttpCorrelation.value(context))
                .body(new EventRegistrationErrorResponse(code, message));
    }
}
