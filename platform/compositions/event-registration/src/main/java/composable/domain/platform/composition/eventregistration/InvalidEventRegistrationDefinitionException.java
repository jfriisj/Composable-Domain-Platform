package composable.domain.platform.composition.eventregistration;

public final class InvalidEventRegistrationDefinitionException extends RuntimeException {

    public InvalidEventRegistrationDefinitionException() {
        super("Event registration definition is invalid");
    }
}
