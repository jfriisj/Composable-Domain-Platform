# Project Scope

## Authority

This document is the authoritative source for the current accepted project scope of Composable Domain Platform.

## Purpose

Composable Domain Platform provides a reliable foundation for composing independently bounded business capabilities. It is not tied to one business domain.

Event management is the first reference capability, but it is not the platform core and does not define the platform's general model.

## Current phase

**Event reference module foundation**

The repository, architecture, and executable Gradle build foundations have been accepted. The current phase introduces the first concrete bounded context to validate the platform's physical module boundary and Hexagonal Architecture direction with real business code.

## Concrete use case

A platform operator can define an Event with explicit identity, name, slug, scheduled start/end, and timezone, and obtain the resulting Event state through an application-level contract.

The initial Event model must enforce only invariants required by this use case, including that required textual identity fields are not blank and the scheduled end is after the scheduled start.

Durable persistence, HTTP exposure, runtime bootstrapping, publication workflows, and external integration are not required in this phase.

## Event ownership

The Event bounded context owns:

- Event identity.
- Event name and slug.
- Scheduled start and end.
- Event timezone.
- The invariants required to create a valid Event definition.

The Event bounded context does not own:

- Registration.
- Ticketing.
- Booking.
- Membership.
- Speakers or program management.
- Content management.
- Payments or accounting.
- Notifications.
- Identity-provider concerns.

## In scope

- Add `modules/event/api` and `modules/event/impl` as separate Gradle projects.
- Add an authoritative Event `module.md` describing ownership, non-ownership, public API, and allowed dependencies.
- Expose the smallest application-level public contract required by the concrete Event use case.
- Keep Event domain and application implementation inside the private implementation project.
- Keep domain code free of Spring, HTTP, persistence, generated contract types, and provider SDKs.
- Use the existing Java 21 `java-library` convention and Gradle API/implementation semantics to enforce the physical boundary.
- Add JUnit 5 tests required to prove the Event invariants and application use case.
- Add only dependency versions required by this phase to the Gradle Version Catalog.
- Update the authoritative architecture model and documentation to reflect the implemented Event reference module.
- Keep root `./gradlew check` green.

## Acceptance criteria

The phase is complete when:

1. `modules/event/api` and `modules/event/impl` build as separate Gradle projects.
2. The API project does not depend on the implementation project.
3. Event domain and application implementation remain private to `impl`.
4. The concrete Event creation use case is covered by tests.
5. Required Event invariants are covered by tests.
6. No Spring, persistence, HTTP/OpenAPI, external integration, or deployment concern is introduced.
7. Event ownership and non-ownership are explicit in `module.md`.
8. The authoritative architecture model reflects the current Event reference module.
9. `./gradlew check` succeeds from the repository root.

## Explicitly out of scope

The following remain intentionally excluded from the current phase:

- Spring Boot application bootstrap.
- Spring Modulith configuration.
- ArchUnit architecture rules.
- PostgreSQL schemas and Flyway migrations.
- jOOQ configuration.
- OpenAPI contracts or generation.
- HTTP controllers or other external interfaces.
- Durable persistence adapters.
- Event publication or messaging infrastructure.
- Registration, ticketing, booking, membership, speaker/program, content, payment, accounting, notification, or other business capabilities.
- Frontend implementation.
- Docker or deployment configuration.
- GitHub Actions or other CI/CD automation.
- External provider integrations.
- Kafka, RabbitMQ, Redis, Kubernetes, or other infrastructure without a demonstrated requirement.
- Multi-model development workflow automation.

These items may enter a later phase only through an explicit scope decision.

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

Examples currently include content management, registration, ticketing, booking, membership, surveys, payment integrations, and accounting integrations.

Their eventual bounded-context boundaries must be determined from real use cases rather than assumed in advance.
