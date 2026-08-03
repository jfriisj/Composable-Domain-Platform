package composable.domain.platform.registration.persistence;

import composable.domain.platform.registration.application.RegistrationRepository;
import composable.domain.platform.registration.domain.RegistrantReference;
import composable.domain.platform.registration.domain.Registration;
import composable.domain.platform.registration.domain.TargetReference;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;

final class JooqRegistrationRepository implements RegistrationRepository {

    private static final Table<Record> REGISTRATIONS =
            DSL.table(DSL.name("registration", "registrations"));
    private static final Field<String> REGISTRATION_ID =
            DSL.field(DSL.name("registration_id"), String.class);
    private static final Field<String> REGISTRANT_NAMESPACE =
            DSL.field(DSL.name("registrant_namespace"), String.class);
    private static final Field<String> REGISTRANT_REFERENCE =
            DSL.field(DSL.name("registrant_reference"), String.class);
    private static final Field<String> TARGET_NAMESPACE =
            DSL.field(DSL.name("target_namespace"), String.class);
    private static final Field<String> TARGET_REFERENCE =
            DSL.field(DSL.name("target_reference"), String.class);

    private final DataSource dataSource;

    JooqRegistrationRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public boolean addIfAbsent(Registration registration) {
        Objects.requireNonNull(registration, "registration must not be null");

        return dsl()
                        .insertInto(REGISTRATIONS)
                        .columns(
                                REGISTRATION_ID,
                                REGISTRANT_NAMESPACE,
                                REGISTRANT_REFERENCE,
                                TARGET_NAMESPACE,
                                TARGET_REFERENCE)
                        .values(
                                registration.id(),
                                registration.registrantReference().namespace(),
                                registration.registrantReference().reference(),
                                registration.targetReference().namespace(),
                                registration.targetReference().reference())
                        .onConflict()
                        .doNothing()
                        .execute()
                == 1;
    }

    @Override
    public Optional<Registration> findById(String registrationId) {
        Objects.requireNonNull(registrationId, "registrationId must not be null");

        Record record = dsl()
                .select(
                        REGISTRATION_ID,
                        REGISTRANT_NAMESPACE,
                        REGISTRANT_REFERENCE,
                        TARGET_NAMESPACE,
                        TARGET_REFERENCE)
                .from(REGISTRATIONS)
                .where(REGISTRATION_ID.eq(registrationId))
                .fetchOne();

        return Optional.ofNullable(record).map(JooqRegistrationRepository::toRegistration);
    }

    private DSLContext dsl() {
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }

    private static Registration toRegistration(Record record) {
        return new Registration(
                record.get(REGISTRATION_ID),
                new RegistrantReference(
                        record.get(REGISTRANT_NAMESPACE),
                        record.get(REGISTRANT_REFERENCE)),
                new TargetReference(
                        record.get(TARGET_NAMESPACE),
                        record.get(TARGET_REFERENCE)));
    }
}
