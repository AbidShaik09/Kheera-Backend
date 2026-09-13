# Issue #55: Harden CI/CD safety and require relevant test checks on pull requests

## Scope and baseline
- Issue URL: https://github.com/AbidShaik09/Kheera-Backend/issues/55
- Branch and synchronized develop commit: `issue/55_harden-ci-cd-safety` from `origin/develop` at `4c3da1b`.
- Problem and intended behavior: backend pull requests and deployments need repeatable test/build gates, least-privilege workflow permissions, pinned actions, and documentation that keeps production credentials out of PR checks.
- In scope / out of scope: update backend GitHub Actions workflows and backend delivery/testing documentation. Frontend workflow implementation and GitHub branch-protection settings are documented as remaining cross-repository or repository-admin work because they cannot be enforced from this backend branch alone.
- Dependencies and blockers: no application-code dependency. Repository branch protection may need owner/admin configuration after this PR merges.
- References read: issue #55, engineering standards, testing strategy, implementation TODO, existing `.github/workflows/verify.yml`, `.github/workflows/deploy.yml`, and `.github/workflows/deploy-develop.yml`.
- Open decisions: none blocking for backend workflow hardening.

## Acceptance criteria and tests
| Criterion | Named test / manual check | Expected result |
| --- | --- | --- |
| PR checks run without production credentials | Inspect `verify.yml`; run `.\mvnw.cmd clean verify` locally | PR workflow has read-only permissions, no secrets, and runs Maven verify. |
| Required checks gate ordinary PRs to `develop` | Inspect docs updates in engineering/testing/progress files | Required `Verify Backend` check and branch-protection expectation are documented for repository admins. |
| Backend test/build results are visible on each PR | Inspect `verify.yml` triggers and workflow name | Pull requests to `develop` run `Verify Backend`. |
| Deployment secrets never appear in logs or repository files | Inspect deploy workflows and docs | Workflows do not echo secrets and rely on protected runner/environment context. |
| Documented expedited categories are auditable and require explicit authorization | Inspect engineering standards and TODO/progress updates | Expedited paths remain documented and ordinary workflow requires PR checks. |

## Design and affected files
- Existing behavior and proposed change: keep the backend verify workflow, add concurrency and least-privilege permissions to all workflows, pin checkout/setup actions, require Maven package during deployments, and introduce GitHub environments for deployment approval/audit boundaries.
- Files/layers to change and responsibility of each: `.github/workflows/verify.yml` for PR verification; `.github/workflows/deploy.yml` and `.github/workflows/deploy-develop.yml` for protected deployments; docs under `docs/` for policy and delivery records.
- API, schema, consumers, authorization, validation, and error impacts: N/A, no application API or database schema changes.
- Compatibility, concurrency, data risks, and migration/forward-fix approach: deployment workflows still operate on existing self-hosted checkout paths. Concurrency prevents overlapping deploys to the same environment. If environment names do not exist yet, GitHub can create them and repository owners can attach required reviewers/secrets.
- Documentation paths to update: `docs/planning/IMPLEMENTATION_TODO.md`, `docs/testing/TESTING_STRATEGY.md`, `docs/engineering/ENGINEERING_STANDARDS.md`, `docs/workspace/progress/APPLICATION_PROGRESS.md`, and `docs/workspace/progress/CHANGE_HISTORY.md`.
- Alternatives considered and reason for chosen approach: replacing self-hosted deployments with artifact promotion would be stronger but exceeds this issue and host setup. This PR hardens the existing workflow path without requiring infrastructure replacement.

## Ordered execution checklist
- [x] Confirm issue requirements, dependencies, references, and latest develop.
- [x] Create issue branch; update TODO; complete and commit this initial plan.
- [ ] Write and run failing regression/unit/PostgreSQL tests; N/A: workflow hardening is validated by workflow inspection and full Maven verification rather than new Java tests.
- [ ] Add required Flyway migration and compatible mappings, with database tests; N/A: no schema change.
- [ ] Implement repository methods; run repository tests; N/A: no repository code change.
- [ ] Implement services and transaction/authorization rules; run service tests; N/A: no service code change.
- [ ] Implement controllers, DTOs, and errors; run controller tests; N/A: no controller code change.
- [ ] Run targeted regression after each phase.
- [ ] Update API, schema, architecture, testing, README, and TODO documents.
- [ ] Start locally; verify health, affected API success/failure/auth, and OpenAPI; N/A unless workflow edits touch runtime behavior.
- [ ] Run docker info and full `.\mvnw.cmd clean verify`; record counts and zero skips.
- [ ] Fix failures and rerun failed/affected checks plus full clean verification.
- [ ] Self-review diff against all criteria, scope, security, and documentation.
- [ ] Commit/push issue branch with plan; create PR to develop with evidence.
- [ ] Post exactly `@codex review`; verify accepted comment and record URL.
- [ ] Follow repository issue-closure policy; keep remaining work on the PR.
- [ ] Inspect CI/reviews; fix valid findings, rerun required validation, and push.
- [ ] Resolve addressed threads with evidence; post fresh `@codex review`.
- [ ] Review blockers and update rules where needed.
- [ ] Verify checks/review and applicable authorization before merge.
- [ ] After merge/deployment, update progress/history and run applicable smoke checks.

## Validation evidence
| Step | Command or manual procedure | Expected | Actual result / counts / evidence | Tested commit or working-tree state |
| --- | --- | --- | --- | --- |
| Baseline sync | `git fetch origin develop`; branch from `origin/develop` | Branch starts from latest fetched develop | Passed; branch created at `4c3da1b` | `issue/55_harden-ci-cd-safety` |
| Workflow syntax inspection | Inspect edited `.github/workflows/*.yml` | Valid YAML and issue criteria represented | Pending | Pending |
| Full backend verification | `.\mvnw.cmd clean verify` | Build passes with zero failures/errors/skips | Pending | Pending |
| Docker availability | `docker info` | Docker reachable for Testcontainers | Pending | Pending |

## Plan changes and resume notes
| Date | New evidence / deviation | Reason and scope decision | Steps/checks to repeat |
| --- | --- | --- | --- |
| 2026-09-13 | Selected #55 instead of #67/#68 because #67/#68 depend on unmerged foundation work. | #55 can be delivered directly from `develop` without duplicating dependent application features. | Inspect workflows, update docs, run full verification. |

## Delivery
- PR URL: Pending.
- Initial Codex request URL: Pending.
- Follow-up review URLs and findings: Pending.
- CI results: Pending.
- Merge/deployment status and evidence: Pending.
- Remaining steps or blockers: frontend workflow and repository branch-protection settings may need separate repository/admin follow-up.
