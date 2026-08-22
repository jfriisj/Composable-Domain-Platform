# Project Scope

## Purpose

This document is the authority for current accepted scope. It records accepted product and platform boundaries, not project history, implementation inventories, or planning status. Governance controls scope change; concrete contracts, architecture, migrations, build files, source, and tests own their narrower truths.

Composable Domain Platform provides a modular platform for composing independently bounded capabilities. Event is the current reference business capability, not platform core.

## Current accepted product boundary

The accepted product experience is the minimum adult Event-registration lifecycle:

- Event can be defined and retrieved by identity.
- A new Event is `unpublished`; Event owns the one-way `unpublished -> published` transition.
- Public discovery returns published Events only; known-id retrieval remains publication-independent.
- An authenticated participant can create an Event Registration, retrieve their private Event-registration state, cancel it, and later retrieve the same durable Registration as `cancelled`.
- Registration owns a domain-neutral registrant-to-target relation, uniqueness, the `active -> cancelled` lifecycle, durable retrieval, and idempotent cancellation.
- Cancellation preserves Registration identity and the complete registrant-target uniqueness relation; a cancelled pair remains occupied.
- Event-Registration owns Event-specific cross-capability orchestration and maps the authenticated actor to the participant registrant reference.
- Security owns Authentication and the final opaque actor-versus-resource-owner Authorization decision. Event-Registration supplies Event/Registration workflow facts and maps denial to its workflow result.
- Authenticated non-owner access to private Event-registration state uses the same external not-found disclosure as unknown private state; unauthenticated access remains a distinct authentication failure.
- Participant identity is an opaque stable platform actor reference. Registration persists only its own opaque participant reference. Correlation/causation identifiers remain identity-free.
- Event publication state and Registration lifecycle state are durable across application-process restart against the same PostgreSQL database.

The authoritative HTTP behavior is defined by the versioned OpenAPI source contracts under `platform/contracts/http/v1/`. Current contract allocation follows externally addressable behavior ownership: Event-owned behavior and the Event-Registration participant workflow have independent authoritative source units, while concrete applications statically aggregate only the units they select.

## Accepted capabilities

The accepted platform baseline includes:

- Event as an independently owned module with public API/private implementation and Event-owned PostgreSQL persistence.
- Registration as an independently owned, domain-neutral module with public API/private implementation and Registration-owned PostgreSQL persistence.
- Security as an independently owned Authentication + Authorization module with framework-neutral public contracts and private Security mechanism implementation.
- Event-Registration as a non-module composition that collaborates only through Event, Registration, and Security public APIs.
- `core` as small business-neutral foundation for accepted execution-context primitives.
- inbound HTTP adapters outside business modules; generated OpenAPI transport types remain adapter/build output.
- static application composition through explicit Gradle project dependencies, including an Event-only application that omits unrelated Registration/Security/Event-Registration dependencies and the full Platform Application.
- static application-level OpenAPI aggregation with deterministic fail-closed conflict validation; aggregate application contracts are derived build output.
- Spring Boot/JVM executable runtime artifacts. Application runtimes own technical selection, construction, configuration, migration startup, readiness, and wiring only.
- externally supplied PostgreSQL and runtime configuration. Runtime readiness is machine-checkable and becomes not-ready when PostgreSQL prevents accepted serviceability.
- a repository-controlled Linux Docker developer environment using JDK 21, the repository Gradle Wrapper, host-Docker/Testcontainers sibling access, and optional disposable Compose PostgreSQL for manual development. Testcontainers-owned PostgreSQL remains the automated-validation dependency.
- correlation context propagated from supported external boundaries without becoming business identity or business state.

Current authentication proof uses Spring Security with stateless HTTP Basic, externally supplied encoded credential verifiers, and stable opaque platform principal identifiers. Security implementation details remain private; the public Security boundary remains framework- and transport-neutral.

## Durable exclusions

Current scope does not authorize:

- Event unpublish/withdraw or unrelated Event lifecycle expansion;
- same-pair Registration re-registration/reactivation, additional Registration lifecycle policy, capacity, quotas, waitlists, ticketing/payment, notifications, or check-in/attendance;
- a generic Registration HTTP dispatcher;
- Person/Account or participant-profile capability, provider-specific identity as platform domain state, durable provider-to-platform identity mapping, credential persistence/enrollment/reset/recovery/admin APIs, or external identity-provider integration;
- roles/permissions, RBAC/ABAC, or a generic policy engine;
- OAuth/OIDC, JWT bearer authentication, browser session/cookie/login flows, or a broader authentication mechanism merely by extension of the current HTTP Basic proof;
- application OCI/container deployment packaging, Kubernetes, Terraform/OpenTofu, cloud/provider provisioning, production TLS termination, secrets-management products, production PostgreSQL operations, or artifact publication infrastructure;
- macOS/Windows/Docker Desktop/Podman/Colima/Rancher Desktop/remote or rootless Docker as part of the initial supported developer-environment contract, or Docker-in-Docker for the minimum proof;
- runtime module/contract discovery, dynamic plugins, feature flags, Spring-profile capability selection, service extraction, or speculative shared abstractions.

These exclusions are boundaries, not a prohibition on later accepted scope changes. Use-case-specific non-goals belong with their Goal/issue unless they become durable project exclusions.

## Scope admission rule

A new capability, use case, persistent concern, external contract surface, deployment/infrastructure responsibility, or currently excluded technology requires an accepted scope change before implementation.

A scope proposal must define the concrete actor/operator/developer outcome, ownership and non-ownership, durable exclusions, objective acceptance, affected authorities, and required dependencies. Significant architecture decisions also require an ADR; technology admission requires a demonstrated problem and accepted requirement.

Accepted scope does not by itself make implementation ready. Executable work must also have resolved ownership, dependencies, contract/persistence/architecture impact where applicable, and validation evidence under `docs/governance.md` and `docs/workflow.md`.

Planning issues and Goals do not change scope. Scope becomes accepted only when the authoritative repository change is merged into `development`.
