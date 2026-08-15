package composable.domain.platform.security.impl;

import composable.domain.platform.security.api.AuthenticatedActorReference;
import composable.domain.platform.security.api.AuthorizationDecision;
import composable.domain.platform.security.api.AuthorizeResourceOwnership;
import composable.domain.platform.security.api.ResourceOwnerReference;
import java.util.Objects;

public final class ResourceOwnershipAuthorization
        implements AuthorizeResourceOwnership {

    @Override
    public AuthorizationDecision authorize(
            AuthenticatedActorReference actor,
            ResourceOwnerReference owner) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(owner, "owner must not be null");

        return actor.reference().equals(owner.reference())
                ? AuthorizationDecision.ALLOWED
                : AuthorizationDecision.DENIED;
    }
}
