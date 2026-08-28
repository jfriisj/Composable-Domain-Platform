package composable.domain.platform.composition.eventregistration;

public final class EventRegistrationClosedException extends RuntimeException {

    public EventRegistrationClosedException() {
        super("Event registration is closed");
    }
}
