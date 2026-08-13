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
- Composition modules for cross-capability workflows.
- Adapter-based integrations for external systems.

The modular monolith is an implementation and deployment choice, not permission for modules to share internals.

## Hard boundaries

A business module owns its domain model, application use cases, persistence, migrations, and internal adapters.

Other business modules must not:

- Import its internal domain or implementation classes.
- Access its repositories.
- Read or write its database tables directly.
- Depend on its persistence records.
- Reuse internal DTOs as shared contracts.

Collaboration happens only through explicit public module APIs, published events, or composition modules.

The executable application composition root may depend on private implementation types only for explicit technical wiring authorized by scope. That exception does not permit business logic or cross-module implementation collaboration.

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

When two independent capabilities need to cooperate, prefer a composition that depends on their public APIs rather than making either capability depend on the other's implementation.

~~~text
module A API <- composition -> module B API
~~~

A composition owns the cross-capability workflow; neither participating bounded context owns the other's business rules.

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
- `GET /api/v1/event-registrations/{registrationId}` through the cross-capability composition.

The unified OpenAPI document may use separate `Event` and `EventRegistration` tags. Contract-file grouping represents the coherent external Event-facing surface; it does not merge internal ownership. Event and Registration remain independent, and the Event-Registration composition continues to own orchestration.

The transport contract keeps the current Event workflow language (`registrationId`, `eventId`, and `participantReference`) and does not expose generic Registration namespace/reference mechanics or a generic target dispatcher.

Authentication identity and Registration registrant identity remain separate concepts. Authentication/authorization implementation and a Person capability are not introduced by this phase.

The existing HTTP interface and Spring Boot application remain the external adapter and technical composition root respectively.

ADR-0008 supersedes ADR-0007 and records the domain-neutral Registration boundary, Event-specific composition, persistence isolation, and security/identity separation. ADR-0009 supersedes only ADR-0008's separate-contract-file decision by making `event.yaml` the unified Event-facing HTTP contract.

## Accepted minimum participant lifecycle scope

The minimum usable adult Event Registration lifecycle is accepted implementation scope and is being implemented incrementally. Current architecture statements below distinguish accepted implemented state from remaining planned behavior.

The scope preserves the current bounded contexts, composition, persistence owners, external HTTP adapter, executable application container, and dependency direction. No new bounded context, container, persistence owner, or Event/Registration dependency relationship is accepted solely by this lifecycle scope.

Within the existing Event bounded context, Event now implements durable Event-owned publication state with `unpublished` and `published` states, initial `unpublished` state, a one-way `unpublished -> published` transition, and transport-neutral public discovery of published Events. Known-id Event retrieval remains independent of publication. Publication does not own Registration eligibility, capacity, waitlists, payment, participant identity, or authorization.

Within the existing Registration bounded context, the generic lifecycle accepted by ADR-0011 is now implemented: initial `active` state, idempotent `active -> cancelled` behavior, lifecycle state in Registration retrieval, a transport-neutral generic cancellation operation, and Registration-owned durable persistence. Cancellation preserves Registration identity and the existing complete registrant-target uniqueness rule; cancelled pairs remain occupied. Registration remains domain-neutral and security-neutral. Event-facing participant cancellation remains planned through Event-Registration composition.

The Event-Registration composition now implements an additive transport-neutral participant-private path for create, retrieve, and cancel. That path accepts an opaque stable authenticated actor reference, derives `RegistrantReference("participant", actorReference)`, authorizes retrieval/cancellation against Registration-owned registrant state, exposes Registration lifecycle state, and invokes Registration-owned cancellation only after authorization. The existing legacy HTTP-facing composition create/find contracts remain temporarily unchanged for compatibility and are not the accepted participant-private path. Technical authentication identity and establishment of the platform-facing actor reference remain external to Event, Registration, and this composition implementation.

The actor reference is participant-linked private data for project handling. Registration persists only its own opaque participant reference; raw upstream/provider security-subject identifiers are not accepted as Registration durable state. Authenticated non-owner access keeps an internal authorization-denied semantic but uses the same external not-found resource-existence disclosure as an unknown private Event-registration. Normal structured logs exclude actor and participant registrant-reference values. Correlation and causation identifiers remain identity-free.

ADR-0012 and scope #87 select the minimum external participant authentication boundary. Spring Security is the accepted technical authentication framework inside the existing `platform/apps/platform` Spring Boot runtime, with stateless HTTP Basic as the minimum non-browser proof for participant-private Event-registration create, retrieve, and cancel operations. Published Event discovery remains public.

Participant proof credentials are supplied through external runtime configuration as an opaque stable platform principal identifier plus an encoded password verifier. The runtime may hold those entries in memory; no credential database, Person/Account persistence, identity database, provider integration, or durable identity-mapping store is introduced. The configured principal identifier is itself the platform-facing pseudonym and is adapted directly as:

`authenticatedPrincipalName -> AuthenticatedActorReference(authenticatedPrincipalName)`

`platform/apps/platform` owns Spring Security filter-chain configuration, stateless HTTP Basic setup, runtime credential verification, authenticated technical-principal establishment, and adaptation to the narrow transport-neutral actor-reference boundary. `platform/interfaces/http` receives that actor reference and remains responsible for HTTP adaptation and external error/privacy mapping; it does not parse Basic credentials, verify passwords, own participant authorization, derive Registration references, or expose Spring Security `Authentication` types into Event-Registration composition.

Event-Registration composition continues to own `AuthenticatedActorReference(x) -> RegistrantReference("participant", x)`, participant ownership authorization, and Event-specific orchestration. Registration remains security-neutral and has no dependency on Spring Security, HTTP authentication, credentials, or actor semantics.

The proof is stateless and non-browser: no form login, session/cookie authentication, remember-me, logout/session lifecycle, OAuth/OIDC login, or JWT bearer authentication is accepted. Any CSRF exclusion is bounded to that stateless non-browser API proof and does not establish a browser security design. HTTP Basic requires secure transport across untrusted networks, while TLS termination/deployment infrastructure remains outside Goal #57.

This selected mechanism introduces no new bounded context, Gradle module, application container, persistence owner, external authenticator, identity provider, identity-mapping store, Event/Registration dependency, or modeled relationship. Spring Security is implementation technology inside the existing Platform Application/runtime boundary rather than a new architectural participant. Therefore `docs/architecture/workspace.dsl` remains structurally unchanged.

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

`platform/apps/platform` starts the Spring Boot process and wires the HTTP interface, Event services, Registration services, persistence adapters, and Event-Registration composition. Event- and Registration-owned Flyway migrations run during application context construction before their repositories and application services become available to serve requests. Runtime wiring contains technical composition only; Event-registration workflow rules remain in the composition.

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

`platform/contracts/http/v1/event.yaml` is the authoritative external HTTP contract for the current Event define/retrieve and Event-registration create/retrieve surfaces.

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
