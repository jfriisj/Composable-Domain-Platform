package composable.domain.platform.event.api;

public final class InvalidEventDefinitionException extends RuntimeException {

    public InvalidEventDefinitionException() {
        super("Event definition is invalid");
    }
}
