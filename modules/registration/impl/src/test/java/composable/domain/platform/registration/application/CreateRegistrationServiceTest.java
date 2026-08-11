package composable.domain.platform.registration.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.registration.api.CreateRegistrationCommand;
import composable.domain.platform.registration.api.InvalidRegistrationDefinitionException;
import composable.domain.platform.registration.api.RegistrantReference;
import composable.domain.platform.registration.api.RegistrationLifecycle;
import composable.domain.platform.registration.api.RegistrationUniquenessConflictException;
import composable.domain.platform.registration.api.RegistrationView;
import composable.domain.platform.registration.api.TargetReference;
import org.junit.jupiter.api.Test;

class CreateRegistrationServiceTest {

    private static final ExecutionContext CONTEXT =
            new ExecutionContext(new CorrelationId("registration-test"));

    @Test
    void createsDomainNeutralRegistration() {
        InMemoryRegistrationRepository repository = new InMemoryRegistrationRepository();

        RegistrationView created = new CreateRegistrationService(repository).create(
                CONTEXT,
                command("registration-1", "registrant", "one", "target", "one"));

        assertEquals(
                view("registration-1", "registrant", "one", "target", "one"),
                created);
        assertEquals(RegistrationLifecycle.ACTIVE, created.lifecycle());
    }

    @Test
    void rejectsBlankRegistrationId() {
        assertInvalid(command(" ", "registrant", "one", "target", "one"));
    }

    @Test
    void rejectsBlankRegistrantNamespace() {
        assertInvalid(command("registration-1", " ", "one", "target", "one"));
    }

    @Test
    void rejectsBlankRegistrantReference() {
        assertInvalid(command("registration-1", "registrant", " ", "target", "one"));
    }

    @Test
    void rejectsBlankTargetNamespace() {
        assertInvalid(command("registration-1", "registrant", "one", " ", "one"));
    }

    @Test
    void rejectsBlankTargetReference() {
        assertInvalid(command("registration-1", "registrant", "one", "target", " "));
    }

    @Test
    void duplicateRegistrationIdDoesNotReplaceExistingState() {
        InMemoryRegistrationRepository repository = new InMemoryRegistrationRepository();
        CreateRegistrationService service = new CreateRegistrationService(repository);

        RegistrationView original = service.create(
                CONTEXT,
                command("registration-1", "registrant", "one", "target", "one"));

        assertThrows(
                RegistrationUniquenessConflictException.class,
                () -> service.create(
                        CONTEXT,
                        command("registration-1", "registrant", "two", "target", "two")));

        assertEquals(
                original,
                new FindRegistrationService(repository)
                        .findById(CONTEXT, "registration-1")
                        .orElseThrow());
    }

    @Test
    void duplicateCompleteReferencePairDoesNotCreateSecondState() {
        InMemoryRegistrationRepository repository = new InMemoryRegistrationRepository();
        CreateRegistrationService service = new CreateRegistrationService(repository);

        service.create(
                CONTEXT,
                command("registration-1", "registrant", "one", "target", "one"));

        assertThrows(
                RegistrationUniquenessConflictException.class,
                () -> service.create(
                        CONTEXT,
                        command("registration-2", "registrant", "one", "target", "one")));

        assertTrue(new FindRegistrationService(repository)
                .findById(CONTEXT, "registration-2")
                .isEmpty());
    }

    @Test
    void differentRegistrantsMayRegisterAgainstSameTarget() {
        InMemoryRegistrationRepository repository = new InMemoryRegistrationRepository();
        CreateRegistrationService service = new CreateRegistrationService(repository);

        service.create(
                CONTEXT,
                command("registration-1", "registrant", "one", "target", "same"));

        RegistrationView second = service.create(
                CONTEXT,
                command("registration-2", "registrant", "two", "target", "same"));

        assertEquals(
                view("registration-2", "registrant", "two", "target", "same"),
                second);
    }

    @Test
    void sameRegistrantMayRegisterAgainstDifferentTargets() {
        InMemoryRegistrationRepository repository = new InMemoryRegistrationRepository();
        CreateRegistrationService service = new CreateRegistrationService(repository);

        service.create(
                CONTEXT,
                command("registration-1", "registrant", "same", "target", "one"));

        RegistrationView second = service.create(
                CONTEXT,
                command("registration-2", "registrant", "same", "target", "two"));

        assertEquals(
                view("registration-2", "registrant", "same", "target", "two"),
                second);
    }

    @Test
    void identicalRawReferencesInDifferentNamespacesAreDistinct() {
        InMemoryRegistrationRepository repository = new InMemoryRegistrationRepository();
        CreateRegistrationService service = new CreateRegistrationService(repository);

        service.create(
                CONTEXT,
                command("registration-1", "registrant-a", "same", "target-a", "same"));

        RegistrationView second = service.create(
                CONTEXT,
                command("registration-2", "registrant-b", "same", "target-b", "same"));

        assertEquals(
                view("registration-2", "registrant-b", "same", "target-b", "same"),
                second);
    }

    private static void assertInvalid(CreateRegistrationCommand command) {
        InvalidRegistrationDefinitionException error = assertThrows(
                InvalidRegistrationDefinitionException.class,
                () -> new CreateRegistrationService(new InMemoryRegistrationRepository())
                        .create(CONTEXT, command));

        assertEquals("Registration definition is invalid", error.getMessage());
    }

    private static CreateRegistrationCommand command(
            String registrationId,
            String registrantNamespace,
            String registrantReference,
            String targetNamespace,
            String targetReference) {
        return new CreateRegistrationCommand(
                registrationId,
                new RegistrantReference(registrantNamespace, registrantReference),
                new TargetReference(targetNamespace, targetReference));
    }

    private static RegistrationView view(
            String registrationId,
            String registrantNamespace,
            String registrantReference,
            String targetNamespace,
            String targetReference) {
        return new RegistrationView(
                registrationId,
                new RegistrantReference(registrantNamespace, registrantReference),
                new TargetReference(targetNamespace, targetReference));
    }
}
