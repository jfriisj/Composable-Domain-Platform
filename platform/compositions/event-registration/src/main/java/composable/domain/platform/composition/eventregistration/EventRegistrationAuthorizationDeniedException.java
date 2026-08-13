package composable.domain.platform.composition.eventregistration;

public final class EventRegistrationAuthorizationDeniedException extends RuntimeException {

    public EventRegistrationAuthorizationDeniedException() {
        super("Event registration authorization denied");
    }
}
