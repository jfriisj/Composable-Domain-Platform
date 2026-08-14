package composable.domain.platform.http;

import composable.domain.platform.core.execution.ExecutionContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public final class ParticipantAuthenticationFailureResponder {

    private static final String RESPONSE_BODY =
            "{\"code\":\"authentication_required\",\"message\":\"Authentication required\"}";

    public void respond(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        ExecutionContext context = HttpCorrelation.establish(
                request.getHeader(HttpCorrelation.HEADER_NAME));

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader(
                HttpCorrelation.HEADER_NAME,
                HttpCorrelation.value(context));
        response.setHeader("WWW-Authenticate", "Basic realm=\"platform\"");
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(RESPONSE_BODY);
    }
}
