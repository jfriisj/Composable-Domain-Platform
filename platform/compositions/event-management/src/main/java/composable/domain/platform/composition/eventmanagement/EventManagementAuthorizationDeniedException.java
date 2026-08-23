package composable.domain.platform.composition.eventmanagement;

public final class EventManagementAuthorizationDeniedException extends RuntimeException {

    public EventManagementAuthorizationDeniedException() {
        super("Access denied: authenticated actor is not the resource owner");
    }
}
