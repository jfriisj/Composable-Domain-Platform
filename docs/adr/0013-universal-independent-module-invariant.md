# ADR-0013: Universal independent module invariant

## Status

- Status: Accepted
- Date: 2026-08-14

## Context

Composable Domain Platform is intended to behave as independently owned, composable building blocks even while deployed as a modular monolith.

Research #93 showed that the previous module model allowed weaker rules for some technical/workflow capabilities and let runtime code accumulate capability implementation. Decision #94 established that `module` must have one project-wide meaning.

## Decision

Every architectural construct classified as a **module**:

- is independently owned;
- can be selected into or out of a valid platform composition;
- exposes an explicit public API;
- hides private implementation behind that API;
- collaborates with other modules only through public contracts/adapters;
- does not depend on another module's private implementation or persistence;
- is not owned or implemented by an application runtime, another module, or a composition.

A module's public API is its functional collaboration surface. Private implementation includes owned domain/application implementation, framework/provider adapters, persistence adapters/repositories/migrations, and other internals not deliberately exposed.

Selectable composition means application assembly chooses modules explicitly; it does not mean a workflow remains valid without a capability it declares as required. Unrelated modules remain buildable/testable without an omitted module.

Application runtimes own only technical selection, construction, external configuration, wiring, and process startup. Depending on private implementation types for construction does not transfer ownership.

Compositions own cross-module workflow only and depend on participating public APIs. A composition is not automatically a module.

Interfaces, integrations/adapters, shared foundation, and contracts are not automatically modules. Any construct deliberately classified as a module must satisfy the same invariant.

The modular monolith is a deployment choice. One process/repository/build/database server does not weaken ownership or public/private boundaries.

## Rationale

One invariant prevents `module` from becoming a label with different strengths for business, security, technical, or workflow code. It keeps capability ownership independent from runtime convenience and preserves objective selectability and future extraction options.

## Alternatives considered

- Strong independence only for domain modules — rejected because technical/workflow capabilities could become runtime-owned.
- Runtime as a shared implementation module — rejected because the composition root would become an ownership sink.
- Special Security exception — rejected because Authentication/Authorization must obey the same ownership rule.
- Require every architectural construct to be a module — rejected; adapters, runtime, contracts, foundation, and compositions may remain distinct constructs.
- Migrate every deviation in the same decision — rejected; the ADR defines architecture truth, while implementation requires separate accepted scope/readiness.

## Consequences

The repository has one unambiguous module definition. Module admission and architecture tests can enforce independent ownership, public/private boundaries, and no private cross-module collaboration.

Existing constructs that do not satisfy the invariant must be classified as non-modules or migrated through explicit accepted work; they are not exceptions that weaken the rule.

ADR-0012 remains historical rationale for its authentication proof, while this ADR constrains permanent ownership.
