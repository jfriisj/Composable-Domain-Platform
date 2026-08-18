package composable.domain.platform.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.DefineEvent;
import composable.domain.platform.event.api.DefineEventCommand;
import composable.domain.platform.event.api.DiscoverEvents;
import composable.domain.platform.event.api.EventAlreadyDefinedException;
import composable.domain.platform.event.api.EventAlreadyPublishedException;
import composable.domain.platform.event.api.EventNotFoundException;
import composable.domain.platform.event.api.EventPublicationState;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.FindEvent;
import composable.domain.platform.event.api.InvalidEventDefinitionException;
import composable.domain.platform.event.api.PublishEvent;
import composable.domain.platform.http.event.generated.model.DefineEventRequest;
import composable.domain.platform.http.event.generated.model.ErrorResponse;
import composable.domain.platform.http.event.generated.model.EventResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class EventHttpAdapterTest {

    private static final EventView EVENT = new EventView(
            "event-1",
            "Platform Day",
            "platform-day",
            Instant.parse("2026-09-01T08:00:00.123456789Z"),
            Instant.parse("2026-09-01T10:00:00.987654321Z"),
            ZoneId.of("Europe/Copenhagen"),
            EventPublicationState.UNPUBLISHED);

    @Test
    void definesEventWithSuppliedCorrelationAndMapsTransportFields() {
        AtomicReference<ExecutionContext> capturedContext = new AtomicReference<>();
        AtomicReference<DefineEventCommand> capturedCommand = new AtomicReference<>();

        DefineEvent defineEvent = (context, command) -> {
            capturedContext.set(context);
            capturedCommand.set(command);
            return EVENT;
        };

        EventHttpAdapter adapter =
                adapter(defineEvent, missingFindEvent());

        ResponseEntity<EventResponse> response =
                adapter.defineEvent(request(), "corr-supplied");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("corr-supplied", response.getHeaders().getFirst(HttpCorrelation.HEADER_NAME));
        assertEquals("corr-supplied", capturedContext.get().correlationId().value());

        DefineEventCommand command = capturedCommand.get();
        assertEquals("event-1", command.eventId());
        assertEquals("Platform Day", command.name());
        assertEquals("platform-day", command.slug());
        assertEquals(Instant.parse("2026-09-01T08:00:00.123456789Z"), command.startsAt());
        assertEquals(Instant.parse("2026-09-01T10:00:00.987654321Z"), command.endsAt());
        assertEquals(ZoneId.of("Europe/Copenhagen"), command.timezone());

        assertEventResponse(response.getBody());
    }

    @Test
    void generatesCorrelationWhenHeaderIsAbsentAndPropagatesTheSameValue() {
        AtomicReference<ExecutionContext> capturedContext = new AtomicReference<>();

        DefineEvent defineEvent = (context, command) -> {
            capturedContext.set(context);
            return EVENT;
        };

        ResponseEntity<EventResponse> response =
                adapter(defineEvent, missingFindEvent()).defineEvent(request(), null);

        String responseCorrelation =
                response.getHeaders().getFirst(HttpCorrelation.HEADER_NAME);

        assertNotNull(responseCorrelation);
        assertTrue(!responseCorrelation.isBlank());
        assertEquals(responseCorrelation, capturedContext.get().correlationId().value());
    }

    @Test
    void mapsExplicitInvalidDefinitionFailureToBadRequest() {
        DefineEvent defineEvent = (context, command) -> {
            throw new InvalidEventDefinitionException();
        };

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter(defineEvent, missingFindEvent())
                        .defineEvent(request(), "corr-invalid"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        assertEquals(ErrorResponse.CodeEnum.INVALID_REQUEST, exception.code());
        assertEquals("corr-invalid", exception.context().correlationId().value());
    }

    @Test
    void mapsDuplicateIdentityToConflict() {
        DefineEvent defineEvent = (context, command) -> {
            throw new EventAlreadyDefinedException(command.eventId());
        };

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter(defineEvent, missingFindEvent())
                        .defineEvent(request(), "corr-duplicate"));

        assertEquals(HttpStatus.CONFLICT, exception.status());
        assertEquals(ErrorResponse.CodeEnum.EVENT_ALREADY_DEFINED, exception.code());
    }

    @Test
    void mapsInvalidTransportTimezoneToBadRequestWithoutCallingEventUseCase() {
        DefineEvent defineEvent = (context, command) -> {
            throw new AssertionError("Event use case must not be called");
        };

        DefineEventRequest request = request();
        request.setTimezone("not/a-zone");

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter(defineEvent, missingFindEvent())
                        .defineEvent(request, "corr-timezone"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        assertEquals(ErrorResponse.CodeEnum.INVALID_REQUEST, exception.code());
    }

    @Test
    void mapsUnexpectedEventFailureToInternalServerErrorWithoutExposingIt() {
        DefineEvent defineEvent = (context, command) -> {
            throw new IllegalStateException("database-specific detail");
        };

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter(defineEvent, missingFindEvent())
                        .defineEvent(request(), "corr-internal"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.status());
        assertEquals(ErrorResponse.CodeEnum.INTERNAL_ERROR, exception.code());
        assertEquals("Internal server error", exception.getMessage());
    }

    @Test
    void discoversEventsThroughEventCapabilityAndPreservesCorrelation() {
        AtomicReference<ExecutionContext> capturedContext = new AtomicReference<>();

        DiscoverEvents discoverEvents = context -> {
            capturedContext.set(context);
            return List.of(EVENT);
        };

        ResponseEntity<List<EventResponse>> response = new EventHttpAdapter(
                        unusedDefineEvent(),
                        missingFindEvent(),
                        unusedPublishEvent(),
                        discoverEvents)
                .discoverEvents("corr-discover");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("corr-discover", response.getHeaders().getFirst(HttpCorrelation.HEADER_NAME));
        assertEquals("corr-discover", capturedContext.get().correlationId().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEventResponse(response.getBody().get(0));
    }

    @Test
    void returnsEmptyDiscoveryAsSuccessfulEmptyArrayRepresentation() {
        ResponseEntity<List<EventResponse>> response = new EventHttpAdapter(
                        unusedDefineEvent(),
                        missingFindEvent(),
                        unusedPublishEvent(),
                        context -> List.of())
                .discoverEvents("corr-empty");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(), response.getBody());
        assertEquals("corr-empty", response.getHeaders().getFirst(HttpCorrelation.HEADER_NAME));
    }

    @Test
    void mapsUnexpectedDiscoveryFailureToInternalServerError() {
        DiscoverEvents discoverEvents = context -> {
            throw new IllegalStateException("database-specific detail");
        };

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> new EventHttpAdapter(
                                unusedDefineEvent(),
                                missingFindEvent(),
                                unusedPublishEvent(),
                                discoverEvents)
                        .discoverEvents("corr-discovery-internal"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.status());
        assertEquals(ErrorResponse.CodeEnum.INTERNAL_ERROR, exception.code());
        assertEquals("corr-discovery-internal", exception.context().correlationId().value());
    }

    @Test
    void publishesEventThroughEventCapabilityAndPreservesCorrelation() {
        AtomicReference<ExecutionContext> capturedContext = new AtomicReference<>();
        AtomicReference<String> capturedEventId = new AtomicReference<>();

        PublishEvent publishEvent = (context, eventId) -> {
            capturedContext.set(context);
            capturedEventId.set(eventId);
            return EVENT;
        };

        ResponseEntity<Void> response = new EventHttpAdapter(
                        unusedDefineEvent(),
                        missingFindEvent(),
                        publishEvent,
                        emptyDiscoverEvents())
                .publishEvent("event-1", "corr-publish");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertEquals("corr-publish", response.getHeaders().getFirst(HttpCorrelation.HEADER_NAME));
        assertEquals("corr-publish", capturedContext.get().correlationId().value());
        assertEquals("event-1", capturedEventId.get());
    }

    @Test
    void mapsUnknownPublicationTargetToNotFound() {
        PublishEvent publishEvent = (context, eventId) -> {
            throw new EventNotFoundException(eventId);
        };

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> new EventHttpAdapter(
                                unusedDefineEvent(),
                                missingFindEvent(),
                                publishEvent,
                                emptyDiscoverEvents())
                        .publishEvent("missing-event", "corr-publish-missing"));

        assertEquals(HttpStatus.NOT_FOUND, exception.status());
        assertEquals(ErrorResponse.CodeEnum.EVENT_NOT_FOUND, exception.code());
        assertEquals("corr-publish-missing", exception.context().correlationId().value());
    }

    @Test
    void mapsAlreadyPublishedEventToConflict() {
        PublishEvent publishEvent = (context, eventId) -> {
            throw new EventAlreadyPublishedException(eventId);
        };

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> new EventHttpAdapter(
                                unusedDefineEvent(),
                                missingFindEvent(),
                                publishEvent,
                                emptyDiscoverEvents())
                        .publishEvent("event-1", "corr-already-published"));

        assertEquals(HttpStatus.CONFLICT, exception.status());
        assertEquals(ErrorResponse.CodeEnum.EVENT_ALREADY_PUBLISHED, exception.code());
        assertEquals("corr-already-published", exception.context().correlationId().value());
    }

    @Test
    void mapsUnexpectedPublicationFailureToInternalServerError() {
        PublishEvent publishEvent = (context, eventId) -> {
            throw new IllegalStateException("database-specific detail");
        };

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> new EventHttpAdapter(
                                unusedDefineEvent(),
                                missingFindEvent(),
                                publishEvent,
                                emptyDiscoverEvents())
                        .publishEvent("event-1", "corr-publish-internal"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.status());
        assertEquals(ErrorResponse.CodeEnum.INTERNAL_ERROR, exception.code());
        assertEquals("corr-publish-internal", exception.context().correlationId().value());
    }

    @Test
    void retrievesExistingEventWithSuppliedCorrelation() {
        AtomicReference<ExecutionContext> capturedContext = new AtomicReference<>();
        AtomicReference<String> capturedEventId = new AtomicReference<>();

        FindEvent findEvent = (context, eventId) -> {
            capturedContext.set(context);
            capturedEventId.set(eventId);
            return Optional.of(EVENT);
        };

        ResponseEntity<EventResponse> response =
                adapter(unusedDefineEvent(), findEvent)
                        .findEventById("event-1", "corr-find");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("corr-find", response.getHeaders().getFirst(HttpCorrelation.HEADER_NAME));
        assertEquals("corr-find", capturedContext.get().correlationId().value());
        assertEquals("event-1", capturedEventId.get());
        assertEventResponse(response.getBody());
    }

    @Test
    void mapsUnknownEventToNotFound() {
        FindEvent findEvent = (context, eventId) -> Optional.empty();

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter(unusedDefineEvent(), findEvent)
                        .findEventById("missing-event", "corr-missing"));

        assertEquals(HttpStatus.NOT_FOUND, exception.status());
        assertEquals(ErrorResponse.CodeEnum.EVENT_NOT_FOUND, exception.code());
        assertEquals("corr-missing", exception.context().correlationId().value());
    }

    private static DefineEventRequest request() {
        return new DefineEventRequest(
                "event-1",
                "Platform Day",
                "platform-day",
                OffsetDateTime.ofInstant(
                        Instant.parse("2026-09-01T08:00:00.123456789Z"),
                        ZoneOffset.UTC),
                OffsetDateTime.ofInstant(
                        Instant.parse("2026-09-01T10:00:00.987654321Z"),
                        ZoneOffset.UTC),
                "Europe/Copenhagen");
    }

    private static EventHttpAdapter adapter(DefineEvent defineEvent, FindEvent findEvent) {
        return new EventHttpAdapter(
                defineEvent,
                findEvent,
                unusedPublishEvent(),
                emptyDiscoverEvents());
    }

    private static DefineEvent unusedDefineEvent() {
        return (context, command) -> {
            throw new AssertionError("DefineEvent must not be called");
        };
    }

    private static FindEvent missingFindEvent() {
        return (context, eventId) -> Optional.empty();
    }

    private static PublishEvent unusedPublishEvent() {
        return (context, eventId) -> {
            throw new AssertionError("PublishEvent must not be called");
        };
    }

    private static DiscoverEvents emptyDiscoverEvents() {
        return context -> List.of();
    }

    private static void assertEventResponse(EventResponse response) {
        assertNotNull(response);
        assertEquals("event-1", response.getEventId());
        assertEquals("Platform Day", response.getName());
        assertEquals("platform-day", response.getSlug());
        assertEquals(
                OffsetDateTime.ofInstant(EVENT.startsAt(), ZoneOffset.UTC),
                response.getStartsAt());
        assertEquals(
                OffsetDateTime.ofInstant(EVENT.endsAt(), ZoneOffset.UTC),
                response.getEndsAt());
        assertEquals("Europe/Copenhagen", response.getTimezone());
    }
}
