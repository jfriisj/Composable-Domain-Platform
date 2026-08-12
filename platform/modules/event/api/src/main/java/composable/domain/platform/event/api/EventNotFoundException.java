package composable.domain.platform.event.api;

public final class EventNotFoundException extends RuntimeException {

    private final String eventId;

    public EventNotFoundException(String eventId) {
        super("Event not found: " + eventId);
        this.eventId = eventId;
    }

    public String eventId() {
        return eventId;
    }
}
