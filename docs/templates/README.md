# Authoritative Documentation Model

## Authority

This document is the authority for registered authoritative-document responsibilities, templates, and size budgets.

It does not replace the concern-specific authorities registered by `docs/governance.md`. A template controls how an authority is represented; it does not redefine that authority's product, architecture, contract, persistence, build, or implementation truth.

## Core rule

An authoritative fact is defined fully in one most-specific authoritative source. Other documents reference that source instead of maintaining a competing copy.

When content exceeds a document's registered responsibility, move the content to its actual authority or remove duplicated/history-only material. Do not solve document growth by silently increasing the budget.

## Registration

| Type | Path or pattern | Template | Responsibility | Must not own | Max lines |
| --- | --- | --- | --- | --- | ---: |
| Scope | `docs/scope.md` | `scope.template.md` | current accepted product boundary, accepted capabilities, durable exclusions, scope admission | project history, PR/issue ledger, implementation design, architecture inventory | 150 |
| Project status | `docs/project-status.md` | `project-status.template.md` | current state, active Goal, current work, current gaps, next action | changelog, completed-work ledger, historical phases, design rationale | 100 |
| Governance | `docs/governance.md` | `governance.template.md` | authority map, change control, Goal rules, architecture/module control, definition of done | shell procedure, implementation sequence, current project state, history | 180 |
| Workflow | `docs/workflow.md` | `workflow.template.md` | bootstrap, readiness, branch/PR flow, validation, post-merge, release procedure | duplicated governance definitions, architecture narrative, project history | 220 |
| Architecture | `docs/architecture.md` | `architecture.template.md` | architectural style, durable boundaries, dependency direction, construct semantics, architecture change rule | module-local snapshots, Goal history, contract detail, implementation inventory | 250 |
| Module model | `docs/modules.md` | `modules.template.md` | universal module invariant, public/private rules, construct classification, module admission | detailed current module ownership/API/persistence snapshots | 150 |
| Module responsibility | `platform/modules/*/module.md` | `module.template.md` | one implemented module's purpose, ownership/non-ownership, boundaries, dependencies, authority links | concrete Java signatures, schema/migration detail, build truth, history | 80 |
| ADR | `docs/adr/[0-9][0-9][0-9][0-9]-*.md` | `adr.template.md` | durable context, significant decision, rationale, alternatives, consequences | implementation journal, current project status, repeated source truth | 120 |
| Engineering standard | `docs/engineering/*.md` | `engineering-standard.template.md` | durable language-specific engineering rules and language-specific validation | project status, capability design, module inventory, duplicated general workflow | 600 |

The engineering-standard profile is deliberately larger because it is a reference standard, but it remains bounded and must be consumed section-specifically rather than loaded as routine bootstrap context.

## Required structure

The H1 and H2 headings present in each registered template are required and remain in the template order. A document may add H3 subsections inside a required H2 only when they remain inside that section's registered responsibility and the document remains within budget.

Do not add new H2 responsibility areas merely because a document has remaining line budget. A new durable responsibility requires an explicit template-model change.

The templates are responsibility contracts, not prose-generation targets. Empty boilerplate and repeated explanatory text should be deleted rather than retained to mimic a template.

## Size measurement

Budgets are measured as physical text lines after repository line-ending normalization. Registered Markdown files must end with a final newline; blank lines, comments, tables, and fenced blocks count toward the budget.

The deterministic measure is equivalent to:

```bash
last_byte="$(tail -c 1 "$file")"
test -z "$last_byte"
lines="$(wc -l < "$file")"
test "$lines" -le "$max_lines"
```

Budgets are hard limits. A permanent or temporary exception must be recorded explicitly in this document with the exact path, limit, reason, and removal condition. Raising the common budget is not the default exception mechanism.

### Registered exceptions

No exceptions are currently registered. When one is required, record it as a row in this table so repository validation can enforce the effective limit deterministically.

| Path | Max lines | Reason | Removal condition |
| --- | ---: | --- | --- |

## Deterministic enforcement contract

Repository validation includes a repository-owned checker through `build-logic:check`; root `./gradlew --no-daemon check` therefore exercises the same validation.

The checker fails closed. It must:

1. enumerate the registered exact paths and registered path patterns;
2. require every matched document to have its registered template;
3. verify the required H1/H2 headings exactly once and in template order;
4. reject a document whose physical line count exceeds its registered budget or explicit registered exception;
5. reject missing final newlines;
6. reject project-status H2 headings `Completed`, `History`, `Changelog`, or `Previous phases`;
7. report the exact file and violated rule;
8. return non-zero on any violation;
9. run from the normal repository validation path without introducing a new external service or semantic/AI dependency.

The checker must not attempt to infer semantic duplication or ownership from prose. Those rules remain reviewable governance constraints backed by the authority map and templates.

## Template change rule

Changing a template responsibility, required H2, path registration, budget, or exception is a governance change.

The change must explain why the existing most-specific authority cannot represent the required durable truth within its current responsibility. Prefer moving facts to the correct authority, deleting history, or referencing a narrower source before expanding a template.
