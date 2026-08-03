package composable.domain.platform.composition.eventregistration;

public record EventRegistrationView(
        String registrationId,
        String eventId,
        String participantReference) {
}
