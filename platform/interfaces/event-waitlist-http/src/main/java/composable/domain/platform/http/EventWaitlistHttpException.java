package composable.domain.platform.http;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.http.eventwaitlist.generated.model.EventWaitlistErrorResponse;
import org.springframework.http.HttpStatus;

final class EventWaitlistHttpException extends RuntimeException {

    private final HttpStatus status;
    private final EventWaitlistErrorResponse.CodeEnum code;
    private final ExecutionContext context;

    private EventWaitlistHttpException(
            HttpStatus status,
            EventWaitlistErrorResponse.CodeEnum code,
            String message,
            ExecutionContext context,
            Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
        this.context = context;
    }

    static EventWaitlistHttpException invalidRequest(ExecutionContext context) {
        return new EventWaitlistHttpException(
                HttpStatus.BAD_REQUEST,
                EventWaitlistErrorResponse.CodeEnum.INVALID_REQUEST,
                "Event waitlist request is invalid",
                context,
                null);
    }

    static EventWaitlistHttpException eventNotFound(ExecutionContext context) {
        return new EventWaitlistHttpException(
                HttpStatus.NOT_FOUND,
                EventWaitlistErrorResponse.CodeEnum.EVENT_NOT_FOUND,
                "Referenced Event was not found",
                context,
                null);
    }

    static EventWaitlistHttpException eventNotPublished(ExecutionContext context) {
        return new EventWaitlistHttpException(
                HttpStatus.CONFLICT,
                EventWaitlistErrorResponse.CodeEnum.EVENT_NOT_PUBLISHED,
                "Referenced Event is not published",
                context,
                null);
    }

    static EventWaitlistHttpException waitlistUnavailable(ExecutionContext context) {
        return new EventWaitlistHttpException(
                HttpStatus.CONFLICT,
                EventWaitlistErrorResponse.CodeEnum.EVENT_WAITLIST_UNAVAILABLE,
                "Event waitlist participation is unavailable",
                context,
                null);
    }

    static EventWaitlistHttpException registrationExists(ExecutionContext context) {
        return new EventWaitlistHttpException(
                HttpStatus.CONFLICT,
                EventWaitlistErrorResponse.CodeEnum.EVENT_REGISTRATION_EXISTS,
                "Event Registration already exists",
                context,
                null);
    }

    static EventWaitlistHttpException participationNotFound(ExecutionContext context) {
        return new EventWaitlistHttpException(
                HttpStatus.NOT_FOUND,
                EventWaitlistErrorResponse.CodeEnum.WAITLIST_PARTICIPATION_NOT_FOUND,
                "Event waitlist participation was not found",
                context,
                null);
    }

    static EventWaitlistHttpException internal(
            ExecutionContext context,
            RuntimeException cause) {
        return new EventWaitlistHttpException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                EventWaitlistErrorResponse.CodeEnum.INTERNAL_ERROR,
                "Internal server error",
                context,
                cause);
    }

    HttpStatus status() {
        return status;
    }

    EventWaitlistErrorResponse.CodeEnum code() {
        return code;
    }

    ExecutionContext context() {
        return context;
    }
}
