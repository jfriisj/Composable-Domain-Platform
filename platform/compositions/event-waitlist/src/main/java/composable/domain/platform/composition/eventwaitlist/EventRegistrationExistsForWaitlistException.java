package composable.domain.platform.composition.eventwaitlist;

public final class EventRegistrationExistsForWaitlistException
        extends RuntimeException {

    public EventRegistrationExistsForWaitlistException() {
        super("Event Registration already exists");
    }
}
