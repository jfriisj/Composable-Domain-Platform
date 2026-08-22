# Security Module

## Purpose

Own the platform Authentication and Authorization capability behind a framework- and transport-neutral public boundary.

## Owns

- The authenticated platform actor semantic and Authentication collaboration boundary.
- The current resource-ownership Authorization decision.
- Private Security mechanism implementation and actor adaptation required by the accepted authentication proof.

## Does not own

- Event, Registration, or Event-Registration workflow/domain facts.
- Event-Registration external privacy mapping.
- Person or Account capabilities.
- Roles, permissions, a generic policy engine, or provider-specific identity contracts.
- Security persistence or unrelated business behavior.

## Public boundary

`api/` is the framework- and transport-neutral Security collaboration boundary. Functional consumers depend on this boundary rather than Security implementation or mechanism types.

The Java source under `api/` is authoritative for the concrete public types and operation semantics.

## Private implementation boundary

`impl/` owns the admitted Security mechanism and adapters, including the current Spring Security/stateless HTTP Basic proof, credential-verifier configuration, technical-principal extraction, actor adaptation, and implementations of the public Authentication and Authorization contracts.

Spring Security, Servlet, credential, and mechanism details remain private and do not cross the public boundary.

## Dependencies

The Security public boundary has no production dependency on `core` or another module. The private implementation depends inward on the Security public boundary.

Security has no functional dependency on Event, Registration, or Event-Registration and does not use another module's private implementation or persistence.

## Related authorities

- `docs/modules.md` and ADR-0013 define the universal module invariant.
- ADR-0012 records the historical minimum authentication-mechanism rationale.
- ADR-0014 records the Security public Authentication and Authorization boundary.
- `platform/modules/security/api/` owns concrete public Java contract truth.
- `platform/modules/security/impl/` owns implementation and test truth.
- Security Gradle build files own build dependency truth.
- `docs/architecture/workspace.dsl` owns current architecture relationships.
