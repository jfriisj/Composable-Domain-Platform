# ADR-0012: Minimum participant authentication boundary

- Status: Accepted
- Date: 2026-08-13

## Context

Goal #57 requires participant-private Event-registration create, retrieve, and cancel operations to be protected by real technical authentication while preserving the existing capability boundaries.

Decision #60 established that technical authentication belongs at an external/security boundary and that Event-Registration composition receives a transport-neutral opaque stable `AuthenticatedActorReference`. The composition maps that actor reference to `RegistrantReference("participant", actorReference)` and owns the participant-owns-registration authorization decision.

ADR-0008 established Registration as a domain-neutral capability. Registration owns Registration identity, opaque registrant and target references, uniqueness, persistence, retrieval, and—through ADR-0011—its generic `active` / `cancelled` lifecycle. Registration does not authenticate callers, authorize participant ownership, interpret Event semantics, or depend on security frameworks.

Decision #64 further constrained participant identity handling: Registration durable state may contain only a platform-facing opaque stable participant reference, not a raw provider or security-subject identifier. Credentials, raw authentication subjects, actor references, and participant registrant-reference values are excluded from normal structured logging, while correlation remains identity-independent.

Research #83 established that accepted `development` contains no configured technical participant-authentication mechanism. It evaluated container-managed Servlet authentication, a custom Servlet authentication filter, Spring Security, and an external trusted authenticator. It also established that a caller-controlled participant or actor header is not authentication and that no durable identity-mapping store is demonstrated as necessary.

Decision #84 selected the minimum authentication design needed to continue Goal #57 without introducing a broader identity capability or changing the accepted Event, Registration, or Event-Registration ownership model.

ADR-0006 already established Spring Boot as the executable application runtime, `platform/interfaces/http` as the external HTTP adapter boundary, and `platform/apps/platform` as the composition root that owns technical runtime wiring. That ADR explicitly deferred security until a concrete requirement existed. Goal #57 now provides that requirement.

## Decision

### Technical authentication framework

Use Spring Security for technical participant authentication inside the existing Spring Boot application/runtime boundary.

The intended later dependency boundary is:

`org.springframework.boot:spring-boot-starter-security`

The compatible Spring Security version remains managed through the existing Spring Boot dependency-management baseline rather than being independently pinned.

Spring Security owns only the technical request-authentication responsibilities required by this proof:

- the Servlet security filter chain;
- HTTP Basic credential extraction and challenge behavior;
- username/password authentication;
- encoded password-verifier matching;
- establishment of the authenticated security context;
- request-level distinction between public and participant-private HTTP operations.

Spring Security does not own:

- Event or Registration business rules;
- participant-owns-registration authorization;
- Event-Registration orchestration;
- Registration lifecycle;
- participant roles or permissions;
- Person or Account state;
- provider integration;
- durable identity mapping;
- resource-existence disclosure policy beyond the external HTTP mapping required by the accepted privacy boundary.

Participant business authorization remains in Event-Registration composition.

Registration remains authentication- and authorization-neutral.

### Minimum proof authentication mechanism

Use stateless HTTP Basic authentication as the minimum application-boundary proof mechanism for participant-private Event-registration operations.

The participant-private operations are:

- create Event registration;
- retrieve private Event-registration state;
- cancel Event registration.

Published Event discovery remains unauthenticated and public.

Missing or invalid credentials are authentication failures. The external authentication failure must not disclose whether a configured principal exists or whether only the supplied password was incorrect.

HTTP Basic is selected only for this minimum proof. Basic credentials are not considered secure over an unprotected network. Secure transport such as TLS is an external prerequisite when this mechanism is used across an untrusted network.

This ADR does not introduce production TLS termination, certificate lifecycle, ingress, proxy, deployment, or other transport-security infrastructure. Local and end-to-end tests may exercise the application authentication semantics without constituting proof of production transport security.

### Credential-verification truth

Participant proof credentials come from externally supplied runtime configuration and are held in memory only for the running process.

Each configured participant entry contains only:

- an opaque stable platform principal identifier;
- an encoded password verifier.

Password verifiers are not stored as plain text or with a no-op encoder. Later implementation may use Spring Security's standard password-encoding facilities, with the encoded verifier identifying its encoding.

The application runtime may load the configured entries into an in-memory authentication service.

No participant credential table, credential repository, Person or Account persistence, identity database, identity-mapping persistence, credential migration subsystem, enrollment API, password-reset API, account-administration API, or identity-provider integration is introduced by this decision.

