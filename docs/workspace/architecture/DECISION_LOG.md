# Kheera Architecture Decision Log

Record durable, cross-cutting technical or product decisions here. This is not
an issue list: it captures the chosen direction, why it was chosen, and where
the decision is implemented. Add a row when a decision affects more than one
module, repository, environment, or future implementation choice.

| Date | Decision | Status | Rationale | Reference |
| --- | --- | --- | --- | --- |
| 2026-09-11 | Use an issue-first workflow with issue branches based on `develop`; PRs always target `develop`. | Active | Keeps changes traceable, reviewable, and isolated from release history. | [Backend #52](https://github.com/AbidShaik09/Kheera-Backend/issues/52), [frontend #66](https://github.com/AbidShaik09/Kheera-Frontend/issues/66). |
| 2026-09-11 | Treat Flyway migrations as the database source of truth and use Hibernate validation rather than schema generation. | Active | Preserves reproducible schema history across development and production. | `../../architecture/DATABASE_DESIGN.md`. |
| 2026-09-11 | Require semantic design tokens with light and dark modes; future named themes are token overrides. | Active | Prevents page-specific styling drift and makes themes scalable. | [Frontend style guide](https://github.com/AbidShaik09/Kheera-Frontend/blob/develop/docs/design/STYLE_GUIDE.md). |
| 2026-09-11 | Maintain architecture, progress, change history, and ownership-specific documentation with every material change. | Active | Keeps implementation, operations, and roadmap state discoverable and current. | `../governance/DOCUMENTATION_STANDARDS.md`. |
| 2026-09-12 | Allow explicitly requested documentation-only rule/process updates to push directly to `develop`, and require post-PR blocker/rule review. | Active | Keeps workflow corrections lightweight while preserving branch-and-PR discipline for code, schema, configuration, dependencies, and generated artifacts. | `../../engineering/ENGINEERING_STANDARDS.md`, `../governance/DOCUMENTATION_STANDARDS.md`. |

When a decision is superseded, add a new row that links to the replacement and
mark the old decision `Superseded`; do not rewrite history.
