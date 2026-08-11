create schema event;

create table event.events (
    event_id text primary key,
    name text not null,
    slug text not null,
    starts_at_epoch_second bigint not null,
    starts_at_nano integer not null check (starts_at_nano between 0 and 999999999),
    ends_at_epoch_second bigint not null,
    ends_at_nano integer not null check (ends_at_nano between 0 and 999999999),
    timezone text not null
);
