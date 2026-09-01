package composable.domain.platform.waitlist.api;

import java.util.Objects;

public record WaitlistParticipationView(
        String waitlistParticipationId,
        WaitlistParticipantReference participantReference,
        WaitlistEventReference eventReference) {

    public WaitlistParticipationView {
        if (waitlistParticipationId == null || waitlistParticipationId.isBlank()) {
            throw new IllegalArgumentException(
                    "waitlistParticipationId must not be blank");
        }
        Objects.requireNonNull(
                participantReference,
                "participantReference must not be null");
        Objects.requireNonNull(
                eventReference,
                "eventReference must not be null");
    }
}
