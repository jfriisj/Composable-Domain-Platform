package composable.domain.platform.waitlist.api;

public record WaitlistEventReference(String reference) {

    public WaitlistEventReference {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException(
                    "Event reference must not be blank");
        }
    }
}
