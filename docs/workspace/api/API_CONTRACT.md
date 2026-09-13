# Kheera API Contract

## Status

This is the working API contract for building Kheera's Angular frontend and
Spring Boot backend together. It is based on:

- the backend controllers, DTOs, JPA entities, and Flyway migrations;
- the frontend routes and completed login/registration flow; and
- the Penpot draft reviewed on 2026-09-11.

The Penpot file is a visual and flow draft, not a complete functional
specification. It establishes the intended look and feel: clean desktop
workspace UI, white surfaces, light teal accents, compact controls, a global
top bar, and a left space/project navigation. It does not yet define every
state, field, permission rule, responsive view, filter, or failure state.

**Contract labels**

- **Implemented**: endpoint exists in the current Java backend.
- **Required**: endpoint the frontend needs to realise a drafted screen.
- **Design decision**: requires product/schema agreement before implementation.

## Product Flow Inferred from Penpot

```text
Landing -> Get Started -> Login
                         -> Forgot password
                         -> Signup: email -> OTP -> profile/password -> dashboard

Authenticated app
  -> Dashboard: favourites, spaces, project overview, activity
  -> Space details: project list and space context
  -> Project details: progress, work board, epics, activity
  -> Task details: task title, state, description, metadata and updates
```

### Drafted boards reviewed

| Board | Intended purpose | Contract impact |
| --- | --- | --- |
| Landing Board | Entry point and Get Started action | No authenticated API required. |
| Login Board | Email/password login, forgotten-password route | Auth login and password-reset endpoints. |
| SignUp Board 1/3 | Enter email and request OTP | Signup OTP endpoint. |
| SignUp Board 2/3 | Enter/resend OTP | OTP validation and resend behavior. |
| SignUp Board 3/3 | Name, password, confirmation | Account creation endpoint. |
| Dashboard | Favourite spaces, spaces, project summaries, activity, global search/create/alerts | Dashboard aggregate, spaces, search, favourites, notifications. |
| Space Details | A space and its projects | Space detail, project list, space management. |
| Project Details | Project overview, task columns, epics, activity | Project detail, work-item board, types, workflow states, activity. |
| Task Details | Inspect/edit a task | Work-item detail, update, comments, attachments, assignment. |

## API-Wide Rules

### Base URL and transport

- Base path: `/api`.
- Production traffic must use HTTPS.
- JSON request and response bodies use `Content-Type: application/json`.
- UUIDs are lowercase UUID strings.
- Date-times are ISO-8601 UTC strings, for example
  `2026-09-11T12:30:00Z`.
- All authenticated calls send `Authorization: Bearer <accessToken>`.

### Response convention for new endpoints

The current authentication endpoints return raw text. Preserve that behaviour
until they are intentionally versioned or migrated; the frontend already
handles their text responses.

