package composable.domain.platform.composition.eventregistration;

public record AuthenticatedActorReference(String reference) {

    public AuthenticatedActorReference {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("reference must not be blank");
        }
    }
}
