package composable.domain.platform.registration.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.registration.api.CreateRegistrationCommand;
import composable.domain.platform.registration.api.RegistrantReference;
import composable.domain.platform.registration.api.RegistrationLifecycle;
import composable.domain.platform.registration.api.RegistrationView;
import composable.domain.platform.registration.api.TargetReference;
import org.junit.jupiter.api.Test;

class FindRegistrationServiceTest {

    private static final ExecutionContext CONTEXT =
            new ExecutionContext(new CorrelationId("registration-find-test"));

    @Test
    void retrievesRegistrationByRegistrationId() {
        InMemoryRegistrationRepository repository = new InMemoryRegistrationRepository();

        new CreateRegistrationService(repository).create(
                CONTEXT,
                new CreateRegistrationCommand(
                        "registration-1",
                        new RegistrantReference("registrant", "one"),
                        new TargetReference("target", "one")));

        RegistrationView found =
                new FindRegistrationService(repository)
                        .findById(CONTEXT, "registration-1")
                        .orElseThrow();

        assertEquals(
                new RegistrationView(
                        "registration-1",
                        new RegistrantReference("registrant", "one"),
                        new TargetReference("target", "one")),
                found);
        assertEquals(RegistrationLifecycle.ACTIVE, found.lifecycle());
    }

    @Test
    void returnsEmptyForUnknownRegistration() {
        assertTrue(
                new FindRegistrationService(new InMemoryRegistrationRepository())
                        .findById(CONTEXT, "registration-missing")
                        .isEmpty());
    }
}
