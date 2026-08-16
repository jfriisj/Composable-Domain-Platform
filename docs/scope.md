# Project Scope

## Authority

This document is the authoritative source for the current accepted project scope of Composable Domain Platform.

## Purpose

Composable Domain Platform provides a reliable foundation for composing independently bounded business capabilities. It is not tied to one business domain.

Event management is the first reference capability, but it is not the platform core and does not define the platform's general model.

## Current accepted phase

**Selectable application composition proof**

Goal #114 makes the developer-facing selectable-application-composition proof selected by decision #112 and accepted for scope through issue #115 the current accepted phase.

The accepted outcome is that a platform developer can construct a valid executable application composition using only the already accepted modules and architectural constructs required for that composition's declared outcome.

The proof must demonstrate that:

1. at least one valid executable application composition deliberately omits at least one otherwise accepted module that is unrelated to that composition's declared outcome;
2. the omitted module's private implementation is absent from that valid composition's functional compile-time dependency graph;
3. the omitted module is not required at runtime for that valid composition to start and serve its declared behavior;
4. selected modules collaborate only through accepted public APIs/contracts and adapters;
5. unrelated modules remain independently buildable and testable when omitted from that application composition;
6. a composition whose declared workflow requires a missing public capability is invalid and fails explicitly rather than acquiring a hidden dependency;
7. the application runtime remains technical selection, construction, configuration, and wiring only;
8. executable architecture/dependency verification covers the accepted selectable-composition property where mechanically practical;
9. later build-affecting implementation must pass focused validation and the canonical root `./gradlew --no-daemon check` gate.

Existing ownership remains unchanged: Event, Registration, and Security retain their module ownership; Event-Registration remains a non-module cross-module workflow composition; HTTP remains an inbound adapter; `platform/apps/platform` remains a technical composition root; `core` remains small and business-neutral; and no module may depend on another module's private implementation or persistence.

This scope is solution-neutral. It does not select another application project, Gradle feature variants, Spring profiles, conditional wiring, feature flags, dynamic plugins, runtime module discovery, service extraction, another dependency-injection mechanism, code generation, or new module APIs. A material architecture or technology requirement discovered during implementation planning must return to the applicable decision, ADR, technology, or scope gate.

This phase does not authorize a new business capability or bounded context, module ownership changes, Event-Registration reclassification or splitting, new persistence or migrations, new external HTTP/product contracts, Person/Account, payment/ticketing/capacity/waitlists/notifications/frontend, dynamic extension systems, deployment/hosting/TLS/observability/secrets infrastructure, unrelated ADR-0013 migration debt, or unrelated engineering-quality hardening.

Scope acceptance removes the accepted-scope blocker only. Implementation is not ready until this scope change is accepted into `development` and post-merge planning re-reads Goal #114, the accepted scope, and relevant architecture/module/build truth.

## Completed accepted product scope

**Minimum usable adult Event Registration lifecycle — completed**

The completed product phase proved the participant lifecycle selected by decision issue #53 and accepted for scope through issue #65:

> An adult participant can discover an Event that has intentionally been made available, register participation, later retrieve their private Event-registration state, and cancel that registration.

This scope translates the completed planning decisions #59, #60, #61, #64, #84, Accepted ADR-0011, Accepted ADR-0012, and the ADR-0013 corrective boundary admitted through #97 into implementation authorization. Event, Registration, persistence, correlation, HTTP-adapter, and product behavior remain bounded as before; Security ownership is corrected so Authentication + Authorization belong to the independently owned Security module rather than the application runtime or Event-Registration composition.

Scope acceptance is not automatic implementation readiness. Each executable subgoal must still satisfy normal dependency, architecture, technology, ownership, and validation gates.

### Concrete participant use case

The accepted implementation must support one coherent workflow in which:

1. a newly defined Event is not participant-discoverable until Event explicitly publishes it;
2. public participant discovery returns published Events only;
3. a participant-private Event-registration operation receives a transport-neutral authenticated actor reference established by an external/security boundary;
4. Event-Registration composition derives participant Registration ownership from that actor reference rather than trusting caller-supplied participant ownership;
5. the owning participant can create and retrieve their Event-registration state;
6. Registration exposes its generic `active` / `cancelled` lifecycle;
7. the owning participant can cancel through Event-Registration composition and later retrieve the same durable Registration as `cancelled`;
8. a different authenticated participant cannot learn whether another participant's private Event-registration resource exists;
9. correlation remains independent from participant identity;
10. Event publication state and Registration lifecycle state survive application-process restart against the same PostgreSQL database.

### Event publication and discovery

Event owns participant publication truth.

The accepted minimum publication lifecycle is:

- `unpublished` — the Event exists and remains retrievable by known Event identity but is absent from participant discovery;
- `published` — the Event has intentionally been made available for participant discovery.

A newly defined Event is `unpublished`. The only accepted transition is:

`unpublished -> published`

No unpublish/withdraw transition is accepted.

Participant discovery is public and returns only published Events. Known-id Event retrieval remains independent of publication state.

Publication does not define registration eligibility, registration opening/closing periods, capacity, quotas, waitlists, participant eligibility, authorization, payment, or ticketing. This scope does not infer a rule that an unpublished Event cannot otherwise be referenced by an already known Event identity.

Publication state is durable Event-owned state and must not require Registration or participant-private state.

### Participant identity and authorization boundary

Published Event discovery is public.

Event-registration creation, retrieval of a participant's private Event-registration state, and participant cancellation are participant-private.

Authentication + Authorization belong to the independently owned Security module admitted by ADR-0013/#97. Participant-private application behavior receives only framework- and transport-neutral Security public contracts/results; Spring Security, Servlet, HTTP Basic, password-verifier, and provider-specific types remain private implementation concerns.

The external caller must not choose participant ownership through an arbitrary `participantReference`. Event-Registration composition continues to own the Event-registration workflow and derives the domain-specific Registration reference:

`AuthenticatedActorReference(x) -> RegistrantReference("participant", x)`

The participant-owns-registration **authorization decision** moves to Security. Decision #99, recorded by ADR-0014, selects the minimum public boundary: Security owns `AuthenticatedActorReference`, `AuthenticatedActorProvider`, opaque `ResourceOwnerReference`, `AuthorizationDecision`, and `AuthorizeResourceOwnership`. Event-Registration retains Event/Registration interpretation and supplies only the opaque expected-owner reference after validating the Event target and participant registrant namespaces. Security must not become owner of Event publication truth, Registration lifecycle/ownership state, Event-registration orchestration, or other domain invariants.

