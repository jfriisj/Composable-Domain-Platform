# ADR-0013: Universal independent module invariant

- Status: Accepted
- Date: 2026-08-14

## Context

Composable Domain Platform is intended to behave as a set of independently owned, composable building blocks even while the current deployment model is a modular monolith.

Research #93 identified that the accepted module model was weaker than that intent. `docs/modules.md` gave the strongest public-API/private-implementation separation to domain modules, described independence as an eventual target, and allowed the application runtime to accumulate technical implementation concerns. The participant-authentication proof accepted by ADR-0012 and implemented through #91 made the gap concrete: Spring Security configuration, credential verification, and actor establishment currently live in `platform/apps/platform`.

Decision #94 establishes that this is not a Security-specific rule. Every architectural construct classified as a module must obey the same ownership and independence invariant.

The purpose of this ADR is to make the module rule authoritative before corrective migration. It does not pretend that the current repository has already completed that migration.

## Decision

### Universal module invariant

Every architectural construct classified as a **module**:

- is independently owned;
- can be selected into or out of a valid platform composition;
- exposes an explicit public API;
- hides its private implementation behind that API;
- collaborates with other modules only through explicit public contracts and adapters;
- does not depend on another module's private implementation or persistence;
- is not owned or implemented by the application runtime;
- is not owned or implemented by another module;
- is not owned or implemented by a composition.

A module's public API is the only module-owned surface that other modules, compositions, interfaces, integrations, or runtimes may use for functional collaboration.

Private implementation includes domain/application implementation details, framework adapters, persistence adapters, repositories, migrations, provider-specific code, and other module-owned internals that are not part of the deliberate collaboration contract.

### Selectable composition semantics

"Can be selected into or out of a valid platform composition" means that application assembly chooses participating modules explicitly.

It does not mean every composition remains valid when a declared dependency is absent. A composition that requires module A and module B is valid only when those required public module capabilities are supplied.

Unrelated modules remain independently buildable and testable without an omitted module. Omitting one module must not require unrelated modules to import, compile against, or execute that module's private implementation.

### Application runtime

`platform/apps/platform` is a technical composition root, not a module owner.

The runtime may:

- select which accepted modules and compositions participate in an application;
- construct private module implementations;
- supply external configuration;
- connect public APIs to required adapters;
- wire compositions;
- start the executable process.

The runtime may not become the permanent owner of a module's behavior merely because framework configuration or dependency injection is technically convenient there.

Depending on private implementation types for construction/wiring does not transfer ownership to the runtime.

### Compositions

A composition owns only the cross-module workflow it coordinates.

A composition may depend on public APIs of participating modules. It must not own, implement, or reach into the private implementation or persistence of those modules.

A composition is not automatically a module. If a composition is classified and packaged as a module, it must satisfy the universal module invariant, including explicit public-API/private-implementation separation.

### Interfaces, integrations, adapters, and foundation

An interface or integration is an architectural adapter boundary and is not automatically a module. If one is deliberately classified as a module, it must satisfy the same invariant.

Adapters translate protocols, providers, and mechanisms to or from module public contracts. Adapter placement does not transfer module ownership.

Shared platform foundation such as the current `core` project is not automatically a module. Foundation must remain minimal and business-neutral. If a future decision classifies a foundation construct as a module, it must satisfy this invariant.

Contracts such as OpenAPI documents are contracts, not modules.

### Modular-monolith deployment

The modular monolith is a deployment choice only.

Co-location in one process, one repository, one Gradle build, or one database server does not weaken logical module ownership, public/private boundaries, or adapter-based collaboration.

The invariant is intentionally compatible with a future deployment in which selected modules could be separated further without changing who owns their contracts and implementation.

### Current-state consequences

Event and Registration already approximate the required shape through separate public API and private implementation Gradle projects.

The current Event-Registration composition is one Gradle project. It must not be treated as conforming to a module classification unless a later accepted migration gives it the required public/private separation; until then it is an accepted composition with migration debt against any module classification.

The current participant-authentication/security proof is implemented inside `platform/apps/platform`. ADR-0012 remains historical accepted rationale for that minimum proof, but application-runtime ownership of Security is not the target architecture under this ADR.

Authentication and authorization receive no special exception. Security is a platform module and must have independent ownership, its own public API and private implementation, selectable composition semantics, and adapter-based collaboration like every other module.

The current HTTP interface and `core` project remain current executable architectural constructs. This ADR does not silently reclassify them as conforming modules; terminology must distinguish modules from non-module adapter/foundation/runtime constructs.

Current deviations are migration debt, not exceptions to the universal rule.

### Migration control

This ADR changes architectural truth but does not authorize source movement, new Gradle projects, Security API design, composition splitting, or other corrective implementation by itself.

Corrective migration must pass through explicit scope admission and then be decomposed into the smallest coherent implementation slices with executable architecture enforcement.

The authoritative Structurizr model continues to represent current implemented participants and relationships until those migrations are accepted and implemented. Planned module structures must not be shown as Current before implementation.

## Alternatives considered

### Keep the stronger independence rule only for domain modules

Rejected because it permits technical or workflow capabilities to accumulate implementation ownership in the runtime or compositions and makes "module" mean different things across the platform.

### Treat the application runtime as a shared implementation module

Rejected because the composition root would become an ownership sink. Runtime selection, construction, configuration, and wiring are technical assembly responsibilities, not capability ownership.

### Make Security a special exception

Rejected because the architecture must not depend on the first consumer or first technology used by a capability. Authentication and authorization belong to the Security module, which must obey the same universal module rule.

### Require every architectural construct to be a module

Rejected. Runtime, contracts, adapters, foundation, and compositions may be distinct architectural constructs. The invariant applies whenever the project calls a construct a module; constructs that should not satisfy it must be classified explicitly rather than weakening the definition.

### Migrate all current deviations in the same decision

Rejected because the ADR establishes architectural truth. Source/build migration requires explicit scope, ownership, dependency, and validation gates and should proceed as coherent implementation slices.

## Consequences

The repository gains one unambiguous meaning of "module".

Future module admission and architecture review can reject implementations that place module behavior in the application runtime, another module, or a composition.

Security relocation is now a known corrective architecture need rather than an optional cleanup, but its exact public API, private implementation, adapter structure, and migration remain subject to later scope and implementation decisions.

Event-Registration and any other construct called a module must be audited against the same rule.

Architecture tests and Gradle boundaries must eventually enforce the accepted invariant where mechanically practical.

ADR-0012 remains Accepted as the rationale and historical record for the minimum participant-authentication proof. ADR-0013 constrains the permanent ownership model going forward; it does not rewrite ADR-0012's historical decision.

Refs: Goal #57; research #93; decision #94; documentation #95.
