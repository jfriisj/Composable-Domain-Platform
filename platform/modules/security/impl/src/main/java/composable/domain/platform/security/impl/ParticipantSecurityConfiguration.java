package composable.domain.platform.security.impl;

import composable.domain.platform.security.api.AuthenticatedActorProvider;
import composable.domain.platform.security.api.AuthenticatedActorReference;
import composable.domain.platform.security.api.AuthenticationRequiredException;
import composable.domain.platform.security.api.AuthorizeResourceOwnership;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ParticipantSecurityProperties.class)
public class ParticipantSecurityConfiguration {

    private static final String EVENT_REGISTRATION_COLLECTION =
            "/api/v1/event-registrations";
    private static final String EVENT_REGISTRATION_ITEMS =
            "/api/v1/event-registrations/**";
    private static final String EVENT_COLLECTION =
            "/api/v1/events";
    private static final String EVENT_ITEMS =
            "/api/v1/events/**";

    @Bean
    PasswordEncoder participantPasswordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    UserDetailsService participantUserDetailsService(
            ParticipantSecurityProperties properties,
            PasswordEncoder participantPasswordEncoder) {
        List<UserDetails> users = new ArrayList<>();
        Set<String> principals = new HashSet<>();

        if (properties.getParticipants().isEmpty()) {
            throw new IllegalStateException(
                    "at least one participant credential must be configured");
        }

        for (ParticipantSecurityProperties.Participant participant :
                properties.getParticipants()) {
            String principal = validatePrincipal(participant.getPrincipal());
            String passwordVerifier =
                    validatePasswordVerifier(
                            participant.getPasswordVerifier(),
                            participantPasswordEncoder);

            if (!principals.add(principal)) {
                throw new IllegalStateException(
                        "participant principals must be unique");
            }

            users.add(new User(principal, passwordVerifier, List.of()));
        }

        return new InMemoryUserDetailsManager(users);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    AuthenticationManager participantAuthenticationManager(
            UserDetailsService participantUserDetailsService,
            PasswordEncoder participantPasswordEncoder) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(participantUserDetailsService);
        provider.setPasswordEncoder(participantPasswordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    AuthenticatedActorProvider authenticatedActorProvider() {
        return () -> {
            Authentication authentication =
                    org.springframework.security.core.context.SecurityContextHolder
                            .getContext()
                            .getAuthentication();

            if (authentication == null
                    || !authentication.isAuthenticated()
                    || authentication
                            instanceof org.springframework.security.authentication
                                    .AnonymousAuthenticationToken) {
                throw new AuthenticationRequiredException();
            }

            return new AuthenticatedActorReference(authentication.getName());
        };
    }

    @Bean
    AuthorizeResourceOwnership authorizeResourceOwnership() {
        return new ResourceOwnershipAuthorization();
    }

    @Bean
    ParticipantAuthenticationFailureResponder participantAuthenticationFailureResponder() {
        return new ParticipantAuthenticationFailureResponder();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    SecurityFilterChain participantSecurityFilterChain(
            HttpSecurity http,
            AuthenticationManager participantAuthenticationManager,
            ParticipantAuthenticationFailureResponder failureResponder)
            throws Exception {
        http.securityMatcher(
                        EVENT_REGISTRATION_COLLECTION,
                        EVENT_REGISTRATION_ITEMS,
                        EVENT_COLLECTION,
                        EVENT_ITEMS)
                .authenticationManager(participantAuthenticationManager)
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, EVENT_COLLECTION, "/api/v1/events/*")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, EVENT_COLLECTION)
                        .authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/events/*")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/events/*/publication")
                        .authenticated()
                        .requestMatchers(
                                EVENT_REGISTRATION_COLLECTION,
                                EVENT_REGISTRATION_ITEMS)
                        .authenticated()
                        .anyRequest()
                        .authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint(
                        (request, response, exception) ->
                                failureResponder.respond(request, response)));

        return http.build();
    }

    private static String validatePrincipal(String principal) {
        if (principal == null || principal.isBlank()) {
            throw new IllegalStateException(
                    "participant principal must not be blank");
        }
        if (!principal.equals(principal.strip())) {
            throw new IllegalStateException(
                    "participant principal must not have surrounding whitespace");
        }
        if (principal.contains("@")) {
            throw new IllegalStateException(
                    "participant principal must be an opaque non-email identifier");
        }
        if (principal.contains(":")) {
            throw new IllegalStateException(
                    "participant principal must not contain the HTTP Basic delimiter");
        }
        return principal;
    }

    private static String validatePasswordVerifier(
            String passwordVerifier,
            PasswordEncoder passwordEncoder) {
        if (passwordVerifier == null || passwordVerifier.isBlank()) {
            throw new IllegalStateException(
                    "participant password verifier must not be blank");
        }
        if (!passwordVerifier.startsWith("{")
                || passwordVerifier.indexOf('}') < 2) {
            throw new IllegalStateException(
                    "participant password verifier must use an encoded format");
        }
        if (passwordVerifier.regionMatches(
                true,
                0,
                "{noop}",
                0,
                "{noop}".length())) {
            throw new IllegalStateException(
                    "no-op participant password verification is not accepted");
        }

        try {
            passwordEncoder.matches(
                    "__participant_configuration_validation_probe__",
                    passwordVerifier);
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "participant password verifier must use a supported encoded format");
        }

        return passwordVerifier;
    }
}
