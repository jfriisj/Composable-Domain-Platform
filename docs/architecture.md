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

## Composition over coupling

When two independent capabilities need to cooperate, prefer a composition that depends on their public APIs rather than making either capability depend on the other's implementation.

```text
module A API <- composition -> module B API
```

A composition owns the cross-capability workflow; neither participating bounded context owns the other's business rules.

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

## Conceptual repository layout

This layout is architectural intent, not yet implemented project state:

```text
platform/
├── apps/
├── core/
├── modules/
├── compositions/
├── integrations/
├── interfaces/
├── contracts/
├── docs/
└── build-logic/
```

Business domain modules are expected to use public API and private implementation separation when implementation enters scope.

## Architecture enforcement

Once implementation begins, boundaries are intended to be enforced at multiple levels:

1. Gradle multi-project dependencies.
2. `java-library` API/implementation separation.
3. Spring Modulith module verification.
4. ArchUnit architecture tests.
5. PostgreSQL schema ownership and permissions where appropriate.
6. Automated checks through `./gradlew check`.

These enforcement mechanisms are planned architecture, not yet implemented project state.

## Architecture model

`docs/architecture/workspace.dsl` is the authoritative diagram model. Rendered images are derived views and are not edited as independent sources of truth.