Create requires Authentication but no separate Authorization decision: Event-Registration continues to derive `RegistrantReference("participant", actorReference)`. Retrieve and cancel use the same Security ownership decision. No action enum, role/permission model, generic policy engine, or `security-api -> core` dependency is accepted.

Registration remains security-neutral. It does not authenticate, authorize, inspect credentials or tokens, know identity-provider semantics, interpret actor semantics, or depend on Security implementation.

Application semantics keep at least these outcomes distinct:

- authentication required;
- Registration not found;
- authorization denied;
- invalid request.

ADR-0012 and #87 remain the accepted historical rationale/technology admission for the current minimum proof: Spring Security with stateless HTTP Basic protects participant-private Event-registration create, retrieve, and cancel operations, while published Event discovery remains public. Scope #97 changes permanent ownership, not that mechanism.

Participant proof credentials remain externally supplied runtime configuration. Each configured proof participant contains only an opaque stable platform principal identifier and an encoded password verifier. The application runtime supplies configuration while the Security implementation owns credential-verification behavior. Plain-text/no-op password storage remains prohibited. No credential database, participant user repository, Person/Account persistence, identity database, credential migration subsystem, enrollment/reset/recovery/admin API, or identity-provider integration is introduced.

After successful authentication, decision #99, recorded by ADR-0014, preserves the platform-facing actor behavior while moving ownership of the semantic to `security-api`:

`authenticatedPrincipalName -> AuthenticatedActorReference(authenticatedPrincipalName)`

The configured principal identifier remains the platform pseudonym. It must be nonblank, opaque, unique within the configured authentication boundary, stable across restart, and free of participant profile or business meaning. It must not be an email address, display name, raw provider/security subject, credential, token, role, authority, correlation identifier, or causation identifier. No HMAC derivation or durable provider-to-platform identity mapping is accepted for this phase.

The target ownership is:

- `platform/modules/security/api` — framework-neutral public Authentication + Authorization contracts;
- `platform/modules/security/impl` — private Security implementation/adapters, including the currently admitted Spring Security/stateless HTTP Basic proof and encoded credential verification;
- `platform/apps/platform` — selects, constructs, configures, and wires Security but owns no Authentication/Authorization behavior;
- `platform/interfaces/http` — remains an inbound transport adapter and consumes only the public Security boundary needed for HTTP adaptation;
- `platform/compositions/event-registration` — owns Event-registration orchestration and domain fact/context preparation, but not the Security authorization decision.

No consumer may depend on `security-impl` for functional collaboration. Composition-root construction/wiring may reference private implementation types without transferring ownership.

The accepted proof remains stateless and non-browser. It does not introduce form login, login pages, session/cookie authentication, remember-me, logout/session lifecycle, OAuth/OIDC login, or JWT bearer authentication. The existing bounded CSRF treatment remains specific to the stateless non-browser proof and does not establish a browser security design.

HTTP Basic is not considered secure across an untrusted network without secure transport. TLS therefore remains an external prerequisite for such use, while production TLS termination, certificate lifecycle, ingress/proxy, deployment, and hosting infrastructure remain outside this phase.

### Registration cancellation lifecycle

Registration owns the minimum generic lifecycle accepted by decision #61 and ADR-0011.

A new Registration is `active`.

The only accepted transition is:

`active -> cancelled`

Cancelling an already cancelled Registration is idempotent and returns the same cancelled Registration without creating a new relation or changing its identities.

Cancellation preserves:

- `registrationId`;
- `RegistrantReference`;
- `TargetReference`;
- durable retrieval;
- the complete `(RegistrantReference, TargetReference)` uniqueness rule.

A cancelled pair therefore remains occupied. Same-pair re-registration and reactivation are not accepted.

Registration owns lifecycle state, generic cancellation behavior, persistence, and retrieval. Event-Registration composition owns Event-specific orchestration and supplies required domain facts/context; the Security module owns the participant authorization decision before generic Registration cancellation is invoked. Event remains Registration-independent.

### Participant-data and privacy boundary

The authenticated actor reference and derived participant `RegistrantReference` are participant-linked private data for project handling.

Participant-private application behavior must receive a platform-facing opaque stable actor reference rather than a raw upstream/provider security-subject identifier. Registration persists only its own opaque participant reference.

No participant profile or business attributes are required by this phase.

For an authenticated non-owner, the Event-facing external contract must conceal private resource existence using the same not-found disclosure behavior as an unknown private Event-registration. Internal authorization-denied semantics remain distinct.

Unauthenticated participant-private access remains a distinct authentication-required failure.

Normal structured application logs must not contain:

- `Authorization` header values;
- passwords;
- password verifiers;
- configured principal values;
- authenticated actor reference values;
- participant `RegistrantReference.reference` values;
- raw upstream/provider security-subject identifiers.

Correlation and causation identifiers remain identity-free under ADR-0004 and must not be reused as participant identity.

No additional participant-data retention/deletion mechanism is accepted beyond the durable Registration lifecycle. ADR-0011 therefore continues to govern retention of the participant-linked opaque registrant reference as part of durable Registration state.

### Implementation-readiness boundary

ADR-0012/#87 already admit Spring Security and stateless HTTP Basic, and #91 already implements that bounded proof. Scope #97 admits the corrective Security-module ownership migration required by ADR-0013; decision #99 and ADR-0014 now define the minimum public Security boundary without admitting a new authentication mechanism.

The selected public boundary is implementation-ready at architecture-contract level:

1. `security-api` owns `AuthenticatedActorReference` and `AuthenticatedActorProvider`;
2. `security-api` owns opaque `ResourceOwnerReference`, `AuthorizationDecision`, and `AuthorizeResourceOwnership`;
3. neither public contract uses `ExecutionContext` or depends on `core`;
4. Event-Registration retains Event/Registration interpretation, registrant mapping, and namespace/domain-fact validation;
5. create uses Authentication only, while retrieve/cancel use the same ownership Authorization decision;
6. `security-impl` privately owns Spring Security/stateless HTTP Basic, credential verification, technical-principal extraction/adaptation, and Security mechanism adapters;
7. HTTP and Event-Registration depend only on `security-api`, while the application runtime may reference `security-impl` only for construction/configuration/wiring;
8. later Gradle/ArchUnit rules must enforce those directions.

