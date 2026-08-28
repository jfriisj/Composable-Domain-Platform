package composable.domain.platform.http;

import composable.domain.platform.composition.eventregistration.CancelParticipantEventRegistration;
import composable.domain.platform.composition.eventregistration.CreateParticipantEventRegistration;
import composable.domain.platform.composition.eventregistration.CreateParticipantEventRegistrationCommand;
import composable.domain.platform.composition.eventregistration.EventNotPublishedForRegistrationException;
import composable.domain.platform.composition.eventregistration.EventRegistrationClosedException;
import composable.domain.platform.composition.eventregistration.EventRegistrationAuthorizationDeniedException;
import composable.domain.platform.composition.eventregistration.EventRegistrationLifecycle;
import composable.domain.platform.composition.eventregistration.EventRegistrationUniquenessConflictException;
import composable.domain.platform.composition.eventregistration.FindOrganizerEventRegistrations;
import composable.domain.platform.composition.eventregistration.FindParticipantEventRegistration;
import composable.domain.platform.composition.eventregistration.InvalidEventRegistrationDefinitionException;
import composable.domain.platform.composition.eventregistration.InvalidOrganizerEventRegistrationRequestException;
import composable.domain.platform.composition.eventregistration.OrganizerEventRegistrationAuthorizationDeniedException;
import composable.domain.platform.composition.eventregistration.OrganizerEventRegistrationView;
import composable.domain.platform.composition.eventregistration.ParticipantEventRegistrationView;
import composable.domain.platform.composition.eventregistration.UnknownEventForRegistrationException;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.http.eventregistration.generated.api.EventRegistrationApi;
import composable.domain.platform.http.eventregistration.generated.model.CreateEventRegistrationRequest;
import composable.domain.platform.http.eventregistration.generated.model.EventRegistrationResponse;
import composable.domain.platform.security.api.AuthenticatedActorProvider;
import composable.domain.platform.security.api.AuthenticatedActorReference;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventRegistrationHttpAdapter implements EventRegistrationApi {

    private final CreateParticipantEventRegistration createEventRegistration;
    private final FindParticipantEventRegistration findEventRegistration;
    private final CancelParticipantEventRegistration cancelEventRegistration;
    private final FindOrganizerEventRegistrations findOrganizerEventRegistrations;
    private final AuthenticatedActorProvider authenticatedActorProvider;

    public EventRegistrationHttpAdapter(
            CreateParticipantEventRegistration createEventRegistration,
            FindParticipantEventRegistration findEventRegistration,
            CancelParticipantEventRegistration cancelEventRegistration,
            FindOrganizerEventRegistrations findOrganizerEventRegistrations,
            AuthenticatedActorProvider authenticatedActorProvider) {
        this.createEventRegistration =
                Objects.requireNonNull(
                        createEventRegistration,
                        "createEventRegistration must not be null");
        this.findEventRegistration =
                Objects.requireNonNull(
                        findEventRegistration,
                        "findEventRegistration must not be null");
        this.cancelEventRegistration =
                Objects.requireNonNull(
                        cancelEventRegistration,
                        "cancelEventRegistration must not be null");
        this.findOrganizerEventRegistrations =
                Objects.requireNonNull(
                        findOrganizerEventRegistrations,
                        "findOrganizerEventRegistrations must not be null");
        this.authenticatedActorProvider =
                Objects.requireNonNull(
                        authenticatedActorProvider,
                        "authenticatedActorProvider must not be null");
    }

    @Override
    public ResponseEntity<EventRegistrationResponse> createEventRegistration(
            CreateEventRegistrationRequest request,
            String suppliedCorrelationId) {
        ExecutionContext context = EventRegistrationHttpCorrelation.establish(suppliedCorrelationId);

        try {
            AuthenticatedActorReference actorReference =
                    authenticatedActorProvider.authenticatedActor();
            ParticipantEventRegistrationView registration =
                    createEventRegistration.create(
                            context,
                            actorReference,
                            new CreateParticipantEventRegistrationCommand(
                                    request.getRegistrationId(),
                                    request.getEventId()));
            return response(HttpStatus.CREATED, context, registration);
        } catch (InvalidEventRegistrationDefinitionException exception) {
            throw EventRegistrationHttpException.invalidDefinition(context);
        } catch (UnknownEventForRegistrationException exception) {
            throw EventRegistrationHttpException.eventNotFound(context);
        } catch (EventNotPublishedForRegistrationException exception) {
            throw EventRegistrationHttpException.eventNotPublished(context);
        } catch (EventRegistrationClosedException exception) {
            throw EventRegistrationHttpException.eventRegistrationClosed(context);
        } catch (EventRegistrationUniquenessConflictException exception) {
            throw EventRegistrationHttpException.conflict(context);
        } catch (RuntimeException exception) {
            throw EventRegistrationHttpException.internal(context, exception);
        }
    }

    @Override
    public ResponseEntity<EventRegistrationResponse> findEventRegistrationById(
            String registrationId,
            String suppliedCorrelationId) {
        ExecutionContext context = EventRegistrationHttpCorrelation.establish(suppliedCorrelationId);

        try {
            AuthenticatedActorReference actorReference =
                    authenticatedActorProvider.authenticatedActor();
            ParticipantEventRegistrationView registration =
                    findEventRegistration.findById(
                                    context,
                                    actorReference,
                                    registrationId)
                            .orElseThrow(
                                    () -> EventRegistrationHttpException.registrationNotFound(context));
            return response(HttpStatus.OK, context, registration);
        } catch (EventRegistrationAuthorizationDeniedException exception) {
            throw EventRegistrationHttpException.registrationNotFound(context);
        } catch (EventRegistrationHttpException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw EventRegistrationHttpException.internal(context, exception);
        }
    }

    @Override
    public ResponseEntity<EventRegistrationResponse> cancelEventRegistration(
            String registrationId,
            String suppliedCorrelationId) {
        ExecutionContext context = EventRegistrationHttpCorrelation.establish(suppliedCorrelationId);

        try {
            AuthenticatedActorReference actorReference =
                    authenticatedActorProvider.authenticatedActor();
            ParticipantEventRegistrationView registration =
                    cancelEventRegistration.cancel(
                                    context,
                                    actorReference,
                                    registrationId)
                            .orElseThrow(
                                    () -> EventRegistrationHttpException.registrationNotFound(context));
            return response(HttpStatus.OK, context, registration);
        } catch (EventRegistrationAuthorizationDeniedException exception) {
            throw EventRegistrationHttpException.registrationNotFound(context);
        } catch (EventRegistrationHttpException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw EventRegistrationHttpException.internal(context, exception);
        }
    }

    @Override
    public ResponseEntity<List<EventRegistrationResponse>> findOrganizerEventRegistrations(
            String eventId,
            String suppliedCorrelationId) {
        ExecutionContext context = EventRegistrationHttpCorrelation.establish(suppliedCorrelationId);

        try {
            AuthenticatedActorReference actorReference =
                    authenticatedActorProvider.authenticatedActor();
            List<EventRegistrationResponse> registrations =
                    findOrganizerEventRegistrations.findByEventId(
                                    context,
                                    actorReference,
                                    eventId)
                            .stream()
                            .map(EventRegistrationHttpAdapter::toOrganizerResponse)
                            .toList();
            return response(HttpStatus.OK, context, registrations);
        } catch (InvalidOrganizerEventRegistrationRequestException exception) {
            throw EventRegistrationHttpException.invalidRequest(context, exception.getMessage());
        } catch (OrganizerEventRegistrationAuthorizationDeniedException exception) {
            throw EventRegistrationHttpException.forbidden(context);
        } catch (UnknownEventForRegistrationException exception) {
            throw EventRegistrationHttpException.eventNotFound(context);
        } catch (EventRegistrationHttpException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw EventRegistrationHttpException.internal(context, exception);
        }
    }

    private static ResponseEntity<EventRegistrationResponse> response(
            HttpStatus status,
            ExecutionContext context,
            ParticipantEventRegistrationView registration) {
        return ResponseEntity.status(status)
                .header(EventRegistrationHttpCorrelation.HEADER_NAME, EventRegistrationHttpCorrelation.value(context))
                .body(toResponse(registration));
    }

    private static ResponseEntity<List<EventRegistrationResponse>> response(
            HttpStatus status,
            ExecutionContext context,
            List<EventRegistrationResponse> registrations) {
        return ResponseEntity.status(status)
                .header(EventRegistrationHttpCorrelation.HEADER_NAME, EventRegistrationHttpCorrelation.value(context))
                .body(registrations);
    }

    private static EventRegistrationResponse toResponse(
            ParticipantEventRegistrationView registration) {
        return new EventRegistrationResponse(
                registration.registrationId(),
                registration.eventId(),
                lifecycle(registration.lifecycle()));
    }

    private static EventRegistrationResponse toOrganizerResponse(
            OrganizerEventRegistrationView registration) {
        return new EventRegistrationResponse(
                registration.registrationId(),
                registration.eventId(),
                lifecycle(registration.lifecycle()));
    }

    private static composable.domain.platform.http.eventregistration.generated.model.EventRegistrationLifecycle lifecycle(
            EventRegistrationLifecycle lifecycle) {
        return switch (lifecycle) {
            case ACTIVE ->
                    composable.domain.platform.http.eventregistration.generated.model.EventRegistrationLifecycle.ACTIVE;
            case CANCELLED ->
                    composable.domain.platform.http.eventregistration.generated.model.EventRegistrationLifecycle.CANCELLED;
        };
    }
}
