package composable.domain.platform.event.persistence;

import composable.domain.platform.event.application.EventRepository;
import composable.domain.platform.event.domain.Event;
import composable.domain.platform.event.domain.PublicationState;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
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
    private static final Field<String> PUBLICATION_STATE =
            DSL.field(DSL.name("publication_state"), String.class);
    private static final Field<String> OWNER_REFERENCE =
            DSL.field(DSL.name("owner_reference"), String.class);

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
                                TIMEZONE,
                                PUBLICATION_STATE,
                                OWNER_REFERENCE)
                        .values(
                                event.id(),
                                event.name(),
                                event.slug(),
                                event.startsAt().getEpochSecond(),
                                event.startsAt().getNano(),
                                event.endsAt().getEpochSecond(),
                                event.endsAt().getNano(),
                                event.timezone().getId(),
                                toPersistenceValue(event.publicationState()),
                                event.owner().orElse(null))
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
                        TIMEZONE,
                        PUBLICATION_STATE,
                        OWNER_REFERENCE)
                .from(EVENTS)
                .where(EVENT_ID.eq(eventId))
                .fetchOne();

        return Optional.ofNullable(record).map(JooqEventRepository::toEvent);
    }

    @Override
    public boolean updatePublicationState(Event event, PublicationState expectedState) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(expectedState, "expectedState must not be null");

        return dsl()
                        .update(EVENTS)
                        .set(PUBLICATION_STATE, toPersistenceValue(event.publicationState()))
                        .where(EVENT_ID.eq(event.id()))
                        .and(PUBLICATION_STATE.eq(toPersistenceValue(expectedState)))
                        .execute()
                == 1;
    }

    @Override
    public boolean updateDefinition(Event event) {
        Objects.requireNonNull(event, "event must not be null");

        return dsl()
                        .update(EVENTS)
                        .set(NAME, event.name())
                        .set(SLUG, event.slug())
                        .set(STARTS_AT_EPOCH_SECOND, event.startsAt().getEpochSecond())
                        .set(STARTS_AT_NANO, event.startsAt().getNano())
                        .set(ENDS_AT_EPOCH_SECOND, event.endsAt().getEpochSecond())
                        .set(ENDS_AT_NANO, event.endsAt().getNano())
                        .set(TIMEZONE, event.timezone().getId())
                        .where(EVENT_ID.eq(event.id()))
                        .and(PUBLICATION_STATE.eq(toPersistenceValue(PublicationState.UNPUBLISHED)))
                        .execute()
                == 1;
    }

    @Override
    public Collection<Event> findPublished() {
        return dsl()
                .select(
                        EVENT_ID,
                        NAME,
                        SLUG,
                        STARTS_AT_EPOCH_SECOND,
                        STARTS_AT_NANO,
                        ENDS_AT_EPOCH_SECOND,
                        ENDS_AT_NANO,
                        TIMEZONE,
                        PUBLICATION_STATE,
                        OWNER_REFERENCE)
                .from(EVENTS)
                .where(PUBLICATION_STATE.eq(toPersistenceValue(PublicationState.PUBLISHED)))
                .fetch(JooqEventRepository::toEvent);
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
                ZoneId.of(record.get(TIMEZONE)),
                fromPersistenceValue(record.get(PUBLICATION_STATE)),
                Optional.ofNullable(record.get(OWNER_REFERENCE)));
    }

    private static String toPersistenceValue(PublicationState publicationState) {
        return switch (publicationState) {
            case UNPUBLISHED -> "unpublished";
            case PUBLISHED -> "published";
            case WITHDRAWN -> "withdrawn";
        };
    }

    private static PublicationState fromPersistenceValue(String publicationState) {
        return switch (publicationState) {
            case "unpublished" -> PublicationState.UNPUBLISHED;
            case "published" -> PublicationState.PUBLISHED;
            case "withdrawn" -> PublicationState.WITHDRAWN;
            default -> throw new IllegalStateException(
                    "Unsupported persisted Event publication state");
        };
    }
}
