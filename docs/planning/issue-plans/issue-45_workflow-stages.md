# Issue #45: Define and implement workflow stages for the project board

## Scope and baseline

- Issue URL: https://github.com/AbidShaik09/Kheera-Backend/issues/45
- Branch and synchronized develop commit: `issue/45_workflow-stages` from `380c574a0c5973cb3d06d8753e62a869c0cf70ca`.
- Problem and intended behavior: `project_workflows` exists but has neither ordering nor a work-item relationship. A project needs ordered board columns, and every active work item must belong to an active column in the same project.
- In scope: migrate `project_workflows` into the canonical workflow-stage table by adding `position` and `is_complete`; backfill an ordered Backlog stage for projects without stages; add a non-null work-item stage/position relationship; stage list/create/update/delete APIs; an authorized work-item move API; DTOs, repositories, service validation, PostgreSQL and controller tests; contract/architecture/progress documentation.
- Out of scope: project CRUD, general work-item writes/details and non-stage filters, type administration, sprints, comments, and frontend board wiring. #45 supplies the stage-filtered/grouped paginated board read explicitly required by its acceptance criteria; #69 extends that route.
- Dependencies and blockers: #66 and #67 are merged in the baseline. #68 and #69 are open, so their controllers are not duplicated. The move endpoint is implemented against persisted work items so it can be consumed when #69 lands.
- References read: issue #45, `docs/engineering/ENGINEERING_STANDARDS.md`, API contract, database design, existing Flyway migrations/entities, access service, and membership tests.
- Open decisions: resolved—`project_workflows` represents board stages. Positions are zero-based, contiguous within each project/stage; moving to position `n` clamps to the end; a stage may be deleted only when it has no active work items. Completion is semantic (`is_complete`), never inferred from a name.

## Acceptance criteria and tests

| Criterion | Named test / manual check | Expected result |
| --- | --- | --- |
| A project has ordered board stages | WorkflowBoardIntegrationTest.freshProjectsHaveSixOrderedStagesAndSemanticCompletion | Six ordered columns; only default Done is complete. |
| Board-visible items have a stage in their project | WorkflowBoardIntegrationTest.schemaRejectsForeignProjectStage | Composite FK and active-stage checks reject invalid references. |
| Filter/group board reads | WorkflowBoardIntegrationTest.groupedPaginationIncludesEmptyColumnsAndOnlyCurrentPage | Correct page totals, stage groups and empty columns. |
| Stage administration and access | WorkflowBoardIntegrationTest.stageAdministrationReordersWithoutUniqueCollisionsAndProtectsNonemptyAndLastStage; excludesDeletedMembershipUserRoleSpaceAndWorkItem | Correct CRUD, permission and deletion boundaries. |
| Safe moves | WorkflowBoardIntegrationTest.moveReordersItemsAndFilteredBoardReturnsTheirPositions; concurrentMovesPreserveContiguousUniquePositions; concurrentStageDeletionAndMoveCannotLeaveAnActiveItemInADeletedStage | Atomic contiguous order; both serial race outcomes are safe. |
| Existing data migration | WorkflowMigrationIntegrationTest.upgradesLegacyProjectsStagesAndItemsWithoutLosingRecords | V25 fixtures retain IDs/items through V26, including null names and deleted stages. |

## Design and affected files

- Existing behavior and proposed change: retain `project_workflows` and map it as an ordered stage. V26 adds `position` and `is_complete`, creates a Backlog stage for each project without one, assigns legacy work items, and adds `workflow_id` plus `position` to `work_items` with project-scoped validation in the service.
- Files/layers to change: migration; project/work-item mappings; project/work-item/workflow repositories; stage request/response DTOs; `WorkflowStageService`; `WorkflowStageController`; shared API advice; unit/controller/PostgreSQL tests; API/database/progress/TODO docs.
- API, schema, consumers, authorization, validation, and error impacts: stage routes are under `/api/projects/{projectId}/workflow-stages`; move is `/api/work-items/{workItemId}/move`. Active membership is required for reads; `space.update` is required for stage mutations and moves. Names are trimmed, 1–100 characters; position is non-negative; deleted/foreign resources are not exposed.
- Compatibility, concurrency, data risks, and migration/forward-fix approach: transaction locks the project for stage writes and the work item plus involved stage rows for moves, then compacts positions. Database FKs and indexes support the relationship; service validates that both IDs share a project. A forward fix can add a new migration without rewriting V26.
- Documentation paths to update: implementation TODO, API contract, database design, application progress, change history, and this plan.
- Alternatives considered and reason for chosen approach: a separate `project_workflow_stages` table would introduce an unneeded workflow container that has no current product use. Reusing the existing project-owned table preserves existing records and matches the board-column concept.

