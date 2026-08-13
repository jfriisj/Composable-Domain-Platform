package composable.domain.platform.composition.eventregistration;

import composable.domain.platform.core.execution.ExecutionContext;

public interface CreateParticipantEventRegistration {

    ParticipantEventRegistrationView create(
            ExecutionContext context,
            AuthenticatedActorReference actorReference,
            CreateParticipantEventRegistrationCommand command);
}
