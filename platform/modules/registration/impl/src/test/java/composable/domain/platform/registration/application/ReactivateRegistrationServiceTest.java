package composable.domain.platform.registration.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.registration.api.CreateRegistrationCommand;
import composable.domain.platform.registration.api.RegistrantReference;
import composable.domain.platform.registration.api.RegistrationLifecycle;
import composable.domain.platform.registration.api.RegistrationView;
import composable.domain.platform.registration.api.TargetReference;
import org.junit.jupiter.api.Test;

class ReactivateRegistrationServiceTest {

    private static final ExecutionContext CONTEXT =
            new ExecutionContext(
                    new CorrelationId("registration-reactivate-test"));

    @Test
    void reactivatesCancelledRegistrationAndPreservesIdentityAndReferences() {
        InMemoryRegistrationRepository repository =
                new InMemoryRegistrationRepository();
        RegistrationView created = create(repository, "registration-1");
        RegistrationView cancelled =
                new CancelRegistrationService(repository)
                        .cancel(CONTEXT, created.registrationId())
                        .orElseThrow();

        RegistrationView reactivated =
                new ReactivateRegistrationService(repository)
                        .reactivate(CONTEXT, cancelled.registrationId())
                        .orElseThrow();

        assertEquals(
                cancelled.registrationId(),
                reactivated.registrationId());
        assertEquals(
                cancelled.registrantReference(),
                reactivated.registrantReference());
        assertEquals(
                cancelled.targetReference(),
                reactivated.targetReference());
        assertEquals(
                RegistrationLifecycle.ACTIVE,
                reactivated.lifecycle());

        assertEquals(
                reactivated,
                new FindRegistrationService(repository)
                        .findById(CONTEXT, created.registrationId())
                        .orElseThrow());
    }

    @Test
    void repeatedReactivationIsIdempotent() {
        InMemoryRegistrationRepository repository =
                new InMemoryRegistrationRepository();
        RegistrationView created = create(repository, "registration-1");
        new CancelRegistrationService(repository)
                .cancel(CONTEXT, created.registrationId())
                .orElseThrow();

        ReactivateRegistrationService service =
                new ReactivateRegistrationService(repository);

        RegistrationView first =
                service.reactivate(CONTEXT, created.registrationId())
                        .orElseThrow();
        RegistrationView second =
                service.reactivate(CONTEXT, created.registrationId())
                        .orElseThrow();

        assertEquals(first, second);
        assertEquals(
                RegistrationLifecycle.ACTIVE,
                second.lifecycle());
    }

    @Test
    void returnsEmptyForUnknownRegistration() {
        assertTrue(
                new ReactivateRegistrationService(
                                new InMemoryRegistrationRepository())
                        .reactivate(
                                CONTEXT,
                                "registration-missing")
                        .isEmpty());
    }

    @Test
    void rejectsBlankRegistrationId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ReactivateRegistrationService(
                                new InMemoryRegistrationRepository())
                        .reactivate(CONTEXT, " "));
    }

    private static RegistrationView create(
            InMemoryRegistrationRepository repository,
            String registrationId) {
        return new CreateRegistrationService(repository).create(
                CONTEXT,
                new CreateRegistrationCommand(
                        registrationId,
                        new RegistrantReference(
                                "registrant",
                                "one"),
                        new TargetReference(
                                "target",
                                "one")));
    }
}
