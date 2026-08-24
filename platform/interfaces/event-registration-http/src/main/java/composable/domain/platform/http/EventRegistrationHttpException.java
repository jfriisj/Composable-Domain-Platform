package composable.domain.platform.http;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.http.eventregistration.generated.model.EventRegistrationErrorResponse;
import org.springframework.http.HttpStatus;

final class EventRegistrationHttpException extends RuntimeException {

    private final HttpStatus status;
    private final EventRegistrationErrorResponse.CodeEnum code;
    private final ExecutionContext context;

    private EventRegistrationHttpException(
            HttpStatus status,
            EventRegistrationErrorResponse.CodeEnum code,
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
                EventRegistrationErrorResponse.CodeEnum.INVALID_REQUEST,
                "Event registration definition is invalid",
                context,
                null);
    }

    static EventRegistrationHttpException invalidRequest(
            ExecutionContext context,
            String message) {
        return new EventRegistrationHttpException(
                HttpStatus.BAD_REQUEST,
                EventRegistrationErrorResponse.CodeEnum.INVALID_REQUEST,
                message,
                context,
                null);
    }

    static EventRegistrationHttpException eventNotFound(ExecutionContext context) {
        return new EventRegistrationHttpException(
                HttpStatus.NOT_FOUND,
                EventRegistrationErrorResponse.CodeEnum.EVENT_NOT_FOUND,
                "Referenced Event was not found",
                context,
                null);
    }

    static EventRegistrationHttpException registrationNotFound(ExecutionContext context) {
        return new EventRegistrationHttpException(
                HttpStatus.NOT_FOUND,
                EventRegistrationErrorResponse.CodeEnum.EVENT_REGISTRATION_NOT_FOUND,
                "Event registration was not found",
                context,
                null);
    }

    static EventRegistrationHttpException forbidden(ExecutionContext context) {
        return new EventRegistrationHttpException(
                HttpStatus.FORBIDDEN,
                EventRegistrationErrorResponse.CodeEnum.FORBIDDEN,
                "Authenticated actor is not the Event owner",
                context,
                null);
    }

    static EventRegistrationHttpException conflict(ExecutionContext context) {
        return new EventRegistrationHttpException(
                HttpStatus.CONFLICT,
                EventRegistrationErrorResponse.CodeEnum.REGISTRATION_CONFLICT,
                "Registration uniqueness conflict",
                context,
                null);
    }

    static EventRegistrationHttpException eventNotPublished(ExecutionContext context) {
        return new EventRegistrationHttpException(
                HttpStatus.CONFLICT,
                EventRegistrationErrorResponse.CodeEnum.EVENT_NOT_PUBLISHED,
                "Referenced Event is not published",
                context,
                null);
    }

    static EventRegistrationHttpException internal(
            ExecutionContext context,
            RuntimeException cause) {
        return new EventRegistrationHttpException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                EventRegistrationErrorResponse.CodeEnum.INTERNAL_ERROR,
                "Internal server error",
                context,
                cause);
    }

    HttpStatus status() {
        return status;
    }

    EventRegistrationErrorResponse.CodeEnum code() {
        return code;
    }

    ExecutionContext context() {
        return context;
    }
}
