package composable.domain.platform.security.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import composable.domain.platform.security.api.AuthenticatedActorReference;
import composable.domain.platform.security.api.AuthorizationDecision;
import composable.domain.platform.security.api.ResourceOwnerReference;
import org.junit.jupiter.api.Test;

class ResourceOwnershipAuthorizationTest {

    private final ResourceOwnershipAuthorization authorization =
            new ResourceOwnershipAuthorization();

    @Test
    void allowsEqualOpaqueActorAndOwnerReferences() {
        assertEquals(
                AuthorizationDecision.ALLOWED,
                authorization.authorize(
                        new AuthenticatedActorReference("opaque-a"),
                        new ResourceOwnerReference("opaque-a")));
    }

    @Test
    void deniesDifferentOpaqueActorAndOwnerReferences() {
        assertEquals(
                AuthorizationDecision.DENIED,
                authorization.authorize(
                        new AuthenticatedActorReference("opaque-a"),
                        new ResourceOwnerReference("opaque-b")));
    }

    @Test
    void rejectsMissingAuthorizationInputs() {
        assertThrows(
                NullPointerException.class,
                () -> authorization.authorize(
                        null,
                        new ResourceOwnerReference("opaque-a")));
        assertThrows(
                NullPointerException.class,
                () -> authorization.authorize(
                        new AuthenticatedActorReference("opaque-a"),
                        null));
    }
}
