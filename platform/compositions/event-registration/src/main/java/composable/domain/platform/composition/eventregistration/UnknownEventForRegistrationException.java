package composable.domain.platform.composition.eventregistration;

public final class UnknownEventForRegistrationException extends RuntimeException {

    public UnknownEventForRegistrationException() {
        super("Referenced Event does not exist");
    }
}
