alter table event.events
    drop constraint events_publication_state_check;

alter table event.events
    add constraint events_publication_state_check
        check (publication_state in ('unpublished', 'published', 'withdrawn'));
