package composable.domain.platform.security.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class ParticipantAuthenticationFailureResponder {

    static final String CORRELATION_HEADER = "X-Correlation-Id";

    private static final String RESPONSE_BODY =
            "{\"code\":\"authentication_required\","
                    + "\"message\":\"Authentication required\"}";

    void respond(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        String suppliedCorrelationId = request.getHeader(CORRELATION_HEADER);
        String correlationId =
                suppliedCorrelationId == null || suppliedCorrelationId.isBlank()
                        ? UUID.randomUUID().toString()
                        : suppliedCorrelationId;

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader(CORRELATION_HEADER, correlationId);
        response.setHeader("WWW-Authenticate", "Basic realm=\"platform\"");
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(RESPONSE_BODY);
    }
}
