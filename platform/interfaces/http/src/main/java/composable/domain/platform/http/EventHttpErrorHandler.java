package composable.domain.platform.http;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.http.event.generated.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public final class EventHttpErrorHandler {

    @ExceptionHandler(EventHttpException.class)
    public ResponseEntity<ErrorResponse> handleEventFailure(EventHttpException exception) {
        return response(
                exception.status(),
                exception.code(),
                exception.getMessage(),
                exception.context());
    }

    @ExceptionHandler({
        ConstraintViolationException.class,
        HandlerMethodValidationException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentNotValidException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequest(
            Exception exception,
            HttpServletRequest request) {
        ExecutionContext context = HttpCorrelation.establish(
                request.getHeader(HttpCorrelation.HEADER_NAME));

        return response(
                HttpStatus.BAD_REQUEST,
                ErrorResponse.CodeEnum.INVALID_REQUEST,
                "Request is invalid",
                context);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {
        ExecutionContext context = HttpCorrelation.establish(
                request.getHeader(HttpCorrelation.HEADER_NAME));

        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorResponse.CodeEnum.INTERNAL_ERROR,
                "Internal server error",
                context);
    }

    private static ResponseEntity<ErrorResponse> response(
            HttpStatus status,
            ErrorResponse.CodeEnum code,
            String message,
            ExecutionContext context) {
        return ResponseEntity.status(status)
                .header(HttpCorrelation.HEADER_NAME, HttpCorrelation.value(context))
                .body(new ErrorResponse(code, message));
    }
}