## Ordered execution checklist

- [x] Confirm issue requirements, dependencies, references, and latest develop.
- [x] Update TODO and complete/commit this initial plan before code or tests (912d596).
- [x] Write and run failing PostgreSQL/MockMvc behavior tests (four failures against draft); tests-first deviation recorded below.
- [x] Add V26 migration and compatible mappings, with database tests.
- [x] Implement repositories and service transaction/authorization rules; run service tests.
- [x] Implement controllers, DTOs, and errors; run real JWT/MockMvc tests.
- [x] Update API, schema, architecture, progress, README, testing and TODO docs. Release history remains pending merge under documentation policy.
- [x] Start locally; verify health, affected API success/failure/auth, and OpenAPI.
- [x] Run `docker info` and `./mvnw.cmd clean verify`; 107 tests, zero failures/errors/skips.
- [x] Self-review, commit/push issue branch, create PR to `develop`, and request Codex review.
- [ ] Inspect CI/reviews, fix valid findings, and follow the repository issue-closure policy.

## Validation evidence

| Step | Command or manual procedure | Expected | Actual result / counts / evidence | Tested commit or working-tree state |
| --- | --- | --- | --- | --- |
| Baseline synchronization | `git fetch origin develop` then `git merge --ff-only origin/develop` | HEAD equals origin/develop and clean | Passed: both `380c574a0c5973cb3d06d8753e62a869c0cf70ca` | initial branch |
| Initial regression | `./mvnw.cmd test -Dtest=WorkflowBoardIntegrationTest` | Expose missing behavior | 4 failed: hidden routes and missing JSON auth error; fixed | 2026-09-14 draft |
| Database/HTTP targeted | `./mvnw.cmd test -Dtest=WorkflowMigrationIntegrationTest,WorkflowBoardIntegrationTest` | PostgreSQL/Flyway passes | 12 passed, zero skips | 2026-09-19 implementation |
| Local smoke | `./mvnw.cmd spring-boot:run` with dedicated local PostgreSQL, port 18045; HTTP health, stages, move, grouped board and OpenAPI | Success and error outcomes | Passed 200/201/400/401/403/404/409 checks; health remained 200 | 2026-09-19 final production code |
| Full verification | `docker info`; `./mvnw.cmd clean verify` | Zero failures/errors/skips | Docker 29.7.2; 107 tests passed, zero failures/errors/skips; packaged successfully | 2026-09-19 final code and tests |

## Plan changes and resume notes

| Date | New evidence / deviation | Reason and scope decision | Steps/checks to repeat |
| --- | --- | --- | --- |
| 2026-09-14 | #68 and #69 remain open after foundation #66/#67 merged. | Implement only stage administration and explicit move behavior; do not preempt project/work-item CRUD. | All implementation and validation steps. |
| 2026-09-14 | Initial draft omitted the explicit #45 board-read criterion, wrote implementation before behavioral tests, and used conditional beans to mask missing test mocks. | Corrected scope to include stage-filtered/grouped reads; removed conditional production beans and added repository mocks only in the no-database context test. Four new HTTP integration tests failed against the draft, then passed after corrections. This documents the earlier tests-first deviation rather than claiming compliance. | Full verification and migration tests. |
| 2026-09-19 | Resumed from saved work; origin/develop is still 380c574. | Six default stages are provisioned by a project-insert trigger, including existing projects without active stages. Custom stage IDs survive migration; their completion stays false until explicitly classified. Composite FK enforces project ownership; deferred active-position uniqueness permits transactional reordering. Writers lock space then project, matching membership/deletion locking. | Re-run focused tests, clean verify, local HTTP smoke and self-review. |

## Delivery

- PR URL: https://github.com/AbidShaik09/Kheera-Backend/pull/80
- Initial Codex request URL: https://github.com/AbidShaik09/Kheera-Backend/pull/80#issuecomment-5740691558
- Follow-up review URLs and findings: Pending.
- CI results: Pending.
- Merge/deployment status and evidence: Pending.
- Self-review: checked schema backfill/ownership, authorization and ancestor visibility, strict input, lock order and concurrent moves/deletion, deferred position uniqueness, DTO boundaries, page totals, documentation and diff whitespace. No credentials or runtime artifacts included.
- Remaining steps or blockers: CI and Codex review pending; merge/deployment are not requested or complete. Automatic approval review rejected closing #45 before merge as premature and without explicit authorization, so the issue remains open. PR creation is complete.
