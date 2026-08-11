package composable.domain.platform.event.persistence;

import composable.domain.platform.event.application.EventRepository;
import composable.domain.platform.event.domain.Event;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;

public final class JooqEventRepository implements EventRepository {

    private static final Table<Record> EVENTS = DSL.table(DSL.name("event", "events"));
    private static final Field<String> EVENT_ID =
            DSL.field(DSL.name("event_id"), String.class);
    private static final Field<String> NAME =
            DSL.field(DSL.name("name"), String.class);
    private static final Field<String> SLUG =
            DSL.field(DSL.name("slug"), String.class);
    private static final Field<Long> STARTS_AT_EPOCH_SECOND =
            DSL.field(DSL.name("starts_at_epoch_second"), Long.class);
    private static final Field<Integer> STARTS_AT_NANO =
            DSL.field(DSL.name("starts_at_nano"), Integer.class);
    private static final Field<Long> ENDS_AT_EPOCH_SECOND =
            DSL.field(DSL.name("ends_at_epoch_second"), Long.class);
    private static final Field<Integer> ENDS_AT_NANO =
            DSL.field(DSL.name("ends_at_nano"), Integer.class);
    private static final Field<String> TIMEZONE =
            DSL.field(DSL.name("timezone"), String.class);

    private final DataSource dataSource;

    public JooqEventRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public boolean addIfAbsent(Event event) {
        Objects.requireNonNull(event, "event must not be null");

        return dsl()
                        .insertInto(EVENTS)
                        .columns(
                                EVENT_ID,
                                NAME,
                                SLUG,
                                STARTS_AT_EPOCH_SECOND,
                                STARTS_AT_NANO,
                                ENDS_AT_EPOCH_SECOND,
                                ENDS_AT_NANO,
                                TIMEZONE)
                        .values(
                                event.id(),
                                event.name(),
                                event.slug(),
                                event.startsAt().getEpochSecond(),
                                event.startsAt().getNano(),
                                event.endsAt().getEpochSecond(),
                                event.endsAt().getNano(),
                                event.timezone().getId())
                        .onConflict(EVENT_ID)
                        .doNothing()
                        .execute()
                == 1;
    }

    @Override
    public Optional<Event> findById(String eventId) {
        Objects.requireNonNull(eventId, "eventId must not be null");

        Record record = dsl()
                .select(
                        EVENT_ID,
                        NAME,
                        SLUG,
                        STARTS_AT_EPOCH_SECOND,
                        STARTS_AT_NANO,
                        ENDS_AT_EPOCH_SECOND,
                        ENDS_AT_NANO,
                        TIMEZONE)
                .from(EVENTS)
                .where(EVENT_ID.eq(eventId))
                .fetchOne();

        return Optional.ofNullable(record).map(JooqEventRepository::toEvent);
    }

    private DSLContext dsl() {
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }

    private static Event toEvent(Record record) {
        return new Event(
                record.get(EVENT_ID),
                record.get(NAME),
                record.get(SLUG),
                Instant.ofEpochSecond(
                        record.get(STARTS_AT_EPOCH_SECOND),
                        record.get(STARTS_AT_NANO)),
                Instant.ofEpochSecond(
                        record.get(ENDS_AT_EPOCH_SECOND),
                        record.get(ENDS_AT_NANO)),
                ZoneId.of(record.get(TIMEZONE)));
    }
}
