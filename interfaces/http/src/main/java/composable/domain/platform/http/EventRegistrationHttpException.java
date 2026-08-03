package composable.domain.platform.http;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.http.generated.model.ErrorResponse;
import org.springframework.http.HttpStatus;

final class EventRegistrationHttpException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorResponse.CodeEnum code;
    private final ExecutionContext context;

    private EventRegistrationHttpException(
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

    static EventRegistrationHttpException invalidDefinition(ExecutionContext context) {
        return new EventRegistrationHttpException(
                HttpStatus.BAD_REQUEST,
                ErrorResponse.CodeEnum.INVALID_REQUEST,
                "Event registration definition is invalid",
                context,
                null);
    }

    static EventRegistrationHttpException eventNotFound(ExecutionContext context) {
        return new EventRegistrationHttpException(
                HttpStatus.NOT_FOUND,
                ErrorResponse.CodeEnum.EVENT_NOT_FOUND,
                "Referenced Event was not found",
                context,
                null);
    }

    static EventRegistrationHttpException registrationNotFound(ExecutionContext context) {
        return new EventRegistrationHttpException(
                HttpStatus.NOT_FOUND,
                ErrorResponse.CodeEnum.EVENT_REGISTRATION_NOT_FOUND,
                "Event registration was not found",
                context,
                null);
    }

    static EventRegistrationHttpException conflict(ExecutionContext context) {
        return new EventRegistrationHttpException(
                HttpStatus.CONFLICT,
                ErrorResponse.CodeEnum.REGISTRATION_CONFLICT,
                "Registration uniqueness conflict",
                context,
                null);
    }

    static EventRegistrationHttpException internal(
            ExecutionContext context,
            RuntimeException cause) {
        return new EventRegistrationHttpException(
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