The eventual Goal #57 proof requires at least two distinct configured participants so owner and non-owner behavior can be demonstrated.

For restart and ownership continuity, the same configured principal identifier must continue to identify the same proof participant. Credentials may change independently from Registration ownership as long as the principal identifier remains stable.

Changing the configured principal identifier changes the participant actor identity and therefore does not preserve ownership of previously created Registrations.

### Platform actor-reference rule

The authenticated technical principal identifier is itself the platform-facing opaque stable pseudonym used by the application boundary.

After successful authentication:

`authenticatedPrincipalName -> AuthenticatedActorReference(authenticatedPrincipalName)`

The configured principal identifier must be:

- nonblank;
- opaque;
- stable across application restart;
- unique within the configured authentication boundary;
- free of participant profile or business meaning;
- not an email address;
- not a display name;
- not a raw provider or security-subject identifier;
- not a credential or token;
- not a role or authority;
- not a correlation or causation identifier.

This preserves the distinction between technical authentication responsibility and application actor semantics while allowing both to carry the same opaque value for the minimum proof.

No HMAC actor derivation is selected for this Goal. Consequently, no derivation secret, derivation-key lifecycle, or provider-to-platform mapping state is required.

A keyed pseudonymization mechanism such as HMAC may be reconsidered later if a concrete upstream identity provider introduces a raw subject that must be transformed before it crosses the application boundary.

### Boundary placement

The selected security mechanism remains within the already modeled runtime and HTTP-interface boundaries.

`platform/apps/platform` owns the technical runtime wiring for:

- Spring Security configuration;
- the stateless HTTP Basic filter chain;
- externally supplied credential-verification configuration;
- establishment of the authenticated technical principal;
- adaptation from that principal to the transport-neutral actor-reference input exposed to the HTTP boundary.

`platform/interfaces/http` remains the transport adapter. For participant-private Event-registration operations it:

- receives a transport-neutral authenticated actor reference through a narrow technical boundary supplied by the application runtime;
- maps HTTP transport input to the participant-private Event-Registration contracts;
- maps application outcomes to external HTTP behavior.

The HTTP interface does not:

- parse Basic credentials itself;
- verify passwords;
- own password encoding;
- own participant authorization;
- derive Registration references directly;
- expose Spring Security `Authentication` or other framework types to Event-Registration composition.

Event-Registration composition continues to receive transport-neutral application inputs and owns:

- `AuthenticatedActorReference(x) -> RegistrantReference("participant", x)`;
- participant ownership authorization;
- Event-specific create, retrieve, and cancel orchestration.

Registration receives only its domain-neutral Registration inputs and remains free of HTTP authentication, credentials, Spring Security, and actor semantics.

No new Gradle module, bounded context, application container, persistence owner, external authenticator, identity provider, gateway, or modeled architecture relationship is introduced.

Spring Security is an implementation technology inside the existing Platform Application runtime boundary, not a new architectural participant.

Therefore `docs/architecture/workspace.dsl` requires no structural change for this decision.

### Stateless and non-browser boundary

The minimum proof is a stateless non-browser HTTP API boundary.

It does not introduce:

- form login;
- application login pages;
- session or cookie authentication;
- remember-me;
- logout/session lifecycle;
- OAuth/OIDC login;
- JWT bearer authentication.

A non-browser API implementation may exclude participant-private operations from CSRF-token requirements so that no session/cookie/CSRF-token workflow is introduced.

That exclusion is bounded to this non-browser proof. A later browser-facing authentication use case must revisit CSRF and credential-handling behavior explicitly and may not assume this design can be reused unchanged.

### External Event-facing semantics

The later scope and OpenAPI implementation must preserve these semantics:

- published Event discovery remains public;
- Event-registration create requires valid authentication;
- Event-registration retrieval requires valid authentication;
- Event-registration cancellation requires valid authentication;
- create does not accept a caller-authoritative `participantReference`;
- participant ownership is derived from the authenticated actor;
- an owner may retrieve private state;
- an owner may cancel the Registration;
- cancelled lifecycle remains externally observable;
- repeated cancellation inherits Registration idempotency;
- authenticated non-owner access and authenticated access to an unknown private Registration use the same external not-found existence disclosure;
- internal authorization-denied behavior remains distinct from not-found;
- unauthenticated access remains a distinct authentication failure;
- correlation and causation remain independent from participant identity.

The exact OpenAPI syntax, cancellation verb/path, DTO changes, and error-schema edits remain later implementation details subject to the accepted semantic boundary.

### Privacy and logging

