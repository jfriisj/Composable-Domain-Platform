package composable.domain.platform.composition.eventregistration;

public record ParticipantEventRegistrationView(
        String registrationId,
        String eventId,
        EventRegistrationLifecycle lifecycle) {
}
