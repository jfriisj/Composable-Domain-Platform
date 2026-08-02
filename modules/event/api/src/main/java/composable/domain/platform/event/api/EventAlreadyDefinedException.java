package composable.domain.platform.event.api;

public final class EventAlreadyDefinedException extends RuntimeException {

    private final String eventId;

    public EventAlreadyDefinedException(String eventId) {
        super("Event is already defined: " + eventId);
        this.eventId = eventId;
    }

    public String eventId() {
        return eventId;
    }
}
