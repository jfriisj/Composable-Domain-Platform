# ADR-0016: Selectable external contracts with static application aggregation

## Status

- Status: Accepted
- Date: 2026-08-18
- Supersedes ADR-0009's unified-source decision and the contract-reuse portions of ADR-0015.

## Context

ADR-0015 proved executable module selectability, but the then-current single authoritative `event.yaml` and shared generated transport allocation still coupled Event and Event-Registration external contract ownership.

Research #137 confirmed that executable selectability was valid while external source contracts/generated transport were not independently selectable. Decision #140 selected the permanent external-contract direction for Goal #141.

## Decision

Externally addressable behavior ownership determines authoritative OpenAPI source ownership; there is no one-YAML-per-module rule.

- Event owns an independently authoritative source unit for Event-owned HTTP behavior.
- Event-Registration remains a non-module composition and owns an independently authoritative source unit for its participant workflow.
- Registration remains without a generic HTTP dispatcher.
- Security owns Authentication + Authorization without invented HTTP endpoints.
- Contracts remain contracts, not modules.

A concrete application statically aggregates only the authoritative source units it selects into one coherent application-facing OpenAPI contract. The aggregate is derived build output, not an authoritative source.

Event-only selects Event only; the full Platform Application selects Event plus Event-Registration. Selecting a workflow contract never makes an application valid without the module capabilities that workflow requires.

Generated server transport interfaces/models are physically selectable with their owning source contract. An adapter must not depend on an unrelated adapter solely to reuse generated transport types. Generated OpenAPI types remain adapter-layer build artifacts and never enter module domain/application APIs.

Shared technical OpenAPI components are allowed only for genuinely common protocol semantics and must not become a generic shared-contract dumping ground.

Static aggregation/generation must fail closed on conflicting paths, operation IDs, components, parameters/headers/security schemes, or incompatible definitions.

Aggregation remains build-time/static. Runtime contract discovery, dynamic plugins, feature flags, Spring-profile selection, service extraction, and another runtime composition mechanism are not introduced.

## Rationale

External contract ownership should align with externally addressable behavior while remaining independent from module classification. Static application aggregation preserves a coherent consumer-facing document without forcing unrelated source contracts/generated transport into every application.

The design extends the already accepted static composition model and keeps generated transport outside business boundaries.

## Alternatives considered

- Keep one unified authoritative source — rejected because it couples independent generated transport/source selection.
- Independent sources without application aggregation — rejected because concrete applications still need one coherent external contract.
- One OpenAPI file per module — rejected because module classification and externally addressable behavior ownership are different concerns.

## Consequences

Event and Event-Registration contract sources/generated transport are independently selectable; concrete applications expose coherent derived contracts from explicit selections.

Static aggregation requires deterministic conflict validation and coordinated build/adapter ownership, but introduces no new business behavior, generic Registration HTTP, Security endpoints, identity/account capability, persistence change, or dynamic runtime mechanism.

ADR-0009 remains historical rationale for internal ownership principles but its unified-source decision is superseded. ADR-0013 remains governing module architecture; ADR-0015 remains governing static application composition except for the superseded contract-reuse portions.
