# Project Scope

## Authority

This document is the authoritative source for the current accepted project scope of Composable Domain Platform.

## Purpose

Composable Domain Platform provides a reliable foundation for composing independently bounded business capabilities. It is not tied to one business domain.

Event management is expected to become the first reference capability, but it is not the platform core and does not define the platform's general model.

## Current phase

**Repository and architecture foundation**

The current phase establishes the rules, architecture model, documentation structure, and technical boundaries that future implementation must follow.

## In scope

- Define platform vision and architectural principles.
- Define authoritative project governance and sources of truth.
- Define the approved baseline technology stack.
- Define the repository and Gradle project structure conceptually.
- Define allowed module types and their responsibilities.
- Define hard bounded-context and Hexagonal Architecture rules.
- Define Git branching and pull-request workflow.
- Define architecture diagrams as version-controlled authoritative artifacts.
- Establish the Structurizr workspace foundation.
- Define the ADR process.
- Define how scope changes are proposed and accepted.
- Define what the first reference implementation must prove.

## Explicitly out of scope

The following are intentionally excluded from the current phase:

- Java implementation code.
- Spring Boot application bootstrap.
- Gradle module implementation.
- Database schemas and migrations.
- OpenAPI implementation.
- Frontend implementation.
- Event domain implementation.
- Registration, ticketing, booking, membership, survey, payment, accounting, or other future business capabilities.
- External provider integrations.
- Kafka, RabbitMQ, Redis, Kubernetes, or other infrastructure without a demonstrated requirement.
- Multi-model development workflow automation.
- Deployment automation.

These items may become future scope only through an explicit scope decision.

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

Examples currently include event management, content management, registration, ticketing, booking, membership, surveys, payment integrations, and accounting integrations.

Their eventual bounded-context boundaries must be determined from real use cases rather than assumed in advance.
