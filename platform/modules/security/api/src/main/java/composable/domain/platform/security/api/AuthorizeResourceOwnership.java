package composable.domain.platform.security.api;

@FunctionalInterface
public interface AuthorizeResourceOwnership {

    AuthorizationDecision authorize(
            AuthenticatedActorReference actor,
            ResourceOwnerReference owner);
}
