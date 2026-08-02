package composable.domain.platform.app;

import composable.domain.platform.http.EventHttpAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        proxyBeanMethods = false,
        scanBasePackageClasses = {PlatformApplication.class, EventHttpAdapter.class})
public class PlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}
