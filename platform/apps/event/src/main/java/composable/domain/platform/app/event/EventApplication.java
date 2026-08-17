package composable.domain.platform.app.event;

import composable.domain.platform.http.EventHttpAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        proxyBeanMethods = false,
        scanBasePackageClasses = {EventApplication.class, EventHttpAdapter.class})
public class EventApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventApplication.class, args);
    }
}
