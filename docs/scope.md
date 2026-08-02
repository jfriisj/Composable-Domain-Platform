# Project Scope

## Authority

This document is the authoritative source for the current accepted project scope of Composable Domain Platform.

## Purpose

Composable Domain Platform provides a reliable foundation for composing independently bounded business capabilities. It is not tied to one business domain.

Event management is the first reference capability, but it is not the platform core and does not define the platform's general model.

## Current phase

**Event durable persistence**

The repository, architecture, executable Gradle build, project workflow, first Event reference module, continuous-integration foundation, and executable Event architecture verification have been accepted.

The current phase gives the existing Event bounded context its first durable outbound adapter and proves persistence ownership without introducing an application runtime or external transport.

## Concrete requirement

A platform operator must be able to define an Event and later retrieve the same Event by its identity after the original application-service instance no longer exists.

A successful Event definition must be durably stored before success is returned. Retrieval must reconstruct the accepted Event state from durable storage without exposing persistence-specific types through the Event public API.

The retrieval contract must define an explicit, transport-independent result for an unknown Event identity.

Because Event identity identifies one durable Event and update/replace behavior is outside this phase, defining an Event whose identity already exists must be rejected without changing the persisted Event. The failure must be expressed independently of PostgreSQL/jOOQ exceptions or persistence records.

This phase extends only the existing Event capability. It does not add Event update/delete lifecycle behavior, another bounded context, HTTP exposure, application bootstrap, messaging, or external integration.

## Event persistence admission

### Concrete use case

Define an Event now and retrieve its accepted state later by Event identity.

### Why the current baseline is insufficient

The current `DefineEvent` use case constructs and returns Event state but has no outbound persistence port, durable adapter, database schema, or retrieval use case. State therefore cannot survive beyond the execution that created it.

### Event owns

- The durable representation of the Event state already owned by the Event bounded context.
- The Event persistence port used by Event application services.
- The Event PostgreSQL schema and its migrations.
- Mapping between Event domain state and Event-owned persistence records.
- Retrieval of an Event by its identity through the Event public application API.

### Event does not own

- A shared platform-wide business repository or shared business schema.
- Persistence for another bounded context.
- Cross-context joins or direct access to another bounded context's tables.
- Registration, ticketing, booking, membership, speakers/program management, content, payments/accounting, notifications, or identity-provider concerns.
- HTTP transport, application runtime, deployment, or external-provider concerns.

## Technology decision

### Problem

The accepted Event implementation has no durable storage mechanism. In-memory state or test doubles cannot satisfy the requirement that Event state survive the application-service instance that created it.

The first persistence implementation must also make Event schema ownership reproducible and test the actual database semantics used by the accepted platform direction.

### Requirement

The Event implementation needs a durable relational store, version-controlled Event-owned schema migrations, explicit SQL access inside a private persistence adapter, and automated integration tests against the real selected database.

The domain and application layers must remain independent of database libraries and runtime frameworks.

### Alternatives considered

- **PostgreSQL + Flyway + jOOQ + Testcontainers** — matches the accepted technology directions, keeps schema ownership explicit, supports version-controlled migrations and explicit SQL, and can be tested against real PostgreSQL without requiring a Spring runtime.
- **In-memory or file-backed test storage** — useful as a test double but does not satisfy or prove the durable PostgreSQL requirement.
- **H2 or another substitute database** — durable in some modes, but does not verify PostgreSQL schema and SQL behavior and would add a second database technology without a requirement.
- **Plain JDBC with ad-hoc schema creation** — can access PostgreSQL but leaves migration/versioning concerns bespoke and bypasses the accepted Flyway/jOOQ direction.
- **JPA/Hibernate or Spring Data** — can provide persistence, but adds an ORM and/or Spring runtime surface that is not required for this use case.

### Decision

Use PostgreSQL as the Event durable store, Flyway as the authoritative Event schema-migration mechanism, jOOQ for SQL access inside the Event persistence adapter, and Testcontainers for integration tests against real PostgreSQL.

Pin exact dependency versions through the repository version catalog when implementation begins.

Dependencies required to connect these selected technologies to PostgreSQL are authorized only as implementation details of this persistence slice.

Spring Boot, Spring Data, Hibernate/JPA, and Spring Modulith are not authorized by this decision.

## In scope

- Extend the Event public API with the smallest application-level contract required to retrieve an Event by identity.
- Keep public Event API types independent of PostgreSQL, Flyway, jOOQ, Testcontainers, and other infrastructure types.
- Add an outbound persistence port owned by the Event application implementation.
- Persist every successfully defined Event before returning the successful definition result.
- Retrieve persisted Event state by Event identity and reconstruct the existing Event domain state and `EventView`.
- Define explicit behavior for an unknown Event identity without leaking database exceptions or persistence records.
- Reject definition of an Event whose identity already exists without overwriting the stored Event and without leaking persistence-specific exceptions or types.
- Add a private persistence adapter inside the existing `event-impl` Gradle project; do not create a new persistence Gradle project or top-level architectural area.
- Add an Event-owned PostgreSQL schema through version-controlled Flyway migrations.
- Use jOOQ only inside the Event persistence adapter.
- Add the PostgreSQL connectivity required by the selected persistence stack.
- Add Testcontainers-backed integration tests against real PostgreSQL and apply the Event Flyway migrations in those tests.
- Prove durability by retrieving a previously defined Event through a fresh application-service composition connected to the same database state.
- Preserve every currently accepted Event field across the persistence round trip: identity, name, slug, scheduled start, scheduled end, and timezone.
- Extend the existing ArchUnit verification so Event domain and application code cannot depend on the persistence adapter or database infrastructure, while the persistence adapter may depend inward on the Event application persistence port and Event domain.
- Keep the existing `event-api` / `event-impl` Gradle boundary.
- Keep root `./gradlew --no-daemon check` as the authoritative validation gate and run the real PostgreSQL integration validation through that gate.
- Keep the existing GitHub Actions workflow and required `validate` status check as the merge gate.
- Update Event module documentation, architecture documentation/model descriptions, README current-state text, and project status when implementation is accepted.
- Record the persistence architecture rationale in ADR-0005.

