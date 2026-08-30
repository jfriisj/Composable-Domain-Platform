package composable.domain.platform.registration.persistence;

import composable.domain.platform.registration.application.RegistrationRepository;
import composable.domain.platform.registration.domain.RegistrantReference;
import composable.domain.platform.registration.domain.Registration;
import composable.domain.platform.registration.domain.RegistrationLifecycle;
import composable.domain.platform.registration.domain.TargetReference;
import java.util.List;
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
    private static final Field<String> LIFECYCLE =
            DSL.field(DSL.name("lifecycle"), String.class);

    private final DataSource dataSource;

    JooqRegistrationRepository(DataSource dataSource) {
        this.dataSource =
                Objects.requireNonNull(
                        dataSource,
                        "dataSource must not be null");
    }

    @Override
    public boolean addIfAbsent(Registration registration) {
        Objects.requireNonNull(
                registration,
                "registration must not be null");

        return dsl()
                        .insertInto(REGISTRATIONS)
                        .columns(
                                REGISTRATION_ID,
                                REGISTRANT_NAMESPACE,
                                REGISTRANT_REFERENCE,
                                TARGET_NAMESPACE,
                                TARGET_REFERENCE,
                                LIFECYCLE)
                        .values(
                                registration.id(),
                                registration.registrantReference().namespace(),
                                registration.registrantReference().reference(),
                                registration.targetReference().namespace(),
                                registration.targetReference().reference(),
                                toPersistenceValue(registration.lifecycle()))
                        .onConflict()
                        .doNothing()
                        .execute()
                == 1;
    }

    @Override
    public Optional<Registration> findById(String registrationId) {
        Objects.requireNonNull(
                registrationId,
                "registrationId must not be null");

        Record record = dsl()
                .select(
                        REGISTRATION_ID,
                        REGISTRANT_NAMESPACE,
                        REGISTRANT_REFERENCE,
                        TARGET_NAMESPACE,
                        TARGET_REFERENCE,
                        LIFECYCLE)
                .from(REGISTRATIONS)
                .where(REGISTRATION_ID.eq(registrationId))
                .fetchOne();

        return Optional.ofNullable(record)
                .map(JooqRegistrationRepository::toRegistration);
    }

    @Override
    public Optional<Registration> findByRegistrantAndTarget(
            RegistrantReference registrantReference,
            TargetReference targetReference) {
        Objects.requireNonNull(
                registrantReference,
                "registrantReference must not be null");
        Objects.requireNonNull(
                targetReference,
                "targetReference must not be null");

        Record record = dsl()
                .select(
                        REGISTRATION_ID,
                        REGISTRANT_NAMESPACE,
                        REGISTRANT_REFERENCE,
                        TARGET_NAMESPACE,
                        TARGET_REFERENCE,
                        LIFECYCLE)
                .from(REGISTRATIONS)
                .where(REGISTRANT_NAMESPACE.eq(
                                registrantReference.namespace())
                        .and(REGISTRANT_REFERENCE.eq(
                                registrantReference.reference()))
                        .and(TARGET_NAMESPACE.eq(
                                targetReference.namespace()))
                        .and(TARGET_REFERENCE.eq(
                                targetReference.reference())))
                .fetchOne();

        return Optional.ofNullable(record)
                .map(JooqRegistrationRepository::toRegistration);
    }

    @Override
    public List<Registration> findByTarget(
            TargetReference targetReference) {
        Objects.requireNonNull(
                targetReference,
                "targetReference must not be null");

        return dsl()
                .select(
                        REGISTRATION_ID,
                        REGISTRANT_NAMESPACE,
                        REGISTRANT_REFERENCE,
                        TARGET_NAMESPACE,
                        TARGET_REFERENCE,
                        LIFECYCLE)
                .from(REGISTRATIONS)
                .where(TARGET_NAMESPACE.eq(targetReference.namespace())
                        .and(TARGET_REFERENCE.eq(
                                targetReference.reference())))
                .orderBy(REGISTRATION_ID.asc())
                .fetch(JooqRegistrationRepository::toRegistration);
    }

    @Override
    public void updateLifecycle(Registration registration) {
        Objects.requireNonNull(
                registration,
                "registration must not be null");

        int updated = dsl()
                .update(REGISTRATIONS)
                .set(
                        LIFECYCLE,
                        toPersistenceValue(registration.lifecycle()))
                .where(REGISTRATION_ID.eq(registration.id()))
                .execute();

        if (updated != 1) {
            throw new IllegalStateException(
                    "Expected exactly one Registration lifecycle row to be updated");
        }
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
                        record.get(TARGET_REFERENCE)),
                fromPersistenceValue(record.get(LIFECYCLE)));
    }

    private static String toPersistenceValue(
            RegistrationLifecycle lifecycle) {
        return switch (lifecycle) {
            case ACTIVE -> "active";
            case CANCELLED -> "cancelled";
        };
    }

    private static RegistrationLifecycle fromPersistenceValue(
            String lifecycle) {
        return switch (lifecycle) {
            case "active" -> RegistrationLifecycle.ACTIVE;
            case "cancelled" -> RegistrationLifecycle.CANCELLED;
            default -> throw new IllegalStateException(
                    "Unsupported persisted Registration lifecycle");
        };
    }
}
