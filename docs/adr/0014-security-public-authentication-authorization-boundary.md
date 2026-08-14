# ADR-0014: Security public authentication and authorization boundary

- Status: Accepted
- Date: 2026-08-14

## Context

ADR-0013 establishes one universal invariant for every construct classified as a module: independent ownership, an explicit public API, a private implementation, selectable composition, and no functional collaboration through another module's private implementation.

Scope #97 applies that invariant to Security. Authentication + Authorization belong to an independently owned Security module with planned `api` and `impl` projects. The application runtime becomes construction/configuration/wiring only for Security, while Event-Registration remains a non-module workflow composition.

The current executable participant-private proof predates that target. ADR-0012 selected Spring Security with stateless HTTP Basic, externally supplied encoded verifier configuration, and direct opaque stable principal-to-actor adaptation. Implementation #91 places Spring Security configuration and actor establishment in `platform/apps/platform`, while Event-Registration currently performs the participant owner comparison. Those locations are accepted executable history but are migration debt under ADR-0013 and scope #97.

Decision #99 resolves the remaining public-contract question before corrective implementation. The required boundary must preserve the existing Goal #57 behavior without introducing roles, a generic policy engine, Person/Account, durable identity mapping, or Event/Registration business truth into Security.

## Decision

### Public Security boundary

The planned Security module has the standard module shape:

~~~text
platform/modules/security/
├── api/
└── impl/
~~~

`security-api` owns the complete framework- and transport-neutral collaboration surface for the demonstrated Authentication + Authorization need.

`security-impl` privately owns the admitted Security implementation and mechanism adapters.

Security remains **Planned** until corrective implementation is accepted. Recording this boundary does not make the module Current.

### Authenticated actor semantic

`security-api` owns `AuthenticatedActorReference`.

It represents only the authenticated platform actor required by participant-private application behavior. Its value is:

- non-null;
- nonblank;
- opaque;
- stable for the configured platform principal;
- free of participant profile or business meaning;
- framework-neutral;
- transport-neutral.

The current behavior remains:

`authenticatedPrincipalName -> AuthenticatedActorReference(authenticatedPrincipalName)`

The configured-principal validation rules selected by ADR-0012 remain private Security implementation/configuration concerns rather than additional public actor fields.

The actor semantic does not move into `core`. No `security-api -> core` dependency is selected.

### Authentication public contract

The minimum Authentication public contract is:

~~~text
AuthenticatedActorProvider
    -> AuthenticatedActorReference authenticatedActor()
~~~

The contract returns only the currently authenticated opaque actor.

It does not accept credentials and does not expose Spring Security `Authentication`, HTTP Basic evidence, Servlet objects, roles, authorities, provider subjects, sessions, or tokens.

Absence of an authenticated actor is an explicit framework-neutral authentication-required failure. A dedicated public exception may represent that semantic; Spring Security exceptions must not cross `security-api`.

`ExecutionContext` is not part of the Authentication contract. Authentication establishes actor identity for the current technical request boundary; correlation remains separate.

The current functional consumer is the HTTP interface.

### Authorization public contract

The minimum demonstrated Authorization contract is ownership-only:

~~~text
ResourceOwnerReference(reference)

AuthorizationDecision = ALLOWED | DENIED

AuthorizeResourceOwnership.authorize(
    AuthenticatedActorReference actor,
    ResourceOwnerReference owner
) -> AuthorizationDecision
~~~

`ResourceOwnerReference` is a Security-owned opaque policy input. Security does not persist it and does not interpret it as Event identity, Registration identity, a database row, a provider subject, or another business entity type.

For this proof the final Authorization rule is equality between the opaque authenticated actor reference and the opaque expected-owner reference.

Denial is an expected decision result, not an infrastructure failure. `AuthorizationDecision.DENIED` crosses the public boundary as a value.

`ExecutionContext` is not part of this pure ownership decision. The decision introduces no logging or audit side effect.

