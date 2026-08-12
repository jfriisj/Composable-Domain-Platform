package composable.domain.platform.event.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.EventPublicationState;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.domain.Event;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import org.junit.jupiter.api.Test;

class DiscoverEventsServiceTest {

    private static final ExecutionContext CONTEXT =
            new ExecutionContext(new CorrelationId("discover-test-correlation"));

    @Test
    void discoversPublishedEventsAndExcludesUnpublishedEvents() {
        InMemoryEventRepository repository = new InMemoryEventRepository();
        Event published = event("published-event", "Published Event");
        Event unpublished = event("unpublished-event", "Unpublished Event");
        repository.addIfAbsent(published);
        repository.addIfAbsent(unpublished);
        repository.updatePublicationState(published.publish(), published.publicationState());

        Collection<EventView> discovered =
                new DiscoverEventsService(repository).discover(CONTEXT);

        assertEquals(1, discovered.size());
        EventView result = discovered.iterator().next();
        assertEquals("published-event", result.eventId());
        assertEquals("Published Event", result.name());
        assertEquals(EventPublicationState.PUBLISHED, result.publicationState());
    }

    @Test
    void rejectsMissingExecutionContextAsProgrammingError() {
        DiscoverEventsService service =
                new DiscoverEventsService(new InMemoryEventRepository());

        assertThrows(NullPointerException.class, () -> service.discover(null));
    }

    private static Event event(String eventId, String name) {
        return new Event(
                eventId,
                name,
                eventId,
                Instant.parse("2026-09-01T08:00:00Z"),
                Instant.parse("2026-09-01T10:00:00Z"),
                ZoneId.of("Europe/Copenhagen"));
    }
}