A later implementation child must still define the exact source/build migration slice and validation against this accepted boundary before executable work begins.

Required authentication configuration must continue to fail closed when absent or structurally invalid. Production credential values must not be committed to repository configuration. Deterministic test-only credentials may be supplied through test configuration. No secrets-management product is selected.

A concrete provider-to-platform identity mapping store remains unauthorized. A later need for HMAC derivation, durable mapping, a persistence owner, external authenticator, identity provider, broader authorization policy, or other significant architectural relationship must return to normal decision and architecture control.

The Security module remains **Planned** with separate API and implementation projects until corrective implementation is accepted. It introduces no new application container, persistence owner, Event/Registration dependency, Person/Account capability, or Security persistence. `docs/architecture/workspace.dsl` continues to represent Security API/Implementation and collaboration as Planned only; Current views remain unchanged until implementation is accepted.

### Required implementation validation

The complete accepted implementation must prove at least:

1. a newly defined Event is unpublished;
2. an unpublished Event remains retrievable by known identity and absent from participant discovery;
3. publishing an Event creates durable Event-owned published state;
4. public discovery returns published Events;
5. unauthenticated participant-private creation is rejected;
6. authenticated creation derives participant ownership from the accepted actor boundary rather than caller-controlled ownership input;
7. a new Registration is active;
8. the owning participant can retrieve private Event-registration state;
9. an authenticated non-owner receives the same external resource-existence disclosure as an unknown private Registration;
10. unauthenticated retrieval remains a distinct authentication failure;
11. the owning participant can cancel the Registration;
12. cancellation changes the same durable Registration to cancelled;
13. repeated cancellation is idempotent;
14. cancelled state remains retrievable;
15. the cancelled registrant-target pair continues to prevent same-pair duplicate Registration;
16. Event publication and Registration lifecycle state survive process restart against the same PostgreSQL database;
17. normal structured logs contain no actor or participant registrant-reference values;
18. correlation remains independent from participant identity;
19. Event and Registration ownership/dependency boundaries remain enforced;
20. persistence and authorized, unknown, non-owner, and unauthenticated workflows are validated against real PostgreSQL;
21. focused validation and root `./gradlew --no-daemon check` succeed for build-affecting implementation;
22. `git diff --check` succeeds and implementation remains inside this scope.

The corrective Security migration must additionally prove:

- `platform/modules/security/api` and `platform/modules/security/impl` are separate Gradle projects;
- Security API contains no Spring, Servlet, HTTP Basic, password-verifier, or provider-specific types;
- no functional consumer depends on `security-impl`;
- `platform/apps/platform` contains no owned Authentication/Authorization implementation behavior;
- Event-Registration no longer owns the participant authorization decision;
- Security does not own Event/Registration business truth;
- architecture tests enforce the new Security module boundary;
- current participant-private authentication, privacy, owner/non-owner/unknown/unauthenticated, correlation, restart, and real-PostgreSQL behavior remains preserved.

### Explicitly out of scope

This phase does not authorize minors/guardian/consent flows, organizations or multi-tenancy, Event unpublish/withdraw, registration opening/closing periods, capacity/quotas/waitlists, same-pair re-registration/reactivation, cancellation reasons/history or required cancellation timestamps, Event-specific cancellation deadlines/policy, payments/pricing/ticketing/invoicing/refunds, notifications/messaging, check-in/attendance, frontend implementation, Person/Account capability, participant profile data, roles/permissions, OAuth/OIDC, JWT, sessions/cookies, form login, a specific identity provider, raw provider/security-subject identity as Registration durable state, HMAC actor derivation, durable identity-mapping storage, credential persistence, participant-identifier audit/security logging infrastructure, new participant retention/deletion/anonymization workflows, production TLS termination or deployment infrastructure, secrets-management products, Docker/OCI application/runtime/deployment packaging outside the separately admitted developer-environment scope below, Kubernetes, Terraform/OpenTofu, cloud/provider provisioning, unrelated Event or Registration lifecycle expansion, or any other technology/capability not separately accepted.

Spring Security with stateless HTTP Basic remains admitted only for the minimum participant-private proof described in this phase. Scope #97 relocates ownership of that existing mechanism into private Security implementation/adapters; it does not admit another authentication technology, RBAC/role model, OAuth/OIDC, JWT, or provider.

## Accepted developer-environment scope

**Reproducible Docker developer environment — admitted for Goal #116**

Goal #116 admits one repository-controlled developer environment as engineering scope. This developer-tooling boundary is separate from application runtime and deployment packaging and does not claim that the environment is already implemented.

The initial supported host boundary is Linux with Git, Docker Engine, the Docker Compose plugin, and developer permission to control the Docker daemon. macOS, Windows, Docker Desktop, Podman, Colima, Rancher Desktop, remote Docker, rootless Docker, and other Docker-compatible engines are not part of the initial support contract.

The repository may provide one Docker Compose developer container that:

- uses an official Eclipse Temurin JDK 21 image input pinned by immutable digest;
- contains only the minimum tooling required for repository development;
- uses the repository Gradle Wrapper as the Gradle authority and does not install an independent Gradle distribution;
- bind-mounts repository source and repository-local generated output;
- runs normal development work as a non-root user and preserves usable host ownership for generated repository files through host UID/GID handling;
- uses the host Docker Engine through the Docker socket / sibling-container pattern so Testcontainers can continue to create its own containers;
- handles Docker-host-visible repository paths and Docker-socket permissions explicitly rather than assuming primary UID/GID mapping alone is sufficient;
- may persist Gradle user-home/cache data in a Docker-managed persistent volume as disposable, non-authoritative performance state.

Docker-daemon access is an explicitly accepted trusted-host privilege boundary for this developer workflow. The developer container and repository code executed inside it are trusted with Docker-daemon authority; the container is not a security-isolation boundary from the host. Docker-in-Docker is not admitted for the minimum Goal #116 proof.

Automated validation continues to use Testcontainers-owned ephemeral real PostgreSQL, including the current `postgres:18.4` evidence. `./gradlew --no-daemon check` must remain able to create and destroy its own test containers through the host Docker Engine. A shared Compose database must not replace Testcontainers for automated validation.

