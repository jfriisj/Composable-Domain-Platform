package composable.domain.platform.composition.eventregistration;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.security.api.AuthenticatedActorReference;

public interface CreateParticipantEventRegistration {

    ParticipantEventRegistrationView create(
            ExecutionContext context,
            AuthenticatedActorReference actorReference,
            CreateParticipantEventRegistrationCommand command);
}
