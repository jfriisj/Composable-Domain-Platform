package composable.domain.platform.waitlist.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.waitlist.api.WaitlistEventReference;
import composable.domain.platform.waitlist.api.WaitlistParticipantReference;
import composable.domain.platform.waitlist.application.JoinWaitlistService;
import composable.domain.platform.waitlist.application.WaitlistParticipationRepository;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

class WaitlistPersistenceIntegrationTest {

    private static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer("postgres:18.4");

    private static DataSource dataSource;

    @BeforeAll
    static void startPostgresql() {
        POSTGRESQL.start();

        PGSimpleDataSource configured = new PGSimpleDataSource();
        configured.setURL(POSTGRESQL.getJdbcUrl());
        configured.setUser(POSTGRESQL.getUsername());
        configured.setPassword(POSTGRESQL.getPassword());
        dataSource = configured;

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/waitlist")
                .load()
                .migrate();
    }

    @AfterAll
    static void stopPostgresql() {
        POSTGRESQL.stop();
    }

    @BeforeEach
    void clearState() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("truncate table waitlist.participations");
        }
    }

    @Test
    void concurrentSamePairJoinPreservesOneDurableParticipation()
            throws Exception {
        WaitlistParticipationRepository repository =
                WaitlistPersistence.repository(dataSource);
        JoinWaitlistService service = new JoinWaitlistService(repository);
        ExecutionContext context =
                new ExecutionContext(new CorrelationId("waitlist-concurrent"));
        WaitlistParticipantReference participant =
                new WaitlistParticipantReference("participant-concurrent");
        WaitlistEventReference event =
                new WaitlistEventReference("event-concurrent");
        CyclicBarrier start = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);

        try {
            var first = executor.submit(() -> {
                start.await();
                return service.join(context, participant, event);
            });
            var second = executor.submit(() -> {
                start.await();
                return service.join(context, participant, event);
            });

            var firstResult = first.get(10, TimeUnit.SECONDS);
            var secondResult = second.get(10, TimeUnit.SECONDS);

            assertEquals(
                    firstResult.waitlistParticipationId(),
                    secondResult.waitlistParticipationId());
            assertEquals(1, participationCount());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private int participationCount() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "select count(*) from waitlist.participations")) {
            result.next();
            return result.getInt(1);
        }
    }
}
