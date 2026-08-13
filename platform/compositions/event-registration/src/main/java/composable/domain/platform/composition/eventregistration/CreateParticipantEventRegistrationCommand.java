package composable.domain.platform.composition.eventregistration;

public record CreateParticipantEventRegistrationCommand(
        String registrationId,
        String eventId) {
}
