# Issue #66: Space lifecycle and creator membership

Branch: `issue/66_space-lifecycle`; separate PR to `develop`.
Baseline: fresh `develop` b86ee28; foundation PRs #62, #63 and #64 are merged.

## Plan

1. Write service, request-validation, secured MockMvc and PostgreSQL tests first.
2. Add explicit input/detail DTOs and scoped JSON errors. Preserve legacy list
   and authentication contracts.
3. Bootstrap space, administrator role, permission grants and creator membership
   in one transaction. Resolve the active creator from JWT email.
4. Centralize active membership and permission checks for future descendant APIs.
   Require same-space, active roles, grants and permissions. Soft-delete only the
   space; retain descendants. Serialize space writes with a database row lock.
5. Document field limits, PATCH semantics, minimum permission catalogue and
   downstream authorization obligations. No schema change is needed: reuse the
   existing Flyway tables and constraints.
6. Run targeted tests, local startup/health/API/OpenAPI smoke checks, full unit
   suite and Docker-backed clean verify; self-review before push and PR creation.
7. Request `@codex review` immediately, close the issue per repository policy,
   and inspect CI/review findings.

## Decisions

- Active members can read; nonmembers/deleted resources receive 404. Active
  members without the requested permission receive 403; inactive users get 401.
- Name is trimmed, required and at most 255 characters; description at most 500;
  profilePic at most 255 and an absolute HTTP(S) URL without embedded credentials.
- PATCH omission preserves a value; null clears optional fields, never name.
  Unknown fields and non-string values are rejected. Empty PATCH is a no-op.
- Minimum catalogue: `space.update`, `space.delete`, `space.members.manage`.
  Administrator receives all three; #67 reuses the membership-management grant.

## Verification

- TDD baseline failed on missing lifecycle types before implementation.
- Targeted service/controller/PostgreSQL suite: 15 tests passed initially;
  subsequent full HTTP/OpenAPI regressions are included in clean verification.
- Docker Desktop 29.7.2; `mvnw.cmd clean verify`: 73 tests, zero failures,
  errors or skips (2026-09-13). Includes the entire existing unit suite.
- Local `mvnw.cmd spring-boot:run` against an isolated PostgreSQL 16 container:
  health 200 before/after, create 201, list/detail/update 200, delete 204;
  invalid fields/null name 400, missing JWT 401, revoked grant 403,
  nonmember/deleted-space reads and deleted-space updates 404. PATCH null and
  omission behavior confirmed. OpenAPI input limits and POST 201/DELETE 204
  verified in tests and live generated docs.
- Self-review checked DTO isolation, grants/soft deletes, transaction rollback,
  field validation, concurrency, API compatibility and documentation. No schema
  changes or production credentials. Diff whitespace checks pass.
- Friction addressed: persistence uses the managed role returned by JPA merge;
  the no-database context test mocks the new repository; explicit response
  statuses prevent OpenAPI from incorrectly documenting 200 for create/delete.
  These are covered by regressions; no further global rule is needed beyond the
  newly published requirement for initial `@codex review`.
- PR creation, CI and Codex review pending; no merge/deployment claimed.
