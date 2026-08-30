package composable.domain.platform.waitlist.persistence;

import composable.domain.platform.waitlist.application.WaitlistParticipationRepository;
import composable.domain.platform.waitlist.domain.WaitlistParticipation;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;

final class JooqWaitlistParticipationRepository
        implements WaitlistParticipationRepository {

    private static final Table<Record> PARTICIPATIONS =
            DSL.table(DSL.name("waitlist", "participations"));
    private static final Field<String> PARTICIPATION_ID =
            DSL.field(DSL.name("waitlist_participation_id"), String.class);
    private static final Field<String> PARTICIPANT_REFERENCE =
            DSL.field(DSL.name("participant_reference"), String.class);
    private static final Field<String> EVENT_REFERENCE =
            DSL.field(DSL.name("event_reference"), String.class);

    private final DataSource dataSource;

    JooqWaitlistParticipationRepository(DataSource dataSource) {
        this.dataSource =
                Objects.requireNonNull(
                        dataSource,
                        "dataSource must not be null");
    }

    @Override
    public boolean addIfAbsent(WaitlistParticipation participation) {
        Objects.requireNonNull(
                participation,
                "participation must not be null");

        return dsl()
                        .insertInto(PARTICIPATIONS)
                        .columns(
                                PARTICIPATION_ID,
                                PARTICIPANT_REFERENCE,
                                EVENT_REFERENCE)
                        .values(
                                participation.id(),
                                participation.participantReference(),
                                participation.eventReference())
                        .onConflict(PARTICIPANT_REFERENCE, EVENT_REFERENCE)
                        .doNothing()
                        .execute()
                == 1;
    }

    @Override
    public Optional<WaitlistParticipation> findByParticipantAndEvent(
            String participantReference,
            String eventReference) {
        Objects.requireNonNull(
                participantReference,
                "participantReference must not be null");
        Objects.requireNonNull(
                eventReference,
                "eventReference must not be null");

        Record record = dsl()
                .select(
                        PARTICIPATION_ID,
                        PARTICIPANT_REFERENCE,
                        EVENT_REFERENCE)
                .from(PARTICIPATIONS)
                .where(PARTICIPANT_REFERENCE.eq(participantReference)
                        .and(EVENT_REFERENCE.eq(eventReference)))
                .fetchOne();

        return Optional.ofNullable(record)
                .map(JooqWaitlistParticipationRepository::toParticipation);
    }

    private DSLContext dsl() {
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }

    private static WaitlistParticipation toParticipation(Record record) {
        return new WaitlistParticipation(
                record.get(PARTICIPATION_ID),
                record.get(PARTICIPANT_REFERENCE),
                record.get(EVENT_REFERENCE));
    }
}
