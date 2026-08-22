# ADR-0014: Security public authentication and authorization boundary

## Status

- Status: Accepted
- Date: 2026-08-14

## Context

ADR-0013 requires Security to be an independently owned module. Scope #97 assigns Authentication + Authorization to Security while the runtime becomes wiring-only and Event-Registration remains a non-module workflow composition.

Decision #99 resolved the smallest public Security boundary needed to preserve the existing participant-private lifecycle without introducing roles, a generic policy engine, Person/Account, durable identity mapping, or Event/Registration business truth into Security.

## Decision

Security uses the standard module shape:

```text
platform/modules/security/
├── api/
└── impl/
```

`security-api` owns the framework- and transport-neutral collaboration contracts:

- `AuthenticatedActorReference` — opaque authenticated platform actor;
- `AuthenticatedActorProvider` — returns the current actor or an explicit framework-neutral authentication-required failure;
- `ResourceOwnerReference` — opaque expected-owner input;
- `AuthorizationDecision` — `ALLOWED` or `DENIED`;
- `AuthorizeResourceOwnership` — compares actor and expected owner.

`security-api` does not depend on `core` and exposes no Spring Security, Servlet/HTTP Basic, credential, provider, role/authority, Event, Registration, Event-Registration, or persistence types.

For the accepted ownership rule, Authorization is equality between the opaque actor reference and opaque owner reference. Denial is an expected decision value, not infrastructure failure. No action enum, RBAC/ABAC model, resource hierarchy, or generic policy engine is introduced.

Event-Registration retains Event/Registration interpretation. For retrieve/cancel it validates target/registrant namespaces, converts only the opaque registrant reference into `ResourceOwnerReference`, and asks Security for the final decision. It maps `DENIED` to its workflow authorization-denied result; the HTTP adapter owns external `404` existence concealment.

Create requires Authentication only and continues to derive:

`AuthenticatedActorReference(x) -> RegistrantReference("participant", x)`

`security-impl` privately owns Spring Security/stateless HTTP Basic, encoded verifier validation/configuration, technical-principal extraction, actor adaptation, implementations of the public contracts, and Security-specific mechanism adapters.

Functional consumers depend only on `security-api`. Application roots may reference `security-impl` only for selection/construction/configuration/wiring.

## Rationale

The boundary gives Security complete ownership of demonstrated Authentication + Authorization semantics while keeping business facts with Event/Registration/Event-Registration and framework details private.

The contract is deliberately ownership-specific because the accepted use case demonstrates only actor identity and resource ownership. A broader policy/action model would be speculative.

## Alternatives considered

- Broad security context — rejected because roles/provider/session metadata are unnecessary.
- Generic action/resource policy engine — rejected because current operations share one ownership predicate.
- Keep actor or final Authorization ownership in Event-Registration — rejected by the independent Security module boundary.
- Put actor identity in `core` — rejected because identity is not general business-neutral execution context.
- Roles/RBAC/ABAC, provider identity, Person/Account, durable mapping, Security persistence — deferred/rejected for lack of accepted need.

## Consequences

Security has an explicit independently owned public API/private implementation boundary. HTTP and Event-Registration collaborate through `security-api`; Event and Registration acquire no Security implementation dependency.

The accepted participant-private behavior, privacy mapping, correlation, restart stability, and authentication mechanism are preserved without adding a role model, identity provider, Security persistence, or new business ownership.

ADR-0012 remains historical mechanism rationale; ADR-0013 remains the governing universal module invariant.
