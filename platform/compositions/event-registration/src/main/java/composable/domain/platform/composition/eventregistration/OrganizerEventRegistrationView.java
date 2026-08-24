package composable.domain.platform.composition.eventregistration;

import java.util.Objects;

public record OrganizerEventRegistrationView(
        String registrationId,
        String eventId,
        EventRegistrationLifecycle lifecycle) {

    public OrganizerEventRegistrationView {
        Objects.requireNonNull(registrationId, "registrationId must not be null");
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(lifecycle, "lifecycle must not be null");
    }
}