Docker Compose may additionally provide an optional PostgreSQL 18.4 service for manual repository-local development and `bootRun`. Its image input must be pinned by immutable digest, and any development database volume is disposable, non-authoritative state. This service must not be required merely to enter the developer environment or run the authoritative repository validation gate.

Developer-image logic must avoid architecture-specific installation logic where selected official images provide native variants. Linux `amd64` and `arm64` may remain design-compatible targets, but the project must not claim an architecture as validated until the complete developer-environment acceptance gate has actually succeeded on that architecture.

The accepted operational artifact remains the executable Spring Boot/JVM JAR. This developer-tooling admission does not authorize application OCI images, application/runtime container packaging, registry publication, orchestration, Terraform/OpenTofu, cloud/provider provisioning, deployment automation, production PostgreSQL operations, TLS/ingress infrastructure, or secrets-management products.

## Accepted operational-runtime baseline

**Minimum operational-runtime proof — completed**

The previously accepted phase established that one accepted platform version can be built into a distinct executable runtime artifact, started reproducibly outside the development workstation, and judged objectively ready without introducing infrastructure provisioning or unrelated production concerns.

The accepted operational contract was established through research issue #30 and decision issue #45.

### Concrete operator use case

A single platform operator must be able to:

1. build one accepted platform version into one executable Spring Boot/JVM application artifact;
2. transfer and start that artifact on one non-developer host or VM without repository checkout, IDE state, or Gradle `bootRun` on the runtime host;
3. supply a compatible Java runtime, reachable PostgreSQL, database configuration, networking, and an available HTTP listen port externally;
4. determine through a machine-checkable signal when the application is ready to serve the accepted HTTP contract;
5. exercise the accepted Event and Event-registration HTTP operations after readiness;
6. stop and restart the application process against the same PostgreSQL database and retrieve durable Event and Registration state created before the restart.

### Packaging and run boundary

The operational artifact is one executable Spring Boot/JVM application artifact produced from the accepted repository build.

Repository checkout plus Gradle `bootRun` remains a development workflow and is not the operational proof boundary.

The runtime host supplies a compatible Java runtime. Docker or OCI packaging is not required or authorized by this phase.

Artifact publication to a registry or repository is not required. The proof may transfer the built artifact through a simpler controlled mechanism.

### Externally supplied dependencies

The operator supplies:

- one non-developer host or VM;
- a compatible Java runtime;
- a reachable PostgreSQL instance;
- database URL, username, and password through the existing externalized runtime configuration boundary;
- networking required for application-to-PostgreSQL and caller-to-application communication;
- an available HTTP listen port.

The project does not provision those resources in this phase.

The platform runtime continues to own application startup and execution of the accepted Event and Registration Flyway migrations.

### Readiness contract

The runtime must expose a machine-checkable readiness signal distinct from mere process existence.

Readiness must remain false until:

1. required runtime configuration has been accepted;
2. PostgreSQL is reachable with the supplied configuration;
3. Event Flyway migrations have completed successfully;
4. Registration Flyway migrations have completed successfully;
5. the application HTTP runtime is available to serve the accepted external contract.

After startup, readiness must report not-ready when PostgreSQL unavailability prevents the runtime from servicing the accepted HTTP use cases.

Readiness is an operational signal, not a business-domain API. It must not expose credentials, database details, stack traces, or implementation internals.

This scope authorizes only the minimum readiness implementation required to satisfy these semantics. It does not preselect Spring Boot Actuator, a readiness endpoint path, a new library, or another readiness technology.

No separate liveness contract is required by this phase.

### Infrastructure-provisioning boundary

Infrastructure remains externally supplied.

This phase does not provision:

- host or VM infrastructure;
- PostgreSQL infrastructure;
- networking or firewall resources;
- cloud or hosting-provider resources.

Terraform, OpenTofu, or another Infrastructure-as-Code technology is not required or authorized. A later provisioning use case requires a separate accepted decision against a concrete provider/operator requirement.

### Operational validation

Implementation must prove at least:

1. one executable JVM application artifact is produced from an accepted repository version;
2. the artifact starts on a clean non-developer host with only documented Java/runtime prerequisites and external configuration;
3. the runtime host does not require repository checkout, IDE state, or Gradle `bootRun`;
4. missing or invalid required database configuration fails closed;
5. unavailable PostgreSQL prevents readiness;
6. Event and Registration Flyway migrations complete before readiness becomes true;
7. readiness is machine-checkable and follows the accepted semantics;
8. accepted Event and Event-registration HTTP operations are serviceable after readiness;
9. durable Event and Registration state survives application-process restart against the same PostgreSQL database;
10. operator instructions are sufficient to repeat the run;
11. focused validation and root `./gradlew --no-daemon check` succeed;
12. `git diff --check` succeeds and the implementation remains inside this scope.

### Explicitly out of scope

This phase does not authorize Docker or OCI packaging, Docker Compose, Kubernetes, Terraform, OpenTofu, cloud or hosting-provider configuration, host/VM provisioning, PostgreSQL provisioning, networking/firewall provisioning, container or artifact registries, artifact-publication infrastructure, TLS termination or ingress, secrets-management products, production PostgreSQL backup/restore/HA/tuning, zero-downtime deployment, rollback automation, horizontal scaling or orchestration, observability infrastructure, authentication or authorization, or unrelated product/domain capability work.

No new technology is admitted by this phase. Java, Gradle, Spring Boot, PostgreSQL, Flyway, and the existing testing baseline remain the applicable accepted directions. Docker remains a candidate only.

## Accepted Registration composition baseline

**Registration composition proof — completed**

The previously accepted phase established the second independently owned business capability and explicit cross-capability Event-registration workflow through the minimum participant-registration use case identified through issues #28, #31, #32, and the domain-boundary correction in #35.

### Concrete requirement

A participant must be able to register participation in an Event that already exists and later retrieve the resulting Event-registration state.

The complete workflow preserves separate ownership of business truth:

- Event owns whether an Event exists and all Event business state.
- Registration owns a domain-neutral registrant-to-target registration relation and Registration-specific invariants.
- An Event-Registration composition owns the Event-specific workflow and the translation from Event workflow identities to Registration references.

### Registration state and invariants

Registration owns only:

