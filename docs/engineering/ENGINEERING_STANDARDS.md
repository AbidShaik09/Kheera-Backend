# Backend Engineering Standards

## Mandatory Develop Synchronization

Before starting any implementation or documentation edit, including TODO updates,
issue planning, and docs-only direct pushes, use a clean checkout and synchronize
`develop` with GitHub. Being on `develop` or having a cached `origin/develop`
reference is not proof that the branch is current.

1. Check `git status --short`. Preserve existing changes; use a separate clean
   checkout/worktree when needed. Never reset, discard, or overwrite user work.
2. Check out `develop`, then run `git fetch origin develop` and
   `git merge --ff-only origin/develop`. If local `develop` does not exist,
   fetch first and create it tracking `origin/develop`, then repeat the sync.
3. Verify `git rev-parse HEAD` equals `git rev-parse origin/develop` and the
   worktree is clean. If fetching fails, fast-forwarding fails, or local
   `develop` is ahead/diverged, resolve the baseline before editing. Never
   treat a failed fetch as permission to use stale code or documentation.
4. Read the current rules, API contracts, and relevant implementation from this
   synchronized baseline before making decisions or edits.
5. For a new GitHub issue implementation, create
   `issue/<number>_<short-kebab-title>` only from this verified latest
   `develop`. Refresh and verify again immediately before branch creation if
   other work has intervened. Never branch from `main`, an old issue branch,
   or an unrefreshed local/tracking branch.
6. For resumed issue work, fetch current `develop` and integrate it into the
   existing issue branch before new edits, preserving its commits and resolving
   conflicts. Do not recreate the issue branch or discard its work.

Docs-only changes follow the standing direct-to-`develop` push instruction;
they are not exempt from this synchronization rule. Inspect the diff and verify
documentation before committing. If the remote advances before the push, fetch,
integrate the new commits safely, and recheck the result; never force-push.


## Mandatory Delivery Flow

Follow these steps in order. "Push", "create PR", or "merge" does not bypass a
validation gate. The docs-only exception below is the only alternate path.

1. Complete **Mandatory Develop Synchronization** above. Read the full issue,
   attachments, API contract, Flyway schema, consumers, and existing tests.
   Confirm the problem, scope, acceptance criteria, security impact, dependencies,
   and whether a separate PR is required. Resolve blocking ambiguity before coding.
2. Create `issue/<number>_<short-kebab-title>` from the verified baseline.
   Add/update the issue in `docs/planning/IMPLEMENTATION_TODO.md` in dependency
   order. Create and commit the **Required Issue Plan** below.
3. Write tests first. For bugs, reproduce the defect with a failing regression
   test. For features, add tests for the specified behavior before implementing it.
   Run them and record the expected failure; a setup error is not a valid failing
   behavior test. Mock collaborators outside the unit under test. Use PostgreSQL
   Testcontainers for repository queries, mappings, migrations, and database behavior.
4. Implement in the plan's dependency order: new Flyway migration and compatible
   mapping when schema changes; repository methods; service/business rules and
   transaction boundaries; controller/DTO/error handling. Write each layer's tests
   before its implementation. Mark unused layers inapplicable with a reason.
   Keep the change within the acceptance criteria.
5. Run targeted tests after each phase. If a test fails, inspect the application
   contract, fix the defect, and rerun the failed test plus affected regression
   tests. Do not weaken assertions or change expectations just to get a pass.
6. Update affected API, database, architecture, testing, README, and TODO docs
   with the implementation, following
   [documentation standards](../workspace/governance/DOCUMENTATION_STANDARDS.md).
   Record validation evidence and deviations in the plan.
7. Start the backend locally with the development configuration using
   `./mvnw spring-boot:run` (`.\mvnw.cmd spring-boot:run` on Windows).
   Confirm it remains healthy, `/api/health` succeeds, and affected endpoints
   meet success, failure, validation, and authorization criteria. For API changes,
   inspect `/api/swagger-ui/index.html` or raw OpenAPI.
8. Run `docker info` and `./mvnw clean verify` (use `.\mvnw.cmd` on Windows).
   The entire unit suite and Docker/Testcontainers tests must execute with zero
   skipped tests. Never use skip flags, exclusions, `disabledWithoutDocker`, or
   substitute databases to bypass tests. Record commands and test counts.
9. If any regression or required check fails, fix branch-caused defects and
   repeat the failed checks, affected local smoke checks, and full clean
   verification. Repeat until all gates pass. After upstream integration,
   conflict resolution, or review fixes, rerun validation on the resulting branch.
   Never report an earlier run as evidence for changes it did not test.
