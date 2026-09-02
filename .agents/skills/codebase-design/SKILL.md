---
name: codebase-design
description: Design or review deep modules and clean seams inside this repository's accepted scope and architecture. Use when public API shape, seam placement, dependency direction, module depth, or testability is genuinely unresolved.
---

# Codebase design

## Precedence

This skill is advisory. Repository state and concern-specific authorities win.

Use this repository's established meanings for **module**, **public API**, **architecture boundary**, **port**, **adapter**, and **composition**. Do not rename or reinterpret those concepts to match external vocabulary. Do not perform a broad architecture scan without concrete repository evidence that the current bounded change needs it.

## When to use

Use this skill during readiness or implementation design when a concrete accepted outcome raises a real question about:

- which module or composition owns behavior;
- the smallest public API needed by callers;
- where an adapter/port seam belongs;
- whether an abstraction hides meaningful complexity or merely forwards calls;
- how the design can be tested through stable public behavior.

Accepted scope and readiness come first. A design technique cannot authorize product scope, module admission, architecture change, technology, or implementation.

## Design discipline

Prefer a small, coherent public API that hides substantial owned behavior and keeps change local. Treat the public API as everything a caller must know: operations, invariants, failure modes, ordering constraints, and relevant lifecycle semantics—not merely Java method signatures.

Apply these checks:

1. **Ownership** — the behavior belongs to the selected module/composition under current authorities.
2. **Depth** — removing the abstraction would force meaningful rules or knowledge back into multiple callers. If removal mostly deletes delegation, the abstraction is probably shallow.
3. **Surface area** — expose only operations required by the accepted use case; avoid speculative hooks and generic extension points.
4. **Seam evidence** — introduce an interface/port because architecture, ownership, testing, or real substitution needs a seam; do not create interface-per-class by default.
5. **Dependency direction** — adapters depend inward; domain stays independent of Spring, HTTP, databases, generated transport types, and provider SDKs.
6. **Module isolation** — cross-module collaboration uses public module APIs only. A module never reaches into another module's implementation or persistence. Cross-module workflow remains composition-owned.
7. **Foundation discipline** — do not move business semantics into `core` to make dependencies convenient.
8. **Testability** — callers and behavior tests should exercise stable public behavior rather than private structure.

If two materially different interface shapes remain plausible after reading the applicable authorities, compare the alternatives explicitly on ownership, surface area, coupling, locality of change, test seam, and future deletion cost. Do not multiply alternatives when one design is already dictated by accepted authority.

## Required output

For a design/readiness decision, state only the evidence needed for the bounded change:

- owner and explicit non-owners;
- caller(s);
- smallest required public API or seam;
- behavior kept private;
- allowed dependencies and dependency direction;
- failure/invariant semantics callers must know;
- intended test seam;
- whether architecture documentation or an ADR is required.

If the evidence implies a significant architecture decision, stop implementation and use the repository's ADR/change-control path before proceeding.