- `registrationId`;
- `RegistrantReference` containing `namespace` and `reference`;
- `TargetReference` containing `namespace` and `reference`.

Both references are opaque namespaced identities. Registration validates that their namespace and reference values are nonblank, but it does not interpret namespaces, resolve referenced objects, or branch behavior by namespace.

Registration owns these rules:

1. `registrationId` must be nonblank.
2. Registrant namespace and reference must be nonblank.
3. Target namespace and reference must be nonblank.
4. `registrationId` must be unique.
5. `(RegistrantReference, TargetReference)` must be unique.
6. A uniqueness conflict must not replace or mutate existing Registration state.
7. Registration state can be retrieved independently by `registrationId`.

Whether a registrant or target exists is not Registration-owned truth.

### Registration ownership

Create Registration as an independently bounded domain capability using the established shape:

~~~text
platform/modules/registration/
├── api/
└── impl/
~~~

The Registration public API owns the minimum transport-independent application contracts required to:

- register domain-neutral Registration state;
- retrieve Registration state by `registrationId`;
- expose only `registrationId`, `RegistrantReference`, and `TargetReference`;
- represent invalid Registration definition explicitly;
- represent Registration uniqueness conflict explicitly;
- carry the existing `ExecutionContext`.

The Registration implementation owns Registration domain rules, application services, an application-owned persistence port, Registration persistence, and private persistence adapters.

Registration must not depend on Event, Person, Participant as a bounded capability, HTTP transport types, Spring runtime concepts, authentication, authorization, credentials, identity-provider types, or another business capability.

### Event ownership

Event continues to own Event identity, definition, name, slug, schedule, timezone, existence, application behavior, persistence, and schema.

Event must not store Registration identities and must not depend on Registration or the Event-Registration composition. No Event lifecycle behavior is added by this phase.

### Cross-capability composition

Create one composition project:

`platform/compositions/event-registration`

The composition owns only the Event-specific workflow spanning Event and Registration. Its registration operation must:

1. receive `ExecutionContext`, `registrationId`, `eventId`, and the opaque `participantReference`;
2. resolve the Event through the Event public API;
3. return an explicit transport-independent unknown-Event workflow outcome when the Event does not exist;
4. map `participantReference` to a Registration `RegistrantReference` in the `participant` namespace;
5. map `eventId` to a Registration `TargetReference` in the `event` namespace;
6. invoke the Registration public API;
7. propagate Registration success or explicit Registration failure as a transport-independent Event-registration workflow outcome.

For retrieval, the composition retrieves Registration through its public API and exposes it as Event-registration state only when the target reference belongs to the Event namespace.

The composition may depend on `core`, Event public API, and Registration public API only. It must not depend on either capability implementation or persistence, HTTP, Spring Web, jOOQ, Flyway, or PostgreSQL APIs.

A separate API/implementation split is not required for this first composition because it owns orchestration only and has no independent domain or persistence model.

### Dependency direction

~~~text
event-impl -> event-api -> core

registration-impl -> registration-api -> core

event-registration composition -> event-api
event-registration composition -> registration-api
event-registration composition -> core

http-interface -> event-registration composition
http-interface -> event-api
http-interface -> core

platform-app -> http-interface
platform-app -> event-registration composition
platform-app -> event-impl
platform-app -> registration-impl
~~~

Runtime dependencies on private implementation types remain technical-wiring exceptions only.

### External HTTP contract

The authoritative Event-facing HTTP contract remains `platform/contracts/http/v1/event.yaml`. It contains the existing Event operations and is the accepted home for the Event-registration workflow operations. The document may separate responsibilities with tags such as `Event` and `EventRegistration`, but contract-file grouping does not change bounded-context or composition ownership. No generic Registration target dispatcher is introduced.

Within the unified Event-facing contract, the Event-registration surface is:

- `POST /api/v1/event-registrations`
- `GET /api/v1/event-registrations/{registrationId}`

POST input contains only `registrationId`, `eventId`, and `participantReference`. Generic Registration namespace/reference mechanics are not exposed as transport state.

POST behavior:

- `201` — Event registration created;
- `400` — structurally invalid input or explicit Registration invalid-definition failure;
- `404` — referenced Event does not exist;
- `409` — Registration uniqueness conflict;
- `500` — sanitized unexpected failure.

GET behavior:

- `200` — Event registration found;
- `404` — Registration is unknown or is not an Event-target registration;
- `500` — sanitized unexpected failure.

Both POST and GET map through the Event-Registration composition. A single OpenAPI generation step may derive separate generated API interfaces for the `Event` and `EventRegistration` tags plus shared transport models from the unified contract.

Every response preserves the accepted `X-Correlation-Id` behavior, and the resulting `ExecutionContext` is propagated through composition and Registration application calls.

### Registration persistence

Registration owns PostgreSQL schema `registration` and table `registration.registrations` with only:

- `registration_id`;
- `registrant_namespace`;
- `registrant_reference`;
- `target_namespace`;
- `target_reference`.

Registration migrations belong under `platform/modules/registration/impl/src/main/resources/db/migration/registration/`.

Registration follows the accepted persistence pattern: application-owned persistence port, private jOOQ adapter, Registration-owned Flyway migrations, and PostgreSQL integration validation through Testcontainers.

The database must enforce atomically:

- unique `registration_id`;
- unique `(registrant_namespace, registrant_reference, target_namespace, target_reference)`.

A duplicate must preserve existing durable state.

There must be no Event-specific Registration column, foreign key to `event.events`, cross-schema Event lookup, or direct cross-capability table access. Event existence is validated through the Event public API by the Event-Registration composition.

### Authentication, authorization, and future identity ownership

Authentication identity and Registration registrant identity are distinct concepts. An authenticated actor is not inherently the registrant.

Registration does not authenticate or authorize callers and does not depend on credentials, JWT, OAuth/OIDC, Spring Security, users, roles, Person, or an identity provider.

Technical authentication belongs at an external/security boundary. Domain-specific authorization belongs with the capability or composition owning the required business truth. Any future security information crossing application boundaries must use transport-neutral contracts.

A future Person capability may supply a canonical identity used to construct a namespaced `RegistrantReference`; Registration remains unaware of Person. Identity reconciliation or canonicalization across namespaces is not Registration-owned behavior.

Authentication/authorization implementation and a Person capability remain outside this phase.

