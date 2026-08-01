# ADR-0001: Modular monolith with bounded contexts and Hexagonal Architecture

- Status: Accepted
- Date: 2026-08-01

## Context

The platform must support independently evolving business capabilities without forcing early distributed-system complexity.

The architecture must protect business-domain boundaries while allowing capabilities to be composed into larger solutions.

## Decision

Use a modular monolith as the initial deployment model.

Business capabilities are modeled as explicit DDD bounded contexts. Hexagonal Architecture is used inside business modules so domain and application logic remain independent of frameworks, persistence, transport, and external providers.

Module boundaries are treated as hard architectural boundaries even though modules initially execute in the same process.

## Alternatives considered

- Traditional layered monolith with shared domain/services.
- Microservices from the first implementation.
- Package conventions without build-level module boundaries.

## Consequences

- Initial deployment and operations remain comparatively simple.
- Domain boundaries can be enforced before distribution becomes necessary.
- Future extraction of a bounded context remains possible but is not a current goal.
- The project must invest in automated architecture enforcement so the modular monolith does not decay into a shared-code monolith.
