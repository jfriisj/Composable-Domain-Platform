# Architecture

## Objective

Composable Domain Platform is a modular application platform in which independently bounded business capabilities can be developed, tested, composed, and evolved without relying on each other's internal implementation or persistence model.

The architecture optimizes for explicit ownership and replaceable boundaries rather than for maximum abstraction.

## Architectural style

The baseline combines:

- Domain-Driven Design bounded contexts for business ownership.
- Hexagonal Architecture inside business modules.
- A modular monolith as the initial deployment model.
- Explicit contracts for collaboration.
- Explicit compositions for cross-capability workflows.
- Adapter-based integrations for external systems.

The modular monolith is an implementation and deployment choice, not permission for modules to share internals.

## Universal module invariant

ADR-0013 defines one meaning of **module** across the platform.

Every module is independently owned, selectable into or out of a valid platform composition, exposes an explicit public API, hides its private implementation, and collaborates with other modules only through public contracts and adapters.

A module is never owned or implemented by the application runtime, another module, or a composition. No module depends on another module's private implementation or persistence.

The modular monolith does not weaken this rule. Co-location in one process or repository is a deployment/build choice, not shared ownership.

A composition owns only cross-module workflow. The application runtime owns only technical assembly. Interfaces and integrations are adapter boundaries and are not automatically modules. Shared `core` is current business-neutral foundation and is not automatically a module.

If a construct is called a module, it must satisfy the invariant. Constructs that should not satisfy it must be classified explicitly as something else.

## Hard boundaries

A business module owns its domain model, application use cases, and internal adapters. When it owns durable state it also owns its persistence and migrations.

Other modules and architectural constructs must not:

- import its internal domain or implementation classes for collaboration;
- access its repositories;
- read or write its database tables directly;
- depend on its persistence records;
- reuse internal DTOs as shared contracts.

Collaboration happens only through explicit public module APIs, published events, contracts, and adapters/compositions that themselves respect module ownership.

The executable application composition root may depend on private implementation types only to construct and wire module implementations. That technical dependency does not transfer ownership and does not permit business logic or cross-module implementation collaboration.

## Hexagonal rule

Dependencies point inward:

~~~text
adapter -> application -> domain
             |
             v
        outbound port
             ^
             |
        outbound adapter
~~~

Domain code must not depend on Spring, HTTP, database frameworks, generated OpenAPI types, provider SDKs, or other infrastructure technologies.

Application code orchestrates use cases and declares required outbound ports. Adapters translate between external mechanisms and application/domain concepts.

## Platform core

Platform core must remain small and contain platform mechanisms rather than business concepts.

The current `core` project contains the minimum business-neutral `CorrelationId` and `ExecutionContext` primitives required to propagate correlation context from the HTTP boundary into Event application calls.

Potential additional core responsibilities require their own concrete accepted need. Business concepts such as Event, Ticket, Registration, Payment, Invoice, Speaker, or Booking must not move into core merely to make them reusable.

## Execution context and traceability

Cross-boundary operations must carry explicit execution metadata so a logical flow can be followed through modules, asynchronous work, integrations, and logs.

- **Correlation ID** identifies the complete logical flow. It is preserved when work crosses synchronous or asynchronous boundaries.
- **Causation ID** identifies the immediate operation, command, event, or message that caused a new asynchronous action or message.
- A new entry point without an existing correlation context creates a new Correlation ID.
- Boundary adapters propagate the correlation context when calling another module or external system where the protocol supports it.
- Published messages and events carry correlation metadata in their envelope rather than embedding it in business-domain state.
- Structured logs include the Correlation ID and, where applicable, the Causation ID.
- Correlation and causation identifiers are opaque technical identifiers. They must not contain personal data or business meaning and must not be used for business decisions.

The current HTTP boundary represents correlation as `X-Correlation-Id`, preserves a supplied nonblank value, creates one when absent, and passes the resulting value through `ExecutionContext` into the Event public application boundary.

