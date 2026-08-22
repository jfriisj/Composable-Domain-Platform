# ADR-0008: Domain-neutral Registration and Event-registration composition

## Status
- Status: Accepted
- Date: 2026-08-03
- Supersedes: [ADR-0007](0007-registration-capability-and-cross-capability-composition.md)

## Context

ADR-0007 correctly separated Registration from Event implementation and persistence, but its accepted Registration state still contained Event-specific concepts: `eventId`, `participantReference`, and uniqueness over `(eventId, participantReference)`.

That model was technically decoupled from Event implementation while remaining semantically Event-specific. It would make a module named Registration implicitly about Event participation and would constrain later compositions such as a future Person-based registrant without an accepted reason to make Registration Event-aware.

The platform needs a Registration capability whose own state and invariants can be described without Event, Person, authentication, or another business capability while retaining an Event-specific product workflow through composition.

## Decision

Registration remains a separate bounded business capability, but its model is domain-neutral.

Registration owns:

- `registrationId`;
- `RegistrantReference` containing opaque `namespace` and `reference` values;
- `TargetReference` containing opaque `namespace` and `reference` values;
- Registration uniqueness rules;
- Registration persistence;
- Registration retrieval.

Registration validates that identifier components are present, but it does not interpret namespace values, resolve referenced business objects, or branch behavior by namespace.

Registration owns uniqueness of `registrationId` and of the complete `(RegistrantReference, TargetReference)` pair. A uniqueness conflict preserves existing state.

Registration does not depend on Event, Person, authentication, authorization, credentials, identity providers, HTTP, or another business capability.

The Event-Registration composition owns the Event-specific workflow. For creation it resolves Event existence through Event API, maps the opaque participant reference to a Registration `RegistrantReference` in the `participant` namespace, maps Event identity to a Registration `TargetReference` in the `event` namespace, and invokes Registration API. Neither Event nor Registration depends on the other capability, and Event does not store Registration identities.

Registration persistence contains only `registration_id`, `registrant_namespace`, `registrant_reference`, `target_namespace`, and `target_reference`. It has no Event-specific column, Event foreign key, cross-schema Event lookup, or direct Event persistence access.

The external contract remains product/workflow-specific rather than exposing a generic Registration dispatcher. The planned contract is `contracts/http/v1/event-registration.yaml` with `POST /api/v1/event-registrations` and `GET /api/v1/event-registrations/{registrationId}`. Both operations pass through the Event-Registration composition. Transport input remains expressed in Event workflow language and does not expose generic namespace mechanics.

Authentication identity and Registration registrant identity are separate concepts. Registration does not authenticate or authorize callers. Technical authentication belongs at an external/security boundary, while domain-specific authorization belongs with the capability or composition owning the required business truth. Any future security information crossing application boundaries must use transport-neutral contracts.

A future Person capability may supply a canonical identity used to construct a namespaced `RegistrantReference`, but Registration remains unaware of Person. Identity reconciliation or canonicalization across namespaces is outside Registration ownership.

## Rationale
The decision is retained for the constraints recorded in Context and the trade-offs recorded in Alternatives considered and Consequences; this migration changes document structure only.

## Alternatives considered

### Keep the ADR-0007 Event-specific Registration state

Rejected because the module name and ownership would imply a general Registration capability while its durable state and uniqueness rules remained Event-specific.

### Put the Event-to-Registration relation inside Event

Rejected because Event would become Registration-aware, would need to store Registration identities or consume Registration contracts, and would no longer remain independently bounded from Registration.

### Make Registration a generic dispatcher that understands target types

Rejected because branching on Event, Course, Person, or other namespace values would turn Registration into a central domain dispatcher and move business-specific rules into the wrong capability.

### Make Registration responsible for authentication and authorization

Rejected because authenticated actor identity is not inherently the registrant identity, and domain-specific authorization requires business truth owned outside Registration.

### Introduce Person or security capabilities now

Rejected because the current Event-registration proof does not require them. The architecture preserves room for those capabilities without authorizing speculative implementation.

## Consequences

Registration can be implemented and tested without Event-specific state while retaining meaningful intrinsic invariants and durable ownership.

The Event-specific product workflow remains explicit in `compositions/event-registration`, which becomes responsible for translating Event workflow identities into Registration-owned references.

The Event HTTP surface for registration is explicitly Event-specific, avoiding a generic external target dispatcher. Both creation and retrieval pass through the composition.

The Registration database schema no longer contains `event_id` or `participant_reference`; it stores namespaced registrant and target references instead.

Event and Registration remain mutually independent. Event does not store Registration identities, and Registration does not validate Event existence.

Authentication/authorization implementation, Person capability implementation, identity reconciliation, and domain-specific lifecycle concerns remain deferred until concrete accepted requirements exist.
