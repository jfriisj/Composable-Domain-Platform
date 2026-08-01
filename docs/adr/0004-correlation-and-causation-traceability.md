# ADR-0004: Correlation and causation identifiers for cross-boundary traceability

- Status: Accepted
- Date: 2026-08-01

## Context

The platform is designed around independently bounded modules, asynchronous events, background work, and external integrations.

Operational troubleshooting must be able to reconstruct one logical flow even when execution crosses module or process boundaries. A distributed tracing product may not always be installed, and business-domain models must not depend on observability frameworks.

## Decision

Define Correlation ID and Causation ID as platform-level execution-context concepts.

A Correlation ID identifies one complete logical flow and is preserved across synchronous calls, asynchronous messages, background work derived from that flow, and supported external integration calls.

A Causation ID identifies the immediate operation or message that caused a new asynchronous action or message. Each newly published message receives its own identity while preserving the existing Correlation ID and referencing its immediate cause.

Entry points create a Correlation ID when no valid correlation context exists.

Correlation metadata belongs to transport/message envelopes and structured execution context, not to business-domain state. The identifiers are opaque, contain no personal or business information, and are never used to make business decisions.

The exact identifier format and protocol-specific header or envelope fields are intentionally deferred until the relevant contracts are implemented.

Distributed trace and span identifiers are separate concepts. OpenTelemetry or W3C Trace Context may later complement this mechanism, but correlation must work without requiring a particular observability provider.

## Alternatives considered

- Rely exclusively on application log request IDs.
- Use distributed tracing trace IDs as the only correlation mechanism.
- Put correlation fields directly on domain entities and events.
- Defer traceability until observability infrastructure is introduced.

## Consequences

- All external and asynchronous boundaries must preserve correlation semantics when they are implemented.
- Structured logging can be queried by Correlation ID independently of the chosen observability stack.
- Event/message contracts require execution metadata envelopes.
- Core may contain minimal opaque identifier/context primitives, but no business behavior.
- Protocol-specific representation remains a later contract decision and therefore does not expand the current implementation scope.