Correlation is independent of distributed tracing. W3C trace/span context or OpenTelemetry may later complement correlation, but adopting an observability technology is not required to preserve the platform-level Correlation ID.

## Composition over coupling

When two independent modules/capabilities need to cooperate, use a composition that depends on their public APIs rather than making either capability depend on the other's implementation.

~~~text
module A API <- composition -> module B API
~~~

A composition owns the cross-capability workflow only. It does not own or implement participating modules.

A composition is not automatically a module. If a composition is deliberately classified as a module, it must have its own public API/private implementation boundary and satisfy ADR-0013.

## Current Registration composition baseline

The current implemented baseline establishes Registration as the second implemented bounded capability and Event-Registration as the first implemented cross-capability composition.

The accepted structure is:

~~~text
platform/modules/registration/
├── api/
└── impl/

platform/compositions/event-registration/
~~~

Registration is domain-neutral. It owns `registrationId`, a namespaced opaque `RegistrantReference`, a namespaced opaque `TargetReference`, Registration uniqueness rules, the generic `active` / `cancelled` lifecycle, idempotent generic cancellation, retrieval, and its own persistence boundary.

Registration does not interpret namespaces or validate referenced business objects. It does not depend on Event, Person, authentication/authorization technologies, or another business capability.

The Event-Registration composition owns the Event-specific workflow. It resolves Event existence through the Event public API, maps the opaque participant reference to the `participant` registrant namespace, maps Event identity to the `event` target namespace, and invokes the Registration public API. Retrieval also passes through the composition so the Event-specific HTTP surface exposes only Event-target registrations.

The accepted dependency direction is:

~~~text
event-api <- event-registration composition -> registration-api
                         |
                         v
                        core
~~~

Neither Event nor Registration depends on the other capability. Event does not store Registration identities. The composition depends on public APIs only.

The Registration persistence boundary is a Registration-owned PostgreSQL `registration` schema and `registration.registrations` table containing `registration_id`, registrant namespace/reference, target namespace/reference, and Registration lifecycle. No Event-specific column, foreign key, or cross-schema Event lookup is permitted.

The authoritative external Event-facing contract remains `platform/contracts/http/v1/event.yaml`. It contains the existing Event operations and is the accepted location for:

- `POST /api/v1/event-registrations` through the cross-capability composition;
- `GET /api/v1/event-registrations/{registrationId}` through the cross-capability composition;
- `DELETE /api/v1/event-registrations/{registrationId}` through the cross-capability composition.

The unified OpenAPI document may use separate `Event` and `EventRegistration` tags. Contract-file grouping represents the coherent external Event-facing surface; it does not merge internal ownership. Event and Registration remain independent, and the Event-Registration composition continues to own orchestration.

The participant-private transport contract uses only `registrationId` and `eventId` as caller-supplied creation inputs, exposes Registration lifecycle state, and does not accept caller-authoritative participant ownership or expose generic Registration namespace/reference mechanics.

Technical authentication identity and Registration registrant identity remain separate concepts. The existing Platform Application runtime currently implements the bounded Spring Security/stateless HTTP Basic mechanism accepted by ADR-0012/#87, while participant authorization and actor-to-registrant mapping remain in Event-Registration composition. No Person/Account capability is introduced.

ADR-0013 establishes that application-runtime ownership of Security is migration debt rather than the permanent module ownership model. Scope #97 admits the corrective Planned Security module boundary, and decision #99 / ADR-0014 now define its minimum public Authentication + Authorization contracts. The source/build migration remains unimplemented.

The existing HTTP interface and Spring Boot application remain the external adapter and technical composition root respectively.

ADR-0008 supersedes ADR-0007 and records the domain-neutral Registration boundary, Event-specific composition, persistence isolation, and security/identity separation. ADR-0009 supersedes only ADR-0008's separate-contract-file decision by making `event.yaml` the unified Event-facing HTTP contract.

## Accepted minimum participant lifecycle scope

The minimum usable adult Event Registration lifecycle is accepted implementation scope and is being implemented incrementally. Current architecture statements below distinguish accepted implemented state from remaining planned behavior.

