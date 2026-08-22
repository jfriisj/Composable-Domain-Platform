# Registration Module

## Purpose

Own a domain-neutral Registration capability representing a durable registrant-to-target relation and its minimum lifecycle.

## Owns

- Registration identity.
- Opaque namespaced registrant and target references.
- Registration identity and registrant-target uniqueness.
- The active-to-cancelled Registration lifecycle.
- Registration-owned durable state, persistence boundary, and retrieval semantics.

## Does not own

- Event identity, Event existence, publication, or other Event behavior.
- Interpretation or resolution of registrant and target namespaces.
- Event-Registration workflow or Event-specific cancellation policy.
- Participant authentication or authorization.
- HTTP transport mapping or persistence for another module.

## Public boundary

`api/` is the domain-neutral Registration application-level collaboration boundary. Consumers use it without depending on Registration implementation or persistence internals.

The Java source under `api/` is authoritative for the concrete public types and operation semantics.

## Private implementation boundary

`impl/` owns Registration domain and application implementation together with Registration-owned outbound ports, persistence adapters, migrations, and implementation tests.

These details remain private to Registration and are not a collaboration surface for other modules.

## Dependencies

The public boundary may depend on the business-neutral `core` execution context required by the accepted application contract. The private implementation depends inward on the Registration public boundary.

Registration has no functional dependency on Event or Security and does not use another module's private implementation or persistence.

## Related authorities

- `docs/modules.md` and ADR-0013 define the universal module invariant.
- ADR-0008 records the domain-neutral Registration boundary.
- ADR-0011 records the Registration-owned cancellation lifecycle.
- `platform/modules/registration/api/` owns concrete public Java contract truth.
- `platform/modules/registration/impl/` owns implementation, tests, and Registration persistence truth.
- Registration Gradle build files own build dependency truth.
- `docs/architecture/workspace.dsl` owns current architecture relationships.