### Architecture verification

Executable architecture verification must prove at least:

1. Registration API depends only on allowed business-neutral platform contracts.
2. Registration domain/application code does not depend on Event, Person, HTTP, Spring runtime, generated OpenAPI types, security technologies, or database technologies.
3. Registration persistence adapters remain private implementation details.
4. Event production code does not depend on Registration or the Event-Registration composition.
5. The composition depends only on `core`, Event API, and Registration API.
6. The composition does not depend on either capability implementation or persistence, HTTP, Spring Web, jOOQ, Flyway, or PostgreSQL APIs.
7. The HTTP interface does not depend on Registration implementation or persistence.
8. The application runtime remains technical wiring only.
9. Registration persistence is validated against real PostgreSQL through Testcontainers.
10. End-to-end HTTP validation proves the Event-to-composition-to-Registration workflow against real PostgreSQL.

Root `./gradlew --no-daemon check` remains the executable repository validation gate.

### Acceptance criteria

The phase is complete when:

1. Registration exists as a separately owned API/implementation domain module.
2. Registration exposes only the accepted domain-neutral state and application contracts.
3. Valid domain-neutral Registration creation succeeds.
4. Duplicate `registrationId` is rejected without replacing existing state.
5. Duplicate `(RegistrantReference, TargetReference)` is rejected atomically without replacing existing state.
6. Registration can be retrieved by `registrationId`.
7. Registration owns its PostgreSQL schema, migrations, persistence port, and private persistence adapter.
8. Registration contains no Event-specific persistence field or direct Event access.
9. Event does not depend on Registration and does not store Registration identities.
10. Registration for an existing Event succeeds through the Event-Registration composition.
11. Registration for an unknown Event returns the accepted unknown-Event outcome and creates no Registration.
12. The composition maps participant and Event identities to the accepted namespaced Registration references.
13. `POST /api/v1/event-registrations` exposes the complete Event-specific workflow.
14. `GET /api/v1/event-registrations/{registrationId}` exposes only Event-target Registration state.
15. HTTP responses preserve the accepted correlation behavior.
16. Unexpected failures remain sanitized.
17. Executable architecture tests enforce the new capability and composition boundaries.
18. Unit, persistence, adapter, and end-to-end tests cover the accepted behavior.
19. Root `./gradlew --no-daemon check` succeeds.
20. No explicitly excluded adjacent capability is introduced.

### Explicitly out of scope

This phase does not authorize registration cancellation, registration status or approval, waitlists, Event capacity, registration opening or closing periods, participant profiles, Person capability implementation, user accounts, authentication or authorization, Spring Security, identity-provider integration, tickets, pricing, payment, invoices, email, notifications, messaging or event publication, asynchronous processing, frontend implementation, external-provider integration, deployment or hosting changes, Docker or OCI packaging, observability infrastructure, unrelated Event lifecycle expansion, Event deletion, or cross-capability deletion consistency behavior.

No new technology is admitted by this phase. The implementation reuses the established technology baseline where applicable.

## Accepted implementation baseline

**Event runtime and HTTP interface — completed**

The Event runtime and HTTP interface phase remains the completed implementation baseline on which the current Registration composition proof builds.

The repository, architecture, executable Gradle build, project workflow, continuous-integration foundation, executable architecture verification, Event reference module, and Event-owned durable PostgreSQL persistence have been accepted.

The completed phase established the first externally callable vertical slice through the existing Event capability without adding new Event lifecycle behavior or another bounded context.

The detailed Event runtime and HTTP sections below record that accepted baseline and do not expand the current Registration composition phase.

## Concrete requirement

A platform operator must be able to start one platform application process, define an Event through a versioned HTTP contract, and retrieve the same Event later through HTTP from the existing durable PostgreSQL state.

The HTTP boundary must expose only the already accepted Event define and retrieve behavior:

- `POST /api/v1/events` defines an Event.
- `GET /api/v1/events/{eventId}` retrieves an Event by identity.
- Successful definition returns HTTP `201`.
- Successful retrieval returns HTTP `200`.
- Duplicate Event identity returns HTTP `409` without changing the persisted Event.
- Unknown Event identity returns HTTP `404`.
- Structurally or domain-invalid client input returns HTTP `400`.
- Unexpected internal failures return HTTP `500` through a contract-defined server-error representation without exposing stack traces, persistence records, SQL, jOOQ exceptions, or implementation types.

The HTTP representation must preserve the currently accepted Event fields: `eventId`, `name`, `slug`, `startsAt`, `endsAt`, and `timezone`.

The OpenAPI document is the authoritative external HTTP contract. Transport models are adapter-layer types and must not become Event domain or application models.

Domain-invalid Event definitions must be represented by an explicit, transport-independent Event public application failure/result. The HTTP adapter must not infer domain-invalid input by catching generic implementation exceptions or by duplicating Event business validation.

Every HTTP response must carry an `X-Correlation-Id` header. If a request supplies a correlation identifier, the boundary preserves it; otherwise the boundary creates one. The correlation identifier is opaque and business-neutral and must be propagated explicitly into the Event application boundary through the minimum shared execution-context primitive required by the already accepted traceability architecture.

The accepted Event runtime and HTTP phase did not add Event update/delete lifecycle behavior, authentication/authorization, another bounded context, messaging, frontend work, deployment, or external-provider integration.

## Runtime and HTTP admission

### Concrete use case

Start the platform application and define or retrieve the existing durable Event capability through a stable external HTTP contract.

### Why the current baseline is insufficient

The Event bounded context can define and retrieve durable Event state, but only through in-process Java application contracts. The repository has no application composition root, executable server runtime, HTTP interface, external contract, runtime database configuration, or external-entry correlation propagation.

Therefore the first reference capability cannot yet be exercised as a running platform boundary.

### HTTP interface owns

- The versioned external Event HTTP contract.
- HTTP request/response transport types.
- Mapping between HTTP transport types and Event public application contracts.
- HTTP status and transport-error mapping.
- Establishing or accepting the HTTP correlation identifier and returning it to the caller.
- HTTP-specific structural validation.

### Application runtime owns

- The executable Spring Boot composition root.
- Wiring the HTTP interface to the existing Event application implementation.
- Constructing the Event PostgreSQL persistence adapter with runtime configuration.
- Applying the existing Event-owned Flyway migrations before the application accepts requests.
- Minimal externalized database configuration required to start the application.
- Runtime propagation of the correlation context across the HTTP-to-Event boundary.

