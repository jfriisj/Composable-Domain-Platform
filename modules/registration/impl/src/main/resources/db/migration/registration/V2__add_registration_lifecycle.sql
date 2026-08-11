alter table registration.registrations
    add column lifecycle text not null default 'active';

alter table registration.registrations
    add constraint registrations_lifecycle_check
        check (lifecycle in ('active', 'cancelled'));

alter table registration.registrations
    alter column lifecycle drop default;
