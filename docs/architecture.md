# Architecture

## Objective

Composable Domain Platform is a modular application platform in which independently bounded business capabilities can be developed, tested, composed, and evolved without relying on each other's internal implementation or persistence model.

The architecture optimizes for explicit ownership and replaceable boundaries rather than for maximum abstraction.

## Architectural style

The baseline combines:

- Domain-Driven Design bounded contexts for business ownership.
- Hexagonal Architecture inside business modules.
- A modular monolith as the initial deployment model.
- Explicit contracts for collaboration.
- Composition modules for cross-capability workflows.
- Adapter-based integrations for external systems.

The modular monolith is an implementation and deployment choice, not permission for modules to share internals.

## Hard boundaries

A business module owns its domain model, application use cases, persistence, migrations, and internal adapters.

Other business modules must not:

- Import its internal domain or implementation classes.
- Access its repositories.
- Read or write its database tables directly.
- Depend on its persistence records.
- Reuse internal DTOs as shared contracts.

Collaboration happens only through explicit public module APIs, published events, or composition modules.

The executable application composition root may depend on private implementation types only for explicit technical wiring authorized by scope. That exception does not permit business logic or cross-module implementation collaboration.

## Hexagonal rule

Dependencies point inward:

~~~text
adapter -> application -> domain
             |
             v
        outbound port
             ^
             |
        outbound adapter
~~~

Domain code must not depend on Spring, HTTP, database frameworks, generated OpenAPI types, provider SDKs, or other infrastructure technologies.

Application code orchestrates use cases and declares required outbound ports. Adapters translate between external mechanisms and application/domain concepts.

## Platform core

Platform core must remain small and contain platform mechanisms rather than business concepts.

The current `core` project contains the minimum business-neutral `CorrelationId` and `ExecutionContext` primitives required to propagate correlation context from the HTTP boundary into Event application calls.

Potential additional core responsibilities require their own concrete accepted need. Business concepts such as Event, Ticket, Registration, Payment, Invoice, Speaker, or Booking must not move into core merely to make them reusable.

## Execution context and traceability

Cross-boundary operations must carry explicit execution metadata so a logical flow can be followed through modules, asynchronous work, integrations, and logs.

- **Correlation ID** identifies the complete logical flow. It is preserved when work crosses synchronous or asynchronous boundaries.
- **Causation ID** identifies the immediate operation, command, event, or message that caused a new asynchronous action or message.
- A new entry point without an existing correlation context creates a new Correlation ID.
- Boundary adapters propagate the correlation context when calling another module or external system where the protocol supports it.
- Published messages and events carry correlation metadata in their envelope rather than embedding it in business-domain state.
- Structured logs include the Correlation ID and, where applicable, the Causation ID.
- Correlation and causation identifiers are opaque technical identifiers. They must not contain personal data or business meaning and must not be used for business decisions.

The current HTTP boundary represents correlation as `X-Correlation-Id`, preserves a supplied nonblank value, creates one when absent, and passes the resulting value through `ExecutionContext` into the Event public application boundary.

Correlation is independent of distributed tracing. W3C trace/span context or OpenTelemetry may later complement correlation, but adopting an observability technology is not required to preserve the platform-level Correlation ID.

## Composition over coupling

When two independent capabilities need to cooperate, prefer a composition that depends on their public APIs rather than making either capability depend on the other's implementation.

~~~text
module A API <- composition -> module B API
~~~

A composition owns the cross-capability workflow; neither participating bounded context owns the other's business rules.

## Current reference module

Event is the first implemented bounded context used to validate the module architecture.

Its physical shape remains:

~~~text
modules/event/
├── api/
└── impl/
~~~

The API project contains application-level contracts for defining and retrieving Event state, explicit duplicate and invalid-definition failures, and the shared execution context carried by the current use-case signatures.

The implementation project contains the Event domain model, application implementation and outbound persistence port, and a private jOOQ PostgreSQL persistence adapter. Event-owned Flyway migrations define its durable schema.

The HTTP adapter and executable application runtime are outside the Event bounded context. They use the Event public API and composition-only implementation wiring without moving Spring, HTTP, generated OpenAPI, or database runtime concepts into Event domain/application code.

## Current runtime boundary

The first executable vertical slice is:

~~~text
external HTTP caller
        |
        v
contracts/http/v1/event.yaml
        |
        v
interfaces/http
        |
        | Event public API + ExecutionContext
        v
modules/event/api
        ^
        |
modules/event/impl ----> event.events
        ^
        |
apps/platform
  Spring Boot composition root
  PostgreSQL configuration
  Event Flyway startup migration
~~~

`apps/platform` starts the Spring Boot process and wires `interfaces/http`, the Event application services, `JooqEventRepository`, and the runtime `DataSource`. Event-owned Flyway migrations run during application context construction before the Event repository/application beans become available to serve requests.

## Persistence ownership

Event implements the persistence baseline through an Event-owned PostgreSQL schema and versioned Flyway migrations.

The Event application layer owns the persistence port. The private jOOQ adapter depends inward on that port and Event domain concepts; domain and application code do not depend on database technologies or persistence-adapter implementation.

One PostgreSQL server does not imply one shared data model. Cross-module joins and direct cross-schema persistence access are prohibited unless a later explicit architecture decision changes this rule.

Database permission enforcement remains deferred until operational scope requires it.

## External contracts

`contracts/http/v1/event.yaml` is the authoritative external HTTP contract for the current Event define/retrieve surface.

OpenAPI Generator derives the server interface and transport models during the build. Generated sources are adapter-layer build output and must not become Event domain or application models.

The HTTP interface owns transport mapping, structural HTTP validation, contract-defined error responses, and correlation establishment. Event continues to own business validation and result semantics.

## Dynamic interfaces

Public and administrative frontends are clients of stable contracts, not owners of business logic or database structure.

Dynamic page composition may be introduced when a concrete use case requires it. Its contracts must remain separate from business-domain internals.

## Repository layout

The currently implemented architectural structure includes:

~~~text
.
├── apps/
│   └── platform/
├── build-logic/
├── contracts/
│   └── http/
│       └── v1/
│           └── event.yaml
├── core/
├── interfaces/
│   └── http/
├── modules/
│   └── event/
│       ├── api/
│       ├── impl/
│       └── module.md
└── docs/
~~~

`compositions/` and `integrations/` remain architectural categories only and must not be created until accepted scope requires them.

## Architecture enforcement

Current build-time enforcement includes:

1. Separate Gradle projects for core, Event API/implementation, HTTP interface, and executable platform runtime.
2. `java-library` dependency semantics for library boundaries.
3. Event ArchUnit tests for domain/application/persistence-adapter dependency direction.
4. Platform ArchUnit tests for core, Event API, HTTP interface, and application-runtime dependency boundaries.
5. Event-owned Flyway migrations and PostgreSQL persistence integration tests through Testcontainers.
6. Running Spring Boot HTTP end-to-end tests against real PostgreSQL through Testcontainers, including contract success/error behavior and correlation handling.
7. Root `./gradlew --no-daemon check` aggregation across all current projects.

Additional enforcement remains deferred until explicitly scoped:

- Spring Modulith module verification.
- PostgreSQL permission enforcement.

## Architecture model

`docs/architecture/workspace.dsl` is the authoritative diagram model. Rendered images are derived views and are not edited as independent sources of truth.
