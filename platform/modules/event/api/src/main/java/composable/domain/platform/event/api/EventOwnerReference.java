package composable.domain.platform.event.api;

public record EventOwnerReference(String reference) {

    public EventOwnerReference {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("reference must not be blank");
        }
    }
}
