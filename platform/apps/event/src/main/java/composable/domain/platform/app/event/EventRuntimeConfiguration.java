package composable.domain.platform.app.event;

import composable.domain.platform.composition.eventmanagement.OrganizerEventManagementService;
import composable.domain.platform.event.api.DefineEvent;
import composable.domain.platform.event.api.DiscoverEvents;
import composable.domain.platform.event.api.FindEvent;
import composable.domain.platform.event.api.PublishEvent;
import composable.domain.platform.event.api.SetEventRegistrationAvailability;
import composable.domain.platform.event.api.UpdateEvent;
import composable.domain.platform.event.api.WithdrawEvent;
import composable.domain.platform.event.application.DefineEventService;
import composable.domain.platform.event.application.DiscoverEventsService;
import composable.domain.platform.event.application.EventRepository;
import composable.domain.platform.event.application.FindEventService;
import composable.domain.platform.event.application.PublishEventService;
import composable.domain.platform.event.application.SetEventRegistrationAvailabilityService;
import composable.domain.platform.event.application.UpdateEventService;
import composable.domain.platform.event.application.WithdrawEventService;
import composable.domain.platform.event.persistence.JooqEventRepository;
import composable.domain.platform.security.api.AuthorizeResourceOwnership;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class EventRuntimeConfiguration {

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
    EventRepository eventRepository(
            DataSource dataSource,
            @Qualifier("eventFlyway") Flyway eventFlyway) {
        return new JooqEventRepository(dataSource);
    }

    @Bean
    DefineEvent defineEvent(EventRepository repository) {
        return new DefineEventService(repository);
    }

    @Bean
    UpdateEvent updateEvent(EventRepository repository) {
        return new UpdateEventService(repository);
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
    WithdrawEvent withdrawEvent(EventRepository repository) {
        return new WithdrawEventService(repository);
    }

    @Bean
    SetEventRegistrationAvailability setEventRegistrationAvailability(
            EventRepository repository) {
        return new SetEventRegistrationAvailabilityService(repository);
    }

    @Bean
    DiscoverEvents discoverEvents(EventRepository repository) {
        return new DiscoverEventsService(repository);
    }

    @Bean
    OrganizerEventManagementService organizerEventManagementService(
            DefineEvent defineEvent,
            UpdateEvent updateEvent,
            PublishEvent publishEvent,
            WithdrawEvent withdrawEvent,
            SetEventRegistrationAvailability setEventRegistrationAvailability,
            FindEvent findEvent,
            AuthorizeResourceOwnership authorizeResourceOwnership) {
        return new OrganizerEventManagementService(
                defineEvent,
                updateEvent,
                publishEvent,
                withdrawEvent,
                setEventRegistrationAvailability,
                findEvent,
                authorizeResourceOwnership);
    }
}
