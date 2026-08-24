package composable.domain.platform.registration.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.registration.api.CreateRegistrationCommand;
import composable.domain.platform.registration.api.RegistrantReference;
import composable.domain.platform.registration.api.RegistrationView;
import composable.domain.platform.registration.api.TargetReference;
import java.util.List;
import org.junit.jupiter.api.Test;

class FindRegistrationsByTargetServiceTest {

    private static final ExecutionContext CONTEXT =
            new ExecutionContext(new CorrelationId("target-find-test"));

    @Test
    void retrievesRegistrationsByTargetReference() {
        InMemoryRegistrationRepository repository = new InMemoryRegistrationRepository();
        CreateRegistrationService createService = new CreateRegistrationService(repository);
        CancelRegistrationService cancelService = new CancelRegistrationService(repository);

        RegistrationView active = createService.create(
                CONTEXT,
                new CreateRegistrationCommand(
                        "registration-active",
                        new RegistrantReference("participant", "actor-1"),
                        new TargetReference("event", "event-100")));

        RegistrationView toCancel = createService.create(
                CONTEXT,
                new CreateRegistrationCommand(
                        "registration-cancelled",
                        new RegistrantReference("participant", "actor-2"),
                        new TargetReference("event", "event-100")));

        RegistrationView cancelled = cancelService.cancel(CONTEXT, toCancel.registrationId())
                .orElseThrow();

        createService.create(
                CONTEXT,
                new CreateRegistrationCommand(
                        "registration-other-event",
                        new RegistrantReference("participant", "actor-1"),
                        new TargetReference("event", "event-200")));

        createService.create(
                CONTEXT,
                new CreateRegistrationCommand(
                        "registration-other-namespace",
                        new RegistrantReference("participant", "actor-1"),
                        new TargetReference("course", "event-100")));

        List<RegistrationView> results =
                new FindRegistrationsByTargetService(repository)
                        .findByTarget(CONTEXT, new TargetReference("event", "event-100"));

        assertEquals(2, results.size());
        assertTrue(results.contains(active));
        assertTrue(results.contains(cancelled));
    }

    @Test
    void findByTargetReturnsEmptyWhenNoRegistrationsMatch() {
        InMemoryRegistrationRepository repository = new InMemoryRegistrationRepository();

        new CreateRegistrationService(repository).create(
                CONTEXT,
                new CreateRegistrationCommand(
                        "registration-1",
                        new RegistrantReference("participant", "actor-1"),
                        new TargetReference("event", "event-1")));

        List<RegistrationView> results =
                new FindRegistrationsByTargetService(repository)
                        .findByTarget(CONTEXT, new TargetReference("event", "other-event"));

        assertTrue(results.isEmpty());
    }

    @Test
    void findByTargetRejectsInvalidTargetReference() {
        FindRegistrationsByTargetService service =
                new FindRegistrationsByTargetService(new InMemoryRegistrationRepository());

        assertThrows(
                NullPointerException.class,
                () -> service.findByTarget(null, new TargetReference("event", "event-1")));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.findByTarget(CONTEXT, null));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.findByTarget(CONTEXT, new TargetReference("", "event-1")));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.findByTarget(CONTEXT, new TargetReference("event", " ")));
    }
}
