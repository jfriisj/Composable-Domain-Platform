# Project Scope

## Purpose

This document is the authority for current accepted scope. It records accepted product and platform boundaries, not project history, implementation inventories, or planning status. Governance controls scope change; concrete contracts, architecture, migrations, build files, source, and tests own their narrower truths.

Composable Domain Platform provides a modular platform for composing independently bounded capabilities. Event is the current reference business capability, not platform core.

## Current accepted product boundary

The accepted product experience combines organizer-owned Event management, the participant Event-registration lifecycle, and bounded participant Event waitlist participation:

- An authenticated platform actor can create an Event and becomes its durable organizer/owner.
- An authenticated owner can modify their owned Event while it is `unpublished`; mutable definition values (`name`, `slug`, `startsAt`, `endsAt`, `timezone`) may be changed, while `eventId` remains immutable identity.
- A new Event is `unpublished`; Event owns the owner-authorized `unpublished -> published` transition through the organizer-management flow.
- An authenticated Event owner may deliberately transition their own `published` Event to terminal `withdrawn`. `withdrawn` is distinct from `unpublished`: withdrawal is not an edit-back-to-draft operation, and restore/re-publish is not supported.
- Event ownership, definition state, and lifecycle state are durable across application-process restart against the same PostgreSQL database. Ownership transfer is not supported.
- Public discovery returns `published` Events only; `unpublished` and `withdrawn` Events are excluded. Known-id retrieval remains lifecycle-independent and exposes the Event's current lifecycle; the anonymous public representation does not require public disclosure of the organizer reference.
- Authenticated non-owners cannot modify, publish, or withdraw another actor's Event.
- For organizer management, Event owns the organizer reference and Event business state, while Security owns Authentication and the final opaque actor-versus-resource-owner Authorization decision; the collaboration mechanism is not selected by scope.
- A `published` Event is initially available for new participant Registration unless its authenticated owner explicitly closes new Registration availability.
- An authenticated Event owner may close and later reopen new Registration availability for their own `published` Event without changing the Event lifecycle. Closing availability does not withdraw the Event: it remains `published`, stays in anonymous discovery, and remains retrievable by known identity.
- An authenticated non-owner cannot close or reopen another actor's Event Registration availability.
- Organizer-controlled Registration availability is durable across application-process restart against the same PostgreSQL database.
- An authenticated participant can create an Event Registration only when the referenced Event exists, is `published`, and its organizer-controlled Registration availability is open. `unpublished`, `withdrawn`, and availability-closed Events are ineligible, and rejected ineligible attempts create no Registration state.
- A terminal `withdrawn` Event remains ineligible for new Registration regardless of its previous organizer-controlled availability; reopening Registration cannot bypass withdrawal.
- Closing or reopening Registration availability does not automatically cancel, delete, reactivate, or otherwise mutate existing Registrations. Existing participant-private retrieval/cancellation and organizer Registration-view behavior remain available while new Registration availability is closed.
- While an Event is `published` and organizer-controlled new Registration availability is `closed`, an authenticated participant with no durable Registration for that Event may express durable waitlist participation intent. `unpublished`, `withdrawn`, and Registration-availability-`open` Events do not accept new waitlist participation, and rejected attempts create no waitlist or Registration state.
- Waitlist participation is distinct from Registration. One participant/Event pair has at most one durable waitlist participation; repeating the same intent is idempotent. A participant with an existing `active` or `cancelled` Registration for the same Event is ineligible for waitlist participation, preserving the accepted Registration uniqueness and cancelled-pair occupancy boundary.
- A participant can retrieve their own waitlist participation. Another authenticated actor cannot retrieve that participant-private state; unauthenticated access remains a distinct authentication failure. Exact external disclosure mapping is selected during contract readiness.
- Reopening Event Registration availability does not automatically promote, delete, complete, or otherwise mutate existing waitlist participation. Event withdrawal prevents new waitlist participation but likewise does not mutate existing waitlist state; existing participation remains privately retrievable.
- Waitlist participation owns its durable participation identity, opaque participant reference, Event target reference, participant/Event uniqueness, and retrieval state as a distinct semantic responsibility. Event and Registration do not own or encode waitlist participation state. Physical capability/module placement and cross-capability collaboration remain post-scope readiness decisions.
- Waitlist participation remains durable across application-process restart against the same PostgreSQL database.
- The participant can retrieve their private Event-registration state, cancel it, and later retrieve the same durable Registration as `cancelled`.
- An authenticated Event owner can retrieve Registrations targeting their owned Event and observe each Registration's current lifecycle, including `active` and `cancelled`; Registrations targeting other Events are excluded from that organizer-private view.
- Organizer Event-registration access is read-only. An authenticated non-owner cannot access another actor's organizer-private Event-registration view, an unknown Event exposes no Registration information, and participant-private retrieval/cancellation semantics remain unchanged.
- Registration owns a domain-neutral registrant-to-target relation, uniqueness, the `active -> cancelled` lifecycle, durable retrieval, and idempotent cancellation.
- Cancellation preserves Registration identity and the complete registrant-target uniqueness relation; a cancelled pair remains occupied.
- Event owns the organizer-controlled new-Registration availability setting as durable organizer-managed Event state, but does not own Registration state, registrant identity, Registration lifecycle, or the final Event-specific Registration eligibility workflow.
- Event-Management owns the organizer-authorized workflow for changing the Event-owned Registration-availability setting.
- Event-Registration owns Event-specific cross-capability orchestration, including deciding participant Registration eligibility from Event-owned lifecycle and Registration-availability state, mapping the authenticated participant to the registrant reference, and presenting Registration state targeting an owned Event to its organizer through public module APIs.
- Security owns Authentication and the final opaque actor-versus-resource-owner Authorization decision. Event-Registration supplies Event/Registration workflow facts and maps denial to its workflow result.
- Authenticated non-owner access to private Event-registration state uses the same external not-found disclosure as unknown private state; unauthenticated access remains a distinct authentication failure.
- Participant identity is an opaque stable platform actor reference. Registration persists only its own opaque participant reference. Event organizer identity is also an opaque stable platform actor reference, with Event persisting its own organizer reference as authorization state. Correlation/causation identifiers remain identity-free.
- Registration lifecycle state remains durable across application-process restart against the same PostgreSQL database.

