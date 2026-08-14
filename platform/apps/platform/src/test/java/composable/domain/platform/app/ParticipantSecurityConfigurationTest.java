package composable.domain.platform.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import composable.domain.platform.http.AuthenticatedActorProvider;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

class ParticipantSecurityConfigurationTest {

    private final ParticipantSecurityConfiguration configuration =
            new ParticipantSecurityConfiguration();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void configuredOpaquePrincipalsUseEncodedVerifiersWithoutAuthorities() {
        PasswordEncoder encoder = configuration.participantPasswordEncoder();
        ParticipantSecurityProperties properties =
                new ParticipantSecurityProperties();
        properties.setParticipants(List.of(
                participant("opaque-proof-a", encoder.encode("alpha-secret")),
                participant("opaque-proof-b", encoder.encode("beta-secret"))));

        UserDetailsService users =
                configuration.participantUserDetailsService(
                        properties,
                        encoder);

        assertTrue(encoder.matches(
                "alpha-secret",
                users.loadUserByUsername("opaque-proof-a").getPassword()));
        assertTrue(
                users.loadUserByUsername("opaque-proof-a")
                        .getAuthorities()
                        .isEmpty());
        assertTrue(encoder.matches(
                "beta-secret",
                users.loadUserByUsername("opaque-proof-b").getPassword()));
    }

    @Test
    void absentParticipantConfigurationFailsClosed() {
        PasswordEncoder encoder = configuration.participantPasswordEncoder();

        assertThrows(
                IllegalStateException.class,
                () -> configuration.participantUserDetailsService(
                        new ParticipantSecurityProperties(),
                        encoder));
    }

    @Test
    void duplicatePrincipalIsStructurallyInvalid() {
        PasswordEncoder encoder = configuration.participantPasswordEncoder();
        ParticipantSecurityProperties properties =
                new ParticipantSecurityProperties();
        properties.setParticipants(List.of(
                participant("opaque-proof-a", encoder.encode("alpha-secret")),
                participant("opaque-proof-a", encoder.encode("beta-secret"))));

        assertThrows(
                IllegalStateException.class,
                () -> configuration.participantUserDetailsService(
                        properties,
                        encoder));
    }

    @Test
    void plaintextAndNoopPasswordVerifiersAreRejected() {
        PasswordEncoder encoder = configuration.participantPasswordEncoder();

        ParticipantSecurityProperties plaintext =
                new ParticipantSecurityProperties();
        plaintext.setParticipants(List.of(
                participant("opaque-proof-a", "plaintext-secret")));

        assertThrows(
                IllegalStateException.class,
                () -> configuration.participantUserDetailsService(
                        plaintext,
                        encoder));

        ParticipantSecurityProperties noop =
                new ParticipantSecurityProperties();
        noop.setParticipants(List.of(
                participant("opaque-proof-a", "{noop}plaintext-secret")));

        assertThrows(
                IllegalStateException.class,
                () -> configuration.participantUserDetailsService(
                        noop,
                        encoder));
    }

    @Test
    void authenticatedTechnicalPrincipalAdaptsDirectlyToActorReference() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "opaque-proof-a",
                        "erased",
                        List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        AuthenticatedActorProvider actorProvider =
                configuration.authenticatedActorProvider();

        assertEquals(
                "opaque-proof-a",
                actorProvider.authenticatedActor().reference());
    }

    @Test
    void emailLikePrincipalIsRejected() {
        PasswordEncoder encoder = configuration.participantPasswordEncoder();
        ParticipantSecurityProperties properties =
                new ParticipantSecurityProperties();
        properties.setParticipants(List.of(
                participant(
                        "participant@example.test",
                        encoder.encode("alpha-secret"))));

        assertThrows(
                IllegalStateException.class,
                () -> configuration.participantUserDetailsService(
                        properties,
                        encoder));
    }

    private static ParticipantSecurityProperties.Participant participant(
            String principal,
            String passwordVerifier) {
        ParticipantSecurityProperties.Participant participant =
                new ParticipantSecurityProperties.Participant();
        participant.setPrincipal(principal);
        participant.setPasswordVerifier(passwordVerifier);
        return participant;
    }
}
