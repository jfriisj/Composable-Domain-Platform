package composable.domain.platform.registration.persistence;

import composable.domain.platform.registration.application.RegistrationRepository;
import java.util.Objects;
import javax.sql.DataSource;

public final class RegistrationPersistence {

    private RegistrationPersistence() {
    }

    public static RegistrationRepository repository(DataSource dataSource) {
        return new JooqRegistrationRepository(
                Objects.requireNonNull(dataSource, "dataSource must not be null"));
    }
}