10. Self-review the final diff against every acceptance criterion and plan step:
    correctness, edge cases, authorization, validation, concurrency, errors,
    logging, secrets, compatibility, cleanup, and documentation.
11. When delivery is requested, commit and push the issue branch and create a PR
    targeting `develop`. Include the issue, plan link, change summary, exact
    verification evidence, and remaining limitations. Immediately request Codex
    review as specified below. Under the backend's existing issue policy, close
    the issue after PR creation; track CI, review, merge, and deployment on the PR.
12. Inspect CI and reviews. Fix valid findings, rerun required validation, push
    the fixes, resolve addressed threads with evidence, and request a fresh
    `@codex review`. A review request alone is not a completed review.
13. Review blockers and misunderstandings encountered. Update relevant rules
    when a concrete change would prevent recurrence. If the user stops work for
    wrong direction, identify the cause and correct the issue/rules before resuming.
14. Merge only after required checks, review, and applicable merge authorization.
    After merge/deployment, update progress/history and perform the development
    deployment smoke check. Keep pending steps visibly pending in the plan.

### Blockers and docs-only delivery

If required local verification cannot run, stop delivery, document the blocker,
and fix it or ask the owner for guidance. Missing tooling requires permission
before installation. Retry sandbox-restricted network/Docker commands with the
appropriate permissions before changing the plan. Missing Docker is a blocker,
not a passing or skipped result.

For owner-requested documentation/rule-only changes, use synchronized clean
`develop`, inspect the diff and verify links/instructions, update
`docs/workspace/progress/CHANGE_HISTORY.md`, commit, and push directly to
`develop` under the standing instruction. No issue, implementation plan,
application tests, or PR is required for this path. It must contain no code,
schema, runtime configuration, dependency, or generated artifact changes.
Issue plans accompanying implementation stay on the issue branch and in its PR.

## Required Issue Plan

Before changing application code or tests, create
`docs/planning/issue-plans/issue-<number>_<short-kebab-title>.md` on the issue
branch. Start from the [issue-plan template](../planning/issue-plans/README.md).
One issue gets one plan; grouped PRs must link each issue's plan.

The initial plan must be executable by another bot without guessing: map each
acceptance criterion to named tests and implementation steps, identify affected
files and contracts, list dependencies and risks, and specify exact commands,
expected results, and manual checks. Include every applicable delivery gate
in these standards, including documentation, regression, PR creation, and Codex review.
Replace template placeholders before implementation. Mark inapplicable steps
with a reason; never silently omit a gate. Missing requirements or unresolved
contract/design decisions that affect implementation must be clarified before
coding.

Use ordered checkboxes. Before each phase, compare the next step with the issue
and plan. Record new evidence, scope decisions, and reasons for changes before
continuing; obtain clarification when a change alters the requested scope.
Keep completed steps and their evidence rather than rewriting history.
After interruptions, read the plan and repository state before resuming.

Commit the initial plan before application code or test changes. Commit plan
updates with the work they describe and push them in the implementation PR.
These documents are expected PR content, not ignored scratch files. Do not
mark a test, review, merge, or deployment complete until evidence exists.
Link the plan from the PR and record PR/review URLs in it.

## GitHub Issue Creation Rules

- Each issue must state whether it needs its own branch and PR, or whether it
  may be grouped with another issue. If separate PRs are required, say so in
  the issue before implementation begins.
- Each issue must define acceptance criteria, affected contracts/docs, required
  verification, and known blockers or dependencies.
- If a previous attempt was stopped because the implementation direction was
  wrong, the follow-up issue or rule update must name the misunderstanding and
  the instruction that would have prevented it.
- Issue descriptions should call out workflow exceptions explicitly, including
  docs-only direct-to-`develop` updates approved by the repository owner.

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

- Issue plan checkboxes and validation evidence reflect the actual delivery state.
- Issue, acceptance criteria, TODO state, API contract, architecture/progress
  records, and all relevant docs are current.
- Unit/repository/controller coverage reflects the intended behavior.
- Local health/API smoke checks and `./mvnw clean verify` pass with zero skipped tests.
- Code is self-reviewed for cleanup, security, and edge cases.
- PR targets `develop`, documents verification, and awaits review before merge.

## Codex Review After PR Creation

Immediately after creating any pull request, add a PR comment containing exactly
`@codex review` to request Codex review. Verify that GitHub accepted the comment
and record its URL in the delivery notes. This applies to every newly created
PR, including drafts; do not wait for review findings before requesting the
initial review. After addressing and resolving review comments, post a new
`@codex review` comment for the updated head. A posted request is not evidence
that review has completed; inspect and address the resulting review before
considering the PR ready.
