# Project-local engineering skills

These skills are advisory engineering techniques for work in this repository. They do not create project authority and never override accepted repository state.

Repository precedence remains unchanged:

- `development` is accepted next-state truth;
- `docs/governance.md` owns governance and the authority map;
- `docs/workflow.md` owns operational sequencing and validation;
- concern-specific architecture, module, engineering, contract, migration, build, source, and test authorities win for their concerns;
- GitHub Issues own Goals, executable work, dependencies, and priority.

Installed project-local skills:

- `codebase-design` — evaluate module/public-API depth, seam placement, dependency direction, and testability inside a bounded change;
- `tdd` — drive ready implementation in small vertical behavior slices with red/green feedback through repository-defined test seams;
- `code-review` — review a bounded diff independently against repository standards and the originating accepted intent;
- `wayfinder` — map genuinely large, ambiguous work through repository-native Goals, readiness questions, decision/research issues, dependencies, and a visible frontier without pre-designing the fog.

These skills must not introduce a parallel issue tracker, triage label vocabulary, `CONTEXT.md`, domain-document hierarchy, `wayfinder:*` labels, architecture authority, or implementation plan. Do not run `setup-matt-pocock-skills` for this repository. Do not replace these adapted files with an automatic upstream update without a normal governed review.

The techniques are adapted for this repository from Matt Pocock's MIT-licensed engineering skills (`mattpocock/skills`). Upstream concepts were reviewed at commit `6654f6b60cd9d5be8b54c6fafe44346dabeb3b76`; these files intentionally use this repository's terminology and constraints rather than copying upstream workflow assumptions.
