# ADR-0009: Unified Event-facing OpenAPI contract

## Status
- Status: Accepted
- Date: 2026-08-03
- Supersedes: the separate-contract-file decision in [ADR-0008](0008-domain-neutral-registration-and-event-registration-composition.md)

## Context

ADR-0008 correctly keeps Event, Registration, and the Event-Registration composition as separate internal ownership boundaries. It also selected a separate `contracts/http/v1/event-registration.yaml` document for the Event-registration workflow.

During implementation of issue #38, that file split proved to conflate two different concerns:

- internal ownership and dependency boundaries;
- external HTTP contract grouping.

The Event-registration workflow is externally Event-facing, but internally it remains composition-owned and spans the public APIs of Event and Registration. A separate OpenAPI file is not required to preserve those internal boundaries.

## Decision

Use one authoritative Event-facing OpenAPI document:

`contracts/http/v1/event.yaml`

The document contains the existing Event operations and is the accepted location for the Event-registration workflow operations:

- `POST /api/v1/events`;
- `GET /api/v1/events/{eventId}`;
- `POST /api/v1/event-registrations`;
- `GET /api/v1/event-registrations/{registrationId}`.

The unified document may use separate tags such as `Event` and `EventRegistration`. A single OpenAPI generation step may generate separate API interfaces by tag plus shared transport models from the same document.

The contract file represents the coherent external Event-facing API surface. It does not define bounded-context ownership.

Internal ownership remains unchanged:

- Event owns Event state, behavior, existence, persistence, and its public API.
- Registration remains domain-neutral and owns Registration state, invariants, persistence, and its public API.
- Event and Registration remain mutually independent.
- The Event-Registration composition owns the cross-capability workflow, including Event existence validation and translation into Registration references.
- The HTTP interface remains an adapter over public Event and composition contracts.
- No generic Registration HTTP dispatcher is introduced.

`contracts/http/v1/event-registration.yaml` is not part of the accepted architecture and must not be created.

This ADR supersedes only ADR-0008's decision to use a separate Event-registration OpenAPI file. ADR-0008's domain-neutral Registration model, composition ownership, persistence isolation, dependency direction, and authentication/identity separation remain accepted.

## Rationale
The decision is retained for the constraints recorded in Context and the trade-offs recorded in Alternatives considered and Consequences; this migration changes document structure only.

## Alternatives considered

### Separate OpenAPI document per workflow responsibility

Rejected because the external contract would be split according to internal implementation responsibility even though callers consume one coherent Event-facing API. The split adds contract-generation and navigation complexity without strengthening the internal module boundaries.

### Generic Registration HTTP contract

Rejected because it would expose Registration namespace/reference mechanics and turn a product-specific Event workflow into a generic external dispatcher. Registration remains an internal reusable capability contract, not a generic HTTP target router.

### Merge Event, Registration, or composition implementation boundaries

Rejected because the contract-file decision does not change business ownership. The existing bounded-context and composition boundaries remain required.

## Consequences

The external Event-facing API has one versioned source of truth while internal architecture remains separated by responsibility.

Event and Event-registration operations can use distinct OpenAPI tags and generated interfaces without requiring distinct YAML documents.

Issue #38 must be re-read and updated after this decision is accepted into `development`; its separate `event-registration.yaml` requirement is no longer valid.

Future decisions about contract-file grouping should follow external API responsibility and consumer coherence rather than mechanically mirror internal Gradle or bounded-context structure.