All **new** product endpoints should return JSON and errors should share one
shape:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "A human-readable explanation.",
  "fieldErrors": {
    "name": "Name is required."
  },
  "traceId": "optional-request-id"
}
```

Use these HTTP outcomes consistently:

| Status | Meaning |
| --- | --- |
| `200` | Successful read or update. |
| `201` | Resource created. Return the created resource. |
| `204` | Successful deletion with no response body. |
| `400` | Invalid request structure or validation failure. |
| `401` | Missing, invalid, or expired access token. |
| `403` | Authenticated user lacks space/project permission. |
| `404` | Resource does not exist or is not visible to this user. |
| `409` | Duplicate or conflicting state, such as a duplicate membership. |
| `422` | Well-formed request that fails a domain rule. |

### Pagination and filtering

List endpoints that can grow should accept:

```text
?page=0&size=25&sort=updatedAt,desc&q=search-text
```

and return:

```json
{
  "items": [],
  "page": 0,
  "size": 25,
  "totalItems": 0,
  "totalPages": 0
}
```

Small, bounded configuration lists such as project workflow stages and work
item types may return JSON arrays directly.

### Authorization rule

The authenticated JWT currently carries the user's **email** as its subject.
Server code must resolve that email to a user before querying memberships.
Do not trust a user ID supplied by the browser for authorization decisions.

Every space, project, sprint, work item, comment, and attachment request must
verify that the caller has a membership in the relevant space. Role permissions
should then decide whether the action is allowed.

## Existing Backend Surface

### Implemented authentication endpoints

These endpoints are available now. Their success and error bodies are plain
text, not JSON.

| Method and path | Auth | Request body | Success | Known error behaviour |
| --- | --- | --- | --- | --- |
| `POST /api/auth/login` | Public | `{ "email", "password" }` | `200`, raw JWT text | `401` `Invalid Email Or Password`; soft-deleted users cannot authenticate |
| `POST /api/auth/signup-email` | Public | `{ "email" }` | `200`, confirmation text | `400` `Email already registered!` |
| `POST /api/auth/otp-validation` | Public | `{ "email", "otp": 123456 }` | `200`, `Valid OTP, Proceed` | `400` `Invalid Or Expired OTP` |
| `POST /api/auth/signup` | Public | `{ "email", "password", "name", "otp" }` | `200`, raw JWT text | `400`, including duplicate email or invalid OTP |
| `POST /api/auth/forgot-password` | Public | `{ "email" }` | `200`, generic OTP-sent text | Does not reveal whether an account exists |
| `POST /api/auth/reset-password` | Public | `{ "email", "password", "otp" }` | `200`, raw JWT text | `400` with service error text; soft-deleted users cannot reset passwords |

The Angular login and three-step registration pages already use the first four
endpoints. The frontend must display raw backend error text when present,
particularly `Email already registered!`.

### Implemented user and utility endpoints

| Method and path | Auth | Response | Notes |
| --- | --- | --- | --- |
| `GET /api/users/me` | Bearer expected | `{ "id", "name", "email" }` | Security configuration currently permits the path but it returns `401` without a valid authentication context. It should remain a protected frontend call. |
| `GET /api/users` | Bearer | `UserResponse[]` | Currently returns all users. Do not use as an unrestricted people directory in production without pagination and membership scoping. |
| `GET /api/spaces` | Bearer | `SpaceListDto[]` with `id`, `name` | Resolves the JWT email subject to active space memberships and excludes soft-deleted users, memberships, and spaces. |
| `GET /api/health` | Public | Raw `Server is Healthy` text | Infrastructure health check. |
| `GET /api/weather` | Bearer | Raw weather text | Development/demo endpoint, not part of Kheera product contract. |
| `POST /api/weather` | Bearer | Raw string body | Development/demo endpoint, not part of Kheera product contract. |

`/api/projects` has a controller base path but no endpoints yet.

## Required Frontend APIs

### Browser origin policy

Credentialed cross-origin requests require an exact origin in
`app.cors.allowed-origins`, configured through the comma-separated
`CORS_ALLOWED_ORIGINS` environment variable. Standalone local startup defaults
to `http://localhost:4200`. Production Compose defaults to
`https://kheera.theknightdevelopers.online`; development Compose defaults to
`https://dev.kheera.theknightdevelopers.online`. An untrusted preflight receives
403 without allow-origin or allow-credentials headers. Wildcard origins are
invalid when credentials are enabled.

The following endpoints are the recommended contract for the drafted screens.
They are not implemented unless marked otherwise.

### 1. Identity, profile, and session

| Method and path | Status | Purpose | Request / response summary |
| --- | --- | --- | --- |
| `GET /api/users/me` | Implemented | Restore session and populate top bar/profile | Return `UserSummary`. |
| `PATCH /api/users/me` | Required | Update name/profile preferences | `{ "name", "profilePic" }` -> `UserSummary`. |
| `POST /api/uploads` | Required | Upload profile or attachment file | Multipart upload -> `{ "url", "name", "contentType", "size" }`. |
| `POST /api/auth/logout` | Optional | Audit/revoke session if refresh-token support is added | Current JWT-only frontend can simply clear local storage. |

```json
// UserSummary
{
  "id": "uuid",
  "name": "Shaik Abid Hussain",
  "email": "abid@example.com",
  "profilePic": "https://.../profile.png"
}
```

### 2. Dashboard, search, favourites, and notifications

| Method and path | Status | Purpose |
| --- | --- | --- |
| `GET /api/dashboard?spaceId={uuid}` | Required | One efficient initial dashboard payload: sidebar spaces, favourite spaces/projects, recent projects, task counts, and activity preview. |
| `GET /api/search?q={text}&spaceId={uuid?}&types=space,project,workItem` | Required | Global search from the top navigation. |
| `GET /api/favourites` | Design decision | List favourite spaces/projects for the left sidebar. |
| `PUT /api/favourites/{resourceType}/{resourceId}` | Design decision | Add a favourite. |
| `DELETE /api/favourites/{resourceType}/{resourceId}` | Design decision | Remove a favourite. |
| `GET /api/notifications?unreadOnly=true` | Design decision | Notification badge and notification panel. |
| `PATCH /api/notifications/{notificationId}` | Design decision | Mark notification read. |

