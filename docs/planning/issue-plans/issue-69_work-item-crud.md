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
- WorkItemWriteRequest validates exact JSON types and presence; WorkItemService owns transactions/security; WorkItemController HTTP/status/OpenAPI; repositories own scoped queries. Existing WorkflowStageController board endpoint delegates enhanced reads; move ownership stays unchanged. Board DTO gains nullable type/assignment/parent metadata without removing existing fields.
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
- [ ] Commit plan/TODO before code/tests.
- [ ] Write HTTP/PostgreSQL behavior tests and record failing missing endpoints; write request/service tests before implementations.
- [ ] Add V27 provisioning/index migration and validate upgrade behavior.
- [ ] Add scoped repository reads, DTOs, service and controller; enhance board metadata/filters without breaking frontend consumers; run targeted tests.
- [ ] Update API_CONTRACT, APPLICATION_ARCHITECTURE, database findings, TESTING_STRATEGY, README, TODO and progress.
- [ ] Start task-owned local PostgreSQL/backend via mvnw.cmd spring-boot:run; verify startup PID/port, /api/health, authorized CRUD, invalid/unauthorized requests and /v3/api-docs. Stop only task-owned resources.
- [ ] Run docker info and mvnw.cmd clean verify, all tests zero skipped. Fix failures and repeat affected smoke/full validation after changes.
- [ ] Self-review security, concurrency, schema, API compatibility and acceptance criteria.
- [ ] Commit/push, PR to develop with plan/evidence; immediate @codex review and close issue per backend policy. Attach PR.
- [ ] Inspect CI/review, fix valid findings, validate, reply/resolve and request fresh review. Record blockers/rules if useful.
- [ ] Merge/deployment outside requested scope; leave pending until explicitly authorized.

## Evidence
Initial plan snapshot; no implementation tests run yet. Local smoke and full clean verification required before push. No tools/dependencies installed.