No action enum, role/permission model, RBAC/ABAC framework, generic policy engine, or resource hierarchy is accepted. Retrieve and cancel currently share the same ownership predicate; creation has no independent owner-authorization predicate.

### Event-Registration domain-fact boundary

Event-Registration retains Event/Registration interpretation and cross-capability workflow.

For participant-private retrieval or cancellation it:

1. retrieves Registration state through Registration public API;
2. verifies the Registration target namespace is `event`;
3. verifies the Registration registrant namespace is `participant`;
4. translates only `registration.registrantReference().reference()` into `ResourceOwnerReference`;
5. asks `AuthorizeResourceOwnership` for the final actor-versus-owner decision.

The target and registrant namespace checks remain Event-Registration workflow/domain facts. They are not Security policy input.

Security receives only:

- `AuthenticatedActorReference`;
- `ResourceOwnerReference`.

Security does not receive or own:

- `RegistrationView`;
- `RegistrantReference`;
- `TargetReference`;
- Event identity or publication state;
- Registration lifecycle;
- persistence access;
- Event-registration resource identity;
- Event or Registration implementation types.

Event-Registration maps `AuthorizationDecision.DENIED` to its existing workflow-specific authorization-denied semantic.

The HTTP adapter continues to own the Event-registration external privacy mapping that conceals that workflow denial as the same `404` existence disclosure used for unknown private Event-registration state.

Security does not own that Event-registration disclosure rule.

### Create versus retrieve/cancel

Create requires Authentication but no separate Authorization call.

Event-Registration continues to derive:

`AuthenticatedActorReference(x) -> RegistrantReference("participant", x)`

and performs its existing Event/workflow validation before invoking Registration.

No pre-existing resource owner exists for creation, so adding an Authorization call would create an artificial policy.

Retrieve and cancel use the same ownership Authorization contract after Event-Registration has obtained and translated the required domain facts.

### Private Security implementation

`security-impl` privately owns the currently admitted mechanism and Security-specific adapters:

- Spring Security dependency and configuration;
- encoded password-verifier validation;
- externally configured in-memory proof participants;
- the `AuthenticationManager` / provider setup required by the current proof;
- stateless HTTP Basic;
- the Spring Security Servlet/filter-chain adapter;
- authenticated technical-principal extraction;
- `authenticatedPrincipalName -> AuthenticatedActorReference(...)` adaptation;
- implementation of `AuthenticatedActorProvider`;
- implementation of `AuthorizeResourceOwnership`;
- the technical HTTP Basic authentication-failure response behavior required by the Security adapter, preserving the accepted generic `401`, `WWW-Authenticate`, correlation header, and no identity leakage.

No Spring Security, Servlet, HTTP Basic, password-verifier, provider-specific, Event, Registration, or Event-Registration implementation type crosses `security-api`.

`security-impl` does not depend on Event, Registration, Event-Registration, or `platform/interfaces/http`.

### Dependency direction

The planned compile-time direction is:

~~~text
security-impl -> security-api

http-interface -> security-api

event-registration composition -> security-api

platform app -> security-impl
platform app -> security-api   // wiring/types as needed
~~~

The application dependency on `security-impl` exists only for selection, construction, configuration, and wiring. It does not transfer Authentication or Authorization ownership to the runtime.

The following directions are not accepted:

- `security-api -> core`;
- `security-api -> Event`;
- `security-api -> Registration`;
- `security-api -> Event-Registration`;
- `security-api -> HTTP`;
- `security-impl -> Event`;
- `security-impl -> Registration`;
- `security-impl -> Event-Registration`;
- `security-impl -> HTTP`;
- functional consumer -> `security-impl`;
- Event or Registration implementation/persistence -> Security.

Event and Registration acquire no Security dependency.

### Selectable composition

The application composition explicitly selects the Security implementation, supplies external participant credential configuration, constructs it, and wires Security public contracts to consumers.

A Goal #57 application assembly containing participant-private operations is valid only when the required Security public capabilities are supplied.

