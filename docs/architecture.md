# Architecture

## Purpose

Composable Domain Platform is a modular application platform in which independently bounded capabilities can be developed, tested, composed, and evolved without using one another's private implementation or persistence.

`docs/architecture/workspace.dsl` is the authority for Current architectural elements and relationships. This document owns durable architectural semantics and boundary rules, not a current component inventory.

## Architectural style

The accepted baseline combines:

- Domain-Driven Design bounded contexts for business ownership;
- Hexagonal Architecture inside business modules;
- a modular monolith as the current deployment model;
- explicit public contracts for collaboration;
- explicit compositions for cross-capability workflow;
- adapter boundaries for external protocols/providers;
- static application composition through explicit build dependencies.

The modular monolith is a deployment/build choice, not permission to share ownership or internals.

Architecture optimizes for explicit ownership and replaceable boundaries rather than maximum abstraction.

## Boundaries

Every module is independently owned, selectable in valid application composition, exposes an explicit public API, hides private implementation, and collaborates only through public contracts/adapters. No module depends on another module's private implementation or persistence. ADR-0013 and `docs/modules.md` own the universal module rule.

A business module owns its domain model, application use cases, and internal adapters. When it owns durable state, it also owns its persistence boundary and migrations. Other constructs do not read/write its tables, use its repositories, or reuse internal DTOs as contracts.

Application runtimes may reference private implementation types only for technical construction/wiring. That dependency transfers no business ownership.

Domain code must remain independent of Spring, HTTP, database frameworks, generated OpenAPI types, provider SDKs, and infrastructure technologies.

`core` remains small and business-neutral. Current cross-boundary execution context uses opaque correlation semantics; correlation/causation metadata is technical context, not business state or identity.

A module-owned PostgreSQL schema remains private to its owner. Sharing one PostgreSQL server does not create a shared business data model; direct cross-module persistence access and cross-module joins are prohibited unless a later explicit architecture decision changes that rule.

## Dependency direction

Hexagonal dependencies point inward:

```text
adapter -> application -> domain
             |
             v
        outbound port
             ^
             |
        outbound adapter
```

Application code orchestrates use cases and declares outbound ports. Adapters translate external mechanisms to/from application/domain concepts.

Cross-module workflow uses:

```text
module A public API <- composition -> module B public API
```

A composition depends only on required public module APIs and owns only the workflow spanning them. Missing module behavior must be implemented by the owning module, not absorbed by composition.

Inbound interfaces depend on public module/composition contracts. Provider integrations implement accepted ports. Runtime composition selects and wires implementations; it does not own their behavior.

Build/architecture tests should enforce these directions where mechanically practical. Root Gradle `check` remains the executable aggregate gate for build-affecting changes.

## Architectural constructs

**Module** — independently owned capability satisfying the universal invariant. Implemented modules use explicit public API/private implementation boundaries; local responsibility is documented in their `module.md`.

**Composition** — cross-module workflow coordinator. It is not automatically a module. If deliberately classified as one, it must satisfy the full module invariant.

**Interface** — inbound adapter exposing accepted capabilities through an external protocol. It owns transport mapping, structural protocol validation, external error/privacy mapping, and boundary correlation behavior, not business logic.

**Integration** — outbound/provider adapter translating an internal port/contract to an external system. Provider-specific models do not leak through module public APIs.

**Application runtime/composition root** — technical selection, construction, configuration, migration startup, readiness, and wiring. It owns no module/business behavior.

**Contracts** — versioned external schemas such as OpenAPI. Contracts are not modules. Authoritative source contracts follow externally addressable behavior ownership; concrete applications statically aggregate only explicitly selected source units. Derived aggregate contracts and generated transport types are build output.

**Shared foundation** — minimal business-neutral mechanisms required across accepted boundaries. It must not become a business/shared-utility dumping ground.

Static selectable application composition uses explicit Gradle project/application boundaries (ADR-0015). Omitted unrelated capabilities must be absent from the selected application's functional compile/runtime graph; required capability dependencies remain explicit.

Selectable external contracts use independent authoritative source units plus static application-level aggregation (ADR-0016). Aggregation/generation remains build-time and fails closed on admitted conflict classes. Runtime discovery, dynamic plugins, feature flags, Spring-profile capability selection, or service extraction are not implied.

The operational artifact is an executable Spring Boot/JVM application artifact (ADR-0010). Runtime infrastructure such as host, Java runtime, PostgreSQL, and networking is externally supplied within the accepted operational boundary. Machine-checkable readiness is distinct from process existence and reflects database/migration/serviceability requirements.

Security is a normal independently owned module. Its public Authentication/Authorization contracts remain framework-neutral; Spring Security/HTTP Basic and credential verification are private mechanism details. Domain/workflow facts remain with the owning business module/composition (ADR-0014).

`docs/architecture/workspace.dsl` records which concrete constructs/relationships are Current. Module-local ownership lives in `platform/modules/*/module.md`; external behavior in contracts; persistence in migrations; build selection in Gradle.

## Change rule

Update `workspace.dsl` when a change alters Current architectural elements, relationships, containers, bounded contexts, or significant flows. Keep Planned/Exploratory state distinguishable from Current.

Update this narrative when durable architecture semantics/boundaries change. Update affected module responsibility docs when module ownership/non-ownership changes.

Create or supersede an ADR for significant architecture decisions where rationale/alternatives must remain durable. An ADR does not replace required scope change.

Architecture documentation does not authorize implementation. Source/build/persistence/contract changes still require accepted scope/readiness and the applicable workflow validation.
