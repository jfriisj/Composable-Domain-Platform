package composable.domain.platform.composition.eventregistration;

public final class EventNotPublishedForRegistrationException extends RuntimeException {

    public EventNotPublishedForRegistrationException() {
        super("Referenced Event is not published");
    }
}
