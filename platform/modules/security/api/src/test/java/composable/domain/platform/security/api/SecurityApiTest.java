package composable.domain.platform.security.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SecurityApiTest {

    @Test
    void actorReferenceRejectsMissingValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthenticatedActorReference(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthenticatedActorReference(" "));
    }

    @Test
    void resourceOwnerReferenceRejectsMissingValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ResourceOwnerReference(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ResourceOwnerReference(" "));
    }

    @Test
    void ownershipAuthorizationContractUsesOpaqueSecurityValues() {
        AuthorizeResourceOwnership authorize =
                (actor, owner) -> actor.reference().equals(owner.reference())
                        ? AuthorizationDecision.ALLOWED
                        : AuthorizationDecision.DENIED;

        assertEquals(
                AuthorizationDecision.ALLOWED,
                authorize.authorize(
                        new AuthenticatedActorReference("opaque-a"),
                        new ResourceOwnerReference("opaque-a")));
        assertEquals(
                AuthorizationDecision.DENIED,
                authorize.authorize(
                        new AuthenticatedActorReference("opaque-a"),
                        new ResourceOwnerReference("opaque-b")));
    }

    @Test
    void authenticationRequiredFailureIsFrameworkNeutral() {
        assertEquals(
                "authentication required",
                new AuthenticationRequiredException().getMessage());
    }
}
