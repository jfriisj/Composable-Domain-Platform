# ADR-0017: Composition-owned cross-module delivery

## Status

- Status: Accepted
- Date: 2026-08-23
- Supersedes ADR-0013 only where ADR-0013 permitted functional module-to-module collaboration through public contracts/adapters; cross-module workflows are now composition-owned and modules have no functional dependencies on other modules.
- Supersedes ADR-0015 only where ADR-0015's Event-only proof composition treated omitting Security as a permanent application-membership restriction.

## Context

ADR-0013 established universal module independence, ADR-0014 defined the framework-neutral Security boundary, ADR-0015 proved static selectable application composition, and ADR-0016 established selectable external contract aggregation.

Several earlier proof-specific delivery rules now obstruct accepted use cases:
1. ADR-0015's Event-only proof composition omitted Security, but accepted organizer-owned Event management now requires Authentication and Security-owned Authorization;
2. module documentation permitted direct public module-to-module dependencies, creating ambiguity about where cross-capability coordination lives;
3. scope admission treated ordinary API, contract, persistence, adapter, and composition changes as requiring separate scope transitions even when implementing already accepted use-case semantics.

## Decision

1. **Module independence without direct cross-module dependencies:** Every module owns its bounded capability, public API, private implementation, and persistence. Modules have no functional compile/runtime dependencies on other modules.
2. **Composition-owned cross-module workflows:** Use cases spanning multiple capabilities are coordinated by compositions that depend only on participating public module APIs (`composition -> module A public API, module B public API`). Participating modules remain unaware of each other. Compositions own cross-module workflow only, not module behavior, state, or persistence.
3. **Owned capability boundaries:**
   - Event owns Event identity, definition, organizer/owner reference, publication lifecycle, unpublished modification, persistence, and public API. Event does not depend on Registration or Security.
   - Registration owns Registration identity, registrant-to-target relationship, lifecycle, uniqueness, persistence, and public API. Registration remains domain-neutral and does not depend on Event or Security.
   - Security owns Authentication and Authorization. Private implementation may use Spring Security; public API remains framework-neutral.
4. **Use-case-driven application selection:** Static selectable composition (ADR-0015) remains accepted. An application selects the modules, compositions, adapters, contracts, and technical infrastructure required by its declared accepted use cases. Historical proof membership does not permanently restrict future application compositions.
5. **Semantic, proportional scope admission:** Accepted scope governs semantic product outcomes and durable ownership boundaries. Implementing already accepted use-case semantics does not require separate scope transitions for necessary module public APIs, OpenAPI operations, module-owned persistence changes/migrations, adapters, compositions, runtime wiring, tests, or directly affected documentation.

## Rationale

Decoupling modules entirely and placing cross-capability coordination in compositions preserves DDD bounded contexts and hexagonal boundaries. It prevents modules from acquiring transitive dependencies on each other.

Aligning application composition with declared use cases allows applications to select the capabilities they actually need without being bound by historical proof snapshots.

Treating scope as semantic rather than layer-based eliminates artificial readiness transitions while preserving strict governance over new capabilities, exclusions, and architecture decisions.

## Alternatives considered

- Direct public module-to-module dependencies (e.g., Event depending on Security API) — rejected because it couples module lifecycles and creates dependency chains between bounded contexts.
- Moving cross-module coordination into application runtime roots — rejected because composition roots must own only technical construction and wiring.
- Requiring separate scope transitions for every technical layer (contract, persistence, composition) — rejected because it creates procedural overhead without adding product or architectural value.

## Consequences

- Modules remain strictly independent with zero functional cross-module dependencies.
- Cross-module workflows live in dedicated compositions consuming public module APIs.
- Application runtimes select modules, compositions, adapters, contracts, and infrastructure based on accepted use cases.
- Scope changes are required only for new semantic outcomes, durable responsibilities, excluded product responsibilities, infrastructure, or unaccepted technologies.
- ADR-0013's foundational independent module invariant (independent ownership, public/private boundary, selectable composition, private persistence isolation, runtime wiring) is preserved, while its collaboration rule is strengthened so cross-module workflows are composition-owned without functional module-to-module dependencies.
- ADR-0014 and ADR-0016 are fully preserved; ADR-0015's static composition mechanism is preserved, superseding only its proof-specific Event-only membership assumption.
