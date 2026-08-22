# ADR-0015: Static selectable application composition

## Status

- Status: Accepted
- Date: 2026-08-16
- ADR-0016 supersedes this ADR only where it relied on one unified authoritative OpenAPI source and adapter-to-adapter generated transport reuse.

## Context

ADR-0013 requires modules to be selectable in application composition. Goal #114 required executable proof that a valid application can select a strict subset of accepted modules while omitted unrelated modules are absent from its functional compile/runtime dependency graph.

The original single Platform Application and HTTP project could hide Registration/Security/Event-Registration dependencies even when runtime beans were conditionally unused. Decision #130 selected the smallest static build/allocation mechanism that makes omission objective.

## Decision

Use explicit static Gradle project/application boundaries for selectable composition.

Do not use runtime module discovery, dynamic plugins, feature flags, Spring profiles/conditional capability wiring, Gradle feature variants, another DI mechanism, or service extraction for this property.

The minimum proof composition is Event-only. It selects Event plus the technical HTTP/runtime/persistence infrastructure required to serve accepted Event behavior and deliberately omits Registration, Security, Event-Registration, and participant-private Event-registration HTTP adaptation from its functional compile/runtime graph.

Keep the full Platform Application for the complete Event/Registration/Security/Event-Registration lifecycle.

Application roots own only technical selection, construction, configuration, and wiring.

Physically separate participant-private Event-registration HTTP adaptation from the Event-only HTTP adapter boundary so dependencies on Event-Registration/Security do not contaminate the Event-only graph.

Selectability does not permit removal of a required capability. Event-Registration remains valid only when its required Event, Registration, and Security public capabilities are supplied.

Ordinary Gradle dependency/configuration evidence and executable tests must prove both omission of unrelated capabilities and required-capability structure.

## Rationale

Static Gradle project boundaries make compile/runtime membership explicit, inspectable, and mechanically testable using the build model already accepted by ADR-0002. Runtime conditional wiring cannot prove dependency absence.

Using an Event-only application exercises real accepted behavior instead of creating a synthetic composition solely for the architecture test.

## Alternatives considered

- Gradle variants/source sets — rejected as more conditional build semantics than needed.
- Spring profiles/conditional runtime wiring — rejected because dependencies remain on compile/runtime classpaths.
- Executable composition without an inbound adapter — rejected as weaker proof that the selected application serves its declared behavior.
- Dynamic plugins/runtime discovery — rejected because no hot-loading/extensibility requirement exists.
- Service extraction/new deployment model — rejected because Goal #114 concerns composition inside the modular application platform.

## Consequences

Module omission becomes objective at the Gradle graph level. The repository has explicit Event-only and full application compositions and physically separated HTTP adapter responsibilities.

The mechanism adds build/application projects and architecture verification but no new business behavior or technology.

ADR-0016 later changes external contract source/generated-transport allocation while preserving this static application-composition decision.