Normal structured application logs must not contain:

- `Authorization` header values;
- passwords;
- password verifiers;
- configured principal values;
- authenticated actor-reference values;
- participant `RegistrantReference.reference` values.

Authentication failures may emit only coarse identity-free outcome information together with permitted correlation metadata.

Correlation and causation identifiers remain identity-independent.

No audit/security logging subsystem is introduced by this decision.

## Alternatives considered

### Trust caller-supplied participant or actor identity

Rejected.

A caller-controlled `participantReference`, actor header, or equivalent identifier is only an assertion and does not establish authenticated identity. It would violate decisions #60 and #64 and would make participant-private ownership forgeable.

### Container-managed Servlet authentication

Rejected as the preferred mechanism for this proof.

The Servlet API can host container-managed authentication, but accepted `development` has no configured realm or authenticator. Selecting this route would introduce container-specific security configuration and credential-source coupling without reusing an existing accepted mechanism.

### Custom Servlet authentication filter

Rejected.

A custom filter would make the project directly own credential parsing, verification, authentication-failure handling, principal establishment, and related security behavior. Spring Security provides those standard responsibilities without moving business authorization out of Event-Registration composition.

### External trusted authenticator or gateway

Deferred.

No accepted external authenticator, gateway, anti-forgery trust relationship, or deployment architecture exists. Introducing one would expand the architecture and infrastructure beyond the demonstrated minimum requirement.

### HMAC actor-reference derivation

Deferred.

A deterministic keyed pseudonym remains technically viable when a future external provider exposes a subject that must be pseudonymized. The minimum proof already authenticates a platform-defined opaque pseudonym directly, so HMAC would add key-stability and rotation responsibility without a demonstrated need.

### Durable identity mapping

Rejected for this Goal.

No provider-to-platform mapping database or persistence owner is required when the authenticated principal is already the platform pseudonym. Introducing durable mapping would create a new identity persistence concern without accepted need.

### Person or Account capability

Deferred.

The minimum Event-registration lifecycle does not require participant profile, account, canonical-person, recovery, enrollment, or administration behavior.

### Roles or permissions for Registration ownership

Rejected.

Participant ownership is a business authorization decision already owned by Event-Registration composition. It is not represented as Spring Security roles, authorities, or domain-object permissions.

### Sessions, OAuth/OIDC, JWT, mTLS, API keys, or a specific identity provider

Deferred.

These alternatives introduce additional protocol, provider, key-management, session, deployment, or infrastructure concerns not required by the minimum Goal #57 proof.

### Application-owned credential persistence

Rejected.

Credential persistence, enrollment, reset, administration, and identity-database ownership are outside the minimum use case. Runtime configuration is sufficient for the proof.

## Consequences

The platform gains an accepted rationale for real participant authentication at the existing Spring Boot security boundary without changing Event, Registration, or Event-Registration ownership.

Spring Security and stateless HTTP Basic become the selected technical direction for this minimum proof, but this ADR alone does not authorize dependency, source, build, OpenAPI, or runtime changes.

A separate scope and technology-admission step remains mandatory before implementation. That step must update authoritative accepted truth, including:

- `docs/scope.md`, to admit the selected minimum authentication mechanism and Spring Security only to the bounded extent chosen here;
- `docs/tech-stack.md`, to record Spring Security as the accepted direction for this concrete need;
- `docs/architecture.md`, to describe the selected external/security boundary and direct opaque pseudonymous principal rule;
- explicit confirmation that `docs/architecture/workspace.dsl` remains structurally unchanged.

Only after those artifacts are accepted into `development` may implementation add `spring-boot-starter-security`, runtime credential configuration, authenticated actor wiring, participant-private HTTP contract changes, or related tests.

The external HTTP contract will later need to represent HTTP Basic authentication for participant-private Event-registration operations while leaving published Event discovery public.

The existing Event-Registration composition remains the owner of participant authorization, including non-owner denial and existence concealment semantics. Registration remains security-neutral.

No new bounded context, Gradle module, persistence owner, identity mapping store, architectural component, or modeled relationship is introduced.

Production TLS termination, secret-management products, browser authentication, OAuth/OIDC, JWT, sessions/cookies, roles, Person/Account, identity-provider integration, credential persistence, and broader deployment/security architecture remain deferred.

This ADR extends the rationale of ADR-0006, ADR-0008, and ADR-0011. Those ADRs remain Accepted and are not superseded.

Refs: Goal #57; decisions #60, #64, #84; research #83; documentation subgoal #85.