The original lifecycle scope preserves the current Event/Registration bounded contexts, composition, persistence owners, external HTTP adapter, executable application container, and Event/Registration dependency direction. Scope #97 additionally admits a planned Security module with separate public API/private implementation projects, without adding a new application container, persistence owner, Person/Account capability, or Event/Registration dependency.

Within the existing Event bounded context, Event now implements durable Event-owned publication state with `unpublished` and `published` states, initial `unpublished` state, a one-way `unpublished -> published` transition, and transport-neutral public discovery of published Events. Known-id Event retrieval remains independent of publication. Publication does not own Registration eligibility, capacity, waitlists, payment, participant identity, or authorization.

Within the existing Registration bounded context, the generic lifecycle accepted by ADR-0011 is now implemented: initial `active` state, idempotent `active -> cancelled` behavior, lifecycle state in Registration retrieval, a transport-neutral generic cancellation operation, and Registration-owned durable persistence. Cancellation preserves Registration identity and the existing complete registrant-target uniqueness rule; cancelled pairs remain occupied. Registration remains domain-neutral and security-neutral. Event-facing participant cancellation is exposed through the actor-bound Event-Registration composition and participant-private HTTP boundary.

The Event-Registration composition implements the transport-neutral participant-private path for create, retrieve, and cancel. That path accepts an opaque stable authenticated actor reference, derives `RegistrantReference("participant", actorReference)`, authorizes retrieval/cancellation against Registration-owned registrant state, exposes Registration lifecycle state, and invokes Registration-owned cancellation only after authorization. The HTTP boundary now uses this actor-bound path, and the transitional caller-owned compatibility contracts have been removed. Technical authentication identity and establishment of the platform-facing actor reference remain external to Event, Registration, and the composition implementation.

The actor reference is participant-linked private data for project handling. Registration persists only its own opaque participant reference; raw upstream/provider security-subject identifiers are not accepted as Registration durable state. Authenticated non-owner access keeps an internal authorization-denied semantic but uses the same external not-found resource-existence disclosure as an unknown private Event-registration. Normal structured logs exclude actor and participant registrant-reference values. Correlation and causation identifiers remain identity-free.

ADR-0012 and scope #87 select the minimum external participant authentication boundary. Spring Security is the accepted technical authentication framework inside the existing `platform/apps/platform` Spring Boot runtime, with stateless HTTP Basic as the minimum non-browser proof for participant-private Event-registration create, retrieve, and cancel operations. Published Event discovery remains public.

Participant proof credentials are supplied through external runtime configuration as an opaque stable platform principal identifier plus an encoded password verifier. The runtime may hold those entries in memory; no credential database, Person/Account persistence, identity database, provider integration, or durable identity-mapping store is introduced. The configured principal identifier is itself the platform-facing pseudonym and is adapted directly as:

`authenticatedPrincipalName -> AuthenticatedActorReference(authenticatedPrincipalName)`

Current executable state still has `platform/apps/platform` configuring the Spring Security filter chain, stateless HTTP Basic, runtime credential verification, authenticated technical-principal establishment, and actor adaptation. `platform/interfaces/http` currently receives that actor reference and remains responsible for HTTP adaptation and external error/privacy mapping.

Under accepted scope #97, that runtime-owned Authentication/Authorization behavior is migration debt. The planned target moves framework-specific authentication behavior into `security-impl`; the runtime only constructs/configures/wires the Security module, and consumers use `security-api`.

Event-Registration continues to own `AuthenticatedActorReference(x) -> RegistrantReference("participant", x)` and Event-specific orchestration/domain fact preparation. Decision #99 / ADR-0014 move ownership of `AuthenticatedActorReference` itself to `security-api` and select the final ownership Authorization contract. For retrieve/cancel, Event-Registration validates the Event target and participant registrant namespaces, translates only the opaque registrant reference value to Security's `ResourceOwnerReference`, and asks `AuthorizeResourceOwnership` for `ALLOWED` / `DENIED`. Create requires Authentication but no separate Authorization call. Registration remains security-neutral and has no dependency on Spring Security, HTTP authentication, credentials, actor semantics, or Security implementation.

