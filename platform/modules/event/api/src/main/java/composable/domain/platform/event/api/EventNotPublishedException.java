package composable.domain.platform.event.api;

public final class EventNotPublishedException extends RuntimeException {

    private final String eventId;

    public EventNotPublishedException(String eventId) {
        super("Event is not published: " + eventId);
        this.eventId = eventId;
    }

    public String eventId() {
        return eventId;
    }
}
