package composable.domain.platform.composition.eventwaitlist;

public final class EventWaitlistUnavailableException
        extends RuntimeException {

    public EventWaitlistUnavailableException() {
        super("Event waitlist participation is unavailable");
    }
}
