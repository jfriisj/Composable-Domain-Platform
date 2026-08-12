package composable.domain.platform.event.api;

public final class EventAlreadyPublishedException extends RuntimeException {

    private final String eventId;

    public EventAlreadyPublishedException(String eventId) {
        super("Event is already published: " + eventId);
        this.eventId = eventId;
    }

    public String eventId() {
        return eventId;
    }
}