The proof is stateless and non-browser: no form login, session/cookie authentication, remember-me, logout/session lifecycle, OAuth/OIDC login, or JWT bearer authentication is accepted. Any CSRF exclusion is bounded to that stateless non-browser API proof and does not establish a browser security design. HTTP Basic requires secure transport across untrusted networks, while TLS termination/deployment infrastructure remains outside Goal #57.

ADR-0012's selected mechanism introduced no new bounded context, Gradle module, application container, persistence owner, external authenticator, identity provider, identity-mapping store, Event/Registration dependency, or modeled relationship. Spring Security therefore remains part of the current executable Platform Application/runtime state.

ADR-0013 does not pretend that current state is already migrated. Scope #97 now accepts the future Security module boundary while keeping the current executable state explicit until implementation.

`docs/architecture/workspace.dsl` therefore adds Security API/Implementation and high-level collaboration as **Planned** elements/relationships only. Current views remain unchanged.

## Planned Security module boundary

Accepted scope #97 introduces the planned physical boundary:

~~~text
platform/modules/security/
├── api/
└── impl/
~~~

Decision #99 / ADR-0014 select the public collaboration surface.

`security-api` owns:

- `AuthenticatedActorReference` — opaque authenticated platform actor;
- `AuthenticatedActorProvider` — narrow Authentication boundary returning the current actor;
- `ResourceOwnerReference` — opaque expected-owner policy input;
- `AuthorizationDecision` — `ALLOWED` / `DENIED`;
- `AuthorizeResourceOwnership` — the current ownership Authorization decision.

Neither public contract uses `ExecutionContext`; `security-api` has no dependency on `core`. No Spring Security, Servlet, HTTP Basic, password-verifier, provider, role/authority, Event, Registration, Event-Registration, or persistence type belongs in the API.

The planned dependency direction is:

~~~text
security-impl -> security-api

platform/interfaces/http -> security-api

platform/compositions/event-registration -> security-api

platform/apps/platform -> security-impl
platform/apps/platform -> security-api
  construction/configuration/wiring only
~~~

`security-impl` privately owns the admitted Spring Security/stateless HTTP Basic implementation, encoded verifier validation, externally configured in-memory proof participants, technical-principal extraction, principal-to-actor adaptation, Authentication/Authorization implementations, and Security-specific Servlet/HTTP Basic adapters.

The HTTP interface consumes `AuthenticatedActorProvider` but does not own credential verification or Security policy.

Event-Registration retains Event/Registration workflow and fact interpretation. For participant-private retrieval/cancellation it verifies the Event target and participant registrant namespaces, translates only the registrant reference value into `ResourceOwnerReference`, and asks Security for the final actor-versus-owner decision. It maps `DENIED` into its workflow authorization-denied semantic; HTTP retains the existing external `404` concealment mapping.

Creation requires Authentication only and continues to derive `RegistrantReference("participant", actorReference)` in Event-Registration. No action enum or generic policy engine is introduced because retrieve and cancel share the same ownership rule and create has no independent owner-authorization predicate.

The application runtime selects, constructs, configures, and wires Security but does not own its behavior. A participant-private Goal #57 assembly is valid only when the required Security public capabilities are supplied.

The following dependencies remain prohibited: `security-api -> core/Event/Registration/Event-Registration/HTTP`; `security-impl -> Event/Registration/Event-Registration/HTTP`; functional consumers -> `security-impl`; and Event/Registration private implementation or persistence -> Security.

Security remains **Planned** in the authoritative Structurizr model until corrective implementation is accepted. Current views continue to describe the executable pre-migration state.

No Security persistence, Person/Account capability, external identity provider, OAuth/OIDC, JWT, RBAC/ABAC/role model, generic policy engine, new application container, or dynamic plugin mechanism is admitted.

## Current reference module

