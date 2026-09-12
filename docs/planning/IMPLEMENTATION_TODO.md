# Kheera Backend Implementation Todo

## How to Use This File

This is the recommended delivery order for currently open backend work. Follow
the phases in sequence unless an item is explicitly marked safe to parallelize.
An issue is ready to close only when its acceptance criteria pass, tests are
present, and the API contract remains accurate.

Reference documents:

- `../workspace/api/API_CONTRACT.md`
- `../workspace/architecture/APPLICATION_ARCHITECTURE.md`
- `../workspace/progress/APPLICATION_PROGRESS.md`
- `../workspace/governance/DOCUMENTATION_STANDARDS.md`
- `../architecture/DATABASE_DESIGN.md`
- `../testing/TESTING_STRATEGY.md`
- `../engineering/ENGINEERING_STANDARDS.md`

## Required Foundation: Engineering Workflow

- [x] [#52 Establish engineering workflow and backend documentation structure](https://github.com/AbidShaik09/Kheera-Backend/issues/52)

Complete this before starting new backend feature work. Follow
`docs/engineering/ENGINEERING_STANDARDS.md`: issue first, TODO second, then a
clean `develop` branch named `issue/<number>_<short-kebab-title>`. Tests are
written or updated before implementation, and every PR targets `develop` and
awaits review.

## Phase 0: Make Testing Runnable

**Goal:** Every developer and CI runner can execute a repeatable Java 21 test
command before feature work expands.

- [ ] Install/configure Java 21 wherever backend development runs.
- [ ] Verify `./mvnw test` works locally and on the self-hosted runners.
- [ ] Add PostgreSQL Testcontainers dependencies and a test profile.
- [ ] Ensure test configuration uses a safe test JWT secret and disables
  scheduled processing by default.

**Exit criteria:** `./mvnw test` works against a disposable test database, never
the development or production database.

## Phase 1: Correct Existing Data and Identity Contracts

### 1. Restore reliable password-reset OTP validation

- [x] [#51 Fix password-reset OTP lookup when legacy records have null created_at](https://github.com/AbidShaik09/Kheera-Backend/issues/51)

Do this first. It is a live account-recovery failure: PostgreSQL orders `NULL`
timestamps before non-null values for the current descending lookup, allowing a
legacy OTP row to be selected instead of the newest reset code. Ignore
null-timestamp legacy rows, enforce OTP expiry, and add a focused regression
test before deploying the fix. The code fix is in progress; the regression test
is intentionally deferred to [#47](https://github.com/AbidShaik09/Kheera-Backend/issues/47)
under the current delivery decision.

### 2. Schema and mapping alignment

- [x] [#44 Synchronize Flyway migrations, JPA mappings, and ER diagram](https://github.com/AbidShaik09/Kheera-Backend/issues/44)

Do this next because every repository/integration test depends on a database
that agrees with JPA. Resolve the `OneTimePassword` table mapping and the known
nullability drift before adding broad test coverage or new migrations.

### 3. Fix authenticated space lookup

- [ ] [#43 Fix JWT principal handling for authenticated space lists](https://github.com/AbidShaik09/Kheera-Backend/issues/43)

The JWT principal is email, but `GET /api/spaces` currently interprets it as a
UUID. Fix this immediately after the mapping baseline. It is a user-visible
blocker for the authenticated workspace sidebar.

**Exit criteria:** an authenticated user can retrieve only their active spaces;
another user's memberships never appear.

### 3a. Harden existing Hibernate boundaries

- [ ] [#46 Harden Hibernate repository and DTO boundaries for existing APIs](https://github.com/AbidShaik09/Kheera-Backend/issues/46) (in progress)

Apply DTO projections and explicit transaction boundaries to the currently
implemented user endpoints. Integration coverage is intentionally deferred to
[#48](https://github.com/AbidShaik09/Kheera-Backend/issues/48) under the current
owner-authorized no-review delivery decision.

## Phase 2: Protect Current Behavior with Tests

### 4. Service unit tests

- [ ] [#47 Add unit tests for authentication, user, and OTP services](https://github.com/AbidShaik09/Kheera-Backend/issues/47) (in review)

Implement this after phase 1. Preserve the focused #51 regression case here as
part of the wider OTP service coverage. Keep these tests fast and independent of
Spring/PostgreSQL.

### 5. Repository integration tests

- [ ] [#48 Add PostgreSQL Testcontainers repository integration tests](https://github.com/AbidShaik09/Kheera-Backend/issues/48) (in review)

Depends on #44. Cover Flyway, PostgreSQL constraints, ordering, DTO projections,
and membership isolation. This is the primary guard against future schema drift.
Local `test-compile` passed; full Testcontainers execution requires a
Docker-capable runner.

### 6. MVC and middleware tests

- [ ] [#49 Add MockMvc tests for implemented controllers and JWT security](https://github.com/AbidShaik09/Kheera-Backend/issues/49)

Depends on #43 for the final space-list regression test. Verify exact current
authentication response contracts because the Angular client consumes raw text
from auth endpoints.

### 7. Email worker tests and CI gate

- [ ] [#50 Add tests for scheduled email worker and enforce tests in CI](https://github.com/AbidShaik09/Kheera-Backend/issues/50)

Start unit tests for the worker in parallel with #47. Enable CI test gating only
after #47, #48, and #49 are stable. The deployment workflows must stop using
`-DskipTests` before this phase is complete.

**Exit criteria:** Pull requests and deployments run the test suite. A failed
test prevents deployment.

## Phase 3: Clean API and Hibernate Boundaries

### 8. Harden repository/service/DTO structure

- [ ] [#46 Harden Hibernate repository and DTO boundaries for existing APIs](https://github.com/AbidShaik09/Kheera-Backend/issues/46)

Do this once tests are in place, so refactoring is protected. Apply the agreed
rules:

- Controllers return DTOs, never JPA entities.
- Services own transaction boundaries, authorization, and entity mutation.
- Read-heavy endpoints use focused projections.
- `spring.jpa.open-in-view=false` is explicit.
- SQL logging is restricted to local development.

**Exit criteria:** existing endpoints retain their frontend contract while their
persistence access is explicit, scoped, and tested.

## Phase 4: Complete the Space Module

### 9. Space, membership, role, and permission APIs

- [ ] [#9 Implement Backend Logic to allow Team creation](https://github.com/AbidShaik09/Kheera-Backend/issues/9)

Treat "Team" as Kheera's `Space` domain. Deliver in small slices:

- [ ] Space create/read/update/soft-delete.
- [ ] Membership list/add/remove.
- [ ] Role assignment and permission enforcement.
- [ ] Space Details and sidebar DTO projections.

**Dependencies:** phases 1 through 3.

**Exit criteria:** all space operations authorize through `space_members`; no
browser-supplied user ID determines access.

## Phase 5: Decide Project Board State Before Building Projects

### 10. Workflow-stage design and implementation

- [ ] [#45 Define and implement workflow stages for the project board](https://github.com/AbidShaik09/Kheera-Backend/issues/45)

This must precede full project/task delivery. The Penpot board uses columns such
as Backlog, To Do, In Progress, In Review, Done, and Blocked, but the current
schema cannot assign a work item to a board stage.

Decide whether `project_workflows` represents ordered stages or whether a
separate workflow-stage table is needed. Deliver Flyway migration, JPA mapping,
DTOs, authorization, and move API together.

**Exit criteria:** a project has ordered stages and each board-visible work item
has a valid stage belonging to its project.

## Phase 6: Complete the Project Module

### 11. Project and planning APIs

- [ ] [#10 Implement Backend Logic for Project Module](https://github.com/AbidShaik09/Kheera-Backend/issues/10)

Deliver in this order:

- [ ] Project CRUD scoped to a space.
- [ ] Project summaries for space and dashboard cards.
- [ ] Project URL CRUD.
- [ ] Work-item type CRUD.
- [ ] Sprint status and sprint CRUD.
- [ ] Project activity read model.

**Dependencies:** #9 and #45.

**Exit criteria:** the frontend can load Space Details and Project Details from
documented, authorization-scoped APIs without mock data.

## Phase 7: Complete the Task Module

### 12. Work items, comments, and board movement

- [ ] [#11 Implement Backend Logic for Task Management Module](https://github.com/AbidShaik09/Kheera-Backend/issues/11)

Deliver in this order:

- [ ] Work-item create/read/update/soft-delete.
- [ ] Parent/child hierarchy with cycle prevention.
- [ ] Assignment limited to members of the project's space.
- [ ] Type, sprint, and workflow-stage validation.
- [ ] Filtered board/list queries and explicit move endpoint.
- [ ] Comments with author membership enforcement.
- [ ] Work-item activity DTOs.

**Dependencies:** #10 and #45.

**Exit criteria:** Task Details and Project board flows work end to end and every
relationship is verified as belonging to the same authorized project/space.

## Phase 8: File Upload and Attachment Delivery

### 13. Upload API and attachment associations

- [ ] [#28 File Upload API Feature](https://github.com/AbidShaik09/Kheera-Backend/issues/28)

Deliver upload infrastructure after work-item/comment ownership exists:

- [ ] Define storage provider and signed/direct upload approach.
- [ ] Validate content type, size, authorization, and object ownership.
- [ ] Associate uploads with work items or comments.
- [ ] Support safe deletion and orphan cleanup.

**Dependencies:** #11. The frontend task-details issue #65 depends on this API.

## Phase 9: Deferred Read Models

Do not implement these until space/project/task data is stable:

- [ ] Dashboard aggregate endpoint.
- [ ] Global search.
- [ ] Activity timeline.
- [ ] Favourites.
- [ ] Notifications.

Favourites and notifications need schema design first because no current database
tables own that data.

## Per-Issue Completion Checklist

Before closing any backend issue:

- [ ] Flyway migration added when persistence changes.
- [ ] Entity and migration names/nullability agree.
- [ ] Request and response DTOs are defined or updated.
- [ ] Authorization is enforced from JWT email -> user -> membership.
- [ ] Unit tests cover business rules.
- [ ] Repository tests cover custom queries/constraints.
- [ ] Controller tests cover HTTP success, validation, unauthenticated, and
      unauthorized behavior.
- [ ] API contract document is updated.
- [ ] CI test gate passes before deployment.
