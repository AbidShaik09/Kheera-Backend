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
- [x] Commit initial plan before tests/application edits.
- [x] Write existing-surface HTTP regression first; run `./mvnw -Dtest=MembershipHttpIntegrationTest test` and record endpoint failure.
- [x] Write repository/service/MVC tests before corresponding implementation.
- [x] Add V25 and repository queries; run `./mvnw -Dtest=MembershipIntegrationTest test`.
- [x] Implement transactional service and DTO/controller/error contracts; run `./mvnw -Dtest=MembershipServiceTest,MembershipControllerTest,MembershipHttpIntegrationTest test`.
- [x] Update affected documents and record targeted evidence; fix failures and rerun affected tests.
- [x] Start isolated PostgreSQL and `.\mvnw.cmd spring-boot:run`; verify `/api/health`, authenticated membership journey, 400/401/403/404/409 and `/v3/api-docs`.
- [x] Run `docker info` and `.\mvnw.cmd clean verify`; require zero failures/errors/skips and record counts.
- [x] Self-review scope, concurrency, permission escalation, validation, retention, SQL and documentation.
- [x] Commit/push branch; create own PR to develop with plan and verification evidence.
- [x] Request `@codex review` under requested repository workflow; record comment URL; close issue per repository policy.
- [ ] Inspect CI and review; fix valid findings, rerun affected startup/tests/full verify, push and request fresh review.
- [ ] Review friction/rules; record ready or blocked state. Merge only with per-PR authorization; deployment/history remain pending until observed.

## Evidence and resume notes
Docker host access verified; use escalated Maven for host
Docker pipe and dependency access. No tests skipped. See recorded evidence below;
CI and review results are pending.


### Validation results (2026-09-13)
- Initial HTTP regression: `mvnw.cmd -Dtest=MembershipHttpIntegrationTest test`
  ran 1 test, failed on expected 200 versus actual 404 (missing endpoint), zero skips.
- Initial targeted suite including space regressions: 27 passed, zero skips.
- Expanded membership suite (migration, query count, retention, races, MVC/service):
  18 passed, zero skips. Fixed a Runnable/ThrowingCallable test compilation error
  before that run; it was not counted as the initial behavioral regression.
- Standalone `mvnw.cmd spring-boot:run`, isolated postgres:16-alpine on localhost
  55467 and backend 18067: health remained 200; membership create/list/change/delete,
  rejoin ID, role/permission lists, 400/401/403/404/409 and generated OpenAPI passed.
  Fixtures were synthetic, with no SMTP activity or development data mutation.
- First full clean verify: 92 tests, 0 failures, 1 context setup error, 0 skips.
  Added MembershipRoleRepository mock to the existing database-free context test.
  No production behavior or test assertion was weakened. Full rerun pending.
- Self-review checked safe sort allow-list/literal search, fetched DTO boundaries,
  no per-member query growth, space lock before authority checks, current/target
  grant subsets, deleted ancestors, rejoin identity and migration forward safety.
- Friction review: Docker recovery is already documented in d1c547f. New repositories
  also require mocks in the existing database-free context test; documented below
  in testing strategy so future service additions preserve the full context gate.

- Final `mvnw.cmd clean verify`: **92 tests, 0 failures, 0 errors, 0 skipped**;
  BUILD SUCCESS, 59.442 seconds. Docker Linux engine 29.7.2. Local evidence logs
  are outside the repository: issue67-red.log, issue67-targeted-final.log,
  issue67-smoke.log, issue67-verify-final.log. Tested implementation and context
  mock are the files committed with this plan update; only delivery docs follow.


### Delivery
- PR: https://github.com/AbidShaik09/Kheera-Backend/pull/78
- Implementation commit: 9cbdcdf (preceded by initial plan cb7ded4).
- Initial review request accepted: https://github.com/AbidShaik09/Kheera-Backend/pull/78#issuecomment-5654375353
- Issue #67 closed after PR creation under repository policy; remaining delivery
  tracked on PR #78. CI/review and merge/deployment pending, not claimed complete.
- This delivery-record commit changes documentation only; implementation remains
  byte-identical to the 92-test clean verification and successful local smoke.