Event is the first implemented bounded context used to validate the module architecture.

Its physical shape remains:

~~~text
platform/modules/event/
├── api/
└── impl/
~~~

The API project contains application-level contracts for defining, retrieving, publishing, and discovering Event state; explicit definition and publication failures; publication state in Event views; and the shared execution context carried by the current use-case signatures.

The implementation project contains the Event domain model, application implementation and outbound persistence port, and a private jOOQ PostgreSQL persistence adapter. Event-owned Flyway migrations define its durable schema.

The HTTP adapter and executable application runtime are outside the Event bounded context. They use the Event public API and composition-only implementation wiring without moving Spring, HTTP, generated OpenAPI, or database runtime concepts into Event domain/application code.

## Current runtime boundary

The executable Event-facing vertical slice is:

~~~text
external HTTP caller
        |
        v
platform/contracts/http/v1/event.yaml
        |
        v
platform/interfaces/http
        |
        +-----------------------> platform/modules/event/api
        |                              ^
        |                              |
        |                        platform/modules/event/impl
        |                              |
        |                              v
        |                         event.events
        |
        v
platform/compositions/event-registration
        |                    |
        v                    v
platform/modules/event/api      platform/modules/registration/api
                              ^
                              |
                      platform/modules/registration/impl
                              |
                              v
                 registration.registrations

platform/apps/platform
  Spring Boot composition root
  shared DataSource
  Event Flyway startup migration
  Registration Flyway startup migration
~~~

`platform/apps/platform` starts the Spring Boot process and wires the HTTP interface, Event services, Registration services, persistence adapters, Event-Registration composition, and the currently embedded participant-authentication proof. Event- and Registration-owned Flyway migrations run during application context construction before their repositories and application services become available to serve requests.

The current Spring Security, encoded participant credential verification, stateless HTTP Basic, and authenticated-principal-to-actor adaptation are accepted executable state from ADR-0012/#91. Under ADR-0013 they are explicit migration debt.

Scope #97 plus decision #99 / ADR-0014 define the planned Security ownership correction: the composition root may construct/configure/wire `security-impl` but must not own Authentication/Authorization behavior. Event-registration workflow/domain facts remain in the composition; the final opaque actor-versus-owner decision belongs to Security. Corrective source/build implementation is still pending.

Event-Registration remains a non-module composition under the current ADR-0013 classification, so #97 does not require a composition `api`/`impl` split.

## Accepted operational-runtime boundary

The accepted next operational scope keeps the existing modular-monolith container and Spring Boot composition root. It adds a build-to-runtime boundary without introducing another application container or infrastructure ownership boundary.

The operational packaging boundary is one executable Spring Boot/JVM application artifact produced from an accepted repository version. The runtime host supplies a compatible Java runtime; repository checkout, IDE state, and Gradle `bootRun` are development concerns and are not required on the operational host.

PostgreSQL remains an externally supplied runtime dependency. The operator also supplies the non-developer host/VM, network reachability, database URL/username/password, and an available HTTP listen port. The platform continues to own application startup and execution of Event- and Registration-owned Flyway migrations.

The runtime must expose a machine-checkable readiness signal distinct from process existence. Readiness remains false until required configuration is accepted, PostgreSQL is reachable, both owned migration sets have completed successfully, and the HTTP runtime can serve the accepted external contract. After startup, PostgreSQL unavailability must make readiness not-ready when the accepted HTTP use cases cannot be serviced.

Readiness is an operational adapter/runtime concern, not a business-domain API. The accepted architecture does not require a separate liveness contract for this proof and does not yet select a specific endpoint, Spring mechanism, or new readiness dependency.

Infrastructure provisioning remains outside the platform boundary for this proof. Host/VM, PostgreSQL, networking/firewall, and provider resources are externally supplied. Docker/OCI packaging and Terraform/OpenTofu/IaC are deliberately not admitted. A later requirement for reproducible infrastructure provisioning requires a separate architecture and technology decision.

