package composable.domain.platform.composition.eventregistration;

public record CreateEventRegistrationCommand(
        String registrationId,
        String eventId,
        String participantReference) {
}