## Acceptance criteria

The phase is complete when:

1. The Event public API supports retrieving an Event by identity using only Event API and Java platform types.
2. Successful Event definition durably stores all currently accepted Event state before returning success.
3. A fresh application-service composition connected to the same PostgreSQL state can retrieve the previously defined Event.
4. Retrieval preserves Event identity, name, slug, start, end, and timezone exactly according to the application contract.
5. Retrieval of an unknown Event identity has explicit tested behavior and does not leak persistence-specific exceptions or types.
6. Defining an Event whose identity already exists is rejected, leaves the previously persisted Event unchanged, and does not leak persistence-specific exceptions or types.
7. Event application code declares and uses an outbound persistence port rather than depending on a PostgreSQL/jOOQ adapter directly.
8. Event domain code remains independent of persistence and database technologies.
9. The persistence adapter remains private inside `event-impl` and depends inward on Event application/domain concepts rather than the reverse.
10. Flyway migrations define the Event-owned PostgreSQL schema reproducibly and no shared business schema or cross-context persistence access is introduced.
11. jOOQ database access is confined to the Event persistence adapter.
12. Integration tests use Testcontainers with real PostgreSQL and execute the accepted Flyway migrations.
13. Existing ArchUnit verification is extended to enforce the introduced persistence-adapter dependency direction.
14. Root `./gradlew --no-daemon check` executes unit, architecture, and required persistence integration validation successfully.
15. The required GitHub `validate` check succeeds for the compliant implementation.
16. No Spring Boot/runtime bootstrap, Spring Modulith, HTTP/OpenAPI, messaging, deployment, external integration, additional bounded context, or unrelated Event lifecycle behavior is introduced.
17. `modules/event/module.md`, relevant architecture/current-state documentation, and `docs/project-status.md` reflect the accepted persistence implementation after completion.

## Explicitly out of scope

The following remain intentionally excluded from the current phase:

- Event update, delete, publication, status, visibility, registration-opening, or other lifecycle behavior beyond define and retrieve.
- Spring Boot application bootstrap.
- Spring Modulith configuration or verification.
- Spring Data, Hibernate, or JPA.
- OpenAPI contracts or generation.
- HTTP controllers or other external interfaces.
- Application-runtime modules or production bootstrap/wiring.
- A separate persistence Gradle project, shared repository framework, generic platform persistence abstraction, or new top-level architectural area.
- Shared business database schemas, cross-context joins, or direct cross-context persistence access.
- Production connection-pool, secrets-management, environment-configuration, backup, replication, or database-operations infrastructure.
- Event publication or messaging infrastructure.
- Registration, ticketing, booking, membership, speaker/program, content, payment, accounting, notification, or other business capabilities.
- Frontend implementation.
- Deployment automation.
- Release automation or automatic version/tag creation.
- Artifact or package publication.
- Docker image builds or registry publication; a local/container runtime required by Testcontainers is test infrastructure only.
- Multi-platform or multi-JDK CI matrices.
- Code coverage services or quality dashboards.
- Broad static-analysis or security-scanning suites beyond the accepted ArchUnit rules.
- External CI services.
- Dependency-update automation.
- External provider integrations.
- Kafka, RabbitMQ, Redis, Kubernetes, or other infrastructure without a demonstrated requirement.
- Multi-model development workflow automation.

These items may enter a later phase only through an explicit scope decision.

## Business capability admission rule

A new business capability may enter active scope only when all of the following can be answered:

1. What concrete use case requires it?
2. Why can the requirement not be satisfied within the currently accepted scope?
3. What does the proposed bounded context own?
4. What does it explicitly not own?

## Technology admission rule

A new technology or infrastructure component may enter active scope only when:

1. A concrete accepted requirement exists.
2. The current baseline cannot satisfy that requirement adequately.
3. Reasonable alternatives have been considered.
4. The operational and architectural consequences are understood.

Technology must solve an accepted requirement; the project must not invent requirements to justify a technology.

## Scope change rule

Changes to this document are project decisions and must be made through a topic branch and pull request.

A pull request that introduces functionality outside the accepted scope must either remove the out-of-scope change or explicitly update this document and justify the scope change.

Hidden scope expansion inside implementation pull requests is not accepted.

## Deferred ideas

Potential future capabilities may be recorded as deferred ideas, but a deferred idea is not planned scope and must not create implementation, module, infrastructure, or API commitments.

Examples currently include content management, registration, ticketing, booking, membership, surveys, payment integrations, and accounting integrations.

Their eventual bounded-context boundaries must be determined from real use cases rather than assumed in advance.
