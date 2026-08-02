# ADR-0005: Event-owned PostgreSQL persistence through a Hexagonal adapter

- Status: Accepted
- Date: 2026-08-02

## Context

The Event bounded context currently defines and returns Event state but cannot preserve that state beyond the application-service execution that created it.

The next accepted use case requires an Event to be stored durably and retrieved later by Event identity.

The architecture already requires each bounded context to own its persistence boundary and prohibits cross-module access to implementation details or database tables. Domain and application code must remain independent of database frameworks.

The repository technology directions already identify PostgreSQL for relational persistence, Flyway for schema migrations, jOOQ for explicit SQL access, and Testcontainers for tests against real infrastructure. Their presence in the technology document did not authorize implementation before this concrete requirement entered scope.

## Decision

Persist Event state in PostgreSQL through a private outbound adapter inside the existing `event-impl` project.

The Event application layer declares the persistence port. The persistence adapter implements that port and may depend inward on Event application/domain concepts. Event domain and application code do not depend on PostgreSQL, Flyway, jOOQ, Testcontainers, persistence records, or the adapter implementation.

Event owns its PostgreSQL schema and versioned Flyway migrations. No shared business schema or cross-context persistence access is introduced.

Use jOOQ for SQL access inside the persistence adapter.

Use Testcontainers to validate the adapter and Flyway migrations against real PostgreSQL as part of the repository validation gate.

No Spring Boot, Spring Data, Hibernate/JPA, application runtime, or HTTP interface is introduced by this decision.

## Alternatives considered

- Keep Event state only in memory or use an in-memory test double.
- Use H2 or another database substitute for persistence tests.
- Use plain JDBC with ad-hoc schema creation.
- Use JPA/Hibernate or Spring Data.
- Introduce a standalone persistence Gradle project or generic shared repository abstraction.

## Consequences

- Event state can survive beyond the service instance that created it.
- The Event bounded context becomes responsible for its own schema and migration history.
- Database-specific code remains replaceable behind the Event application persistence port.
- Integration validation now requires a Docker-compatible container runtime for Testcontainers locally and in CI.
- The root validation gate becomes capable of detecting migration, PostgreSQL mapping, persistence-adapter, and retrieval regressions.
- The existing `event-api` / `event-impl` physical boundary remains unchanged.
- Additional persistence behavior such as update/delete lifecycle, shared database abstractions, production runtime configuration, backups, replication, and cross-context queries remains outside this decision.
