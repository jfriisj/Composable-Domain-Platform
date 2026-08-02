package composable.domain.platform.event.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import composable.domain.platform.event.api.DefineEventCommand;
import composable.domain.platform.event.api.EventAlreadyDefinedException;
import composable.domain.platform.event.api.EventView;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class DefineEventServiceTest {

    @Test
    void definesEventPersistsItAndReturnsResultingState() {
        Instant startsAt = Instant.parse("2026-09-01T08:00:00Z");
        Instant endsAt = Instant.parse("2026-09-01T10:00:00Z");
        ZoneId timezone = ZoneId.of("Europe/Copenhagen");
        InMemoryEventRepository repository = new InMemoryEventRepository();

        DefineEventCommand command = new DefineEventCommand(
                "event-1",
                "Platform Day",
                "platform-day",
                startsAt,
                endsAt,
                timezone);

        EventView result = new DefineEventService(repository).define(command);

        EventView expected = new EventView(
                "event-1",
                "Platform Day",
                "platform-day",
                startsAt,
                endsAt,
                timezone);

        assertEquals(expected, result);
        assertEquals(expected, new FindEventService(repository).findById("event-1").orElseThrow());
    }

    @Test
    void rejectsDuplicateIdentityWithoutReplacingExistingEvent() {
        Instant startsAt = Instant.parse("2026-09-01T08:00:00Z");
        Instant endsAt = Instant.parse("2026-09-01T10:00:00Z");
        ZoneId timezone = ZoneId.of("Europe/Copenhagen");
        InMemoryEventRepository repository = new InMemoryEventRepository();
        DefineEventService service = new DefineEventService(repository);

        service.define(new DefineEventCommand(
                "event-1",
                "Original Event",
                "original-event",
                startsAt,
                endsAt,
                timezone));

        EventAlreadyDefinedException error = assertThrows(
                EventAlreadyDefinedException.class,
                () -> service.define(new DefineEventCommand(
                        "event-1",
                        "Replacement Event",
                        "replacement-event",
                        startsAt,
                        endsAt,
                        timezone)));

        assertEquals("event-1", error.eventId());
        assertEquals(
                "Original Event",
                new FindEventService(repository).findById("event-1").orElseThrow().name());
    }
}