The authoritative HTTP behavior is defined by the versioned OpenAPI source contracts under `platform/contracts/http/v1/`. Current contract allocation follows externally addressable behavior ownership: Event-owned behavior and the Event-Registration participant workflow have independent authoritative source units, while concrete applications statically aggregate only the units they select.

## Accepted capabilities

The accepted platform baseline includes:

- Event as an independently owned module with public API/private implementation and Event-owned PostgreSQL persistence for Event definition, organizer ownership, publication lifecycle, and organizer-controlled new-Registration availability state.
- Registration as an independently owned, domain-neutral module with public API/private implementation and Registration-owned PostgreSQL persistence.
- Security as an independently owned Authentication + Authorization module with framework-neutral public contracts and private Security mechanism implementation.
- Event-Registration as a non-module composition that collaborates only through Event, Registration, and Security public APIs.
- `core` as small business-neutral foundation for accepted execution-context primitives.
- inbound HTTP adapters outside business modules; generated OpenAPI transport types remain adapter/build output.
- static application composition through explicit Gradle project dependencies, where an application selects only the modules, compositions, adapters, contracts, and technical infrastructure required by its declared accepted use cases and omits unrelated capabilities.
- static application-level OpenAPI aggregation with deterministic fail-closed conflict validation; aggregate application contracts are derived build output.
- Spring Boot/JVM executable runtime artifacts. Application runtimes own technical selection, construction, configuration, migration startup, readiness, and wiring only.
- externally supplied PostgreSQL and runtime configuration. Runtime readiness is machine-checkable and becomes not-ready when PostgreSQL prevents accepted serviceability.
- a repository-controlled Linux Docker developer environment using JDK 21, the repository Gradle Wrapper, host-Docker/Testcontainers sibling access, and optional disposable Compose PostgreSQL for manual development. Testcontainers-owned PostgreSQL remains the automated-validation dependency.
- isolated local developer workspaces based on independent Git worktrees in the WSL filesystem, with repository-controlled workspace lifecycle and a loopback-only browser editor over the assigned worktree. Each writable worktree has at most one independent top-level write-capable agent owner at a time; external coding agents remain agent-owned consumers of their assigned worktree and are not repository build/validation dependencies.
- correlation context propagated from supported external boundaries without becoming business identity or business state.

