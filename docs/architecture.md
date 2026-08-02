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

## Hexagonal rule

Dependencies point inward:

```text
adapter -> application -> domain
             |
             v
        outbound port
             ^
             |
        outbound adapter
```

Domain code must not depend on Spring, HTTP, database frameworks, generated OpenAPI types, provider SDKs, or other infrastructure technologies.

Application code orchestrates use cases and declares required outbound ports. Adapters translate between external mechanisms and application/domain concepts.

## Platform core

Platform core must remain small and contain platform mechanisms rather than business concepts.

Potential core responsibilities include module identity/description, capability discovery, shared execution context primitives, and event-dispatch mechanisms when concrete implementation requires them.

Business concepts such as Event, Ticket, Registration, Payment, Invoice, Speaker, or Booking must not move into core merely to make them reusable.

## Execution context and traceability

Cross-boundary operations must carry explicit execution metadata so a logical flow can be followed through modules, asynchronous work, integrations, and logs.

- **Correlation ID** identifies the complete logical flow. It is preserved when work crosses synchronous or asynchronous boundaries.
- **Causation ID** identifies the immediate operation, command, event, or message that caused a new asynchronous action or message.
- A new entry point without an existing correlation context creates a new Correlation ID.
- Boundary adapters propagate the correlation context when calling another module or external system where the protocol supports it.
- Published messages and events carry correlation metadata in their envelope rather than embedding it in business-domain state.
- Structured logs include the Correlation ID and, where applicable, the Causation ID.
- Correlation and causation identifiers are opaque technical identifiers. They must not contain personal data or business meaning and must not be used for business decisions.

The exact wire representation for HTTP, events, and other protocols belongs to the relevant contract work. The architecture requires the semantics and propagation behavior, not a specific identifier format at this stage.

Correlation is independent of distributed tracing. W3C trace/span context or OpenTelemetry may later complement correlation, but adopting an observability technology is not required to preserve the platform-level Correlation ID.

## Composition over coupling

When two independent capabilities need to cooperate, prefer a composition that depends on their public APIs rather than making either capability depend on the other's implementation.

```text
module A API <- composition -> module B API
```

A composition owns the cross-capability workflow; neither participating bounded context owns the other's business rules.

## Current reference module

Event is the first implemented bounded context used to validate the module architecture.

Its current physical shape is:

```text
modules/event/
├── api/
└── impl/
```

The API project contains only the application-level contract required to define an Event and return its state. The implementation project contains the Event domain model and application implementation. No persistence, HTTP adapter, runtime framework, or event publication mechanism is part of the current reference slice.

## Persistence ownership

The intended persistence baseline is PostgreSQL with schema ownership aligned to bounded contexts.

One PostgreSQL server does not imply one shared data model. Cross-module joins and direct cross-schema persistence access are prohibited unless a later explicit architecture decision changes this rule.

Database permissions should eventually reinforce ownership where operationally practical.

## External contracts

OpenAPI is intended to be the authoritative HTTP contract between platform interfaces and external clients.

Generated transport models are adapter-layer types and must not become the domain model.

## Dynamic interfaces

Public and administrative frontends are clients of stable contracts, not owners of business logic or database structure.

Dynamic page composition may be introduced when a concrete use case requires it. Its contracts must remain separate from business-domain internals.

## Repository layout

The currently implemented architectural structure includes:

```text
.
├── build-logic/
├── modules/
│   └── event/
│       ├── api/
│       ├── impl/
│       └── module.md
└── docs/
```

Additional top-level architectural areas such as `core/`, `compositions/`, `integrations/`, `interfaces/`, `contracts/`, and `apps/` remain architectural intent and must not be created until accepted scope requires them.

## Architecture enforcement

Current build-time enforcement includes:

1. Separate Gradle projects for the Event public API and private implementation.
2. `java-library` dependency semantics.
3. ArchUnit architecture tests for the accepted Event domain/application dependency direction.
4. Root `./gradlew check` aggregation across build logic and current projects, including the Event architecture verification.

Additional enforcement remains deferred until explicitly scoped:

- Spring Modulith module verification.
- PostgreSQL schema ownership and permissions.

## Architecture model

`docs/architecture/workspace.dsl` is the authoritative diagram model. Rendered images are derived views and are not edited as independent sources of truth.
