alter table event.events
    add column publication_state text not null default 'unpublished';

alter table event.events
    add constraint events_publication_state_check
        check (publication_state in ('unpublished', 'published'));

alter table event.events
    alter column publication_state drop default;
