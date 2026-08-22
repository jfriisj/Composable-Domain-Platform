# ADR-0012: Minimum participant authentication boundary

## Status

- Status: Accepted
- Date: 2026-08-13
- Current ownership is constrained by ADR-0013 and ADR-0014; this ADR remains the historical rationale for the accepted minimum authentication mechanism.

## Context

Goal #57 required participant-private Event-registration create, retrieve, and cancel operations to use real technical authentication while preserving Event, Registration, Event-Registration, privacy, and correlation boundaries.

Registration is domain-neutral and security-neutral. Participant ownership must be derived from an authenticated platform actor rather than caller-controlled identity. Research #83 found no configured technical participant authentication and no demonstrated need for durable identity mapping, a Person/Account capability, or an external identity provider.

The existing executable runtime is Spring Boot, so the decision needed the minimum authentication mechanism that fits that boundary without inventing broader identity/security architecture.

## Decision

Use Spring Security for technical authentication and stateless HTTP Basic as the minimum non-browser proof mechanism for participant-private Event-registration operations. Published Event discovery remains public.

Participant proof credentials are supplied through external runtime configuration and held in memory. Each entry contains an opaque stable platform principal identifier and an encoded password verifier. Plain-text/no-op password storage is prohibited.

After successful authentication:

`authenticatedPrincipalName -> AuthenticatedActorReference(authenticatedPrincipalName)`

The configured principal identifier is the platform-facing opaque pseudonym. It must be nonblank, stable, unique within the configured authentication boundary, free of participant/business meaning, and must not be an email/display name, raw provider subject, credential/token, role/authority, or correlation/causation identifier.

No HMAC derivation or durable provider-to-platform mapping is selected for this proof.

The proof is stateless and non-browser: no form login, sessions/cookies, remember-me, logout lifecycle, OAuth/OIDC, or JWT bearer authentication. HTTP Basic requires secure transport across untrusted networks, but production TLS/deployment infrastructure is outside this decision.

Normal structured logs must not contain authorization headers, passwords/verifiers, configured principal values, authenticated actor values, or participant registrant-reference values.

ADR-0013/0014 now govern permanent Security ownership: Security owns Authentication + Authorization behind a framework-neutral public API; Spring Security/HTTP Basic and credential verification are private Security implementation concerns. Event-Registration retains workflow/domain-fact translation. Registration remains security-neutral.

## Rationale

Spring Security provides standard credential extraction, verification, authentication-failure behavior, and security-context establishment inside the already accepted Spring Boot runtime while avoiding a bespoke security framework.

Stateless HTTP Basic is sufficient to prove authenticated participant-private behavior without introducing sessions, provider integration, tokens, durable identity state, or a broader account capability. Using an already opaque stable platform principal directly avoids unnecessary mapping/key-management responsibilities.

## Alternatives considered

- Caller-supplied participant/actor identity — rejected because it is an unauthenticated assertion.
- Container-managed Servlet authentication — rejected because no accepted realm/authenticator exists and it adds container-specific coupling.
- Custom Servlet authentication filter — rejected because the project would own standard credential/security mechanics unnecessarily.
- External authenticator/gateway — deferred because no accepted deployment/trust relationship requires it.
- HMAC actor derivation — deferred until a real upstream provider subject creates that need.
- Durable identity mapping, Person/Account, credential persistence — rejected/deferred because the minimum proof does not require them.
- Roles/permissions/RBAC/ABAC — rejected because ownership is not a role model.
- OAuth/OIDC, JWT, mTLS, API keys, sessions — deferred as broader mechanisms.

## Consequences

The platform has an accepted real-authentication proof mechanism for participant-private behavior while preserving opaque participant identity and privacy requirements.

Security mechanism details remain private; Event/Registration business ownership does not move into Security. No Security persistence, provider integration, account/profile capability, role model, or deployment infrastructure is introduced.

Later authentication/provider requirements must return to normal scope, architecture, and technology control rather than extending this proof implicitly.
