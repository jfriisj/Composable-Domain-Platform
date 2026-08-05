package composable.domain.platform.registration.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.registration.api.CreateRegistrationCommand;
import composable.domain.platform.registration.api.RegistrantReference;
import composable.domain.platform.registration.api.RegistrationLifecycle;
import composable.domain.platform.registration.api.RegistrationUniquenessConflictException;
import composable.domain.platform.registration.api.RegistrationView;
import composable.domain.platform.registration.api.TargetReference;
import composable.domain.platform.registration.application.CancelRegistrationService;
import composable.domain.platform.registration.application.CreateRegistrationService;
import composable.domain.platform.registration.application.FindRegistrationService;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
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
    static void startDatabase() throws SQLException {
        POSTGRESQL.start();

        PGSimpleDataSource postgresDataSource = new PGSimpleDataSource();
        postgresDataSource.setURL(POSTGRESQL.getJdbcUrl());
        postgresDataSource.setUser(POSTGRESQL.getUsername());
        postgresDataSource.setPassword(POSTGRESQL.getPassword());
        dataSource = postgresDataSource;

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/registration")
                .target(MigrationVersion.fromVersion("1"))
                .load()
                .migrate();

        insertPreLifecycleRegistration();

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
    void migratesPreLifecycleRegistrationAsActive() {
        RegistrationView migrated =
                new FindRegistrationService(new JooqRegistrationRepository(dataSource))
                        .findById(CONTEXT, "pre-lifecycle-registration")
                        .orElseThrow();

        assertEquals(RegistrationLifecycle.ACTIVE, migrated.lifecycle());
        assertEquals(
                new RegistrantReference("legacy-registrant", "legacy-reference"),
                migrated.registrantReference());
        assertEquals(
                new TargetReference("legacy-target", "legacy-reference"),
                migrated.targetReference());
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
        assertEquals(RegistrationLifecycle.ACTIVE, retrieved.lifecycle());
    }

    @Test
    void persistsCancellationAndMakesRepeatedCancellationIdempotent() {
        CreateRegistrationCommand command = command(
                "persistent-cancel",
                "registrant",
                "cancel-owner",
                "target",
                "cancel-target");

        RegistrationView created =
                new CreateRegistrationService(new JooqRegistrationRepository(dataSource))
                        .create(CONTEXT, command);

        CancelRegistrationService cancellation =
                new CancelRegistrationService(new JooqRegistrationRepository(dataSource));

        RegistrationView first =
                cancellation.cancel(CONTEXT, created.registrationId()).orElseThrow();
        RegistrationView second =
                cancellation.cancel(CONTEXT, created.registrationId()).orElseThrow();

        assertEquals(created.registrationId(), first.registrationId());
        assertEquals(created.registrantReference(), first.registrantReference());
        assertEquals(created.targetReference(), first.targetReference());
        assertEquals(RegistrationLifecycle.CANCELLED, first.lifecycle());
        assertEquals(first, second);

        RegistrationView freshRead =
                new FindRegistrationService(new JooqRegistrationRepository(dataSource))
                        .findById(CONTEXT, created.registrationId())
                        .orElseThrow();

        assertEquals(first, freshRead);
    }

    @Test
    void cancelledReferencePairRemainsUnique() {
        CreateRegistrationCommand originalCommand = command(
                "persistent-cancelled-pair",
                "registrant",
                "cancelled-pair-owner",
                "target",
                "cancelled-pair-target");

        RegistrationView original =
                new CreateRegistrationService(new JooqRegistrationRepository(dataSource))
                        .create(CONTEXT, originalCommand);

        new CancelRegistrationService(new JooqRegistrationRepository(dataSource))
                .cancel(CONTEXT, original.registrationId())
                .orElseThrow();

        assertThrows(
                RegistrationUniquenessConflictException.class,
                () -> new CreateRegistrationService(new JooqRegistrationRepository(dataSource))
                        .create(
                                CONTEXT,
                                command(
                                        "persistent-cancelled-pair-conflict",
                                        "registrant",
                                        "cancelled-pair-owner",
                                        "target",
                                        "cancelled-pair-target")));

        RegistrationView persisted =
                new FindRegistrationService(new JooqRegistrationRepository(dataSource))
                        .findById(CONTEXT, original.registrationId())
                        .orElseThrow();

        assertEquals(RegistrationLifecycle.CANCELLED, persisted.lifecycle());
        assertTrue(
                new FindRegistrationService(new JooqRegistrationRepository(dataSource))
                        .findById(CONTEXT, "persistent-cancelled-pair-conflict")
                        .isEmpty());
    }

    @Test
    void databaseRejectsUnsupportedLifecycleValue() {
        assertThrows(
                SQLException.class,
                () -> {
                    try (var connection = dataSource.getConnection();
                            PreparedStatement statement = connection.prepareStatement(
                                    "update registration.registrations "
                                            + "set lifecycle = ? where registration_id = ?")) {
                        statement.setString(1, "unsupported");
                        statement.setString(2, "pre-lifecycle-registration");
                        statement.executeUpdate();
                    }
                });
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

    private static void insertPreLifecycleRegistration() throws SQLException {
        try (var connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "insert into registration.registrations "
                                + "(registration_id, registrant_namespace, registrant_reference, "
                                + "target_namespace, target_reference) "
                                + "values (?, ?, ?, ?, ?)")) {
            statement.setString(1, "pre-lifecycle-registration");
            statement.setString(2, "legacy-registrant");
            statement.setString(3, "legacy-reference");
            statement.setString(4, "legacy-target");
            statement.setString(5, "legacy-reference");
            statement.executeUpdate();
        }
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
