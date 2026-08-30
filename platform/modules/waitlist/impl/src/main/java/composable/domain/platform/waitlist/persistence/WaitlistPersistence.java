package composable.domain.platform.waitlist.persistence;

import composable.domain.platform.waitlist.application.WaitlistParticipationRepository;
import java.util.Objects;
import javax.sql.DataSource;

public final class WaitlistPersistence {

    private WaitlistPersistence() {
    }

    public static WaitlistParticipationRepository repository(
            DataSource dataSource) {
        return new JooqWaitlistParticipationRepository(
                Objects.requireNonNull(
                        dataSource,
                        "dataSource must not be null"));
    }
}
