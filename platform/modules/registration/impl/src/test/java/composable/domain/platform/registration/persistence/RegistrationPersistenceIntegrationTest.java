package composable.domain.platform.registration.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import composable.domain.platform.registration.application.FindRegistrationsByTargetService;
import composable.domain.platform.registration.application.ReactivateRegistrationService;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
    void persistsReactivationAndPreservesRegistrationIdentityAndReferences() {
        CreateRegistrationCommand command = command(
                "persistent-reactivate",
                "registrant",
                "reactivate-owner",
                "target",
                "reactivate-target");

        RegistrationView created =
                new CreateRegistrationService(new JooqRegistrationRepository(dataSource))
                        .create(CONTEXT, command);

        RegistrationView cancelled =
                new CancelRegistrationService(new JooqRegistrationRepository(dataSource))
                        .cancel(CONTEXT, created.registrationId())
                        .orElseThrow();

        ReactivateRegistrationService reactivation =
                new ReactivateRegistrationService(
                        new JooqRegistrationRepository(dataSource));

        RegistrationView first =
                reactivation.reactivate(CONTEXT, cancelled.registrationId())
                        .orElseThrow();
        RegistrationView second =
                reactivation.reactivate(CONTEXT, cancelled.registrationId())
                        .orElseThrow();

        assertEquals(created.registrationId(), first.registrationId());
        assertEquals(created.registrantReference(), first.registrantReference());
        assertEquals(created.targetReference(), first.targetReference());
        assertEquals(RegistrationLifecycle.ACTIVE, first.lifecycle());
        assertEquals(first, second);

        assertEquals(
                first,
                new FindRegistrationService(new JooqRegistrationRepository(dataSource))
                        .findById(CONTEXT, created.registrationId())
                        .orElseThrow());
    }

    @Test
    void lifecyclePersistenceRejectsStaleExpectedStateInBothDirections() {
        CreateRegistrationCommand command = command(
                "persistent-expected-state",
                "registrant",
                "expected-state-owner",
                "target",
                "expected-state-target");

        new CreateRegistrationService(new JooqRegistrationRepository(dataSource))
                .create(CONTEXT, command);

        JooqRegistrationRepository repository =
                new JooqRegistrationRepository(dataSource);
        var active = repository.findById(command.registrationId()).orElseThrow();
        var cancelled = active.cancel();

        assertTrue(repository.updateLifecycle(cancelled, active.lifecycle()));
        assertFalse(repository.updateLifecycle(active, active.lifecycle()));

        var persistedCancelled =
                repository.findById(command.registrationId()).orElseThrow();
        assertEquals(cancelled, persistedCancelled);

        var reactivated = persistedCancelled.reactivate();
        assertTrue(repository.updateLifecycle(
                reactivated,
                persistedCancelled.lifecycle()));
        assertFalse(repository.updateLifecycle(
                persistedCancelled,
                persistedCancelled.lifecycle()));

        assertEquals(
                reactivated,
                repository.findById(command.registrationId()).orElseThrow());
    }

    @Test
    void concurrentSameTargetReactivationIsIdempotentAndPreservesSingleIdentity()
            throws Exception {
        CreateRegistrationCommand command = command(
                "persistent-concurrent-reactivate",
                "registrant",
                "concurrent-owner",
                "target",
                "concurrent-target");

        RegistrationView created =
                new CreateRegistrationService(new JooqRegistrationRepository(dataSource))
                        .create(CONTEXT, command);
        new CancelRegistrationService(new JooqRegistrationRepository(dataSource))
                .cancel(CONTEXT, created.registrationId())
                .orElseThrow();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<RegistrationView> action = () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent reactivation start timed out");
            }
            return new ReactivateRegistrationService(
                            new JooqRegistrationRepository(dataSource))
                    .reactivate(CONTEXT, created.registrationId())
                    .orElseThrow();
        };

        RegistrationView first;
        RegistrationView second;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstFuture = executor.submit(action);
            var secondFuture = executor.submit(action);
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            first = firstFuture.get(10, TimeUnit.SECONDS);
            second = secondFuture.get(10, TimeUnit.SECONDS);
        }

        assertEquals(RegistrationLifecycle.ACTIVE, first.lifecycle());
        assertEquals(first, second);
        assertEquals(created.registrationId(), first.registrationId());

        assertThrows(
                RegistrationUniquenessConflictException.class,
                () -> new CreateRegistrationService(new JooqRegistrationRepository(dataSource))
                        .create(
                                CONTEXT,
                                command(
                                        "persistent-concurrent-reactivate-conflict",
                                        "registrant",
                                        "concurrent-owner",
                                        "target",
                                        "concurrent-target")));

        assertEquals(
                first,
                new FindRegistrationService(new JooqRegistrationRepository(dataSource))
                        .findById(CONTEXT, created.registrationId())
                        .orElseThrow());
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

    @Test
    void findsRegistrationsByTargetExactMatchIncludingActiveAndCancelled() {
        CreateRegistrationService createService =
                new CreateRegistrationService(new JooqRegistrationRepository(dataSource));
        CancelRegistrationService cancelService =
                new CancelRegistrationService(new JooqRegistrationRepository(dataSource));
        FindRegistrationsByTargetService findService =
                new FindRegistrationsByTargetService(new JooqRegistrationRepository(dataSource));

        RegistrationView active = createService.create(
                CONTEXT,
                command(
                        "persistent-target-active",
                        "participant",
                        "actor-target-1",
                        "event",
                        "target-event-1"));

        RegistrationView toCancel = createService.create(
                CONTEXT,
                command(
                        "persistent-target-cancelled",
                        "participant",
                        "actor-target-2",
                        "event",
                        "target-event-1"));

        RegistrationView cancelled =
                cancelService.cancel(CONTEXT, toCancel.registrationId()).orElseThrow();

        createService.create(
                CONTEXT,
                command(
                        "persistent-target-other-event",
                        "participant",
                        "actor-target-1",
                        "event",
                        "target-event-2"));

        createService.create(
                CONTEXT,
                command(
                        "persistent-target-other-ns",
                        "participant",
                        "actor-target-1",
                        "other-ns",
                        "target-event-1"));

        java.util.List<RegistrationView> results =
                findService.findByTarget(CONTEXT, new TargetReference("event", "target-event-1"));

        assertEquals(2, results.size());
        assertEquals(active, results.get(0));
        assertEquals(cancelled, results.get(1));
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
