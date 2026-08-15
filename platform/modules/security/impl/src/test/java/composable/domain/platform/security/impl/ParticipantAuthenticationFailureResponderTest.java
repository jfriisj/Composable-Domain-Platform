package composable.domain.platform.security.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ParticipantAuthenticationFailureResponderTest {

    private final ParticipantAuthenticationFailureResponder responder =
            new ParticipantAuthenticationFailureResponder();

    @Test
    void returnsIdentityFreeAuthenticationFailureWithSuppliedCorrelation()
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(
                ParticipantAuthenticationFailureResponder.CORRELATION_HEADER,
                "corr-auth-failure");
        MockHttpServletResponse response = new MockHttpServletResponse();

        responder.respond(request, response);

        assertEquals(401, response.getStatus());
        assertEquals(
                "corr-auth-failure",
                response.getHeader(
                        ParticipantAuthenticationFailureResponder.CORRELATION_HEADER));
        assertEquals(
                "Basic realm=\"platform\"",
                response.getHeader("WWW-Authenticate"));
        assertEquals(
                "{\"code\":\"authentication_required\","
                        + "\"message\":\"Authentication required\"}",
                response.getContentAsString());
        assertFalse(response.getContentAsString().contains("principal"));
        assertFalse(response.getContentAsString().contains("password"));
    }

    @Test
    void generatesCorrelationWhenAuthenticationFailureHasNoHeader()
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        responder.respond(request, response);

        String correlation =
                response.getHeader(
                        ParticipantAuthenticationFailureResponder.CORRELATION_HEADER);
        assertFalse(correlation == null || correlation.isBlank());
        assertEquals(401, response.getStatus());
    }
}
