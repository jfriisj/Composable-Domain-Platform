package composable.domain.platform.event.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.DefineEventCommand;
import composable.domain.platform.event.api.EventAlreadyDefinedException;
import composable.domain.platform.event.api.EventPublicationState;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.InvalidEventDefinitionException;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class DefineEventServiceTest {

    private static final ExecutionContext CONTEXT =
            new ExecutionContext(new CorrelationId("test-correlation"));

    @Test
    void definesUnpublishedEventPersistsItAndReturnsResultingState() {
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

        EventView result = new DefineEventService(repository).define(CONTEXT, command);

        EventView expected = new EventView(
                "event-1",
                "Platform Day",
                "platform-day",
                startsAt,
                endsAt,
                timezone,
                EventPublicationState.UNPUBLISHED);

        assertEquals(expected, result);
        assertEquals(
                expected,
                new FindEventService(repository).findById(CONTEXT, "event-1").orElseThrow());
    }

    @Test
    void rejectsDuplicateIdentityWithoutReplacingExistingEvent() {
        Instant startsAt = Instant.parse("2026-09-01T08:00:00Z");
        Instant endsAt = Instant.parse("2026-09-01T10:00:00Z");
        ZoneId timezone = ZoneId.of("Europe/Copenhagen");
        InMemoryEventRepository repository = new InMemoryEventRepository();
        DefineEventService service = new DefineEventService(repository);

        service.define(CONTEXT, new DefineEventCommand(
                "event-1",
                "Original Event",
                "original-event",
                startsAt,
                endsAt,
                timezone));

        EventAlreadyDefinedException error = assertThrows(
                EventAlreadyDefinedException.class,
                () -> service.define(CONTEXT, new DefineEventCommand(
                        "event-1",
                        "Replacement Event",
                        "replacement-event",
                        startsAt,
                        endsAt,
                        timezone)));

        assertEquals("event-1", error.eventId());
        EventView persisted = new FindEventService(repository)
                .findById(CONTEXT, "event-1")
                .orElseThrow();
        assertEquals("Original Event", persisted.name());
        assertEquals(EventPublicationState.UNPUBLISHED, persisted.publicationState());
    }

    @Test
    void translatesInvalidDomainDefinitionToPublicApplicationFailure() {
        InMemoryEventRepository repository = new InMemoryEventRepository();
        DefineEventService service = new DefineEventService(repository);

        InvalidEventDefinitionException error = assertThrows(
                InvalidEventDefinitionException.class,
                () -> service.define(CONTEXT, new DefineEventCommand(
                        "event-invalid",
                        " ",
                        "invalid-event",
                        Instant.parse("2026-09-01T08:00:00Z"),
                        Instant.parse("2026-09-01T10:00:00Z"),
                        ZoneId.of("Europe/Copenhagen"))));

        assertEquals("Event definition is invalid", error.getMessage());
        assertTrue(new FindEventService(repository)
                .findById(CONTEXT, "event-invalid")
                .isEmpty());
    }

    @Test
    void rejectsMissingExecutionContextAsProgrammingError() {
        DefineEventService service = new DefineEventService(new InMemoryEventRepository());

        assertThrows(
                NullPointerException.class,
                () -> service.define(null, new DefineEventCommand(
                        "event-1",
                        "Platform Day",
                        "platform-day",
                        Instant.parse("2026-09-01T08:00:00Z"),
                        Instant.parse("2026-09-01T10:00:00Z"),
                        ZoneId.of("Europe/Copenhagen"))));
    }
}