No dynamic plugin framework, runtime discovery, or hot unload is required.

### Executable architecture enforcement

Corrective implementation must establish separate Gradle projects:

~~~text
platform/modules/security/api
platform/modules/security/impl
~~~

Executable architecture rules must prove at least:

- `security-api` is free of Spring, Servlet, HTTP, provider, Event, Registration, Event-Registration, and persistence dependencies;
- `security-impl` depends on `security-api`;
- HTTP depends on `security-api`, not `security-impl`;
- Event-Registration depends on `security-api`, not `security-impl`;
- only application composition-root construction/wiring may reference `security-impl`;
- Event and Registration remain free of Security implementation/authentication/authorization dependencies;
- private Event/Registration implementation and persistence remain inaccessible to Security.

The authoritative Structurizr model keeps Security Planned until implementation is accepted. Only then may the implemented relationships become Current.

## Relationship to ADR-0012 and ADR-0013

ADR-0012 remains the Accepted historical rationale for the minimum participant-authentication proof: Spring Security, stateless HTTP Basic, external encoded verifier configuration, stable opaque platform principals, privacy constraints, and the bounded non-browser mechanism.

ADR-0012's runtime and Event-Registration ownership statements describe the executable architecture selected before the universal module invariant existed. ADR-0013, scope #97, and this ADR govern permanent target ownership: Security owns Authentication + Authorization, the runtime wires Security, and Event-Registration supplies workflow/domain facts rather than making the final authorization decision.

ADR-0013 remains the governing universal module invariant and is not superseded.

This ADR does not rewrite either prior record.

## Alternatives considered

### Broad authenticated access/security context

Rejected.

Goal #57 demonstrates no need for roles, authorities, provider metadata, session state, or a general-purpose security context in the public API. Returning only the authenticated actor keeps the contract smaller and more stable.

### Generic action/resource policy engine

Rejected.

Retrieve and cancel currently share the same ownership predicate and create has no separate authorization predicate. An action hierarchy, resource model, ABAC/RBAC framework, or generic policy engine would be speculative.

### Keep authenticated actor ownership in Event-Registration

Rejected.

The actor semantic is required by the Security authentication boundary and multiple consumers. Keeping its ownership in a workflow composition would violate the independent Security module boundary accepted by ADR-0013/#97.

### Keep final participant authorization in Event-Registration

Rejected.

Scope #97 explicitly moves Authorization ownership to Security. Event-Registration retains the facts and workflow, but the final actor-versus-owner decision belongs to Security.

### Put the actor semantic in `core`

Rejected.

No general execution/foundation need requires identity in `core`. Security can own the semantic directly without adding a shared-foundation dependency.

### Roles/permissions/RBAC/ABAC

Deferred.

No accepted Goal #57 behavior requires them.

### Provider-specific identity, Person/Account, or durable identity mapping

Deferred.

The current proof authenticates an already opaque stable platform principal and demonstrates no need for broader identity state.

### Security persistence

Rejected for the current proof.

Externally supplied encoded verifier configuration remains sufficient and no Security-owned durable state is required.

## Consequences

The permanent Security public boundary is now explicit enough to implement without inventing architecture while coding.

Corrective implementation can move the current authentication mechanism out of `platform/apps/platform`, move the actor/provider semantic into `security-api`, replace Event-Registration's direct owner comparison with a Security Authorization call while retaining namespace/domain-fact checks, and add executable Gradle/ArchUnit enforcement.

Current external participant-private behavior must remain unchanged, including authentication failure, owner success, non-owner concealment, privacy/logging rules, correlation behavior, restart stability, and real-PostgreSQL evidence.

No new authentication mechanism, role model, provider, identity mapping, Security persistence, Person/Account capability, Event HTTP publication/discovery, Event-Registration module split, or deployment/infrastructure scope is introduced.

Refs: Goal #57; ADR-0012; ADR-0013; scope #97; decision #99; documentation #100.
