# ADR-0016: Selectable external contracts with static application aggregation

- Status: Accepted
- Date: 2026-08-18
- Supersedes: ADR-0009's unified-source decision and the contract-reuse portions of ADR-0015

## Context

ADR-0013 requires every construct classified as a module to remain independently owned, explicitly consumable through its public API, and selectable into or out of valid application compositions. ADR-0015 proved that property at the executable Gradle/application level with an Event-only composition that omits Registration, Security, Event-Registration, and participant-private Event-registration HTTP adaptation from its functional compile/runtime graph.

That proof deliberately preserved ADR-0009's single authoritative `platform/contracts/http/v1/event.yaml`. The current `:http-interface` generates Event and EventRegistration transport interfaces/models from that unified document, while the physically separate `:event-registration-http-interface` depends on `:http-interface` to reuse the generated EventRegistration types.

Research #137 established that this state satisfies completed Goal #114 but does not provide independently selectable external contract sources or generated transport boundaries. Decision #140 selected the permanent direction for external-contract composability.

Goal #141 tracks the bounded migration required to make external contract selection match the platform's independently composable application model.

## Decision

### Independent authoritative contract units

Externally addressable behavior ownership determines authoritative OpenAPI source-contract ownership.

There is no one-YAML-per-module rule.

- Event owns an independently authoritative contract unit for Event-owned externally addressable HTTP behavior.
- Event-Registration remains a non-module cross-module composition and owns an independently authoritative contract unit for its externally addressable participant workflow.
- Registration remains an independently owned reusable module without a generic external HTTP dispatcher.
- Security remains the independent Authentication + Authorization module without gaining invented HTTP endpoints.

Contracts remain contracts, not modules. Contract ownership does not reclassify a workflow, adapter, or runtime as a module.

### Static application-level aggregation

A concrete application composition exposes one coherent application-facing OpenAPI contract assembled statically from only the authoritative contract units selected by that application.

The aggregated application contract is derived build output. It does not replace the independently authoritative source contract units.

The required semantics are:

- an Event-only application selects the Event contract unit and does not advertise Event-Registration behavior;
- the complete Platform Application selects the Event and Event-Registration contract units;
- selecting an external workflow contract does not make the application valid unless the public capabilities required by that workflow are also selected.

Aggregation is build-time/static. Runtime contract discovery, dynamic plugins, feature flags, Spring profiles, service extraction, and another runtime composition mechanism are not introduced.

### Generated transport selectability

Generated server transport interfaces/models must be physically selectable with the authoritative external contract unit that owns their behavior.

An adapter must not depend on an unrelated adapter project solely to obtain generated transport types for its own contract.

The permanent target therefore removes the current Event-Registration HTTP dependency on the Event HTTP adapter project when that dependency exists only to reuse generated EventRegistration transport types.

Generated OpenAPI types remain adapter-layer artifacts and must not enter module domain/application APIs.

### Shared technical OpenAPI components

Shared technical OpenAPI components are permitted only for genuinely shared protocol semantics.

A later implementation may establish a narrowly scoped technical contract source for shared concerns such as correlation headers, a genuinely identical error envelope, or an HTTP authentication security-scheme declaration. Such a source must not become a generic shared-contract dumping ground.

Static aggregation must fail closed on conflicting paths, operation identifiers, schemas, parameters, headers, security schemes, or incompatible component definitions.

### Security boundary

An OpenAPI security scheme such as `ParticipantBasicAuth` is an external HTTP protocol declaration. Referencing it from a workflow contract does not transfer Authentication ownership.

Security continues to own its framework-neutral public Authentication + Authorization contracts and its private credential-verification and Spring Security/HTTP Basic implementation.

This decision does not introduce credential-management APIs, Account/User/Person capability, participant profile, identity-provider integration, or new Security endpoints.

### Architecture state

The existing unified `event.yaml`, current generated transport allocation, current HTTP adapter dependencies, and current application surfaces remain **Current** executable architecture until a later implementation is accepted into `development`.

The independently authoritative Event and Event-Registration contract units, statically aggregated application contracts, and independently selectable generated transport allocation are **Planned** under Goal #141 until executable migration is accepted.

## Relationship to prior ADRs

### ADR-0009

This ADR supersedes ADR-0009's decision that one unified `event.yaml` is the authoritative source contract and its prohibition on a separate Event-Registration workflow source contract.

The following ADR-0009 principles remain accepted:

- external contract organization does not define internal module ownership;
- Event and Registration remain independent;
- Event-Registration owns the cross-capability workflow;
- no generic Registration HTTP dispatcher is introduced;
- callers should still receive a coherent application-facing API surface.

Consumer coherence is now provided by static application-level aggregation rather than by requiring one authoritative source file.

### ADR-0013

ADR-0013 remains Accepted and is not superseded.

Its universal independent-module invariant remains the governing rule for module ownership and collaboration. This ADR extends selectable composition to the external contract/generated-transport boundary without classifying contracts as modules.

### ADR-0015

ADR-0015 remains Accepted for:

- explicit static application composition;
- Gradle/project dependency selectability;
- Event-only proof semantics;
- omission of unrelated modules;
- technical-only application runtime ownership;
- rejection of dynamic plugin/profile/feature-flag mechanisms.

This ADR supersedes only ADR-0015's permanent reliance on the unified `event.yaml` source and Event-Registration HTTP reuse of generated EventRegistration transport types through the Event HTTP project.

The completed Goal #114 proof and its historical evidence remain valid.

## Alternatives considered

### Keep one unified authoritative source contract

Rejected as the permanent direction.

It keeps one simple consumer document but couples generated transport ownership and prevents an application composition from selecting an independently authoritative source surface that matches only its selected externally addressable behavior.

### Independent source contracts without application aggregation

Rejected as incomplete.

It improves ownership and independent generation but leaves consumers without one coherent contract for a concrete application composition.

### One OpenAPI file per module

Rejected.

Module classification and externally addressable behavior ownership are different concerns. Registration and Security do not require HTTP endpoint contracts merely because they are modules, while Event-Registration may own an external workflow contract without being a module.

## Consequences

### Positive

- External contract ownership follows externally addressable behavior ownership.
- Contract and generated transport selection can align with static application composition.
- Event-only and complete application surfaces can be objectively different while remaining coherent.
- Event-Registration transport no longer needs an unrelated Event HTTP adapter merely for its generated types.
- Module ownership and external contract organization remain separate concepts.

### Costs

- The repository will require more than one authoritative contract source.
- Concrete application contracts require a static aggregation and validation step.
- Shared technical components need explicit, narrow ownership.
- Contract conflicts and incompatible shared components require deterministic fail-closed validation.
- Migration requires coordinated contract, generation, adapter/build, architecture, and validation changes.

### Constraints

Implementation under Goal #141 must not:

- add new Event or Registration business behavior;
- create a generic Registration HTTP dispatcher;
- invent Security endpoints;
- create Account/User/Person capability or participant profile;
- introduce credential persistence/enrollment/reset/recovery or identity-provider integration;
- introduce dynamic/runtime contract discovery, plugins, feature flags, Spring-profile selection, or service extraction;
- move generated OpenAPI types into module domain/application APIs;
- mark Planned contract/transport architecture as Current before executable state exists.

Refs: research #137; decision #140; Goal #141; scope #142.
