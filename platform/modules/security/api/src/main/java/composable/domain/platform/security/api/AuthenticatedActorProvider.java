package composable.domain.platform.security.api;

@FunctionalInterface
public interface AuthenticatedActorProvider {

    AuthenticatedActorReference authenticatedActor();
}
