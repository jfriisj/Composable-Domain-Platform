package composable.domain.platform.composition.eventwaitlist;

public final class UnknownEventForWaitlistException
        extends RuntimeException {

    public UnknownEventForWaitlistException() {
        super("Referenced Event was not found");
    }
}
