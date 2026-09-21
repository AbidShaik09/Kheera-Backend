# Issue #68: Space-scoped project CRUD and summaries

## Scope and baseline
- Issue: https://github.com/AbidShaik09/Kheera-Backend/issues/68
- Branch: `issue/68_project-crud`; freshly fetched develop: `d14b8c3397b34fcdc142b8f78dcb171ed756ed3a`.
- Clean isolated worktree preserves the user's main-checkout TODO edit.
- Prerequisites verified merged: #62/#63/#64, #74 (#66), #78 (#67), #80 (#45).
- Read engineering/testing/documentation standards, API contract, product reference,
  database design, V10/V26, existing membership/board services and tests.
- Deliver five project endpoints, metadata validation, scoped paging and real metrics.
  URLs, sprint administration, activity and type configuration remain out of scope.

## Acceptance criteria and tests
| Criterion | Test | Expected |
| --- | --- | --- |
| Authorized CRUD and statuses | ProjectIntegrationTest.lifecycleAndDescendantDeletion | 201/200/204; retained descendants become inaccessible |
| JWT, permissions, foreign IDs, inactive data | ProjectIntegrationTest.authorizationAndInactiveAncestors | JSON 401/403/404; no data or totals leaked |
| Validation and PATCH ownership | ProjectIntegrationTest.validationAndPatchSemantics | 400 for invalid/unknown fields; omission preserved, null clears description |
| Semantic aggregate metrics | ProjectIntegrationTest.semanticMetricsIncludeParentsAndExcludeDeleted | empty 0%; mixed rounded percentage; deleted rows ignored |
| Scoped stable pagination and bounded queries | ProjectIntegrationTest.scopedStablePagingAndBoundedQueries | fixed queries across page sizes; literal search and UUID tie-break |
| Service authority/lock rules | ProjectServiceTest | authorize before summaries; space then project locks; no physical delete |

## Design and affected files
- ProjectController/ProjectWriteRequest/ProjectSummaryDto: HTTP-only DTO boundary,
  strict JSON fields, 255-character name, 500-character description, positive integer
  sprintCycleDays (default 7), PATCH omission semantics; spaceId cannot be patched.
- ProjectService owns transactions. Active membership permits reads; space.update
  permits create/update; space.delete permits delete, matching existing grants.
  Writes serialize space then project, coordinating membership and board mutations.
- ProjectsRepository supplies scoped page/detail; WorkItemsRepository supplies one
  grouped aggregate query for page IDs. No collection traversal. Repeatable-read
  snapshots keep page/count/metrics consistent.
- Count each active work item once, including parents/epics regardless of type or
  assignment. Active stage is_complete defines completion. Nearest whole percentage,
  half up; empty 0%. openTaskCount = total - complete. Deleted stages/items excluded.
- Schema/mapping/migration N/A: existing columns, constraints and V26 board trigger
  fully support this slice; no persistence structure changes. Legacy null sprint
  cycles remain readable; all supplied new values must be positive integers.
- Update API_CONTRACT, DATABASE_DESIGN, APPLICATION_ARCHITECTURE, testing strategy,
  README, TODO and APPLICATION_PROGRESS. History entry awaits material merge.

## Ordered execution checklist
- [x] Confirm requirements/dependencies and clean fetched develop baseline.
- [x] Create branch and TODO entry; write initial plan.
- [x] Commit initial plan before application tests/code.
- [x] Add and run failing PostgreSQL HTTP tests (expected missing-route 404).
- [x] Add service tests before service implementation; implement repository queries,
      DTOs, service and controller; extend JSON error advice.
- [x] Run targeted ProjectIntegrationTest, ProjectServiceTest and board regressions.
- [x] Update affected documentation and self-review acceptance/security/concurrency.
- [x] Start with mvnw.cmd spring-boot:run using disposable local PostgreSQL; check
      health, create/list/read/patch/delete, 400/401/403/404 and raw OpenAPI.
- [x] Run docker info and mvnw.cmd clean verify; require zero skips/failures/errors.
- [x] Commit/push feature branch and create PR to develop with validation evidence.
- [x] Request @codex review and record accepted comment URL.
- [ ] Keep issue #68 open until merge, per explicit owner instruction on 2026-09-21.
- [ ] Inspect CI/reviews; fix findings and repeat affected/full validation.
- [ ] Review blockers/rules; merge/deployment only with applicable authorization.

## Validation evidence
2026-09-21, final implementation working tree: targeted command: `.\mvnw.cmd -Dtest=ProjectIntegrationTest,ProjectServiceTest,WorkflowBoardIntegrationTest test`.
Full gate: `docker info`, `.\mvnw.cmd clean verify`. Local startup and HTTP smoke
are separate gates; no skips or substitute databases.

- Initial PostgreSQL HTTP red run: 1 test, expected 201 but received missing-route
  404, zero setup errors/skips (`issue68-red.log`, workspace parent).
- Targeted project/service/board: 21 tests passed, zero failures/errors/skips.
- Final clean verify: 116 tests passed, zero failures/errors/skips; Docker 29.7.2,
  Java 21.0.12.1, PostgreSQL 16 Testcontainers, all 26 Flyway migrations applied.
- Local spring-boot:run on 18068 against disposable PostgreSQL 15468: startup,
  health before/after, CRUD statuses, 0/100 percent metrics after a board move,
  validation 400, missing auth 401, forbidden update 403, unknown/deleted project
  404, descendant board 404, and OpenAPI 201/204/error responses passed.
- Self-review: authorization precedes metadata/count access; immutable space parent;
  locks match board/membership order; bounded queries and stable sorting tested;
  soft-delete retains children; strict fields prevent audit/ownership writes.
- Existing local TODO edit remains untouched. No production configuration or secrets.

## Plan changes and recovery notes
No scope changes. The host Docker installation is accessible with escalated runtime
permissions. Git operations as the host require an exact safe.directory override
for the sandbox-created worktree; use a command-scoped override, not a broad trust
change. Existing recovery/permission rules cover this friction; no new rule needed.

## Delivery
- PR: https://github.com/AbidShaik09/Kheera-Backend/pull/81 (base develop).
- Initial review request accepted: https://github.com/AbidShaik09/Kheera-Backend/pull/81#issuecomment-5755118525
- Implementation commit: d6bb46b; initial plan commit: cb60acd.
- Local verification complete; CI/review in progress. Merge/deployment pending.
- Automatic approval review rejected issue closure as an additional external
  mutation. Owner explicitly directed keeping #68 open until merge; this overrides
  the repository's default close-after-PR rule. No closure occurred.
- Blocker/rule review: existing Docker and permission recovery rules suffice.
  The issue-closure exception is specific to this delivery, not a global rule change.