Current authentication proof uses Spring Security with stateless HTTP Basic, externally supplied encoded credential verifiers, and stable opaque platform principal identifiers. Security implementation details remain private; the public Security boundary remains framework- and transport-neutral.

## Durable exclusions

Current scope does not authorize:

- reversible Event unpublish/restore/re-publish or Event lifecycle expansion beyond the accepted terminal `withdrawn` state;
- automatic Registration cancellation or other Registration lifecycle mutation caused solely by Event withdrawal;
- scheduled or time-window-based Registration opening/closing, same-pair Registration re-registration/reactivation, additional Registration lifecycle policy, capacity, quotas, ordered/ranked waitlists, automatic waitlist promotion or Registration creation, organizer waitlist management/view, participant waitlist cancellation/removal, ticketing/payment, notifications, or check-in/attendance;
- a generic Registration HTTP dispatcher;
- Person/Account or participant-profile capability, provider-specific identity as platform domain state, durable provider-to-platform identity mapping, credential persistence/enrollment/reset/recovery/admin APIs, or external identity-provider integration;
- roles/permissions, RBAC/ABAC, or a generic policy engine;
- OAuth/OIDC, JWT bearer authentication, browser session/cookie/login flows, or a broader authentication mechanism merely by extension of the current HTTP Basic proof;
- application OCI/container deployment packaging, Kubernetes, Terraform/OpenTofu, cloud/provider provisioning, production TLS termination, secrets-management products, production PostgreSQL operations, or artifact publication infrastructure;
- macOS/Windows/Docker Desktop/Podman/Colima/Rancher Desktop/remote or rootless Docker as part of the initial supported developer-environment contract, or Docker-in-Docker for the minimum proof;
- remote/LAN browser-editor exposure, editor-owned `.devcontainer` lifecycle, central agent/workspace orchestration, shared writable worktrees across independent top-level agents, or repository source stored primarily in Docker-managed volumes;
- runtime module/contract discovery, dynamic plugins, feature flags, Spring-profile capability selection, service extraction, or speculative shared abstractions.

These exclusions are boundaries, not a prohibition on later accepted scope changes. Use-case-specific non-goals belong with their Goal/issue unless they become durable project exclusions.

## Scope admission rule

A new capability, use case, durable responsibility, bounded capability/module responsibility, currently excluded product responsibility, deployment/infrastructure responsibility, or currently excluded technology requires an accepted scope change before implementation.

Accepted scope is semantic and governs product outcomes and durable ownership boundaries. Once a use case is accepted into scope, implementing that use case does not require a separate scope transition for its necessary module public API operations, OpenAPI operations, module-owned persistence changes/migrations, adapters, compositions, runtime wiring, tests, or directly affected documentation.

A scope proposal must define the concrete actor/operator/developer outcome, ownership and non-ownership, durable exclusions, objective acceptance, affected authorities, and required dependencies. Significant architecture decisions also require an ADR; technology admission requires a demonstrated problem and accepted requirement.

Accepted scope does not by itself make implementation ready. Executable work must also have resolved ownership, dependencies, contract/persistence/architecture impact where applicable, and validation evidence under `docs/governance.md` and `docs/workflow.md`.

Planning issues and Goals do not change scope. Scope becomes accepted only when the authoritative repository change is merged into `development`.
