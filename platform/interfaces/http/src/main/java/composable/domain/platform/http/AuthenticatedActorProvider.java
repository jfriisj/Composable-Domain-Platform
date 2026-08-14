package composable.domain.platform.http;

import composable.domain.platform.composition.eventregistration.AuthenticatedActorReference;

@FunctionalInterface
public interface AuthenticatedActorProvider {

    AuthenticatedActorReference authenticatedActor();
}
