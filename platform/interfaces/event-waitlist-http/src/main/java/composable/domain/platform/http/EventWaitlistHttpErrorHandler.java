package composable.domain.platform.http;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.http.eventwaitlist.generated.model.EventWaitlistErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class EventWaitlistHttpErrorHandler {

    @ExceptionHandler(EventWaitlistHttpException.class)
    public ResponseEntity<EventWaitlistErrorResponse> handleEventWaitlistFailure(
            EventWaitlistHttpException exception) {
        return response(
                exception.status(),
                exception.code(),
                exception.getMessage(),
                exception.context());
    }

    private static ResponseEntity<EventWaitlistErrorResponse> response(
            HttpStatus status,
            EventWaitlistErrorResponse.CodeEnum code,
            String message,
            ExecutionContext context) {
        return ResponseEntity.status(status)
                .header(
                        EventWaitlistHttpCorrelation.HEADER_NAME,
                        EventWaitlistHttpCorrelation.value(context))
                .body(new EventWaitlistErrorResponse(code, message));
    }
}
