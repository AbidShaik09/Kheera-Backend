# Issue #69: Authorized work-item CRUD

## Scope and baseline
- Issue: https://github.com/AbidShaik09/Kheera-Backend/issues/69
- Branch issue/69_work-item-crud from clean synchronized develop 102f3f11cae53058138750dfef6ac610d27279f9. Reuse clean checkout; preserve unrelated changes in primary checkout.
- Read issue, API contract, engineering/testing/documentation rules, V17-V26 schema, workflow/project/membership services, frontend board adapter and PostgreSQL test harness. Dependencies #66/#67/#68 and #45 are implemented. Docker Linux engine verified.
- Implement create/detail/PATCH/soft-delete, active project type reads and filtered board reads. Comments/uploads/dashboard visits remain separate issues. Sprint relationship is absent: explicitly reject sprintId, do not migrate speculative sprint behavior. UUID navigation only; no invented issue number.

## Contract decisions and layers
- Canonical assignment field/filter: assigneeMemberId (space_members.id); reject assigneeId. title required trimmed <=255, description nullable <=500, efforts nonnegative integer default 1; planned/actual ISO instant pairs ordered. PATCH omission preserves, null clears description/parent/assignee/dates, required title/type/stage/efforts cannot clear.
- Create defaults to first active stage and first active project type ordered by name/id; V27 provisions Task for existing/new projects without active types. Preserve legacy task/type data. Migration adds provisioning and query indexes only; forward fix with a new migration, never edit released migrations.
- Same-project active type/stage/parent and same-space active assignee required. Deleted/inactive historical assignees remain readable with active=false and can be cleared. Parent cycles rejected; parent deletion with active children returns 409. Space then project locks serialize create/update/delete with existing move/stage/project/member writes; use fresh entities after locks. Reads use repeatable-read snapshots.
- WorkItemWriteRequest validates exact JSON types and presence; WorkItemService owns transactions/security; WorkItemController HTTP/status/OpenAPI; repositories own scoped queries. The existing board URL moves from WorkflowStageController to WorkItemController; move ownership stays unchanged. Board DTO gains nullable type/assignment/parent metadata without removing existing fields.
- Board filters stageId/typeId/assigneeMemberId/parentId/q are ANDed; literal case-insensitive title/description search, page 0..100000, size 1..100, q <=100, stable stage position/item position/UUID order; groups only current-page items and global filtered totals. Deleted parents hide descendants from detail/list/move; direct children discoverable via parentId filter.

## Acceptance and tests
| Criterion | Test | Expected |
|---|---|---|
| CRUD/clear/defaults/DTO/status/OpenAPI | WorkItemIntegrationTest lifecycleAndPatch | 201 Location, 200 DTO, 204 deletion, omission/null distinct |
| Authorization/scoping/soft deletion | WorkItemIntegrationTest authorizationAndRelationships | 401/403/404 and no cross-space writes; historical assignment readable |
| Dates/effort/JSON/cycles/delete policy | WorkItemIntegrationTest validationAndHierarchy; WorkItemWriteRequestTest | 400 or 409; failed writes unchanged |
| Filtering/order/metadata | WorkItemIntegrationTest filteredBoard | bounded deterministic pages, literal search and correct totals |
| Races | WorkItemIntegrationTest concurrentHierarchyAndDeletion | serialized opposite parent changes, create/delete race cannot leave active child of deleted parent |
| Migration | WorkItemMigrationIntegrationTest | V26 legacy fixture upgraded, active types provisioned for existing/new projects, no legacy rewrites |
| Lock/auth boundary | WorkItemServiceTest | permission before data, space before project locking |

## Ordered execution
- [x] Verify issue, baseline, dependencies, Docker and rules.
- [x] Commit plan/TODO before code/tests (2dddb00).
- [x] Write HTTP/PostgreSQL behavior tests and record failing missing endpoints. HTTP tests preceded implementation; supplemental isolated request/service tests were added after those behavior tests passed their initial red phase.
- [x] Add V27 provisioning/index migration and validate upgrade behavior.
- [x] Add scoped repository reads, DTOs, service and controller; enhance board metadata/filters without breaking frontend consumers; run targeted tests.
- [x] Update API_CONTRACT, APPLICATION_ARCHITECTURE, database findings, TESTING_STRATEGY, README, TODO and progress.
- [x] Start task-owned local PostgreSQL/backend via mvnw.cmd spring-boot:run; verify startup PID/port, /api/health, authorized CRUD, invalid/unauthorized requests and /v3/api-docs. Stop only task-owned resources.
- [x] Run docker info and mvnw.cmd clean verify, all tests zero skipped. Fix failures and repeat affected smoke/full validation after changes.
- [x] Self-review security, concurrency, schema, API compatibility and acceptance criteria.
- [x] Commit/push, PR to develop with plan/evidence; immediate @codex review and close issue per backend policy. Attach PR.
- [ ] Inspect CI/review, fix valid findings, validate, reply/resolve and request fresh review. Record blockers/rules if useful.
- [ ] Merge/deployment outside requested scope; leave pending until explicitly authorized.

