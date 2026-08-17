# ADR-0015: Static selectable application composition

- Status: Accepted
- Date: 2026-08-16

## Context

ADR-0013 establishes one universal invariant for every construct classified as a module: independent ownership, an explicit public API, a private implementation, selectable application composition, and collaboration only through public contracts/adapters.

Goal #114 isolates the remaining executable proof of that invariant. Accepted scope #115 requires a platform developer to be able to construct a valid executable composition from a strict subset of already accepted modules while omitted unrelated modules are absent from that composition's functional compile/runtime dependency graph. A composition that actually requires a capability must remain invalid when that required capability is absent.

The current executable architecture has only one Spring Boot composition root, `:platform-app`. Its build/runtime graph includes Event, Registration, Security, Event-Registration composition, and the single `:http-interface` project. The current HTTP project contains logically distinct Event and participant-private Event-registration adapters, but its project-level dependencies include Event-Registration composition and Security. Runtime bean selection alone therefore cannot prove that an otherwise unrelated omitted module is absent from the executable dependency graph.

Decision #130 evaluated concrete static and conditional mechanisms and selected the minimum build/allocation architecture required to make the selectability property objective and mechanically verifiable.

This ADR records that significant architecture decision as durable repository rationale. It does not make the planned projects or runtime allocation Current before implementation #131 is accepted.

## Decision

### Static build-time selection

Selectable application composition is implemented through explicit static Gradle project/application boundaries.

The proof does not use:

- runtime module discovery;
- dynamic plugins;
- feature flags;
- Spring profiles or conditional capability wiring;
- Gradle feature variants/source-set selection;
- another dependency-injection mechanism;
- service extraction.

Project dependencies define the functional compile/runtime graph. Omission is therefore inspectable and mechanically verifiable from ordinary Gradle configurations.

### Minimum proof composition

The minimum valid proof composition is Event-only.

It selects the already accepted Event capability and the technical infrastructure required to serve the existing Event HTTP behavior:

- Platform Core where required by Event public contracts;
- Event API;
- private Event implementation and Event-owned persistence;
- the Event HTTP adapter slice generated from the accepted Event-facing contract;
- a technical Spring Boot composition root.

It deliberately omits:

- Registration API/implementation/persistence;
- Security API/implementation;
- Event-Registration composition;
- participant-private Event-registration HTTP adaptation.

The Event-only composition introduces no new Event behavior, persistence semantics, business capability, or external contract.

### Application allocation

A second explicit Spring Boot composition root is planned for the Event-only proof.

The existing Platform Application remains the complete accepted Event/Registration/Security composition and continues to serve the participant-private Event-registration lifecycle.

Both application roots own only technical selection, construction, configuration, and wiring. Neither application owns Event, Registration, Security, or Event-Registration business behavior.

The Event-only application is **Planned** until implementation #131 is accepted into `development`.

### HTTP adapter allocation

The authoritative external contract remains the single unified:

`platform/contracts/http/v1/event.yaml`

Contract grouping does not force all transport adapters into one functional Gradle dependency boundary.

The current Event HTTP adapter is planned to remain in the existing `:http-interface` project together with the generated transport boundary required by the Event operations.

Participant-private Event-registration HTTP adaptation is planned as a separate Gradle adapter project so its dependencies on Event-Registration composition and Security do not become dependencies of the Event-only HTTP slice.

The separated Event-registration HTTP adapter may reuse the existing generated transport interfaces/models and shared HTTP correlation behavior through the HTTP boundary. That reuse must not introduce a reverse dependency from the Event HTTP slice to Event-Registration or Security.

This is adapter/build allocation only. It does not create a new business module or a second external contract.

### Required-capability invalidity

Selectability does not mean arbitrary dependency removal.

Event-Registration remains a non-module composition whose workflow requires public capabilities from:

- Event;
- Registration;
- Security.

Its implementation must continue to depend only on those public contracts and to require the collaborators explicitly.

An application declaring participant-private Event-registration behavior is therefore invalid when the required Registration or Security capabilities are absent. The selectability proof must not acquire a hidden dependency or create an intentionally broken application merely to demonstrate that fact.

