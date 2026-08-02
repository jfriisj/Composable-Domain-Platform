package composable.domain.platform.event.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.domain.Event;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FindEventServiceTest {

    @Test
    void findsExistingEventByIdentity() {
        Instant startsAt = Instant.parse("2026-09-01T08:00:00Z");
        Instant endsAt = Instant.parse("2026-09-01T10:00:00Z");
        ZoneId timezone = ZoneId.of("Europe/Copenhagen");
        InMemoryEventRepository repository = new InMemoryEventRepository();
        repository.addIfAbsent(new Event(
                "event-1",
                "Platform Day",
                "platform-day",
                startsAt,
                endsAt,
                timezone));

        Optional<EventView> result = new FindEventService(repository).findById("event-1");

        assertEquals(
                Optional.of(new EventView(
                        "event-1",
                        "Platform Day",
                        "platform-day",
                        startsAt,
                        endsAt,
                        timezone)),
                result);
    }

    @Test
    void returnsEmptyForUnknownIdentity() {
        Optional<EventView> result =
                new FindEventService(new InMemoryEventRepository()).findById("missing-event");

        assertTrue(result.isEmpty());
    }

    @Test
    void rejectsBlankIdentity() {
        FindEventService service = new FindEventService(new InMemoryEventRepository());

        assertThrows(IllegalArgumentException.class, () -> service.findById(" "));
    }
}
