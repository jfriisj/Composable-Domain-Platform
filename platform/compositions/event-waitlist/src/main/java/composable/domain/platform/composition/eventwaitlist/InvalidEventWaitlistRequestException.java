package composable.domain.platform.composition.eventwaitlist;

public final class InvalidEventWaitlistRequestException
        extends RuntimeException {

    public InvalidEventWaitlistRequestException() {
        super("Event waitlist request is invalid");
    }
}
