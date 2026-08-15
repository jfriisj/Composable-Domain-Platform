package composable.domain.platform.security.api;

public final class AuthenticationRequiredException extends RuntimeException {

    public AuthenticationRequiredException() {
        super("authentication required");
    }
}
