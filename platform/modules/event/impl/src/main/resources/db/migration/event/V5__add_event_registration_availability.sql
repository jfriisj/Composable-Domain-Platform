alter table event.events
    add column registration_availability text not null default 'open';

alter table event.events
    add constraint events_registration_availability_check
        check (registration_availability in ('open', 'closed'));

alter table event.events
    alter column registration_availability drop default;