### Platform core owns

- Only the smallest business-neutral execution-context type required to carry the Correlation ID explicitly across in-process module boundaries.

The accepted Event runtime and HTTP phase did not authorize a general utilities library, logging framework abstraction, security context, messaging envelope, distributed tracing API, or other speculative core mechanism.

### Event continues to own

- Event business rules and state.
- `DefineEvent`, `FindEvent`, and Event application result semantics.
- Event persistence port, PostgreSQL schema, Flyway migration history, and jOOQ persistence adapter.

The HTTP interface and application runtime must not duplicate or reinterpret Event business rules.

## Technology decision

### Problem

The accepted Event implementation has no executable application host or external transport. A new runtime must compose the existing ports/adapters, expose HTTP without contaminating Event domain/application code with runtime types, and make the external contract executable and version controlled.

The first external entry point must also satisfy the existing correlation-propagation architecture.

### Requirement

The implementation needs:

- one executable Java application runtime;
- dependency injection and HTTP server bootstrap;
- a version-controlled authoritative HTTP contract;
- generated or otherwise build-verified transport interfaces/models derived from that contract;
- explicit transport-to-application mapping;
- minimal externalized PostgreSQL runtime configuration;
- startup execution of the existing Event Flyway migrations;
- real end-to-end HTTP validation against PostgreSQL;
- explicit Correlation ID establishment and propagation.

### Alternatives considered

- **Spring Boot + Spring Web + OpenAPI + OpenAPI Generator** — follows the existing accepted technology directions, provides a focused composition/runtime mechanism, and keeps generated transport types at the interface boundary.
- **JDK HTTP server with manually maintained JSON and contract mapping** — can expose HTTP with fewer dependencies but creates bespoke server/bootstrap and contract-drift mechanisms that the accepted technology direction already intends to avoid.
- **A different lightweight Java HTTP framework** — technically viable, but introduces an additional runtime direction without a demonstrated advantage over the already accepted Spring Boot direction.
- **Place Spring HTTP controllers inside `event-impl`** — reduces project count but couples the Event bounded context to a transport/runtime framework and weakens the existing module boundary.
- **Expose only the in-process Event Java API** — preserves the current architecture but does not satisfy the external runtime use case.

### Decision

Use Spring Boot as the application runtime and Spring Web for the HTTP adapter.

Create one HTTP interface Gradle project under `platform/interfaces/http` and one executable composition-root Gradle project under `platform/apps/platform`.

The HTTP interface depends on the Event public API and the minimum shared execution-context API, not on `event-impl` or Event persistence.

The application composition root may depend on the private Event implementation only for explicit wiring. It must not contain Event business rules.

Store the authoritative versioned Event OpenAPI contract under `platform/contracts/http/`. Use OpenAPI Generator during the build for the server-side transport interface/model surface required by the HTTP adapter. Generated sources belong to the build output and are not an independently edited source of truth.

Use Jakarta Validation only at the HTTP transport boundary when required to enforce structural constraints expressed by the OpenAPI contract.

Keep manual mapping for the small current transport surface; MapStruct is not authorized by this phase because the current mapping requirement does not justify it.

Use the existing PostgreSQL, Flyway, jOOQ, and Testcontainers decisions. Runtime wiring may add the PostgreSQL connectivity and Flyway dependencies required to start against the existing Event-owned database schema.

Spring Boot, Spring Web, OpenAPI Generator, and any newly introduced runtime dependencies must be pinned through the repository's established dependency/version mechanisms during implementation.

Spring Data, Hibernate/JPA, Spring Modulith, Spring Security, and an observability stack are not authorized by this phase.

## Accepted scope for the completed phase

- Create the minimum business-neutral `core` Gradle project required for an explicit execution context containing the Correlation ID.
- Allow `event-api` to depend on that core execution-context contract and extend the existing Event public use-case signatures only as required to carry the execution context explicitly.
- Preserve existing Event business semantics while adding execution-context propagation.
- Add the smallest explicit Event public application failure/result required to represent an invalid Event definition without exposing domain implementation exception types.
- Create `platform/contracts/http/` and add the authoritative versioned OpenAPI contract for the current Event define/retrieve HTTP surface.
- Define `POST /api/v1/events` and `GET /api/v1/events/{eventId}` only.
- Define transport representations for the currently accepted Event fields only.
- Define contract-stable HTTP success and error responses for `201`, `200`, `400`, `404`, `409`, and `500`.
- Map the explicit Event invalid-definition application failure to HTTP `400` without duplicating Event business validation or treating generic implementation exceptions as client errors.
- Define `X-Correlation-Id` request/response behavior and propagate the resulting Correlation ID explicitly into Event application calls.
- Create the `platform/interfaces/http` Gradle project as an inbound adapter.
- Keep the HTTP interface dependent on Event public contracts rather than Event implementation or persistence.
- Use Spring Web only in the HTTP/interface boundary required for this slice.
- Generate the server transport interface/model surface from the authoritative OpenAPI contract during the build.
- Keep generated OpenAPI types and Jakarta Validation annotations out of Event domain and application implementation.
- Create the `platform/apps/platform` executable Gradle project as the Spring Boot composition root.
- Wire the existing Event define/retrieve application services and `JooqEventRepository` in the composition root.
- Make only the minimum implementation-visibility changes required for composition; do not move Event implementation or persistence types into `event-api`.
- Configure a PostgreSQL `DataSource` from minimal externalized runtime properties.
- Apply the existing Event-owned Flyway migrations before the HTTP server accepts application traffic.
- Keep Event schema ownership and migration files inside the Event implementation.
- Add integration validation that starts the HTTP application against real PostgreSQL through Testcontainers.
- Prove through HTTP that an Event can be defined and then retrieved with all accepted fields preserved exactly.
- Prove through HTTP that duplicate identity returns `409` and leaves the existing Event unchanged.
- Prove through HTTP that an unknown identity returns `404`.
- Prove through HTTP that invalid client input returns `400` without infrastructure leakage.
- Prove Correlation ID preservation when supplied and generation when absent.
- Extend executable architecture verification for the new core/interface/runtime dependency boundaries.
- Keep root `./gradlew --no-daemon check` as the authoritative validation gate, including the end-to-end HTTP/PostgreSQL validation.
- Keep the existing GitHub Actions workflow and required `validate` status as the merge gate.
- Update README, module/runtime/interface documentation, architecture documentation/model, and project status when the implementation is accepted.
- Record the runtime/HTTP architecture rationale in ADR-0006.

