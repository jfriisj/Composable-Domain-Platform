package composable.domain.platform.waitlist.domain;

public record WaitlistParticipation(
        String id,
        String participantReference,
        String eventReference) {

    public WaitlistParticipation {
        requireNonBlank(id, "id");
        requireNonBlank(participantReference, "participantReference");
        requireNonBlank(eventReference, "eventReference");
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
