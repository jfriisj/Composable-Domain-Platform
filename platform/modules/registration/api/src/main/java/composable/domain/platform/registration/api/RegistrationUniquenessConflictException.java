package composable.domain.platform.registration.api;

public final class RegistrationUniquenessConflictException extends RuntimeException {

    public RegistrationUniquenessConflictException() {
        super("Registration uniqueness conflict");
    }
}
