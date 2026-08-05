# ADR-0011: Registration-owned cancellation lifecycle

- Status: Accepted
- Date: 2026-08-05

## Context

Goal #57 requires an adult participant to cancel an Event registration and later observe the resulting durable lifecycle state.

ADR-0008 established Registration as a domain-neutral bounded capability that owns Registration identity, the opaque registrant-to-target relation, uniqueness, persistence, and retrieval. It deliberately left domain-specific lifecycle concerns deferred until a concrete accepted requirement demonstrated a need.

Decision issue #61 provides that requirement. The minimum participant lifecycle needs cancellation state that remains observable after cancellation and process restart. The lifecycle question can be stated entirely in Registration terms: whether a registrant-to-target relation is active or cancelled. It does not require Registration to know that a target is an Event or that a registrant is a participant.

Cancellation must also preserve the existing ownership boundaries. Participant authentication and the participant-owns-registration authorization decision remain outside Registration under decision #60 and ADR-0008. Event remains independent of Registration. Event-Registration composition remains responsible for Event-specific orchestration.

## Decision

Registration owns a minimum generic lifecycle for its domain-neutral registrant-to-target relation.

A newly created Registration is `active`.

The minimum accepted lifecycle transition is:

`active -> cancelled`

Cancelling an already cancelled Registration is idempotent. The operation leaves the same Registration in `cancelled` state without creating another Registration or changing its identities.

Cancellation preserves:

- `registrationId`;
- `RegistrantReference`;
- `TargetReference`;
- durable retrieval of the Registration;
- the existing uniqueness rule over the complete `(RegistrantReference, TargetReference)` pair.

A cancelled registrant-to-target pair therefore continues to occupy that unique relation. Same-pair re-registration and reactivation are not part of the minimum lifecycle.

Registration owns the lifecycle state, generic cancellation transition, persistence, and retrieval semantics. It does not interpret registrant or target namespaces when applying the transition.

Event-Registration composition owns Event-specific cancellation orchestration. It performs the participant authorization required by decision #60 and invokes the generic Registration cancellation behavior only after that workflow-specific authorization succeeds.

Registration remains unaware of:

- Event existence, publication, schedule, or other Event lifecycle;
- participant authentication or authorization policy;
- identity-provider or credential concepts;
- Event-specific cancellation eligibility or deadlines;
- capacity, waitlists, payment, refunds, or notifications.

Event remains Registration-independent and does not store Registration identities or cancellation state.

This ADR extends the rationale of ADR-0008. ADR-0008 remains Accepted and is not superseded.

## Alternatives considered

### Physically delete Registration on cancellation

Rejected.

Deletion would remove the durable lifecycle state that Goal #57 requires the participant to observe after cancellation. It would also implicitly free the registrant-target uniqueness pair and introduce re-registration semantics that have not been accepted.

### Store cancellation state in Event-Registration composition

Rejected.

The composition would become a persistence owner solely to shadow lifecycle state for a relation already owned by Registration. That would create competing durable truths and a reconciliation problem without demonstrating a separate business subject.

### Store cancellation state in Event

Rejected.

Event does not own Registration identities or registrant-to-target relations. Making Event own cancellation state would make Event Registration-aware and violate the accepted independent bounded-context relationship.

## Consequences

Later accepted scope must include Registration-owned lifecycle persistence capable of representing at least `active` and `cancelled`, together with a transport-neutral Registration cancellation operation and lifecycle state in Registration retrieval.

Event-facing participant cancellation remains exposed through Event-Registration composition rather than through a generic external Registration dispatcher. Participant authentication and authorization remain governed by decision #60 and the existing separation in ADR-0008.

The existing `(RegistrantReference, TargetReference)` uniqueness rule continues across cancellation. Same-pair re-registration, reactivation, additional Registration lifecycle states, cancellation reasons, timestamps as required domain state, and Event-specific cancellation policy remain deferred.

No new bounded context, persistence owner, container, or Event/Registration dependency relationship is introduced by this decision. Therefore `docs/architecture/workspace.dsl` requires no relationship change solely for this lifecycle extension.

If later implementation planning introduces a new architectural component or relationship, that change must follow normal architecture control and update the authoritative model.
