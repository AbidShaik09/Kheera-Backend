# Kheera Backend Implementation Todo

## Reconciled Status (2026-09-13)

This tracker was checked against latest `develop`, GitHub issue states, and PR
merge evidence. Completed work is separated from remaining delivery; a closed
issue or closed PR alone is not proof that its implementation is present.
Follow `../engineering/ENGINEERING_STANDARDS.md`, including fresh develop
synchronization before edits and issue branches from verified latest develop.

## Remaining Foundation Work

- [ ] [#55 Harden CI/CD safety and require relevant test checks on pull requests](https://github.com/AbidShaik09/Kheera-Backend/issues/55).
  Backend PR verification and test-enabled deployment builds now exist. Issue
  branch `issue/55_harden-ci-cd-safety` hardens backend workflows with
  least-privilege permissions, pinned deployment actions, concurrency guards,
  environment boundaries, and branch-protection documentation. Finish remaining
  frontend and repository-admin branch-protection work outside the backend code
  branch before marking the broader cross-repository issue fully complete.
- [x] [#50 Email worker tests and CI](https://github.com/AbidShaik09/Kheera-Backend/issues/50).
  [PR #64](https://github.com/AbidShaik09/Kheera-Backend/pull/64) was restored,
  reopened, and merged in 86ef05c. Worker coverage and clean test-enabled
  deployment builds are now delivered. The subsequent Docker API failure was
  fixed by #71; no tests were disabled.
- [x] [#71 Deployment Docker API compatibility](https://github.com/AbidShaik09/Kheera-Backend/issues/71).
  Updated the test-scoped Testcontainers BOM to 1.21.4 in c78810c. Local clean
  verification and deployment run 34723685764 passed all 56 tests with zero
  failures/errors/skips. Run 34723801289 also succeeded; the public development
  health endpoint returned 200 Server is Healthy.

## Next Product Delivery Slices

Each implementation issue requires its own branch and PR to `develop`.
Use the linked acceptance criteria and the canonical
[API contract](../workspace/api/API_CONTRACT.md).

1. [ ] [#66 Space lifecycle and creator membership](https://github.com/AbidShaik09/Kheera-Backend/issues/66).
   Create/read/update/soft-delete spaces, bootstrap creator permissions atomically,
   and preserve the existing space-list response. Replaces historical #9. Implemented and locally verified (73 tests, zero skips) on `issue/66_space-lifecycle`; PR review/merge pending; see [plan and validation](issue-66_space-lifecycle.md).
2. [ ] [#67 Membership management and administrator safeguards](https://github.com/AbidShaik09/Kheera-Backend/issues/67).
   Depends on #66; supplies membership management, role/permission reads and
   last-administrator protection.
3. [ ] [#45 Workflow stages and board movement](https://github.com/AbidShaik09/Kheera-Backend/issues/45).
   Decide and migrate ordered stages, completion semantics, and task-stage
   relationships before dependent board APIs. Owns the move/position endpoint.
   Coordinate with #66/#67 authorization; stage design can use seeded project
   fixtures without depending on the project HTTP API.
4. [ ] [#68 Space-scoped project CRUD and summaries](https://github.com/AbidShaik09/Kheera-Backend/issues/68).
   Child slice of #10; depends on #66/#67 and #45 completion semantics.
5. [ ] [#69 Work-item CRUD, hierarchy, and board reads](https://github.com/AbidShaik09/Kheera-Backend/issues/69).
   Child slice of #11; depends on #68, membership authorization, and #45.
   Preserve completed #44 mapping decisions; new relationships need new migrations.
6. [ ] [#70 Dashboard read model and visit history](https://github.com/AbidShaik09/Kheera-Backend/issues/70).
   Follows #66–#69 and #45 after product data stabilizes. Supplies real task
   sections and focus data; defines visit tracking and frontend DTO mappings.

## Remaining Module Scope

- [ ] [#10 Project module](https://github.com/AbidShaik09/Kheera-Backend/issues/10).
  Keep open as the parent of #68. Remaining documented module work includes project
  URLs, full work-item type configuration, sprint statuses/sprints, and project
  activity. Use the current space-scoped contract instead of legacy endpoint
  sketches. Do not implement #68 twice.
- [ ] [#11 Task management module](https://github.com/AbidShaik09/Kheera-Backend/issues/11).
  Keep open as the parent of #69. Comments, author permissions, and task activity
  remain outside #69; movement belongs to #45. Use `/api/work-items`, not the
  legacy `/api/tasks` sketch. Coordinate sprint/type support with #10.
- [ ] [#28 File Upload API Feature](https://github.com/AbidShaik09/Kheera-Backend/issues/28).
  Still needed for task/comment attachments and profile assets. Reconcile its
  legacy `/api/files/upload` public-URL proposal with `/api/uploads` in the
  current contract before implementation. Define storage, validation, access
  controls and ownership; attachment association depends on work-item/comment
  ownership. Do not claim private attachments or cleanup are already delivered.

Custom role editing, invitations, profile editing, global search, full activity
timelines, favourites, notifications, and the calendar remain future work.
Favourites/notifications need persistence decisions. #70 must expose unsupported
capabilities honestly rather than returning fabricated content. Create scoped
issues before implementing untracked follow-ups.

## Frontend Dependencies

- Frontend [#63 Space Details](https://github.com/AbidShaik09/Kheera-Frontend/issues/63)
  needs #66/#67/#68.
- Frontend [#64 Project Details](https://github.com/AbidShaik09/Kheera-Frontend/issues/64)
  needs #68/#69/#45 and remaining project activity APIs.
- Frontend [#65 Task Details](https://github.com/AbidShaik09/Kheera-Frontend/issues/65)
  needs #69 plus #11 comments/activity and #28 uploads/attachments.
- Frontend dashboard layout #60 is complete. API integration is still pending
  backend #70 and needs a frontend integration issue before implementation.

## Completed Baseline

These are historical deliverables, not items to implement again.

| Issue | Delivered work | Evidence |
| --- | --- | --- |
| [#52](https://github.com/AbidShaik09/Kheera-Backend/issues/52) | Engineering and documentation foundation | Merged PR #53 |
| [#51](https://github.com/AbidShaik09/Kheera-Backend/issues/51) | Latest non-null OTP lookup and expiry | Merged PR #54; service/repository coverage subsequently delivered |
| [#44](https://github.com/AbidShaik09/Kheera-Backend/issues/44) | Flyway/JPA alignment; diagram explicitly logical-only | Merged PR #57; DATABASE_DESIGN.md; verification in PR #62 |
| [#43](https://github.com/AbidShaik09/Kheera-Backend/issues/43) | JWT-email scoped active space list | Merged PR #59 |
| [#46](https://github.com/AbidShaik09/Kheera-Backend/issues/46) | DTO/transaction boundaries and active-user handling | Merged PRs #58/#60 |
| [#47](https://github.com/AbidShaik09/Kheera-Backend/issues/47) | Authentication/user/OTP service tests | Merged PR #61 |
| [#48](https://github.com/AbidShaik09/Kheera-Backend/issues/48) | PostgreSQL Testcontainers/Flyway repository coverage | Merged PR #62 |
| [#49](https://github.com/AbidShaik09/Kheera-Backend/issues/49) | MockMvc/JWT/CORS coverage | Merged PR #63 |
| [#56](https://github.com/AbidShaik09/Kheera-Backend/issues/56) | Expedited delivery categories | Closed governance issue; engineering standards |

[#9](https://github.com/AbidShaik09/Kheera-Backend/issues/9) is closed as
**superseded**, not implemented. #66/#67 own the replacement space-domain work.
#44/#47 were closed during this reconciliation using existing merged evidence.
Application tests were not rerun for this documentation-only reconciliation.

## Per-Issue Delivery Checklist

- [ ] Confirm scope and acceptance criteria; synchronize develop before editing.
- [ ] Define DTOs, validation, JWT-to-membership authorization, and soft-delete rules.
- [ ] Add Flyway migrations for persistence changes and keep JPA/schema aligned.
- [ ] Add focused service/controller tests and PostgreSQL tests for persistence.
- [ ] Verify local startup, health, affected APIs, OpenAPI, and required full checks
  according to engineering standards; do not silently skip Docker tests.
- [ ] Update API/architecture/schema documentation, this TODO, and delivery records.
- [ ] Self-review and follow the required issue-branch, PR, CI, and review process.

References: [database design](../architecture/DATABASE_DESIGN.md),
[testing strategy](../testing/TESTING_STRATEGY.md),
[application progress](../workspace/progress/APPLICATION_PROGRESS.md),
[documentation standards](../workspace/governance/DOCUMENTATION_STANDARDS.md).