`favourites` and `notifications` have no current database tables. Do not build
the frontend as though those calls exist until their persistence and retention
rules are designed.

Suggested dashboard response:

```json
{
  "activeSpace": { "id": "uuid", "name": "The Knight Developers" },
  "spaces": [{ "id": "uuid", "name": "The Knight Developers", "isFavourite": true }],
  "recentProjects": [{ "id": "uuid", "name": "Kheera - Project Management", "progressPercent": 10, "openTaskCount": 40 }],
  "activity": []
}
```

### 3. Spaces, memberships, roles, and permissions

| Method and path | Status | Purpose |
| --- | --- | --- |
| `GET /api/spaces` | Implemented | Sidebar list of spaces visible to the current user. |
| `POST /api/spaces` | Implemented | Create a space from the global Create action. |
| `GET /api/spaces/{spaceId}` | Implemented | Load the Space Details page. |
| `PATCH /api/spaces/{spaceId}` | Implemented | Rename/edit description/profile picture. |
| `DELETE /api/spaces/{spaceId}` | Implemented | Soft-delete a space, permission restricted. |
| `GET /api/spaces/{spaceId}/members` | Required | People and roles view. |
| `POST /api/spaces/{spaceId}/members` | Required | Add/invite an existing user to a space. |
| `PATCH /api/spaces/{spaceId}/members/{memberId}` | Required | Change member role. |
| `DELETE /api/spaces/{spaceId}/members/{memberId}` | Required | Remove member. |
| `GET /api/spaces/{spaceId}/roles` | Required | Role configuration. |
| `POST /api/spaces/{spaceId}/roles` | Required | Create a role. |
| `PATCH /api/spaces/{spaceId}/roles/{roleId}` | Required | Rename role or replace permission set. |
| `DELETE /api/spaces/{spaceId}/roles/{roleId}` | Required | Remove unused role. |
| `GET /api/spaces/{spaceId}/permissions` | Required | Permission catalogue. |

Suggested create/update payload:

```json
{
  "name": "The Knight Developers",
  "description": "Product engineering workspace",
  "profilePic": "https://.../space.png"
}
```

### 4. Projects and project links

| Method and path | Status | Purpose |
| --- | --- | --- |
| `GET /api/spaces/{spaceId}/projects` | Required | Project cards in Space Details. |
| `POST /api/spaces/{spaceId}/projects` | Required | Create project. |
| `GET /api/projects/{projectId}` | Required | Project Details header and overview. |
| `PATCH /api/projects/{projectId}` | Required | Edit project name, description, sprint-cycle days. |
| `DELETE /api/projects/{projectId}` | Required | Soft-delete project. |
| `GET /api/projects/{projectId}/urls` | Required | Project links. |
| `POST /api/projects/{projectId}/urls` | Required | Add project link. |
| `PATCH /api/projects/{projectId}/urls/{urlId}` | Required | Edit project link. |
| `DELETE /api/projects/{projectId}/urls/{urlId}` | Required | Remove project link. |
| `GET /api/projects/{projectId}/activity` | Required | Project activity panel/timeline. |

```json
// ProjectSummary
{
  "id": "uuid",
  "spaceId": "uuid",
  "name": "Kheera - Project Management",
  "description": "...",
  "sprintCycleDays": 14,
  "progressPercent": 10,
  "openTaskCount": 40,
  "updatedAt": "2026-09-11T12:30:00Z"
}
```

### 5. Board columns, work-item types, and sprints

The draft project board displays columns such as Backlog, To Do, In Progress,
In Review, Done, and Blocked. The API and data model must have a concrete field
representing a work item's current board column.

The current ER draft suggests `work_items.workflow_id`, but that column is not
implemented in Flyway or JPA. Treat the following as a **design decision**:

- Option A: use `project_workflows` as the board-column table and add
  `work_items.workflow_id` as a UUID foreign key.
- Option B: introduce a dedicated `project_workflow_stages` / `work_item_status`
  table and reference that from work items.

Option B is clearer if a workflow can contain multiple ordered stages. Choose
one before building drag-and-drop board behavior.

