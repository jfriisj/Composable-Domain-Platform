package composable.domain.platform.app;

import composable.domain.platform.composition.eventregistration.ParticipantEventRegistrationService;
import composable.domain.platform.event.api.DefineEvent;
import composable.domain.platform.event.api.DiscoverEvents;
import composable.domain.platform.event.api.FindEvent;
import composable.domain.platform.event.api.PublishEvent;
import composable.domain.platform.event.application.DefineEventService;
import composable.domain.platform.event.application.DiscoverEventsService;
import composable.domain.platform.event.application.EventRepository;
import composable.domain.platform.event.application.FindEventService;
import composable.domain.platform.event.application.PublishEventService;
import composable.domain.platform.event.persistence.JooqEventRepository;
import composable.domain.platform.registration.api.CancelRegistration;
import composable.domain.platform.registration.api.CreateRegistration;
import composable.domain.platform.registration.api.FindRegistration;
import composable.domain.platform.registration.application.CancelRegistrationService;
import composable.domain.platform.registration.application.CreateRegistrationService;
import composable.domain.platform.registration.application.FindRegistrationService;
import composable.domain.platform.registration.application.RegistrationRepository;
import composable.domain.platform.registration.persistence.RegistrationPersistence;
import composable.domain.platform.security.api.AuthorizeResourceOwnership;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class PlatformRuntimeConfiguration {

    @Bean
    DataSource dataSource(
            @Value("${platform.database.url}") String url,
            @Value("${platform.database.username}") String username,
            @Value("${platform.database.password}") String password) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(url);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    @Bean
    Flyway eventFlyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/event")
                .load();
        flyway.migrate();
        return flyway;
    }

    @Bean
    Flyway registrationFlyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/registration")
                .table("flyway_schema_history_registration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
        flyway.migrate();
        return flyway;
    }

    @Bean
    EventRepository eventRepository(
            DataSource dataSource,
            @Qualifier("eventFlyway") Flyway eventFlyway) {
        return new JooqEventRepository(dataSource);
    }

    @Bean
    RegistrationRepository registrationRepository(
            DataSource dataSource,
            @Qualifier("registrationFlyway") Flyway registrationFlyway) {
        return RegistrationPersistence.repository(dataSource);
    }

    @Bean
    DefineEvent defineEvent(EventRepository repository) {
        return new DefineEventService(repository);
    }

    @Bean
    FindEvent findEvent(EventRepository repository) {
        return new FindEventService(repository);
    }

    @Bean
    PublishEvent publishEvent(EventRepository repository) {
        return new PublishEventService(repository);
    }

    @Bean
    DiscoverEvents discoverEvents(EventRepository repository) {
        return new DiscoverEventsService(repository);
    }

    @Bean
    CreateRegistration createRegistration(RegistrationRepository repository) {
        return new CreateRegistrationService(repository);
    }

    @Bean
    FindRegistration findRegistration(RegistrationRepository repository) {
        return new FindRegistrationService(repository);
    }

    @Bean
    CancelRegistration cancelRegistration(RegistrationRepository repository) {
        return new CancelRegistrationService(repository);
    }

    @Bean
    ParticipantEventRegistrationService participantEventRegistrationService(
            FindEvent findEvent,
            CreateRegistration createRegistration,
            FindRegistration findRegistration,
            CancelRegistration cancelRegistration,
            AuthorizeResourceOwnership authorizeResourceOwnership) {
        return new ParticipantEventRegistrationService(
                findEvent,
                createRegistration,
                findRegistration,
                cancelRegistration,
                authorizeResourceOwnership);
    }
}
