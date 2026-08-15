package composable.domain.platform.security.api;

public record ResourceOwnerReference(String reference) {

    public ResourceOwnerReference {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("reference must not be blank");
        }
    }
}