| Method and path | Status | Purpose |
| --- | --- | --- |
| `GET /api/projects/{projectId}/workflow-stages` | Design decision | Ordered board columns. |
| `POST /api/projects/{projectId}/workflow-stages` | Design decision | Create column. |
| `PATCH /api/projects/{projectId}/workflow-stages/{stageId}` | Design decision | Rename/reorder column. |
| `DELETE /api/projects/{projectId}/workflow-stages/{stageId}` | Design decision | Remove column after handling its work items. |
| `GET /api/projects/{projectId}/work-item-types` | Required | Type selector and epic/task grouping. |
| `POST /api/projects/{projectId}/work-item-types` | Required | Create type. |
| `PATCH /api/projects/{projectId}/work-item-types/{typeId}` | Required | Edit type name/icon. |
| `DELETE /api/projects/{projectId}/work-item-types/{typeId}` | Required | Remove unused type. |
| `GET /api/projects/{projectId}/sprint-statuses` | Required | Sprint status configuration. |
| `GET /api/projects/{projectId}/sprints` | Required | Sprint selector and planning. |
| `POST /api/projects/{projectId}/sprints` | Required | Create sprint. |
| `GET /api/sprints/{sprintId}` | Required | Sprint detail. |
| `PATCH /api/sprints/{sprintId}` | Required | Update dates/name/status. |
| `DELETE /api/sprints/{sprintId}` | Required | Soft-delete sprint. |

### 6. Work items and task details

| Method and path | Status | Purpose |
| --- | --- | --- |
| `GET /api/projects/{projectId}/work-items` | Required | Board/list/epic view. Support `stageId`, `typeId`, `sprintId`, `assigneeId`, `parentId`, `q`, and pagination filters. |
| `POST /api/projects/{projectId}/work-items` | Required | Create task, epic, or child work item. |
| `GET /api/work-items/{workItemId}` | Required | Task Details screen. |
| `PATCH /api/work-items/{workItemId}` | Required | Update title, description, dates, effort, parent, type, assignee, sprint, and board stage. |
| `DELETE /api/work-items/{workItemId}` | Required | Soft-delete a work item. |
| `POST /api/work-items/{workItemId}/move` | Required | Explicit drag-and-drop board move with optional ordering position. |

```json
// CreateWorkItemRequest
{
  "title": "Create and set up navbar component",
  "description": "...",
  "workItemTypeId": "uuid",
  "parentItemId": null,
  "assigneeMemberId": "uuid",
  "sprintId": "uuid",
  "stageId": "uuid",
  "efforts": 3,
  "plannedStartDate": "2026-09-14T00:00:00Z",
  "plannedEndDate": "2026-09-18T00:00:00Z"
}
```

```json
// MoveWorkItemRequest
{
  "stageId": "uuid",
  "position": 2
}
```

The backend must verify that the type, sprint, stage, parent, and assignee all
belong to the same project/space as the work item. It must also reject a parent
relationship that creates a hierarchy cycle.

### 7. Comments and attachments

| Method and path | Status | Purpose |
| --- | --- | --- |
| `GET /api/work-items/{workItemId}/comments` | Required | Task activity/comments timeline. |
| `POST /api/work-items/{workItemId}/comments` | Required | Add comment. Author comes from JWT membership, not request body. |
| `PATCH /api/comments/{commentId}` | Required | Edit a comment, restricted to author/privileged role. |
| `DELETE /api/comments/{commentId}` | Required | Soft-delete comment. |
| `GET /api/work-items/{workItemId}/attachments` | Required | Task attachments. |
| `POST /api/work-items/{workItemId}/attachments` | Required | Associate previously uploaded asset metadata. |
| `DELETE /api/work-item-attachments/{attachmentId}` | Required | Remove task attachment. |
| `POST /api/comments/{commentId}/attachments` | Required | Associate attachment with comment. |
| `DELETE /api/comment-attachments/{attachmentId}` | Required | Remove comment attachment. |

```json
// CreateCommentRequest
{ "description": "Navbar behavior is ready for review." }

// AttachExistingUploadRequest
{
  "name": "navbar-spec.pdf",
  "url": "https://storage.example/.../navbar-spec.pdf"
}
```

## Frontend Route Plan

The current Angular routes cover `/`, `/login`, `/register`, `/profile`, and
`/settings`. To represent the drafted workspace flow, add routes only after the
matching APIs are available:

| Route | Required data |
| --- | --- |
| `/` | `GET /dashboard` |
| `/spaces/:spaceId` | Space detail and project list |
| `/projects/:projectId` | Project detail, work-item board, activity |
| `/work-items/:workItemId` | Work item detail, comments, attachments |
| `/profile` | `GET/PATCH /users/me` |
| `/settings` | User settings plus any future notification preferences |