### Planned dependency shape

The planned high-level allocation is:

~~~text
event.yaml
   |
   v
http-interface ------------------------> event-api
   ^                                       ^
   |                                       |
event-app ----------------------------> event-impl
                                           |
                                           v
                                      Event persistence

event-registration-http-interface
   |                 |
   |                 +-----------------> security-api
   v
event-registration-composition
   |              |              |
   v              v              v
event-api   registration-api   security-api

platform-app
   |
   +--> http-interface
   +--> event-registration-http-interface
   +--> event-impl
   +--> registration-impl
   +--> security-impl
   +--> event-registration-composition
~~~

The exact technical wiring remains implementation detail inside #131 as long as it preserves these accepted ownership and dependency constraints.

### Architecture state

Before implementation #131 is accepted:

- the existing Platform Application and HTTP Interface remain **Current**;
- the Event-only application is **Planned**;
- the separated Event-registration HTTP adapter allocation is **Planned**.

The authoritative Structurizr model must keep Planned elements out of Current views and may expose them in a dedicated Planned view.

After implementation is accepted, the model/narrative may promote the implemented allocation to Current in the same change that makes executable truth match it.

## Relationship to prior ADRs

ADR-0013 remains the governing universal independent-module invariant and is not superseded. This ADR provides one concrete static application/build mechanism for proving its selectable-composition property.

ADR-0002 remains the accepted rationale for explicit Gradle multi-project boundaries. This ADR applies that established mechanism to selectable application allocation rather than introducing a new build technology.

ADR-0006 remains the accepted Spring Boot/OpenAPI runtime boundary. This ADR adds a second static composition root and separates adapter allocation without changing the external contract strategy.

ADR-0009 remains the accepted rationale for one unified Event-facing OpenAPI contract. Physical adapter separation does not split or supersede that contract.

## Alternatives considered

### Gradle feature variants or source-set selection

Rejected.

The repository already uses explicit Gradle project boundaries. Variants/source sets would introduce conditional build-selection semantics for a property that ordinary project dependencies can prove more directly and inspectably.

### Spring profiles or conditional runtime wiring

Rejected.

Conditional bean construction does not remove existing project dependencies from the application's functional compile/runtime graph. It therefore cannot prove the required omission property.

### Executable composition without an inbound adapter

Rejected for Goal #114.

A context-only executable would prove construction but provide weaker evidence that the selected application can start and serve its declared behavior. Existing Event HTTP operations already provide a bounded accepted behavior to prove.

### Dynamic plugins or runtime module discovery

Rejected.

No accepted requirement needs hot loading, dynamic discovery, or runtime extensibility. Introducing such mechanisms would expand technology and operational scope beyond the minimum proof.

### Service extraction or another deployment unit model

Rejected.

Goal #114 concerns selectable composition inside the accepted modular application platform. No service boundary, microservice architecture, or deployment expansion is required.

## Consequences

### Positive

- Module omission becomes objective at the Gradle compile/runtime graph level.
- The Event-only proof exercises an already accepted capability and contract rather than inventing a synthetic feature.
- Registration and Security remain independently owned and absent when unrelated.
- The full Platform Application remains available for the complete accepted Event-registration lifecycle.
- The mechanism uses existing Gradle and Spring Boot foundations and adds no new technology.
- Adapter dependencies more accurately match the behavior each physical adapter slice serves.

### Costs

- The repository gains another explicit application project.
- The HTTP adapter allocation gains another Gradle project.
- The unified generated transport boundary must remain reusable without creating adapter dependency cycles.
- Architecture/dependency verification must cover both valid omission and required-capability dependency structure.

### Constraints

Implementation #131 must not:

- change `event.yaml`;
- change Event, Registration, Security, or Event-Registration business semantics;
- create a new business module;
- introduce runtime discovery, profiles, feature flags, build variants, or another DI mechanism;
- move business ownership into an application runtime;
- mark Planned architecture as Current before executable state exists.

If implementation demonstrates that these constraints cannot satisfy Goal #114, work must return to the applicable decision/scope/ADR gate rather than expanding #131 implicitly.
