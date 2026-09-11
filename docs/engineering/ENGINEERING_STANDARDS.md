# Backend Engineering Standards

## Mandatory Delivery Flow

1. Create or confirm a GitHub issue before changing a feature, bug, security
   concern, migration, API behavior, or production configuration. The issue
   must state the problem, scope, acceptance criteria, security impact, and
   relevant links, screenshots, API examples, or design attachments.
2. Add the issue to `docs/planning/IMPLEMENTATION_TODO.md` in dependency order.
   Do not start implementation if the issue has no acceptance criteria.
3. Read the full issue and all attached references before coding. Verify the
   API contract, Flyway schema, affected consumers, existing test coverage, and
   external links rather than guessing requirements.
4. Start only from an up-to-date, clean local `develop` branch. Create a branch
   named `issue/<number>_<short-kebab-title>`, for example
   `issue/51_fix-password-reset-otp`. Never use `main` as a feature base.
5. For a bug, first write a failing regression test. For a feature, write or
   update the relevant unit tests before implementation. Mock collaborators
   outside the unit under test; use PostgreSQL Testcontainers for repository
   queries, mappings, migrations, or PostgreSQL-specific behavior.
6. Implement the smallest complete change that meets the acceptance criteria.
   Keep naming explicit, remove dead code, and update tests whenever the
   contract or expected behavior changes.
7. Update every relevant document under `docs/workspace/` and the backend
   `docs/` directory according to
   `../workspace/governance/DOCUMENTATION_STANDARDS.md`. Update architecture,
   API/schema/design records during implementation; update progress and history
   after a material merge or deployment.
8. Before requesting a push, self-review the diff for correctness, edge cases,
   authorization, validation, errors, concurrency, logging, secrets, and API
   compatibility. Run the relevant test suite and the full backend build.
9. When asked to push, push only the issue branch and create a pull request to
   `develop`, never `main`. Do not merge before code review and required checks
   pass. Close the issue only after merge and any required deployment check.

If the worktree is dirty or the repository has no `develop` branch, stop before
switching branches. Preserve existing work and resolve the branch baseline with
the repository owner first.

## Owner-Authorized Expedited Delivery

The normal flow above is mandatory unless the repository owner explicitly
authorizes one of these categories in the issue or task conversation. Every
expedited change still requires a linked GitHub issue, a clean branch from
`develop`, a PR to `develop`, a focused self-review, updated documentation, and
a PR description that states the exception, reason, verification performed,
and deferred follow-up work.

### Hotfix

A hotfix restores production behavior or remediates a time-sensitive security
or reliability defect. The change must be minimal and directly related to the
reported impact. Tests may be deferred only with explicit owner authorization
and a linked test issue; they remain required follow-up work.

### No-Review Push

A no-review push is an explicit owner authorization to bypass required GitHub
approval for a low-risk, tightly scoped change. It does not waive validation,
security review, or test requirements by default. Use the GitHub bypass only
after the PR documents why the exception is safe and the owner has authorized
it.

### Never Eligible Automatically

Neither category automatically permits credentials or secret handling,
irreversible/destructive data migrations, broad authorization changes, payment
or identity-provider changes, dependency upgrades with known advisories, or
unbounded refactors. These changes require normal review unless the owner gives
specific, recorded authorization after the risks are explained.

Track CI/CD enforcement and secret-handling improvements in
[#55](https://github.com/AbidShaik09/Kheera-Backend/issues/55).

## Design and Code Rules

- Apply SOLID principles without speculative abstraction. Controllers handle
  HTTP concerns, services own business rules and transaction boundaries, and
  repositories encapsulate persistence queries.
- Use request/response DTOs at API boundaries. Do not expose JPA entities or
  accept client-controlled ownership, role, permission, or audit fields.
- Prefer constructor injection, cohesive classes, explicit names, and small
  methods. Depend on interfaces only when there is a real substitution seam.
- Keep modules loosely coupled. Avoid circular dependencies, static mutable
  state, controller-to-repository shortcuts, and cross-domain writes without a
  service boundary.
- Flyway owns schema changes. Every persistence change needs a reviewed
  migration, compatible mapping, rollback/forward-fix plan, and repository
  coverage where behavior depends on PostgreSQL.

## Security and Reliability Rules

- Authenticate every protected request and authorize from JWT email to the
  server-side user, membership, role, and permission. Never trust browser IDs
  for ownership or access decisions.
- Validate and normalize all request input. Use allow-lists for enums, sort
  fields, upload types, and state transitions. Return safe client errors and
  keep internal stack traces out of API responses.
- Store secrets only in environment/secret managers. Never commit passwords,
  keys, JWT secrets, tokens, certificates, `.env` files, SMTP credentials, or
  real production configuration. Provide redacted examples only.
- Review dependencies and transitive vulnerabilities before release. Keep
  Spring, Maven plugins, container bases, and GitHub Actions pinned and updated.
- Consider rate limiting, replay protection, expiry, audit logging, and generic
  user-facing messages for authentication, password-reset, and email flows.
- Test boundary conditions: missing/invalid input, expired data, duplicate
  requests, authorization failures, soft-deleted records, timezone handling,
  races, and downstream failures.

## Definition of Done

- Issue, acceptance criteria, TODO state, API contract, architecture/progress
  records, and all relevant docs are current.
- Unit/repository/controller coverage reflects the intended behavior.
- Relevant tests and `./mvnw verify` pass.
- Code is self-reviewed for cleanup, security, and edge cases.
- PR targets `develop`, documents verification, and awaits review before merge.
