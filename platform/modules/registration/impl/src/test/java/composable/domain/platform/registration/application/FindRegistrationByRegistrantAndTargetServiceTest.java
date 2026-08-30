package composable.domain.platform.registration.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.registration.api.CreateRegistrationCommand;
import composable.domain.platform.registration.api.RegistrantReference;
import composable.domain.platform.registration.api.RegistrationLifecycle;
import composable.domain.platform.registration.api.TargetReference;
import org.junit.jupiter.api.Test;

class FindRegistrationByRegistrantAndTargetServiceTest {

    private static final ExecutionContext CONTEXT =
            new ExecutionContext(new CorrelationId(
                    "registration-exact-query"));

    @Test
    void exactQueryReturnsActiveAndCancelledPairOccupancy() {
        InMemoryRegistrationRepository repository =
                new InMemoryRegistrationRepository();
        CreateRegistrationService create =
                new CreateRegistrationService(repository);
        CancelRegistrationService cancel =
                new CancelRegistrationService(repository);
        FindRegistrationByRegistrantAndTargetService find =
                new FindRegistrationByRegistrantAndTargetService(repository);

        RegistrantReference registrant =
                new RegistrantReference("participant", "participant-a");
        TargetReference activeTarget =
                new TargetReference("event", "event-active");
        TargetReference cancelledTarget =
                new TargetReference("event", "event-cancelled");

        create.create(
                CONTEXT,
                new CreateRegistrationCommand(
                        "registration-active",
                        registrant,
                        activeTarget));
        create.create(
                CONTEXT,
                new CreateRegistrationCommand(
                        "registration-cancelled",
                        registrant,
                        cancelledTarget));
        cancel.cancel(CONTEXT, "registration-cancelled");

        assertEquals(
                RegistrationLifecycle.ACTIVE,
                find.findByRegistrantAndTarget(
                                CONTEXT,
                                registrant,
                                activeTarget)
                        .orElseThrow()
                        .lifecycle());
        assertEquals(
                RegistrationLifecycle.CANCELLED,
                find.findByRegistrantAndTarget(
                                CONTEXT,
                                registrant,
                                cancelledTarget)
                        .orElseThrow()
                        .lifecycle());
        assertTrue(find.findByRegistrantAndTarget(
                        CONTEXT,
                        registrant,
                        new TargetReference("event", "event-unknown"))
                .isEmpty());
    }
}
