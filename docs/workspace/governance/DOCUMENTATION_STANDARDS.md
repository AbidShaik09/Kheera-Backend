# Kheera Documentation Standards

## Non-Negotiable Rule

Every feature, bug fix, security change, API change, schema migration,
infrastructure change, dependency upgrade, or user-visible behavior update must
update the documentation it affects. Documentation work is part of the issue's
acceptance criteria and Definition of Done, not follow-up work.

Documentation-only rule/process updates are the one approved exception to the
usual no-direct-push workflow. When the repository owner explicitly requests a
docs-only rule update, it may be committed and pushed directly to `develop`.
This exception must not include application code, schema, runtime configuration,
dependency, generated artifact, or production-secret changes.

## Required Documents by Change Type

| Change | Required updates |
| --- | --- |
| Any issue | Relevant repository TODO status and issue links. |
| New or changed endpoint, request, response, auth, error, or pagination behavior | `api/API_CONTRACT.md`; backend API reference when applicable. |
| New or changed domain, service boundary, data flow, queue, integration, or deployment topology | `architecture/APPLICATION_ARCHITECTURE.md`. |
| Schema, entity, repository, migration, constraint, or data-retention change | Backend database design findings and API contract if externally visible. |
| New frontend component pattern, token, breakpoint, theme behavior, or accessibility rule | Frontend style guide and frontend engineering standards when the rule is global. |
| Test strategy, test infrastructure, CI quality gate, or coverage policy change | Backend testing strategy or frontend test guidance. |
| Server, DNS, TLS, Docker, CI/CD runner, database topology, or runtime config change | `operations/CONTABO_VM.md` with secrets redacted. |
| Material completed change | `progress/CHANGE_HISTORY.md` and the affected row in `progress/APPLICATION_PROGRESS.md`. |

## Update Timing

1. During issue refinement: identify every document that must change and add it
   to the issue acceptance criteria.
2. During implementation: update the API, architecture, schema, or design
   document together with the behavior it describes.
3. Before opening a PR: verify documentation matches the implementation and
   current tests. Include the document paths in the PR description.
4. After creating a PR: record any blockers, friction, or repeated
   misunderstandings discovered during the issue. If a rule change would reduce
   the chance of recurrence, update the relevant standards before closing the
   task.
5. After merge or deployment: update the application-progress status and add
   one dated history row for a material change.

## Progress and History Rules

- `APPLICATION_PROGRESS.md` is the current whole-system view. Update it when
  work starts, is blocked, changes scope, or is completed.
- `CHANGE_HISTORY.md` is an append-only release/history log. Add an entry only
  for completed, material changes: merged features/fixes, migrations, security
  changes, API changes, deployments, or major operational events.
- A history row must contain date, change summary, affected areas, issue/PR,
  and a concise impact statement. Never include credentials, personal data, or
  opaque production log output.

## Documentation Quality Rules

- Prefer links to source documents, issues, PRs, tests, or migrations instead
  of duplicating volatile technical detail.
- Use dates in `YYYY-MM-DD` format and state whether a status is observed,
  planned, implemented locally, merged, or deployed.
- Keep contracts factual. Mark decisions that are not yet implemented as
  proposed or required; do not present them as current behavior.
- Store documentation with the ownership boundary it describes. Workspace-wide
  material belongs here; backend and frontend implementation material belongs in
  each repository's `docs/` directory.
- Never document secrets. Use variable names and redacted examples only.
