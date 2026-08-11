package composable.domain.platform.registration.api;

public record CreateRegistrationCommand(
        String registrationId,
        RegistrantReference registrantReference,
        TargetReference targetReference) {
}
