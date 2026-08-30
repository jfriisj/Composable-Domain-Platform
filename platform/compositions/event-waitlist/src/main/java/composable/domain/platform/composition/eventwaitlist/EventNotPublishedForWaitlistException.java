package composable.domain.platform.composition.eventwaitlist;

public final class EventNotPublishedForWaitlistException
        extends RuntimeException {

    public EventNotPublishedForWaitlistException() {
        super("Referenced Event is not published");
    }
}
