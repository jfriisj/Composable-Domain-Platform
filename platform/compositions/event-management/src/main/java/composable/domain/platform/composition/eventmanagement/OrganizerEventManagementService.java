package composable.domain.platform.composition.eventmanagement;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.DefineEvent;
import composable.domain.platform.event.api.DefineEventCommand;
import composable.domain.platform.event.api.EventNotFoundException;
import composable.domain.platform.event.api.EventOwnerReference;
import composable.domain.platform.event.api.EventRegistrationAvailability;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.FindEvent;
import composable.domain.platform.event.api.InvalidEventDefinitionException;
import composable.domain.platform.event.api.PublishEvent;
import composable.domain.platform.event.api.SetEventRegistrationAvailability;
import composable.domain.platform.event.api.UpdateEvent;
import composable.domain.platform.event.api.UpdateEventCommand;
import composable.domain.platform.event.api.WithdrawEvent;
import composable.domain.platform.security.api.AuthenticatedActorReference;
import composable.domain.platform.security.api.AuthorizationDecision;
import composable.domain.platform.security.api.AuthorizeResourceOwnership;
import composable.domain.platform.security.api.ResourceOwnerReference;
import java.util.Objects;

public final class OrganizerEventManagementService {

    private final DefineEvent defineEvent;
    private final UpdateEvent updateEvent;
    private final PublishEvent publishEvent;
    private final WithdrawEvent withdrawEvent;
    private final SetEventRegistrationAvailability setEventRegistrationAvailability;
    private final FindEvent findEvent;
    private final AuthorizeResourceOwnership authorizeResourceOwnership;

    public OrganizerEventManagementService(
            DefineEvent defineEvent,
            UpdateEvent updateEvent,
            PublishEvent publishEvent,
            WithdrawEvent withdrawEvent,
            SetEventRegistrationAvailability setEventRegistrationAvailability,
            FindEvent findEvent,
            AuthorizeResourceOwnership authorizeResourceOwnership) {
        this.defineEvent = Objects.requireNonNull(defineEvent, "defineEvent must not be null");
        this.updateEvent = Objects.requireNonNull(updateEvent, "updateEvent must not be null");
        this.publishEvent = Objects.requireNonNull(publishEvent, "publishEvent must not be null");
        this.withdrawEvent = Objects.requireNonNull(withdrawEvent, "withdrawEvent must not be null");
        this.setEventRegistrationAvailability = Objects.requireNonNull(
                setEventRegistrationAvailability,
                "setEventRegistrationAvailability must not be null");
        this.findEvent = Objects.requireNonNull(findEvent, "findEvent must not be null");
        this.authorizeResourceOwnership = Objects.requireNonNull(
                authorizeResourceOwnership,
                "authorizeResourceOwnership must not be null");
    }

    public EventView define(
            ExecutionContext context,
            AuthenticatedActorReference actorReference,
            DefineOrganizerEventCommand command) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(actorReference, "actorReference must not be null");

        if (command == null) {
            throw new InvalidEventDefinitionException();
        }

        EventOwnerReference owner = new EventOwnerReference(actorReference.reference());
        DefineEventCommand defineCommand = new DefineEventCommand(
                command.eventId(),
                command.name(),
                command.slug(),
                command.startsAt(),
                command.endsAt(),
                command.timezone(),
                owner);

        return defineEvent.define(context, defineCommand);
    }

    public EventView update(
            ExecutionContext context,
            AuthenticatedActorReference actorReference,
            UpdateOrganizerEventCommand command) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(actorReference, "actorReference must not be null");

        if (command == null || command.eventId() == null || command.eventId().isBlank()) {
            throw new InvalidEventDefinitionException();
        }

        EventView existing = findEvent.findById(context, command.eventId())
                .orElseThrow(() -> new EventNotFoundException(command.eventId()));

        authorizeOwnership(actorReference, existing);

        UpdateEventCommand updateCommand = new UpdateEventCommand(
                command.eventId(),
                command.name(),
                command.slug(),
                command.startsAt(),
                command.endsAt(),
                command.timezone());

        return updateEvent.update(context, updateCommand);
    }

    public EventView publish(
            ExecutionContext context,
            AuthenticatedActorReference actorReference,
            String eventId) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(actorReference, "actorReference must not be null");

        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }

        EventView existing = findEvent.findById(context, eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        authorizeOwnership(actorReference, existing);

        return publishEvent.publish(context, eventId);
    }

    public EventView withdraw(
            ExecutionContext context,
            AuthenticatedActorReference actorReference,
            String eventId) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(actorReference, "actorReference must not be null");

        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }

        EventView existing = findEvent.findById(context, eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        authorizeOwnership(actorReference, existing);

        return withdrawEvent.withdraw(context, eventId);
    }

    public EventView setRegistrationAvailability(
            ExecutionContext context,
            AuthenticatedActorReference actorReference,
            String eventId,
            EventRegistrationAvailability availability) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(actorReference, "actorReference must not be null");
        Objects.requireNonNull(availability, "availability must not be null");

        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }

        EventView existing = findEvent.findById(context, eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        authorizeOwnership(actorReference, existing);

        return setEventRegistrationAvailability.setRegistrationAvailability(
                context,
                eventId,
                availability);
    }

    private void authorizeOwnership(
            AuthenticatedActorReference actorReference,
            EventView existing) {
        if (existing.owner().isEmpty()) {
            throw new EventManagementAuthorizationDeniedException();
        }

        ResourceOwnerReference resourceOwner =
                new ResourceOwnerReference(existing.owner().get().reference());

        AuthorizationDecision decision =
                authorizeResourceOwnership.authorize(actorReference, resourceOwner);

        if (decision != AuthorizationDecision.ALLOWED) {
            throw new EventManagementAuthorizationDeniedException();
        }
    }
}
