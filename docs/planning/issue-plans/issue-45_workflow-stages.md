# Issue #45: Define and implement workflow stages for the project board

## Scope and baseline

- Issue URL: https://github.com/AbidShaik09/Kheera-Backend/issues/45
- Branch and synchronized develop commit: `issue/45_workflow-stages` from `380c574a0c5973cb3d06d8753e62a869c0cf70ca`.
- Problem and intended behavior: `project_workflows` exists but has neither ordering nor a work-item relationship. A project needs ordered board columns, and every active work item must belong to an active column in the same project.
- In scope: migrate `project_workflows` into the canonical workflow-stage table by adding `position` and `is_complete`; backfill an ordered Backlog stage for projects without stages; add a non-null work-item stage/position relationship; stage list/create/update/delete APIs; an authorized work-item move API; DTOs, repositories, service validation, PostgreSQL and controller tests; contract/architecture/progress documentation.
- Out of scope: project CRUD and general work-item CRUD/listing, stage type administration, sprints, comments, and frontend board wiring. #68 creates projects and must provision their initial stages; #69 owns normal work-item board reads/CRUD and consumes this schema.
- Dependencies and blockers: #66 and #67 are merged in the baseline. #68 and #69 are open, so their controllers are not duplicated. The move endpoint is implemented against persisted work items so it can be consumed when #69 lands.
- References read: issue #45, `docs/engineering/ENGINEERING_STANDARDS.md`, API contract, database design, existing Flyway migrations/entities, access service, and membership tests.
- Open decisions: resolved—`project_workflows` represents board stages. Positions are zero-based, contiguous within each project/stage; moving to position `n` clamps to the end; a stage may be deleted only when it has no active work items. Completion is semantic (`is_complete`), never inferred from a name.

## Acceptance criteria and tests

| Criterion | Named test / manual check | Expected result |
| --- | --- | --- |
| A project has ordered board stages | `WorkflowStageServiceTest.listStagesOrdersByPosition` and PostgreSQL migration test | Stages sort by position then UUID; legacy projects gain Backlog. |
| Board-visible work items have a stage in their own project | migration integration test and `WorkflowStageServiceTest.moveRejectsForeignProjectStage` | Migration backfills/assigns; moves across project boundary are rejected. |
| Stages can be listed and maintained by authorized members | `WorkflowStageControllerTest` | Read needs membership; writes require `space.update`; validation/error responses match contract. |
| A work item can move safely | `WorkflowStageServiceTest.moveReordersSourceAndTargetStages` | Source and target positions are gap-free; row is locked and stage/project is validated. |
| Existing data is compatible | Flyway PostgreSQL integration test | V26 applies to the old schema, preserves data, backfills stages, and establishes FK/indexes. |

## Design and affected files

- Existing behavior and proposed change: retain `project_workflows` and map it as an ordered stage. V26 adds `position` and `is_complete`, creates a Backlog stage for each project without one, assigns legacy work items, and adds `workflow_id` plus `position` to `work_items` with project-scoped validation in the service.
- Files/layers to change: migration; project/work-item mappings; project/work-item/workflow repositories; stage request/response DTOs; `WorkflowStageService`; `WorkflowStageController`; shared API advice; unit/controller/PostgreSQL tests; API/database/progress/TODO docs.
- API, schema, consumers, authorization, validation, and error impacts: stage routes are under `/api/projects/{projectId}/workflow-stages`; move is `/api/work-items/{workItemId}/move`. Active membership is required for reads; `space.update` is required for stage mutations and moves. Names are trimmed, 1–100 characters; position is non-negative; deleted/foreign resources are not exposed.
- Compatibility, concurrency, data risks, and migration/forward-fix approach: transaction locks the project for stage writes and the work item plus involved stage rows for moves, then compacts positions. Database FKs and indexes support the relationship; service validates that both IDs share a project. A forward fix can add a new migration without rewriting V26.
- Documentation paths to update: implementation TODO, API contract, database design, application progress, change history, and this plan.
- Alternatives considered and reason for chosen approach: a separate `project_workflow_stages` table would introduce an unneeded workflow container that has no current product use. Reusing the existing project-owned table preserves existing records and matches the board-column concept.

## Ordered execution checklist

- [x] Confirm issue requirements, dependencies, references, and latest develop.
- [ ] Update TODO and complete/commit this initial plan before code or tests.
- [ ] Write and run failing unit/PostgreSQL/controller tests; record expected failures.
- [ ] Add V26 migration and compatible mappings, with database tests.
- [ ] Implement repositories and service transaction/authorization rules; run service tests.
- [ ] Implement controllers, DTOs, and errors; run controller tests.
- [ ] Update API, schema, architecture, progress, history, and TODO documents.
- [ ] Start locally; verify health, affected API success/failure/auth, and OpenAPI.
- [ ] Run `docker info` and `./mvnw.cmd clean verify`; record counts and zero skips.
- [ ] Self-review, commit/push issue branch, create PR to `develop`, and request Codex review.
- [ ] Inspect CI/reviews, fix valid findings, and follow the repository issue-closure policy.

## Validation evidence

| Step | Command or manual procedure | Expected | Actual result / counts / evidence | Tested commit or working-tree state |
| --- | --- | --- | --- | --- |
| Baseline synchronization | `git fetch origin develop` then `git merge --ff-only origin/develop` | HEAD equals origin/develop and clean | Passed: both `380c574a0c5973cb3d06d8753e62a869c0cf70ca` | initial branch |
| Focused tests | `./mvnw.cmd test -Dtest=...` | Tests demonstrate behavior then pass after implementation | Pending | pending |
| Database migration tests | `./mvnw.cmd test -Dtest=...IntegrationTest` with Docker | PostgreSQL/Flyway passes, no skips | Pending | pending |
| Local smoke | `./mvnw.cmd spring-boot:run`, health and authenticated API requests | Health plus stage/move outcomes | Pending | pending |
| Full verification | `docker info`; `./mvnw.cmd clean verify` | Zero failures/errors/skips | Pending | pending |

## Plan changes and resume notes

| Date | New evidence / deviation | Reason and scope decision | Steps/checks to repeat |
| --- | --- | --- | --- |
| 2026-09-14 | #68 and #69 remain open after foundation #66/#67 merged. | Implement only stage administration and explicit move behavior; do not preempt project/work-item CRUD. | All implementation and validation steps. |

## Delivery

- PR URL: Pending.
- Initial Codex request URL: Pending.
- Follow-up review URLs and findings: Pending.
- CI results: Pending.
- Merge/deployment status and evidence: Pending.
- Remaining steps or blockers: Implementation and required verification pending.
