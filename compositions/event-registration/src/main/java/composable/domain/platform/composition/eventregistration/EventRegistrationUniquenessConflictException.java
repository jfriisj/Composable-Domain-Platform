package composable.domain.platform.composition.eventregistration;

public final class EventRegistrationUniquenessConflictException extends RuntimeException {

    public EventRegistrationUniquenessConflictException() {
        super("Event registration uniqueness conflict");
    }
}
