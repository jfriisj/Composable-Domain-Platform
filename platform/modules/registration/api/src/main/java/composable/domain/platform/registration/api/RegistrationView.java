package composable.domain.platform.registration.api;

public record RegistrationView(
        String registrationId,
        RegistrantReference registrantReference,
        TargetReference targetReference,
        RegistrationLifecycle lifecycle) {

    public RegistrationView(
            String registrationId,
            RegistrantReference registrantReference,
            TargetReference targetReference) {
        this(
                registrationId,
                registrantReference,
                targetReference,
                RegistrationLifecycle.ACTIVE);
    }
}
