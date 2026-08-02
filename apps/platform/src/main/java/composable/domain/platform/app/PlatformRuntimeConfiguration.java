package composable.domain.platform.app;

import composable.domain.platform.event.api.DefineEvent;
import composable.domain.platform.event.api.FindEvent;
import composable.domain.platform.event.application.DefineEventService;
import composable.domain.platform.event.application.EventRepository;
import composable.domain.platform.event.application.FindEventService;
import composable.domain.platform.event.persistence.JooqEventRepository;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
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
    EventRepository eventRepository(DataSource dataSource, Flyway eventFlyway) {
        return new JooqEventRepository(dataSource);
    }

    @Bean
    DefineEvent defineEvent(EventRepository repository) {
        return new DefineEventService(repository);
    }

    @Bean
    FindEvent findEvent(EventRepository repository) {
        return new FindEventService(repository);
    }
}
