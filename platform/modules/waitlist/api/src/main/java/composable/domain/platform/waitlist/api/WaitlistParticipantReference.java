package composable.domain.platform.waitlist.api;

public record WaitlistParticipantReference(String reference) {

    public WaitlistParticipantReference {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException(
                    "participant reference must not be blank");
        }
    }
}
