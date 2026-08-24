package composable.domain.platform.composition.eventregistration;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.security.api.AuthenticatedActorReference;
import java.util.List;

@FunctionalInterface
public interface FindOrganizerEventRegistrations {

    List<OrganizerEventRegistrationView> findByEventId(
            ExecutionContext context,
            AuthenticatedActorReference actorReference,
            String eventId);
}
