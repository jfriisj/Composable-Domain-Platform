package composable.domain.platform.http;

import composable.domain.platform.composition.eventmanagement.DefineOrganizerEventCommand;
import composable.domain.platform.composition.eventmanagement.EventManagementAuthorizationDeniedException;
import composable.domain.platform.composition.eventmanagement.OrganizerEventManagementService;
import composable.domain.platform.composition.eventmanagement.UpdateOrganizerEventCommand;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.DiscoverEvents;
import composable.domain.platform.event.api.EventAlreadyDefinedException;
import composable.domain.platform.event.api.EventAlreadyPublishedException;
import composable.domain.platform.event.api.EventNotFoundException;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.FindEvent;
import composable.domain.platform.event.api.InvalidEventDefinitionException;
import composable.domain.platform.http.event.generated.api.EventApi;
import composable.domain.platform.http.event.generated.model.DefineEventRequest;
import composable.domain.platform.http.event.generated.model.EventResponse;
import composable.domain.platform.http.event.generated.model.UpdateEventRequest;
import composable.domain.platform.security.api.AuthenticatedActorProvider;
import composable.domain.platform.security.api.AuthenticatedActorReference;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventHttpAdapter implements EventApi {

    private final OrganizerEventManagementService organizerEventManagement;
    private final FindEvent findEvent;
    private final DiscoverEvents discoverEvents;
    private final AuthenticatedActorProvider authenticatedActorProvider;

    public EventHttpAdapter(
            OrganizerEventManagementService organizerEventManagement,
            FindEvent findEvent,
            DiscoverEvents discoverEvents,
            AuthenticatedActorProvider authenticatedActorProvider) {
        this.organizerEventManagement = Objects.requireNonNull(
                organizerEventManagement,
                "organizerEventManagement must not be null");
        this.findEvent = Objects.requireNonNull(findEvent, "findEvent must not be null");
        this.discoverEvents = Objects.requireNonNull(discoverEvents, "discoverEvents must not be null");
        this.authenticatedActorProvider = Objects.requireNonNull(
                authenticatedActorProvider,
                "authenticatedActorProvider must not be null");
    }

    @Override
    public ResponseEntity<EventResponse> defineEvent(
            DefineEventRequest request,
            String suppliedCorrelationId) {
        ExecutionContext context = HttpCorrelation.establish(suppliedCorrelationId);

        try {
            AuthenticatedActorReference actorReference =
                    authenticatedActorProvider.authenticatedActor();
            DefineOrganizerEventCommand command = toDefineCommand(request);
            EventView event = organizerEventManagement.define(context, actorReference, command);
            return response(HttpStatus.CREATED, context, toResponse(event));
        } catch (InvalidEventDefinitionException | DateTimeException exception) {
            throw EventHttpException.invalidDefinition(context);
        } catch (EventAlreadyDefinedException exception) {
            throw EventHttpException.alreadyDefined(context);
        } catch (RuntimeException exception) {
            throw EventHttpException.internal(context, exception);
        }
    }

    @Override
    public ResponseEntity<EventResponse> updateEvent(
            String eventId,
            UpdateEventRequest request,
            String suppliedCorrelationId) {
        ExecutionContext context = HttpCorrelation.establish(suppliedCorrelationId);

        try {
            AuthenticatedActorReference actorReference =
                    authenticatedActorProvider.authenticatedActor();
            UpdateOrganizerEventCommand command = toUpdateCommand(eventId, request);
            EventView event = organizerEventManagement.update(context, actorReference, command);
            return response(HttpStatus.OK, context, toResponse(event));
        } catch (InvalidEventDefinitionException | DateTimeException exception) {
            throw EventHttpException.invalidDefinition(context);
        } catch (EventNotFoundException exception) {
            throw EventHttpException.notFound(context);
        } catch (EventManagementAuthorizationDeniedException exception) {
            throw EventHttpException.forbidden(context);
        } catch (EventAlreadyPublishedException exception) {
            throw EventHttpException.alreadyPublished(context);
        } catch (RuntimeException exception) {
            throw EventHttpException.internal(context, exception);
        }
    }

    @Override
    public ResponseEntity<List<EventResponse>> discoverEvents(String suppliedCorrelationId) {
        ExecutionContext context = HttpCorrelation.establish(suppliedCorrelationId);

        try {
            List<EventResponse> events = discoverEvents.discover(context).stream()
                    .map(EventHttpAdapter::toResponse)
                    .toList();
            return response(HttpStatus.OK, context, events);
        } catch (RuntimeException exception) {
            throw EventHttpException.internal(context, exception);
        }
    }

    @Override
    public ResponseEntity<EventResponse> findEventById(
            String eventId,
            String suppliedCorrelationId) {
        ExecutionContext context = HttpCorrelation.establish(suppliedCorrelationId);

        try {
            EventView event = findEvent.findById(context, eventId)
                    .orElseThrow(() -> EventHttpException.notFound(context));
            return response(HttpStatus.OK, context, toResponse(event));
        } catch (EventHttpException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw EventHttpException.internal(context, exception);
        }
    }

    @Override
    public ResponseEntity<Void> publishEvent(
            String eventId,
            String suppliedCorrelationId) {
        ExecutionContext context = HttpCorrelation.establish(suppliedCorrelationId);

        try {
            AuthenticatedActorReference actorReference =
                    authenticatedActorProvider.authenticatedActor();
            organizerEventManagement.publish(context, actorReference, eventId);
            return ResponseEntity.noContent()
                    .header(HttpCorrelation.HEADER_NAME, HttpCorrelation.value(context))
                    .build();
        } catch (EventNotFoundException exception) {
            throw EventHttpException.notFound(context);
        } catch (EventManagementAuthorizationDeniedException exception) {
            throw EventHttpException.forbidden(context);
        } catch (EventAlreadyPublishedException exception) {
            throw EventHttpException.alreadyPublished(context);
        } catch (RuntimeException exception) {
            throw EventHttpException.internal(context, exception);
        }
    }

    private static DefineOrganizerEventCommand toDefineCommand(DefineEventRequest request) {
        if (request == null) {
            throw new InvalidEventDefinitionException();
        }
        return new DefineOrganizerEventCommand(
                request.getEventId(),
                request.getName(),
                request.getSlug(),
                request.getStartsAt() != null ? request.getStartsAt().toInstant() : null,
                request.getEndsAt() != null ? request.getEndsAt().toInstant() : null,
                request.getTimezone() != null ? ZoneId.of(request.getTimezone()) : null);
    }

    private static UpdateOrganizerEventCommand toUpdateCommand(
            String eventId,
            UpdateEventRequest request) {
        if (request == null) {
            throw new InvalidEventDefinitionException();
        }
        return new UpdateOrganizerEventCommand(
                eventId,
                request.getName(),
                request.getSlug(),
                request.getStartsAt() != null ? request.getStartsAt().toInstant() : null,
                request.getEndsAt() != null ? request.getEndsAt().toInstant() : null,
                request.getTimezone() != null ? ZoneId.of(request.getTimezone()) : null);
    }

    private static EventResponse toResponse(EventView event) {
        return new EventResponse(
                event.eventId(),
                event.name(),
                event.slug(),
                event.startsAt().atOffset(ZoneOffset.UTC),
                event.endsAt().atOffset(ZoneOffset.UTC),
                event.timezone().getId());
    }

    private static <T> ResponseEntity<T> response(
            HttpStatus status,
            ExecutionContext context,
            T body) {
        return ResponseEntity.status(status)
                .header(HttpCorrelation.HEADER_NAME, HttpCorrelation.value(context))
                .body(body);
    }
}
