# Issue #67: Membership management and administrator safeguards

## Scope and baseline
- Issue: https://github.com/AbidShaik09/Kheera-Backend/issues/67
- Branch: `issue/67_membership-management`; clean synchronized develop `d1c547f`.
- Foundations #62/#63/#64 and #66 PR #74 are verified merged on GitHub.
- Read engineering/testing/documentation standards, API contract, product reference,
  Flyway schema, space access/lifecycle code and existing PostgreSQL/MVC tests.
- #68 remains blocked on #45 per owner instruction on 2026-09-13. No project or
  workflow implementation in this branch. Invitations/custom-role CRUD excluded.

## Design and acceptance criteria
| Requirement | Named verification | Expected result |
| --- | --- | --- |
| Scoped membership CRUD and catalogue reads | MembershipHttpIntegrationTest.journey; MembershipControllerTest.statuses | GET/PATCH 200, POST 201, DELETE 204; minimal DTOs |
| Active user/member/space/role boundaries | MembershipIntegrationTest.isolationAndDeletedRows | 401 inactive caller, 404 invisible resources, no directory leakage |
| Separate action grants and no privilege escalation | MembershipServiceTest.permissionFailures; MembershipIntegrationTest.grantSubset | 403 for missing action or stronger target/current role |
| Duplicate and rejoin | MembershipIntegrationTest.duplicateAndRejoin; concurrentAdds | 409 duplicate; same membership ID reactivated; one row under races |
| Last administrator and self-removal | MembershipIntegrationTest.lastAdministrator; concurrentDemotionsAndRemovals | 409 last admin; other permitted removals persist |
| Bounded search/page/sort, stable ties | MembershipIntegrationTest.pagination; MembershipControllerTest.invalidRequests | contract envelope, 1..100 size, 0..100000 page, q <=100, allow-listed sort plus ID |
| Migration and bootstrap | MembershipIntegrationTest.catalogue; full regression | existing manage grants receive four actions; new spaces bootstrap same catalogue |
| Revocation and retention | MembershipIntegrationTest.revocation | former member cannot read space; task/comment rows retained |

Services own transactions and call SpaceAccessService before querying targets.
Every mutation locks the active space before rechecking caller authority and
reading members. Rejoin restores the unique (user,space) row. Current and target
role grants must both be subsets of the caller's effective grants; callers cannot
administer stronger members. Administrator means an active member/user/role with
all legacy space.update, space.delete, space.members.manage grants, independent
of display name. This preserves #66's bootstrap and does not add role CRUD.

V25 adds per-space read/add/change-role/remove catalogue entries and grants them
to active roles with the existing manage permission, without reviving deleted
grants. Existing schema mappings remain valid; no destructive migration. New
spaces also receive a Member role with read access for the selector. Repository
queries fetch user/role with each membership page, filter all soft-deleted rows,
and never traverse collections for member summaries. Reads of roles/permissions
are scoped and paginated to bound imported/custom data. Future role/user mutation
APIs must coordinate the same space lock to preserve the administrator invariant.

Affected files: membership DTOs/controller/service, membership and role/permission
repositories, SpaceMembers domain factory/change method, SpaceAccessService
catalogue, SpaceLifecycleService bootstrap, SpaceErrorHandler, V25 migration.
Docs: API_CONTRACT, DATABASE_DESIGN, APPLICATION_ARCHITECTURE, TESTING_STRATEGY,
README, IMPLEMENTATION_TODO, APPLICATION_PROGRESS; history only after merge.

## Ordered execution
- [x] Read requirements/references and verify baseline and dependencies.
- [x] Create issue branch; update TODO and write initial plan.
- [ ] Commit initial plan before tests/application edits.
- [ ] Write existing-surface HTTP regression first; run `./mvnw -Dtest=MembershipHttpIntegrationTest test` and record endpoint failure.
- [ ] Write repository/service/MVC tests before corresponding implementation.
- [ ] Add V25 and repository queries; run `./mvnw -Dtest=MembershipIntegrationTest test`.
- [ ] Implement transactional service and DTO/controller/error contracts; run `./mvnw -Dtest=MembershipServiceTest,MembershipControllerTest,MembershipHttpIntegrationTest test`.
- [ ] Update affected documents and record targeted evidence; fix failures and rerun affected tests.
- [ ] Start isolated PostgreSQL and `.\mvnw.cmd spring-boot:run`; verify `/api/health`, authenticated membership journey, 400/401/403/404/409 and `/v3/api-docs`.
- [ ] Run `docker info` and `.\mvnw.cmd clean verify`; require zero failures/errors/skips and record counts.
- [ ] Self-review scope, concurrency, permission escalation, validation, retention, SQL and documentation.
- [ ] Commit/push branch; create own PR to develop with plan and verification evidence.
- [ ] Request `@codex review` under requested repository workflow; record comment URL; close issue per repository policy.
- [ ] Inspect CI and review; fix valid findings, rerun affected startup/tests/full verify, push and request fresh review.
- [ ] Review friction/rules; record ready or blocked state. Merge only with per-PR authorization; deployment/history remain pending until observed.

## Evidence and resume notes
Initial planning only. Docker host access verified; use escalated Maven for host
Docker pipe and dependency access. No tests skipped. Initial expected red test,
targeted tests, full suite, smoke, PR, CI and review evidence pending.
