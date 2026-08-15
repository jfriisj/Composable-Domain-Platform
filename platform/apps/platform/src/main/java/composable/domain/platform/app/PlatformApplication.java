package composable.domain.platform.app;

import composable.domain.platform.http.EventHttpAdapter;
import composable.domain.platform.security.impl.ParticipantSecurityConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication(
        proxyBeanMethods = false,
        scanBasePackageClasses = {PlatformApplication.class, EventHttpAdapter.class})
@Import(ParticipantSecurityConfiguration.class)
public class PlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}
