package composable.domain.platform.registration.api;

public record RegistrationView(
        String registrationId,
        RegistrantReference registrantReference,
        TargetReference targetReference) {
}