Use route resolvers or component loading states for data-dependent pages. Every
screen needs empty, loading, forbidden, and recoverable-error states even
though the Penpot drafts do not show them yet.

## Implementation Order

1. Complete `GET/PATCH /api/users/me`, spaces, project CRUD, and the dashboard
   read model.
2. Decide and migrate the work-item board-stage relationship before building
   Project Details drag-and-drop.
3. Implement work-item CRUD, member assignment, comments, and attachment upload.
4. Add search, activity, favourites, and notifications once their persistence
   models are agreed.
5. Publish this contract as OpenAPI/Swagger and generate or validate Angular
   client types from it. Avoid hand-maintaining duplicate request interfaces.

## Known Contract Risks

- Current auth success/error bodies are raw text, while new endpoint guidance is
  JSON. Do not silently mix response parsing strategies in the frontend.
- OTP expiry is stored in the database but must be enforced by the verification
  service for the contract claim "expired OTP" to be true.
- Project board columns shown in Penpot are not supported by the current schema.
- Favourites and notifications shown in the UI have no storage model yet.
- `GET /api/users` currently exposes all users to any authenticated caller;
  people search should be scoped to the current space.
- Soft deletes require every read endpoint to exclude `is_deleted = true` unless
  an administrative restore/audit use case intentionally includes them.

## Source Material Reviewed

- Penpot file: `Kheera`, Landing Page canvas; boards listed in the Drafts panel.
- Backend controllers in
  `Kheera-Backend/src/main/java/com/knightdevelopers/kheerabackend/controller`.
- Backend DTOs, JWT filter, JPA entities, and Flyway migrations.
- Frontend routing and API/auth services in `Kheera-Frontend/src/app`.

## Space Lifecycle Contract (#66)

Implemented on the issue branch; awaiting PR review/merge. The existing
`GET /api/spaces` response stays an array of `{id,name}`.

- `POST /api/spaces` returns 201, a relative Location header, and SpaceDetail.
  The active JWT-email user becomes the creator member; one transaction creates
  the space, Administrator role, permissions, grants and membership. Any failure
  rolls back the whole bootstrap.
- `GET /api/spaces/{spaceId}` returns 200 to an active member with an active role
  belonging to that space. Missing/deleted spaces and nonmembers receive 404.
- `PATCH /api/spaces/{spaceId}` requires `space.update` and returns 200.
- `DELETE /api/spaces/{spaceId}` requires `space.delete` and returns 204. It marks
  only the space deleted, retaining projects, work items and bootstrap records.
  Subsequent reads/writes receive 404; it disappears from the caller's list.
- Missing/invalid/expired JWTs or inactive accounts receive 401. An active member
  without the action grant receives 403. Lifecycle failures use `{code,message,
  fieldErrors}`; malformed UUIDs/JSON and unknown fields receive 400. Legacy auth
  responses remain raw text. traceId remains optional and is not currently sent.

SpaceDetail is `{id,name,description,profilePic,createdAt,updatedAt,capabilities}`.
Capabilities is `{canUpdate,canDelete,canManageMembers}` and reflects active grants
for the caller. `name` maps to `Spaces.spaceName`; UUIDs and UTC ISO-8601 timestamps
are returned. No JPA objects, member identities or credentials are exposed.

Input accepts only `name`, `description`, and `profilePic`. Name is required for
creation, trimmed, nonblank, at most 255 Unicode characters, and cannot be null.
Description is optional, at most 500 characters. profilePic is optional, at most
255 characters, and must be an absolute HTTP(S) URL with a host and no credentials.
The server stores the URL and does not fetch or upload images (#28).
PATCH omission preserves a value; explicit null clears description/profilePic;
null name is invalid. Empty PATCH preserves metadata and timestamps. Non-string
values and unknown fields (including owner, creator, roles, grants and audit
fields) are rejected, rather than used for authority.

Minimum per-space permission catalogue: `space.update`, `space.delete`, and
`space.members.manage`. Bootstrap grants all three to Administrator. Active
membership itself permits reading metadata. #67 must reuse `space.members.manage`
for membership mutations; role names alone never grant privileges. Permissions,
roles and grants must all be active and belong to the same space as membership.
Existing spaces receive no implicit role grants or backfill in this issue.

All future descendant services must call `SpaceAccessService.requireSpace` in
their transaction before accessing a project/task, resolving its actual parent
space from the database. For mutations, acquire the active-space write lock before
loading descendants. #68/#69 implement those HTTP APIs; they do not exist yet.
