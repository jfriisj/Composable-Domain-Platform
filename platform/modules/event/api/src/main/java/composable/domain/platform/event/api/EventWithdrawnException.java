package composable.domain.platform.event.api;

public final class EventWithdrawnException extends RuntimeException {

    private final String eventId;

    public EventWithdrawnException(String eventId) {
        super("Event is withdrawn: " + eventId);
        this.eventId = eventId;
    }

    public String eventId() {
        return eventId;
    }
}
