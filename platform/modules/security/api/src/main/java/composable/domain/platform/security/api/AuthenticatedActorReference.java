package composable.domain.platform.security.api;

public record AuthenticatedActorReference(String reference) {

    public AuthenticatedActorReference {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("reference must not be blank");
        }
    }
}