## Acceptance criteria for the completed phase

The phase is complete when:

1. A documented repository command starts one executable platform application using Java 21 and Spring Boot.
2. The authoritative OpenAPI contract defines only the accepted Event define/retrieve HTTP surface for this phase.
3. `POST /api/v1/events` maps transport input to the existing Event definition use case and returns `201` with the accepted Event representation on success.
4. `GET /api/v1/events/{eventId}` maps to the existing Event retrieval use case and returns `200` with the accepted Event representation when found.
5. The HTTP round trip preserves `eventId`, `name`, `slug`, `startsAt`, `endsAt`, and `timezone` exactly according to the contract.
6. Duplicate Event identity returns `409`, leaves the previously persisted Event unchanged, and exposes no persistence-specific detail.
7. Unknown Event identity returns `404` through a contract-defined transport result.
8. Structurally invalid transport input or an explicit Event invalid-definition application failure returns `400` through a contract-defined transport result without duplicating Event business rules in the HTTP adapter.
9. Unexpected internal failures return `500` through a contract-defined transport result and do not expose stack traces, SQL, persistence records, jOOQ exceptions, or implementation types.
10. Every HTTP response carries `X-Correlation-Id`; a supplied value is preserved and an absent value is generated.
11. The resulting Correlation ID is carried explicitly through the shared execution context into the Event application boundary.
12. The HTTP interface depends on Event public contracts and does not depend on `event-impl`, jOOQ, Flyway, PostgreSQL driver APIs, or Event persistence records.
13. Event domain and application implementation remain independent of Spring Boot, Spring Web, generated OpenAPI transport types, Jakarta Validation, and HTTP concepts.
14. The application composition root contains wiring/configuration only and does not implement Event business rules.
15. Application startup constructs the Event persistence adapter from externalized PostgreSQL configuration and applies the existing Event Flyway migrations before serving Event requests.
16. End-to-end tests exercise the running HTTP boundary against real PostgreSQL through Testcontainers.
17. Existing Event unit, persistence, and architecture tests continue to pass.
18. Architecture verification enforces the accepted dependency direction for core, Event, HTTP interface, and application runtime.
19. Root `./gradlew --no-daemon check` succeeds with the required end-to-end validation.
20. The required GitHub `validate` check succeeds for the compliant implementation.
21. No Event update/delete/status/visibility/publication behavior, authentication/authorization, messaging, frontend, additional bounded context, deployment, or external-provider integration is introduced.
22. Current-state documentation and the authoritative architecture model reflect the accepted runtime and HTTP implementation after completion.

## Explicitly out of scope for the completed Event phase

The following were intentionally excluded from the completed Event runtime and HTTP phase. This list records that historical boundary; the current accepted phase above supersedes it where it explicitly admits later work.

- Event update, delete, publication, status, visibility, registration-opening, or lifecycle behavior beyond define and retrieve.
- Additional Event business fields or business invariants not required by the existing define/retrieve contract.
- Additional HTTP resources or endpoints beyond the two accepted Event endpoints.
- GraphQL, gRPC, WebSocket, or other external protocols.
- Authentication, authorization, user/role management, Spring Security, OAuth2, OIDC, or identity-provider integration.
- Spring Data, Hibernate, or JPA.
- Spring Modulith configuration or verification.
- MapStruct.
- Distributed tracing, OpenTelemetry, metrics backends, dashboards, or production observability infrastructure.
- A general-purpose shared `common`, `utils`, framework abstraction, or logging abstraction.
- Causation-ID behavior for asynchronous work; no asynchronous behavior enters this phase.
- Event publication or messaging infrastructure.
- A separate Event persistence Gradle project or shared repository framework.
- Shared business database schemas, cross-context joins, or direct cross-context persistence access.
- Production-grade secrets management.
- Production connection-pool tuning or database-operations infrastructure.
- Backup, replication, failover, or high-availability database concerns.
- TLS termination, reverse proxy, ingress, API gateway, rate limiting, or network-policy configuration.
- Registration, ticketing, booking, membership, speaker/program, content, payment, accounting, notification, or other business capabilities.
- Frontend implementation.
- Docker image builds or registry publication.
- Deployment automation or hosting-provider configuration.
- Kubernetes or other orchestration.
- Release automation or automatic version/tag creation.
- Artifact/package publication.
- Multi-platform or multi-JDK CI matrices.
- Code coverage services, quality dashboards, broad static-analysis suites, or external CI services.
- Dependency-update automation.
- External provider integrations.
- Redis, Kafka, RabbitMQ, or other infrastructure without a demonstrated requirement.
- Multi-model development workflow automation.

Items not explicitly admitted by the current accepted phase remain outside implementation scope until a later explicit scope decision.

## Business capability admission rule

A new business capability may enter active scope only when all of the following can be answered:

1. What concrete use case requires it?
2. Why can the requirement not be satisfied within the currently accepted scope?
3. What does the proposed bounded context own?
4. What does it explicitly not own?

## Technology admission rule

A new technology or infrastructure component may enter active scope only when:

1. A concrete accepted requirement exists.
2. The current baseline cannot satisfy that requirement adequately.
3. Reasonable alternatives have been considered.
4. The operational and architectural consequences are understood.

Technology must solve an accepted requirement; the project must not invent requirements to justify a technology.

## Scope change rule

Changes to this document are project decisions and must be made through a topic branch and pull request.

A pull request that introduces functionality outside the accepted scope must either remove the out-of-scope change or explicitly update this document and justify the scope change.

Hidden scope expansion inside implementation pull requests is not accepted.

## Deferred ideas

Potential future capabilities may be recorded as deferred ideas, but a deferred idea is not planned scope and must not create implementation, module, infrastructure, or API commitments.

Examples currently include content management, ticketing, booking, membership, surveys, payment integrations, accounting integrations, and Registration lifecycle behavior beyond the accepted minimum.

Their eventual bounded-context boundaries must be determined from real use cases rather than assumed in advance.
