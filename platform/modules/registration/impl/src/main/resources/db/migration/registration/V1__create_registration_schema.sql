create schema registration;

create table registration.registrations (
    registration_id text primary key,
    registrant_namespace text not null,
    registrant_reference text not null,
    target_namespace text not null,
    target_reference text not null,
    constraint registrations_registrant_target_unique
        unique (
            registrant_namespace,
            registrant_reference,
            target_namespace,
            target_reference
        )
);
