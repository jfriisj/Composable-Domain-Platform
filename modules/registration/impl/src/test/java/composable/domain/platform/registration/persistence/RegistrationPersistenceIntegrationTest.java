package composable.domain.platform.registration.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.registration.api.CreateRegistrationCommand;
import composable.domain.platform.registration.api.RegistrantReference;
import composable.domain.platform.registration.api.RegistrationUniquenessConflictException;
import composable.domain.platform.registration.api.RegistrationView;
import composable.domain.platform.registration.api.TargetReference;
import composable.domain.platform.registration.application.CreateRegistrationService;
import composable.domain.platform.registration.application.FindRegistrationService;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

class RegistrationPersistenceIntegrationTest {

    private static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer("postgres:18.4");
    private static final ExecutionContext CONTEXT =
            new ExecutionContext(new CorrelationId("registration-persistence-test"));

    private static DataSource dataSource;

    @BeforeAll
    static void startDatabase() {
        POSTGRESQL.start();

        PGSimpleDataSource postgresDataSource = new PGSimpleDataSource();
        postgresDataSource.setURL(POSTGRESQL.getJdbcUrl());
        postgresDataSource.setUser(POSTGRESQL.getUsername());
        postgresDataSource.setPassword(POSTGRESQL.getPassword());
        dataSource = postgresDataSource;

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/registration")
                .load()
                .migrate();
    }

    @AfterAll
    static void stopDatabase() {
        POSTGRESQL.stop();
    }

    @Test
    void persistsAndRetrievesAllAcceptedFieldsExactly() {
        CreateRegistrationCommand command = command(
                "persistent-registration-1",
                "registrant-space",
                "registrant:opaque/value",
                "target-space",
                "target:opaque/value");

        RegistrationView created =
                new CreateRegistrationService(new JooqRegistrationRepository(dataSource))
                        .create(CONTEXT, command);

        RegistrationView retrieved =
                new FindRegistrationService(new JooqRegistrationRepository(dataSource))
                        .findById(CONTEXT, command.registrationId())
                        .orElseThrow();

        assertEquals(created, retrieved);
        assertEquals(command.registrationId(), retrieved.registrationId());
        assertEquals(command.registrantReference(), retrieved.registrantReference());
        assertEquals(command.targetReference(), retrieved.targetReference());
    }

    @Test
    void rejectsDuplicateRegistrationIdWithoutReplacingExistingState() {
        CreateRegistrationService service =
                new CreateRegistrationService(new JooqRegistrationRepository(dataSource));

        RegistrationView original = service.create(
                CONTEXT,
                command(
                        "persistent-duplicate-id",
                        "registrant",
                        "original",
                        "target",
                        "original"));

        assertThrows(
                RegistrationUniquenessConflictException.class,
                () -> new CreateRegistrationService(new JooqRegistrationRepository(dataSource))
                        .create(
                                CONTEXT,
                                command(
                                        "persistent-duplicate-id",
                                        "registrant",
                                        "replacement",
                                        "target",
                                        "replacement")));

        RegistrationView persisted =
                new FindRegistrationService(new JooqRegistrationRepository(dataSource))
                        .findById(CONTEXT, "persistent-duplicate-id")
                        .orElseThrow();

        assertEquals(original, persisted);
    }

    @Test
    void rejectsDuplicateCompleteReferencePairAtomically() {
        CreateRegistrationService service =
                new CreateRegistrationService(new JooqRegistrationRepository(dataSource));

        RegistrationView original = service.create(
                CONTEXT,
                command(
                        "persistent-pair-original",
                        "registrant",
                        "same-registrant",
                        "target",
                        "same-target"));

        assertThrows(
                RegistrationUniquenessConflictException.class,
                () -> new CreateRegistrationService(new JooqRegistrationRepository(dataSource))
                        .create(
                                CONTEXT,
                                command(
                                        "persistent-pair-conflict",
                                        "registrant",
                                        "same-registrant",
                                        "target",
                                        "same-target")));

        assertEquals(
                original,
                new FindRegistrationService(new JooqRegistrationRepository(dataSource))
                        .findById(CONTEXT, "persistent-pair-original")
                        .orElseThrow());

        assertTrue(
                new FindRegistrationService(new JooqRegistrationRepository(dataSource))
                        .findById(CONTEXT, "persistent-pair-conflict")
                        .isEmpty());
    }

    @Test
    void namespacesParticipateInPersistedIdentity() {
        CreateRegistrationService service =
                new CreateRegistrationService(new JooqRegistrationRepository(dataSource));

        RegistrationView first = service.create(
                CONTEXT,
                command(
                        "persistent-namespace-1",
                        "registrant-a",
                        "same",
                        "target-a",
                        "same"));

        RegistrationView second = service.create(
                CONTEXT,
                command(
                        "persistent-namespace-2",
                        "registrant-b",
                        "same",
                        "target-b",
                        "same"));

        assertEquals(
                first,
                new FindRegistrationService(new JooqRegistrationRepository(dataSource))
                        .findById(CONTEXT, first.registrationId())
                        .orElseThrow());
        assertEquals(
                second,
                new FindRegistrationService(new JooqRegistrationRepository(dataSource))
                        .findById(CONTEXT, second.registrationId())
                        .orElseThrow());
    }

    @Test
    void returnsEmptyForUnknownPersistentRegistration() {
        assertTrue(
                new FindRegistrationService(new JooqRegistrationRepository(dataSource))
                        .findById(CONTEXT, "persistent-registration-missing")
                        .isEmpty());
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
}
