package composable.domain.platform.registration.api;

public final class InvalidRegistrationDefinitionException extends RuntimeException {

    public InvalidRegistrationDefinitionException() {
        super("Registration definition is invalid");
    }
}
