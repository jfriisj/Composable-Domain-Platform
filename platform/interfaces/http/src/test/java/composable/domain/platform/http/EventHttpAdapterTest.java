package composable.domain.platform.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import composable.domain.platform.composition.eventmanagement.DefineOrganizerEventCommand;
import composable.domain.platform.composition.eventmanagement.EventManagementAuthorizationDeniedException;
import composable.domain.platform.composition.eventmanagement.OrganizerEventManagementService;
import composable.domain.platform.composition.eventmanagement.UpdateOrganizerEventCommand;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.DefineEvent;
import composable.domain.platform.event.api.DiscoverEvents;
import composable.domain.platform.event.api.EventAlreadyDefinedException;
import composable.domain.platform.event.api.EventAlreadyPublishedException;
import composable.domain.platform.event.api.EventNotFoundException;
import composable.domain.platform.event.api.EventNotPublishedException;
import composable.domain.platform.event.api.EventOwnerReference;
import composable.domain.platform.event.api.EventPublicationState;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.EventWithdrawnException;
import composable.domain.platform.event.api.FindEvent;
import composable.domain.platform.event.api.InvalidEventDefinitionException;
import composable.domain.platform.event.api.PublishEvent;
import composable.domain.platform.event.api.UpdateEvent;
import composable.domain.platform.event.api.WithdrawEvent;
import composable.domain.platform.http.event.generated.model.DefineEventRequest;
import composable.domain.platform.http.event.generated.model.ErrorResponse;
import composable.domain.platform.http.event.generated.model.EventResponse;
import composable.domain.platform.http.event.generated.model.UpdateEventRequest;
import composable.domain.platform.security.api.AuthenticatedActorReference;
import composable.domain.platform.security.api.AuthorizationDecision;
import composable.domain.platform.security.api.AuthorizeResourceOwnership;
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

    private static final AuthenticatedActorReference ACTOR =
            new AuthenticatedActorReference("organizer-1");

    private static final EventView EVENT = new EventView(
            "event-1",
            "Platform Day",
            "platform-day",
            Instant.parse("2026-09-01T08:00:00.123456789Z"),
            Instant.parse("2026-09-01T10:00:00.987654321Z"),
            ZoneId.of("Europe/Copenhagen"),
            EventPublicationState.UNPUBLISHED,
            new EventOwnerReference("organizer-1"));

    @Test
    void definesEventWithSuppliedCorrelationAndMapsTransportFields() {
        AtomicReference<ExecutionContext> capturedContext = new AtomicReference<>();
        AtomicReference<DefineOrganizerEventCommand> capturedCommand = new AtomicReference<>();

        DefineEvent defineEvent = (context, command) -> {
            capturedContext.set(context);
            capturedCommand.set(new DefineOrganizerEventCommand(
                    command.eventId(),
                    command.name(),
                    command.slug(),
                    command.startsAt(),
                    command.endsAt(),
                    command.timezone()));
            return EVENT;
        };

        OrganizerEventManagementService organizerService = new OrganizerEventManagementService(
                defineEvent,
                unusedUpdateEvent(),
                unusedPublishEvent(),
                unusedWithdrawEvent(),
                missingFindEvent(),
                authorizedOwnership());

        EventHttpAdapter adapter = new EventHttpAdapter(
                organizerService,
                missingFindEvent(),
                emptyDiscoverEvents(),
                () -> ACTOR);

        ResponseEntity<EventResponse> response =
                adapter.defineEvent(defineRequest(), "corr-supplied");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("corr-supplied", response.getHeaders().getFirst(HttpCorrelation.HEADER_NAME));
        assertEquals("corr-supplied", capturedContext.get().correlationId().value());

        DefineOrganizerEventCommand command = capturedCommand.get();
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

        OrganizerEventManagementService organizerService = new OrganizerEventManagementService(
                defineEvent,
                unusedUpdateEvent(),
                unusedPublishEvent(),
                unusedWithdrawEvent(),
                missingFindEvent(),
                authorizedOwnership());

        EventHttpAdapter adapter = new EventHttpAdapter(
                organizerService,
                missingFindEvent(),
                emptyDiscoverEvents(),
                () -> ACTOR);

        ResponseEntity<EventResponse> response =
                adapter.defineEvent(defineRequest(), null);

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

        OrganizerEventManagementService organizerService = new OrganizerEventManagementService(
                defineEvent,
                unusedUpdateEvent(),
                unusedPublishEvent(),
                unusedWithdrawEvent(),
                missingFindEvent(),
                authorizedOwnership());

        EventHttpAdapter adapter = new EventHttpAdapter(
                organizerService,
                missingFindEvent(),
                emptyDiscoverEvents(),
                () -> ACTOR);

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter.defineEvent(defineRequest(), "corr-invalid"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        assertEquals(ErrorResponse.CodeEnum.INVALID_REQUEST, exception.code());
        assertEquals("corr-invalid", exception.context().correlationId().value());
    }

    @Test
    void mapsDuplicateIdentityToConflict() {
        DefineEvent defineEvent = (context, command) -> {
            throw new EventAlreadyDefinedException(command.eventId());
        };

        OrganizerEventManagementService organizerService = new OrganizerEventManagementService(
                defineEvent,
                unusedUpdateEvent(),
                unusedPublishEvent(),
                unusedWithdrawEvent(),
                missingFindEvent(),
                authorizedOwnership());

        EventHttpAdapter adapter = new EventHttpAdapter(
                organizerService,
                missingFindEvent(),
                emptyDiscoverEvents(),
                () -> ACTOR);

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter.defineEvent(defineRequest(), "corr-duplicate"));

        assertEquals(HttpStatus.CONFLICT, exception.status());
        assertEquals(ErrorResponse.CodeEnum.EVENT_ALREADY_DEFINED, exception.code());
    }

    @Test
    void mapsInvalidTransportTimezoneToBadRequestWithoutCallingEventUseCase() {
        DefineEvent defineEvent = (context, command) -> {
            throw new AssertionError("Event use case must not be called");
        };

        OrganizerEventManagementService organizerService = new OrganizerEventManagementService(
                defineEvent,
                unusedUpdateEvent(),
                unusedPublishEvent(),
                unusedWithdrawEvent(),
                missingFindEvent(),
                authorizedOwnership());

        EventHttpAdapter adapter = new EventHttpAdapter(
                organizerService,
                missingFindEvent(),
                emptyDiscoverEvents(),
                () -> ACTOR);

        DefineEventRequest request = defineRequest();
        request.setTimezone("not/a-zone");

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter.defineEvent(request, "corr-timezone"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        assertEquals(ErrorResponse.CodeEnum.INVALID_REQUEST, exception.code());
    }

    @Test
    void mapsUnexpectedDefineFailureToInternalServerErrorWithoutExposingIt() {
        DefineEvent defineEvent = (context, command) -> {
            throw new IllegalStateException("database-specific detail");
        };

        OrganizerEventManagementService organizerService = new OrganizerEventManagementService(
                defineEvent,
                unusedUpdateEvent(),
                unusedPublishEvent(),
                unusedWithdrawEvent(),
                missingFindEvent(),
                authorizedOwnership());

        EventHttpAdapter adapter = new EventHttpAdapter(
                organizerService,
                missingFindEvent(),
                emptyDiscoverEvents(),
                () -> ACTOR);

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter.defineEvent(defineRequest(), "corr-internal"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.status());
        assertEquals(ErrorResponse.CodeEnum.INTERNAL_ERROR, exception.code());
        assertEquals("Internal server error", exception.getMessage());
    }

    @Test
    void updatesEventWhenAuthorizedAndReturnsUpdatedEvent() {
        FindEvent findEvent = (context, eventId) -> Optional.of(EVENT);
        UpdateEvent updateEvent = (context, command) -> new EventView(
                command.eventId(),
                command.name(),
                command.slug(),
                command.startsAt(),
                command.endsAt(),
                command.timezone(),
                EventPublicationState.UNPUBLISHED,
                EVENT.owner());

        OrganizerEventManagementService organizerService = new OrganizerEventManagementService(
                unusedDefineEvent(),
                updateEvent,
                unusedPublishEvent(),
                unusedWithdrawEvent(),
                findEvent,
                authorizedOwnership());

        EventHttpAdapter adapter = new EventHttpAdapter(
                organizerService,
                findEvent,
                emptyDiscoverEvents(),
                () -> ACTOR);

        ResponseEntity<EventResponse> response =
                adapter.updateEvent("event-1", updateRequest(), "corr-update");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("corr-update", response.getHeaders().getFirst(HttpCorrelation.HEADER_NAME));
        assertEquals("Updated Platform Day", response.getBody().getName());
    }

    @Test
    void mapsUnauthorizedUpdateToForbidden() {
        FindEvent findEvent = (context, eventId) -> Optional.of(EVENT);

        OrganizerEventManagementService organizerService = new OrganizerEventManagementService(
                unusedDefineEvent(),
                unusedUpdateEvent(),
                unusedPublishEvent(),
                unusedWithdrawEvent(),
                findEvent,
                deniedOwnership());

        EventHttpAdapter adapter = new EventHttpAdapter(
                organizerService,
                findEvent,
                emptyDiscoverEvents(),
                () -> ACTOR);

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter.updateEvent("event-1", updateRequest(), "corr-forbidden"));

        assertEquals(HttpStatus.FORBIDDEN, exception.status());
        assertEquals(ErrorResponse.CodeEnum.FORBIDDEN, exception.code());
    }

    @Test
    void mapsUnknownEventUpdateToNotFound() {
        FindEvent findEvent = (context, eventId) -> Optional.empty();

        OrganizerEventManagementService organizerService = new OrganizerEventManagementService(
                unusedDefineEvent(),
                unusedUpdateEvent(),
                unusedPublishEvent(),
                unusedWithdrawEvent(),
                findEvent,
                authorizedOwnership());

        EventHttpAdapter adapter = new EventHttpAdapter(
                organizerService,
                findEvent,
                emptyDiscoverEvents(),
                () -> ACTOR);

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter.updateEvent("event-missing", updateRequest(), "corr-not-found"));

        assertEquals(HttpStatus.NOT_FOUND, exception.status());
        assertEquals(ErrorResponse.CodeEnum.EVENT_NOT_FOUND, exception.code());
    }

    @Test
    void mapsPublishedEventUpdateToConflict() {
        EventView published = new EventView(
                "event-1",
                "Platform Day",
                "platform-day",
                Instant.parse("2026-09-01T08:00:00Z"),
                Instant.parse("2026-09-01T10:00:00Z"),
                ZoneId.of("Europe/Copenhagen"),
                EventPublicationState.PUBLISHED,
                new EventOwnerReference("organizer-1"));

        FindEvent findEvent = (context, eventId) -> Optional.of(published);
        UpdateEvent updateEvent = (context, command) -> {
            throw new EventAlreadyPublishedException(command.eventId());
        };

        OrganizerEventManagementService organizerService = new OrganizerEventManagementService(
                unusedDefineEvent(),
                updateEvent,
                unusedPublishEvent(),
                unusedWithdrawEvent(),
                findEvent,
                authorizedOwnership());

        EventHttpAdapter adapter = new EventHttpAdapter(
                organizerService,
                findEvent,
                emptyDiscoverEvents(),
                () -> ACTOR);

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter.updateEvent("event-1", updateRequest(), "corr-conflict"));

        assertEquals(HttpStatus.CONFLICT, exception.status());
        assertEquals(ErrorResponse.CodeEnum.EVENT_ALREADY_PUBLISHED, exception.code());
    }

    @Test
    void discoversEventsThroughEventCapabilityAndPreservesCorrelation() {
        AtomicReference<ExecutionContext> capturedContext = new AtomicReference<>();

        DiscoverEvents discoverEvents = context -> {
            capturedContext.set(context);
            return List.of(EVENT);
        };

        EventHttpAdapter adapter = new EventHttpAdapter(
                dummyOrganizerService(),
                missingFindEvent(),
                discoverEvents,
                () -> ACTOR);

        ResponseEntity<List<EventResponse>> response = adapter.discoverEvents("corr-discover");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("corr-discover", response.getHeaders().getFirst(HttpCorrelation.HEADER_NAME));
        assertEquals("corr-discover", capturedContext.get().correlationId().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEventResponse(response.getBody().get(0));
    }

    @Test
    void returnsEmptyDiscoveryAsSuccessfulEmptyArrayRepresentation() {
        EventHttpAdapter adapter = new EventHttpAdapter(
                dummyOrganizerService(),
                missingFindEvent(),
                context -> List.of(),
                () -> ACTOR);

        ResponseEntity<List<EventResponse>> response = adapter.discoverEvents("corr-empty");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(), response.getBody());
        assertEquals("corr-empty", response.getHeaders().getFirst(HttpCorrelation.HEADER_NAME));
    }

    @Test
    void mapsUnexpectedDiscoveryFailureToInternalServerError() {
        DiscoverEvents discoverEvents = context -> {
            throw new IllegalStateException("database-specific detail");
        };

        EventHttpAdapter adapter = new EventHttpAdapter(
                dummyOrganizerService(),
                missingFindEvent(),
                discoverEvents,
                () -> ACTOR);

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter.discoverEvents("corr-discovery-internal"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.status());
        assertEquals(ErrorResponse.CodeEnum.INTERNAL_ERROR, exception.code());
        assertEquals("corr-discovery-internal", exception.context().correlationId().value());
    }

    @Test
    void publishesEventThroughOrganizerManagementWhenAuthorized() {
        FindEvent findEvent = (context, eventId) -> Optional.of(EVENT);
        PublishEvent publishEvent = (context, eventId) -> EVENT;

        OrganizerEventManagementService organizerService = new OrganizerEventManagementService(
                unusedDefineEvent(),
                unusedUpdateEvent(),
                publishEvent,
                unusedWithdrawEvent(),
                findEvent,
                authorizedOwnership());

        EventHttpAdapter adapter = new EventHttpAdapter(
                organizerService,
                findEvent,
                emptyDiscoverEvents(),
                () -> ACTOR);

        ResponseEntity<Void> response = adapter.publishEvent("event-1", "corr-publish");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertEquals("corr-publish", response.getHeaders().getFirst(HttpCorrelation.HEADER_NAME));
    }

    @Test
    void mapsUnauthorizedPublishToForbidden() {
        FindEvent findEvent = (context, eventId) -> Optional.of(EVENT);

        OrganizerEventManagementService organizerService = new OrganizerEventManagementService(
                unusedDefineEvent(),
                unusedUpdateEvent(),
                unusedPublishEvent(),
                unusedWithdrawEvent(),
                findEvent,
                deniedOwnership());

        EventHttpAdapter adapter = new EventHttpAdapter(
                organizerService,
                findEvent,
                emptyDiscoverEvents(),
                () -> ACTOR);

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter.publishEvent("event-1", "corr-forbidden-pub"));

        assertEquals(HttpStatus.FORBIDDEN, exception.status());
        assertEquals(ErrorResponse.CodeEnum.FORBIDDEN, exception.code());
    }

    @Test
    void mapsUnknownPublicationTargetToNotFound() {
        FindEvent findEvent = (context, eventId) -> Optional.empty();

        OrganizerEventManagementService organizerService = new OrganizerEventManagementService(
                unusedDefineEvent(),
                unusedUpdateEvent(),
                unusedPublishEvent(),
                unusedWithdrawEvent(),
                findEvent,
                authorizedOwnership());

        EventHttpAdapter adapter = new EventHttpAdapter(
                organizerService,
                findEvent,
                emptyDiscoverEvents(),
                () -> ACTOR);

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter.publishEvent("missing-event", "corr-publish-missing"));

        assertEquals(HttpStatus.NOT_FOUND, exception.status());
        assertEquals(ErrorResponse.CodeEnum.EVENT_NOT_FOUND, exception.code());
        assertEquals("corr-publish-missing", exception.context().correlationId().value());
    }

    @Test
    void mapsAlreadyPublishedPublicationToConflict() {
        EventView published = new EventView(
                "event-1",
                "Platform Day",
                "platform-day",
                Instant.parse("2026-09-01T08:00:00Z"),
                Instant.parse("2026-09-01T10:00:00Z"),
                ZoneId.of("Europe/Copenhagen"),
                EventPublicationState.PUBLISHED,
                new EventOwnerReference("organizer-1"));

        FindEvent findEvent = (context, eventId) -> Optional.of(published);
        PublishEvent publishEvent = (context, eventId) -> {
            throw new EventAlreadyPublishedException(eventId);
        };

        OrganizerEventManagementService organizerService = new OrganizerEventManagementService(
                unusedDefineEvent(),
                unusedUpdateEvent(),
                publishEvent,
                unusedWithdrawEvent(),
                findEvent,
                authorizedOwnership());

        EventHttpAdapter adapter = new EventHttpAdapter(
                organizerService,
                findEvent,
                emptyDiscoverEvents(),
                () -> ACTOR);

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter.publishEvent("event-1", "corr-already-published"));

        assertEquals(HttpStatus.CONFLICT, exception.status());
        assertEquals(ErrorResponse.CodeEnum.EVENT_ALREADY_PUBLISHED, exception.code());
        assertEquals("corr-already-published", exception.context().correlationId().value());
    }

    @Test
    void mapsUnexpectedPublicationFailureToInternalServerError() {
        FindEvent findEvent = (context, eventId) -> Optional.of(EVENT);
        PublishEvent publishEvent = (context, eventId) -> {
            throw new IllegalStateException("database-specific detail");
        };

        OrganizerEventManagementService organizerService = new OrganizerEventManagementService(
                unusedDefineEvent(),
                unusedUpdateEvent(),
                publishEvent,
                unusedWithdrawEvent(),
                findEvent,
                authorizedOwnership());

        EventHttpAdapter adapter = new EventHttpAdapter(
                organizerService,
                findEvent,
                emptyDiscoverEvents(),
                () -> ACTOR);

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter.publishEvent("event-1", "corr-publish-internal"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.status());
        assertEquals(ErrorResponse.CodeEnum.INTERNAL_ERROR, exception.code());
        assertEquals("corr-publish-internal", exception.context().correlationId().value());
    }

    @Test
    void withdrawsEventThroughOrganizerManagementWhenAuthorized() {
        FindEvent findEvent = (context, eventId) -> Optional.of(EVENT);
        WithdrawEvent withdrawEvent = (context, eventId) -> EVENT;

        OrganizerEventManagementService organizerService = new OrganizerEventManagementService(
                unusedDefineEvent(),
                unusedUpdateEvent(),
                unusedPublishEvent(),
                withdrawEvent,
                findEvent,
                authorizedOwnership());

        EventHttpAdapter adapter = new EventHttpAdapter(
                organizerService,
                findEvent,
                emptyDiscoverEvents(),
                () -> ACTOR);

        ResponseEntity<Void> response = adapter.withdrawEvent("event-1", "corr-withdraw");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertEquals("corr-withdraw", response.getHeaders().getFirst(HttpCorrelation.HEADER_NAME));
    }

    @Test
    void mapsUnauthorizedWithdrawToForbidden() {
        FindEvent findEvent = (context, eventId) -> Optional.of(EVENT);

        OrganizerEventManagementService organizerService = new OrganizerEventManagementService(
                unusedDefineEvent(),
                unusedUpdateEvent(),
                unusedPublishEvent(),
                unusedWithdrawEvent(),
                findEvent,
                deniedOwnership());

        EventHttpAdapter adapter = new EventHttpAdapter(
                organizerService,
                findEvent,
                emptyDiscoverEvents(),
                () -> ACTOR);

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter.withdrawEvent("event-1", "corr-forbidden-withdraw"));

        assertEquals(HttpStatus.FORBIDDEN, exception.status());
        assertEquals(ErrorResponse.CodeEnum.FORBIDDEN, exception.code());
    }

    @Test
    void mapsUnknownWithdrawalTargetToNotFound() {
        FindEvent findEvent = (context, eventId) -> Optional.empty();

        OrganizerEventManagementService organizerService = new OrganizerEventManagementService(
                unusedDefineEvent(),
                unusedUpdateEvent(),
                unusedPublishEvent(),
                unusedWithdrawEvent(),
                findEvent,
                authorizedOwnership());

        EventHttpAdapter adapter = new EventHttpAdapter(
                organizerService,
                findEvent,
                emptyDiscoverEvents(),
                () -> ACTOR);

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter.withdrawEvent("missing-event", "corr-withdraw-missing"));

        assertEquals(HttpStatus.NOT_FOUND, exception.status());
        assertEquals(ErrorResponse.CodeEnum.EVENT_NOT_FOUND, exception.code());
        assertEquals("corr-withdraw-missing", exception.context().correlationId().value());
    }

    @Test
    void mapsUnpublishedWithdrawalToConflict() {
        FindEvent findEvent = (context, eventId) -> Optional.of(EVENT);
        WithdrawEvent withdrawEvent = (context, eventId) -> {
            throw new EventNotPublishedException(eventId);
        };

        OrganizerEventManagementService organizerService = new OrganizerEventManagementService(
                unusedDefineEvent(),
                unusedUpdateEvent(),
                unusedPublishEvent(),
                withdrawEvent,
                findEvent,
                authorizedOwnership());

        EventHttpAdapter adapter = new EventHttpAdapter(
                organizerService,
                findEvent,
                emptyDiscoverEvents(),
                () -> ACTOR);

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter.withdrawEvent("event-1", "corr-not-published"));

        assertEquals(HttpStatus.CONFLICT, exception.status());
        assertEquals(ErrorResponse.CodeEnum.EVENT_NOT_PUBLISHED, exception.code());
        assertEquals("corr-not-published", exception.context().correlationId().value());
    }

    @Test
    void mapsAlreadyWithdrawnWithdrawalToConflict() {
        FindEvent findEvent = (context, eventId) -> Optional.of(EVENT);
        WithdrawEvent withdrawEvent = (context, eventId) -> {
            throw new EventWithdrawnException(eventId);
        };

        OrganizerEventManagementService organizerService = new OrganizerEventManagementService(
                unusedDefineEvent(),
                unusedUpdateEvent(),
                unusedPublishEvent(),
                withdrawEvent,
                findEvent,
                authorizedOwnership());

        EventHttpAdapter adapter = new EventHttpAdapter(
                organizerService,
                findEvent,
                emptyDiscoverEvents(),
                () -> ACTOR);

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter.withdrawEvent("event-1", "corr-withdrawn"));

        assertEquals(HttpStatus.CONFLICT, exception.status());
        assertEquals(ErrorResponse.CodeEnum.EVENT_WITHDRAWN, exception.code());
        assertEquals("corr-withdrawn", exception.context().correlationId().value());
    }

    @Test
    void mapsWithdrawnEventUpdateToConflict() {
        FindEvent findEvent = (context, eventId) -> Optional.of(EVENT);
        UpdateEvent updateEvent = (context, command) -> {
            throw new EventWithdrawnException(command.eventId());
        };

        OrganizerEventManagementService organizerService = new OrganizerEventManagementService(
                unusedDefineEvent(),
                updateEvent,
                unusedPublishEvent(),
                unusedWithdrawEvent(),
                findEvent,
                authorizedOwnership());

        EventHttpAdapter adapter = new EventHttpAdapter(
                organizerService,
                findEvent,
                emptyDiscoverEvents(),
                () -> ACTOR);

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter.updateEvent("event-1", updateRequest(), "corr-update-withdrawn"));

        assertEquals(HttpStatus.CONFLICT, exception.status());
        assertEquals(ErrorResponse.CodeEnum.EVENT_WITHDRAWN, exception.code());
    }

    @Test
    void mapsWithdrawnEventPublicationToConflict() {
        FindEvent findEvent = (context, eventId) -> Optional.of(EVENT);
        PublishEvent publishEvent = (context, eventId) -> {
            throw new EventWithdrawnException(eventId);
        };

        OrganizerEventManagementService organizerService = new OrganizerEventManagementService(
                unusedDefineEvent(),
                unusedUpdateEvent(),
                publishEvent,
                unusedWithdrawEvent(),
                findEvent,
                authorizedOwnership());

        EventHttpAdapter adapter = new EventHttpAdapter(
                organizerService,
                findEvent,
                emptyDiscoverEvents(),
                () -> ACTOR);

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter.publishEvent("event-1", "corr-pub-withdrawn"));

        assertEquals(HttpStatus.CONFLICT, exception.status());
        assertEquals(ErrorResponse.CodeEnum.EVENT_WITHDRAWN, exception.code());
    }

    @Test
    void mapsUnexpectedWithdrawalFailureToInternalServerError() {
        FindEvent findEvent = (context, eventId) -> Optional.of(EVENT);
        WithdrawEvent withdrawEvent = (context, eventId) -> {
            throw new IllegalStateException("database-specific detail");
        };

        OrganizerEventManagementService organizerService = new OrganizerEventManagementService(
                unusedDefineEvent(),
                unusedUpdateEvent(),
                unusedPublishEvent(),
                withdrawEvent,
                findEvent,
                authorizedOwnership());

        EventHttpAdapter adapter = new EventHttpAdapter(
                organizerService,
                findEvent,
                emptyDiscoverEvents(),
                () -> ACTOR);

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter.withdrawEvent("event-1", "corr-withdraw-internal"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.status());
        assertEquals(ErrorResponse.CodeEnum.INTERNAL_ERROR, exception.code());
        assertEquals("corr-withdraw-internal", exception.context().correlationId().value());
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

        EventHttpAdapter adapter = new EventHttpAdapter(
                dummyOrganizerService(),
                findEvent,
                emptyDiscoverEvents(),
                () -> ACTOR);

        ResponseEntity<EventResponse> response =
                adapter.findEventById("event-1", "corr-find");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("corr-find", response.getHeaders().getFirst(HttpCorrelation.HEADER_NAME));
        assertEquals("corr-find", capturedContext.get().correlationId().value());
        assertEquals("event-1", capturedEventId.get());
        assertEventResponse(response.getBody());
    }

    @Test
    void mapsUnknownEventToNotFound() {
        FindEvent findEvent = (context, eventId) -> Optional.empty();

        EventHttpAdapter adapter = new EventHttpAdapter(
                dummyOrganizerService(),
                findEvent,
                emptyDiscoverEvents(),
                () -> ACTOR);

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter.findEventById("missing-event", "corr-missing"));

        assertEquals(HttpStatus.NOT_FOUND, exception.status());
        assertEquals(ErrorResponse.CodeEnum.EVENT_NOT_FOUND, exception.code());
        assertEquals("corr-missing", exception.context().correlationId().value());
    }

    @Test
    void mapsUnexpectedFindEventByIdFailureToInternalServerError() {
        FindEvent findEvent = (context, eventId) -> {
            throw new IllegalStateException("database-specific detail");
        };

        EventHttpAdapter adapter = new EventHttpAdapter(
                dummyOrganizerService(),
                findEvent,
                emptyDiscoverEvents(),
                () -> ACTOR);

        EventHttpException exception = assertThrows(
                EventHttpException.class,
                () -> adapter.findEventById("event-1", "corr-find-internal"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.status());
        assertEquals(ErrorResponse.CodeEnum.INTERNAL_ERROR, exception.code());
        assertEquals("corr-find-internal", exception.context().correlationId().value());
    }

    private static DefineEventRequest defineRequest() {
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

    private static UpdateEventRequest updateRequest() {
        return new UpdateEventRequest(
                "Updated Platform Day",
                "updated-platform-day",
                OffsetDateTime.ofInstant(
                        Instant.parse("2026-10-01T09:00:00Z"),
                        ZoneOffset.UTC),
                OffsetDateTime.ofInstant(
                        Instant.parse("2026-10-01T11:00:00Z"),
                        ZoneOffset.UTC),
                "Europe/Oslo");
    }

    private static OrganizerEventManagementService dummyOrganizerService() {
        return new OrganizerEventManagementService(
                unusedDefineEvent(),
                unusedUpdateEvent(),
                unusedPublishEvent(),
                unusedWithdrawEvent(),
                missingFindEvent(),
                authorizedOwnership());
    }

    private static AuthorizeResourceOwnership authorizedOwnership() {
        return (actor, owner) -> AuthorizationDecision.ALLOWED;
    }

    private static AuthorizeResourceOwnership deniedOwnership() {
        return (actor, owner) -> AuthorizationDecision.DENIED;
    }

    private static DefineEvent unusedDefineEvent() {
        return (context, command) -> {
            throw new AssertionError("DefineEvent must not be called");
        };
    }

    private static UpdateEvent unusedUpdateEvent() {
        return (context, command) -> {
            throw new AssertionError("UpdateEvent must not be called");
        };
    }

    private static PublishEvent unusedPublishEvent() {
        return (context, eventId) -> {
            throw new AssertionError("PublishEvent must not be called");
        };
    }

    private static WithdrawEvent unusedWithdrawEvent() {
        return (context, eventId) -> {
            throw new AssertionError("WithdrawEvent must not be called");
        };
    }

    private static FindEvent missingFindEvent() {
        return (context, eventId) -> Optional.empty();
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
        assertEquals(EventResponse.PublicationStateEnum.UNPUBLISHED, response.getPublicationState());
    }
}
