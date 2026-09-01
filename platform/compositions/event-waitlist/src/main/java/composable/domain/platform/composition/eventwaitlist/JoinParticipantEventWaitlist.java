package composable.domain.platform.composition.eventwaitlist;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.security.api.AuthenticatedActorReference;

@FunctionalInterface
public interface JoinParticipantEventWaitlist {

    ParticipantEventWaitlistView join(
            ExecutionContext context,
            AuthenticatedActorReference actorReference,
            String eventId);
}