This operational scope is accepted architecture for implementation planning but is not represented as a new Structurizr container or relationship because it changes packaging/run and readiness semantics of the existing `platform/apps/platform` container rather than adding a new architectural participant.

ADR-0010 records the rationale for this operational-runtime boundary.

## Persistence ownership

Event implements the persistence baseline through an Event-owned PostgreSQL schema and versioned Flyway migrations.

The Event application layer owns the persistence port. The private jOOQ adapter depends inward on that port and Event domain concepts; domain and application code do not depend on database technologies or persistence-adapter implementation.

One PostgreSQL server does not imply one shared data model. Cross-module joins and direct cross-schema persistence access are prohibited unless a later explicit architecture decision changes this rule.

Database permission enforcement remains deferred until operational scope requires it.

## External contracts

`platform/contracts/http/v1/event.yaml` is the authoritative external HTTP contract for the current Event define/retrieve and participant-private Event-registration create/retrieve/cancel surfaces.

OpenAPI Generator derives the server interface and transport models during the build. Generated sources are adapter-layer build output and must not become Event domain or application models.

The HTTP interface owns transport mapping, structural HTTP validation, contract-defined error responses, and correlation establishment. Event continues to own business validation and result semantics.

## Dynamic interfaces

Public and administrative frontends are clients of stable contracts, not owners of business logic or database structure.

Dynamic page composition may be introduced when a concrete use case requires it. Its contracts must remain separate from business-domain internals.

## Repository layout

The currently implemented architectural structure includes:

~~~text
.
├── platform/
│   ├── apps/
│   │   └── platform/
│   ├── core/
│   ├── modules/
│   │   ├── event/
│   │   │   ├── api/
│   │   │   ├── impl/
│   │   │   └── module.md
│   │   └── registration/
│   │       ├── api/
│   │       └── impl/
│   ├── compositions/
│   │   └── event-registration/
│   ├── interfaces/
│   │   └── http/
│   └── contracts/
│       └── http/
│           └── v1/
│               └── event.yaml
├── build-logic/
└── docs/
~~~

`platform/modules/registration` and `platform/compositions/event-registration` are current architecture. `integrations/` remains an architectural category only and must not be created until later accepted scope requires it.

### Current ADR-0013 conformance

Event and Registration already use separate public API/private implementation Gradle projects.

The current Event-Registration composition remains one Gradle project. It is an accepted composition, but it is not a conforming module if classified as one until a later accepted migration creates an explicit public/private boundary.

Participant authentication/security remains implemented in `platform/apps/platform` as current accepted executable state from ADR-0012/#91. ADR-0013 defines Security as a module and makes that runtime ownership explicit migration debt.

The current HTTP interface, application runtime, contracts, and `core` foundation remain their existing architectural constructs. This documentation slice does not relabel them as implemented modules or change their current Structurizr relationships.

## Architecture enforcement

Current build-time enforcement includes:

1. Separate Gradle projects for core, Event API/implementation, Registration API/implementation, Event-Registration composition, HTTP interface, and executable platform runtime.
2. `java-library` dependency semantics for library boundaries.
3. Event and Registration ArchUnit tests for capability-internal dependency direction and forbidden cross-capability/framework dependencies.
4. Event-Registration composition ArchUnit tests restricting the composition to core, Event API, Registration API, and Java platform types.
5. Platform ArchUnit tests for core, capability APIs/implementations, composition, HTTP interface, and application-runtime dependency boundaries.
6. Event- and Registration-owned Flyway migrations and PostgreSQL persistence integration tests through Testcontainers.
7. Running Spring Boot HTTP end-to-end tests against real PostgreSQL through Testcontainers, including Event-registration success/error behavior, durability, uniqueness, and correlation handling.
8. Root `./gradlew --no-daemon check` aggregation across all current projects.

Additional enforcement remains deferred until explicitly scoped:

- Spring Modulith module verification.
- PostgreSQL permission enforcement.

## Architecture model

`docs/architecture/workspace.dsl` is the authoritative diagram model. Rendered images are derived views and are not edited as independent sources of truth.