## Evidence
Initial TDD: five WorkItemIntegrationTest cases failed with missing endpoint statuses (404/405), zero errors/skips. WorkItemMigrationIntegrationTest failed because default Task types were absent. Initial implementation exposed missing new-controller error advice; registered it with the existing JSON handler.

The deleted-parent move regression failed (200 instead of 404); the existing move service now applies the same ancestor visibility check. Targeted tests then passed 39 cases. Self-review against the existing board contract found position compaction was required: a new stage PATCH/deletion regression failed before compaction was implemented. Final targeted command `mvnw.cmd -Dtest=WorkItemIntegrationTest,WorkItemMigrationIntegrationTest,WorkItemServiceTest,WorkItemWriteRequestTest,WorkflowBoardIntegrationTest,ProjectIntegrationTest test` passed 40 tests, zero failures/errors/skips.

Local startup: isolated PostgreSQL 16 container kheera-issue69-smoke on loopback 15469, backend via `mvnw.cmd spring-boot:run` with disposable environment on 18069. Verified startup PID 8260 owned port 18069. Health, project/type defaults, task CRUD, PATCH clearing/preservation, cyclic-parent 400, parent-delete 409, filtered board, stage PATCH/move, invalid effort/sprint 400, missing authentication 401, missing/deleted task 404, missing grant 403, and generated typed WorkItemInput OpenAPI all passed. No real SMTP/account/database used. Stopped only the verified task-owned server before full clean verification.

Automatic approval review briefly blocked Maven at a usage limit; the owner's continue resumed validation successfully. No workaround or skipped check used. Upstream develop refreshed on resume and was unchanged. No tools/dependencies installed.

Final pre-PR validation (2026-10-02): `docker info` confirmed Linux engine 29.7.2; `mvnw.cmd clean verify` passed 139 tests with zero failures, errors or skipped tests and built the executable JAR. Local startup/HTTP/OpenAPI smoke above tested the same application source. Self-review confirmed bounded paging, safe JSON errors, no client ownership fields, shared write-lock ordering, preserved move URL/board fields, transactional rollback, and no dependencies/secrets/generated files added. Test logs remain outside the repository. No new workflow rule needed: existing TDD and compatibility checks caught the concrete failures. CI/review/merge remain pending.

Delivery: [PR #84](https://github.com/AbidShaik09/Kheera-Backend/pull/84) targets develop; implementation commit 6c7abf4. Immediate [Codex review request](https://github.com/AbidShaik09/Kheera-Backend/pull/84#issuecomment-5958336282) accepted. Issue #69 closed under backend policy; remaining CI/review tracked on the PR. No merge requested.

## Review triage
| Thread | Verdict and evidence | Repair |
|---|---|---|
| 4168478441 | Real: legacy cross-project parent FK accepted; board showed 2 tasks while detail rejected them | Seed hidden traversal with cross-project parent edges and propagate to same-project descendants; active/deleted foreign parents both hidden |
| 4168478447 | Real: foreign-space legacy member was marked active | Suppress foreign member ID/name and mark inactive; apply the same scope check to legacy type metadata |

Both PostgreSQL regression tests failed before repair. Existing write validation already rejects new cross-scope relationships; reads must also validate legacy FK scope. Full validation/local smoke pending for the repair; prior green CI is not evidence for this changed source.

Review repair validation: 10 targeted task integration tests passed. Fresh local startup PID 9240 owned port 18069; complete HTTP/OpenAPI smoke plus legacy foreign parent/type/assignment checks passed. Stopped that process, then docker info and mvnw.cmd clean verify passed 141 tests, zero failures/errors/skips. No schema change required for these read-boundary repairs. Fresh CI/review pending at this snapshot.
