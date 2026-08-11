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

class CancelRegistrationServiceTest {

    private static final ExecutionContext CONTEXT =
            new ExecutionContext(new CorrelationId("registration-cancel-test"));

    @Test
    void cancelsActiveRegistrationAndPreservesIdentityAndReferences() {
        InMemoryRegistrationRepository repository = new InMemoryRegistrationRepository();
        RegistrationView created = create(repository, "registration-1");

        RegistrationView cancelled = new CancelRegistrationService(repository)
                .cancel(CONTEXT, created.registrationId())
                .orElseThrow();

        assertEquals(created.registrationId(), cancelled.registrationId());
        assertEquals(created.registrantReference(), cancelled.registrantReference());
        assertEquals(created.targetReference(), cancelled.targetReference());
        assertEquals(RegistrationLifecycle.CANCELLED, cancelled.lifecycle());

        assertEquals(
                cancelled,
                new FindRegistrationService(repository)
                        .findById(CONTEXT, created.registrationId())
                        .orElseThrow());
    }

    @Test
    void repeatedCancellationIsIdempotent() {
        InMemoryRegistrationRepository repository = new InMemoryRegistrationRepository();
        RegistrationView created = create(repository, "registration-1");
        CancelRegistrationService service = new CancelRegistrationService(repository);

        RegistrationView first =
                service.cancel(CONTEXT, created.registrationId()).orElseThrow();
        RegistrationView second =
                service.cancel(CONTEXT, created.registrationId()).orElseThrow();

        assertEquals(first, second);
        assertEquals(RegistrationLifecycle.CANCELLED, second.lifecycle());
    }

    @Test
    void returnsEmptyForUnknownRegistration() {
        assertTrue(
                new CancelRegistrationService(new InMemoryRegistrationRepository())
                        .cancel(CONTEXT, "registration-missing")
                        .isEmpty());
    }

    @Test
    void rejectsBlankRegistrationId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CancelRegistrationService(new InMemoryRegistrationRepository())
                        .cancel(CONTEXT, " "));
    }

    private static RegistrationView create(
            InMemoryRegistrationRepository repository,
            String registrationId) {
        return new CreateRegistrationService(repository).create(
                CONTEXT,
                new CreateRegistrationCommand(
                        registrationId,
                        new RegistrantReference("registrant", "one"),
                        new TargetReference("target", "one")));
    }
}
