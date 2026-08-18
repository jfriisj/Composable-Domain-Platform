package composable.domain.platform.http;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.http.event.generated.model.ErrorResponse;
import org.springframework.http.HttpStatus;

final class EventHttpException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorResponse.CodeEnum code;
    private final ExecutionContext context;

    private EventHttpException(
            HttpStatus status,
            ErrorResponse.CodeEnum code,
            String message,
            ExecutionContext context,
            Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
        this.context = context;
    }

    static EventHttpException invalidRequest(ExecutionContext context) {
        return new EventHttpException(
                HttpStatus.BAD_REQUEST,
                ErrorResponse.CodeEnum.INVALID_REQUEST,
                "Request is invalid",
                context,
                null);
    }

    static EventHttpException invalidDefinition(ExecutionContext context) {
        return new EventHttpException(
                HttpStatus.BAD_REQUEST,
                ErrorResponse.CodeEnum.INVALID_REQUEST,
                "Event definition is invalid",
                context,
                null);
    }

    static EventHttpException alreadyDefined(ExecutionContext context) {
        return new EventHttpException(
                HttpStatus.CONFLICT,
                ErrorResponse.CodeEnum.EVENT_ALREADY_DEFINED,
                "Event is already defined",
                context,
                null);
    }

    static EventHttpException alreadyPublished(ExecutionContext context) {
        return new EventHttpException(
                HttpStatus.CONFLICT,
                ErrorResponse.CodeEnum.EVENT_ALREADY_PUBLISHED,
                "Event is already published",
                context,
                null);
    }

    static EventHttpException notFound(ExecutionContext context) {
        return new EventHttpException(
                HttpStatus.NOT_FOUND,
                ErrorResponse.CodeEnum.EVENT_NOT_FOUND,
                "Event was not found",
                context,
                null);
    }

    static EventHttpException internal(ExecutionContext context, RuntimeException cause) {
        return new EventHttpException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorResponse.CodeEnum.INTERNAL_ERROR,
                "Internal server error",
                context,
                cause);
    }

    HttpStatus status() {
        return status;
    }

    ErrorResponse.CodeEnum code() {
        return code;
    }

    ExecutionContext context() {
        return context;
    }
}
