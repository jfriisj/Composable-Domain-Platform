package composable.domain.platform.composition.eventregistration;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.FindEvent;
import composable.domain.platform.registration.api.FindRegistrationsByTarget;
import composable.domain.platform.registration.api.RegistrationLifecycle;
import composable.domain.platform.registration.api.RegistrationView;
import composable.domain.platform.registration.api.TargetReference;
import composable.domain.platform.security.api.AuthenticatedActorReference;
import composable.domain.platform.security.api.AuthorizationDecision;
import composable.domain.platform.security.api.AuthorizeResourceOwnership;
import composable.domain.platform.security.api.ResourceOwnerReference;
import java.util.List;
import java.util.Objects;

public final class OrganizerEventRegistrationService implements FindOrganizerEventRegistrations {

    static final String EVENT_NAMESPACE = "event";

    private final FindEvent findEvent;
    private final FindRegistrationsByTarget findRegistrationsByTarget;
    private final AuthorizeResourceOwnership authorizeResourceOwnership;

    public OrganizerEventRegistrationService(
            FindEvent findEvent,
            FindRegistrationsByTarget findRegistrationsByTarget,
            AuthorizeResourceOwnership authorizeResourceOwnership) {
        this.findEvent = Objects.requireNonNull(findEvent, "findEvent must not be null");
        this.findRegistrationsByTarget =
                Objects.requireNonNull(findRegistrationsByTarget, "findRegistrationsByTarget must not be null");
        this.authorizeResourceOwnership =
                Objects.requireNonNull(authorizeResourceOwnership, "authorizeResourceOwnership must not be null");
    }

    @Override
    public List<OrganizerEventRegistrationView> findByEventId(
            ExecutionContext context,
            AuthenticatedActorReference actorReference,
            String eventId) {
        Objects.requireNonNull(context, "context must not be null");
        if (actorReference == null) {
            throw new IllegalArgumentException("actorReference must not be null");
        }
        if (eventId == null || eventId.isBlank()) {
            throw new InvalidOrganizerEventRegistrationRequestException("eventId must not be blank");
        }

        EventView event = findEvent.findById(context, eventId)
                .orElseThrow(UnknownEventForRegistrationException::new);

        if (event.owner().isEmpty()) {
            throw new OrganizerEventRegistrationAuthorizationDeniedException(
                    "Event %s has no owner".formatted(eventId));
        }

        ResourceOwnerReference resourceOwner =
                new ResourceOwnerReference(event.owner().get().reference());

        AuthorizationDecision decision = Objects.requireNonNull(
                authorizeResourceOwnership.authorize(actorReference, resourceOwner),
                "authorization decision must not be null");

        if (decision != AuthorizationDecision.ALLOWED) {
            throw new OrganizerEventRegistrationAuthorizationDeniedException(
                    "Actor is not the owner of event " + eventId);
        }

        return findRegistrationsByTarget.findByTarget(
                        context,
                        new TargetReference(EVENT_NAMESPACE, eventId))
                .stream()
                .map(OrganizerEventRegistrationService::toView)
                .toList();
    }

    private static OrganizerEventRegistrationView toView(RegistrationView registration) {
        return new OrganizerEventRegistrationView(
                registration.registrationId(),
                registration.targetReference().reference(),
                toLifecycle(registration.lifecycle()));
    }

    private static EventRegistrationLifecycle toLifecycle(RegistrationLifecycle lifecycle) {
        return switch (lifecycle) {
            case ACTIVE -> EventRegistrationLifecycle.ACTIVE;
            case CANCELLED -> EventRegistrationLifecycle.CANCELLED;
        };
    }
}
