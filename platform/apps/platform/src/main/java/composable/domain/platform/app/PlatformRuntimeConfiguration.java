package composable.domain.platform.app;

import composable.domain.platform.composition.eventmanagement.OrganizerEventManagementService;
import composable.domain.platform.composition.eventregistration.OrganizerEventRegistrationService;
import composable.domain.platform.composition.eventregistration.ParticipantEventRegistrationService;
import composable.domain.platform.composition.eventwaitlist.ParticipantEventWaitlistService;
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
import composable.domain.platform.registration.api.CancelRegistration;
import composable.domain.platform.registration.api.CreateRegistration;
import composable.domain.platform.registration.api.FindRegistration;
import composable.domain.platform.registration.api.FindRegistrationByRegistrantAndTarget;
import composable.domain.platform.registration.api.FindRegistrationsByTarget;
import composable.domain.platform.registration.application.CancelRegistrationService;
import composable.domain.platform.registration.application.CreateRegistrationService;
import composable.domain.platform.registration.application.FindRegistrationByRegistrantAndTargetService;
import composable.domain.platform.registration.application.FindRegistrationService;
import composable.domain.platform.registration.application.FindRegistrationsByTargetService;
import composable.domain.platform.registration.application.RegistrationRepository;
import composable.domain.platform.registration.persistence.RegistrationPersistence;
import composable.domain.platform.security.api.AuthorizeResourceOwnership;
import composable.domain.platform.waitlist.api.FindWaitlistParticipation;
import composable.domain.platform.waitlist.api.JoinWaitlist;
import composable.domain.platform.waitlist.application.FindWaitlistParticipationService;
import composable.domain.platform.waitlist.application.JoinWaitlistService;
import composable.domain.platform.waitlist.application.WaitlistParticipationRepository;
import composable.domain.platform.waitlist.persistence.WaitlistPersistence;
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
    Flyway waitlistFlyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/waitlist")
                .table("flyway_schema_history_waitlist")
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
    WaitlistParticipationRepository waitlistParticipationRepository(
            DataSource dataSource,
            @Qualifier("waitlistFlyway") Flyway waitlistFlyway) {
        return WaitlistPersistence.repository(dataSource);
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
    CreateRegistration createRegistration(RegistrationRepository repository) {
        return new CreateRegistrationService(repository);
    }

    @Bean
    FindRegistration findRegistration(RegistrationRepository repository) {
        return new FindRegistrationService(repository);
    }

    @Bean
    FindRegistrationByRegistrantAndTarget findRegistrationByRegistrantAndTarget(
            RegistrationRepository repository) {
        return new FindRegistrationByRegistrantAndTargetService(repository);
    }

    @Bean
    FindRegistrationsByTarget findRegistrationsByTarget(
            RegistrationRepository repository) {
        return new FindRegistrationsByTargetService(repository);
    }

    @Bean
    CancelRegistration cancelRegistration(RegistrationRepository repository) {
        return new CancelRegistrationService(repository);
    }

    @Bean
    JoinWaitlist joinWaitlist(WaitlistParticipationRepository repository) {
        return new JoinWaitlistService(repository);
    }

    @Bean
    FindWaitlistParticipation findWaitlistParticipation(
            WaitlistParticipationRepository repository) {
        return new FindWaitlistParticipationService(repository);
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

    @Bean
    OrganizerEventRegistrationService organizerEventRegistrationService(
            FindEvent findEvent,
            FindRegistrationsByTarget findRegistrationsByTarget,
            AuthorizeResourceOwnership authorizeResourceOwnership) {
        return new OrganizerEventRegistrationService(
                findEvent,
                findRegistrationsByTarget,
                authorizeResourceOwnership);
    }

    @Bean
    ParticipantEventWaitlistService participantEventWaitlistService(
            FindEvent findEvent,
            FindRegistrationByRegistrantAndTarget
                    findRegistrationByRegistrantAndTarget,
            JoinWaitlist joinWaitlist,
            FindWaitlistParticipation findWaitlistParticipation) {
        return new ParticipantEventWaitlistService(
                findEvent,
                findRegistrationByRegistrantAndTarget,
                joinWaitlist,
                findWaitlistParticipation);
    }
}
