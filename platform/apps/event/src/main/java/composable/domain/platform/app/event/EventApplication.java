package composable.domain.platform.app.event;

import composable.domain.platform.http.EventHttpAdapter;
import composable.domain.platform.security.impl.ParticipantSecurityConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication(
        proxyBeanMethods = false,
        scanBasePackageClasses = {EventApplication.class, EventHttpAdapter.class})
@Import(ParticipantSecurityConfiguration.class)
public class EventApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventApplication.class, args);
    }
}
