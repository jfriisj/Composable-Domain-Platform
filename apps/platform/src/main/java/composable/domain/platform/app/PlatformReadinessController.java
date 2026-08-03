package composable.domain.platform.app;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class PlatformReadinessController {

    private static final int CONNECTION_VALIDATION_TIMEOUT_SECONDS = 1;

    private final DataSource dataSource;

    public PlatformReadinessController(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @GetMapping("/internal/readiness")
    public ResponseEntity<Void> readiness() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(CONNECTION_VALIDATION_TIMEOUT_SECONDS)) {
                return ResponseEntity.noContent().build();
            }
        } catch (SQLException exception) {
            return unavailable();
        }

        return unavailable();
    }

    private static ResponseEntity<Void> unavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }
}
