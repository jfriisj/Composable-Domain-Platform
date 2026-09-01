create schema waitlist;

create table waitlist.participations (
    waitlist_participation_id text primary key,
    participant_reference text not null,
    event_reference text not null,
    constraint waitlist_participations_participant_event_unique
        unique (
            participant_reference,
            event_reference
        )
);
