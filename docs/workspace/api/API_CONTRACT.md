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
| `GET /api/spaces/{spaceId}/members` | Implemented in #67 branch | Paginated active people and roles. |
| `POST /api/spaces/{spaceId}/members` | Implemented in #67 branch | Add an existing active user; no invitation. |
| `PATCH /api/spaces/{spaceId}/members/{memberId}` | Implemented in #67 branch | Change member role. |
| `DELETE /api/spaces/{spaceId}/members/{memberId}` | Implemented in #67 branch | Soft-delete membership. |
| `GET /api/spaces/{spaceId}/roles` | Implemented in #67 branch | Paginated active role catalogue. |
| `POST /api/spaces/{spaceId}/roles` | Required | Create a role. |
| `PATCH /api/spaces/{spaceId}/roles/{roleId}` | Required | Rename role or replace permission set. |
| `DELETE /api/spaces/{spaceId}/roles/{roleId}` | Required | Remove unused role. |
| `GET /api/spaces/{spaceId}/permissions` | Implemented in #67 branch | Paginated active permission catalogue. |

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
| `GET /api/spaces/{spaceId}/projects` | Implemented (#68 branch) | Project cards in Space Details. |
| `POST /api/spaces/{spaceId}/projects` | Implemented (#68 branch) | Create project. |
| `GET /api/projects/{projectId}` | Implemented (#68 branch) | Project Details header and overview. |
| `PATCH /api/projects/{projectId}` | Implemented (#68 branch) | Edit project name, description, sprint-cycle days. |
| `DELETE /api/projects/{projectId}` | Implemented (#68 branch) | Soft-delete project. |
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

Issue #45 uses `project_workflows` as the ordered board-column table. V26 adds
`work_items.workflow_id`, project-scoped referential integrity, and zero-based
positions. Project insertion provisions Backlog, To Do, In Progress, In Review,
Done and Blocked. Existing custom stages retain their IDs and are ordered by
createdAt then UUID; projects without active stages receive the six defaults.
Existing tasks enter the first active stage, ordered by createdAt then UUID.

Stage DTO: `{id,name,icon,position,complete}`. Completion is the explicit
`is_complete` classification, never the display name or an actual end date.
Only the provisioned Done stage starts complete; legacy custom stages default
to false and must be classified explicitly. Renaming a stage preserves completion.
Project metrics in #68 must consume this flag; changing it reclassifies all tasks
in that stage.

Reads require active account, membership, role and ancestors. Stage writes and
moves require the existing `space.update` grant (no new implicit role grants).
Unknown, deleted or foreign project/stage/item IDs return 404; a visible resource
without the write grant returns 403. Errors follow the JSON contract, including 401.

Create accepts `{name,icon?,position?,complete?}`; PATCH accepts the same optional
fields. Name is trimmed and 1–100 characters; icon is at most 255 characters
(empty string clears it); complete is boolean; position is a nonnegative integer.
Omitted PATCH fields stay unchanged. Explicit null, unknown fields and scalar
type coercions return 400. Maximum 100 active stages; exceeding it returns
409 STAGE_LIMIT. Delete is soft: nonempty stages return 409 STAGE_NOT_EMPTY and
the final active stage returns 409 LAST_STAGE. Historical deleted tasks retain
their stage reference.

| Method and path | Status | Purpose |
| --- | --- | --- |
| `GET /api/projects/{projectId}/workflow-stages` | Implemented | 200 ordered stage array. |
| `POST /api/projects/{projectId}/workflow-stages` | Implemented | 201 created stage DTO. |
| `PATCH /api/projects/{projectId}/workflow-stages/{stageId}` | Implemented | 200 updated stage DTO. |
| `DELETE /api/projects/{projectId}/workflow-stages/{stageId}` | Implemented | 204; remove an empty non-final stage. |
| `GET /api/projects/{projectId}/work-item-types` | Implemented (#69) | Active `{id,name,icon}` array ordered by name, UUID. V27 provisions Task when a project has no active types. |
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
| `GET /api/projects/{projectId}/work-items` | Implemented (#69) | AND filters `stageId`, `typeId`, `assigneeMemberId`, `parentId`, `q`; optional stage grouping and bounded pagination. |
| `POST /api/projects/{projectId}/work-items` | Implemented (#69) | 201 detail DTO and Location header; create task or child. |
| `GET /api/work-items/{workItemId}` | Implemented (#69) | 200 task detail DTO. |
| `PATCH /api/work-items/{workItemId}` | Implemented (#69) | 200 detail DTO; edit metadata and relationships. Sprint fields unsupported. |
| `DELETE /api/work-items/{workItemId}` | Implemented (#69) | 204 soft deletion; 409 TASK_HAS_CHILDREN when active direct children remain. |
| `POST /api/work-items/{workItemId}/move` | Implemented | 200 moved BoardWorkItem DTO; atomic stage/position change. |

BoardWorkItem is `{id,projectId,title,stageId,stageName,complete,position,typeId,typeName,parentId,assigneeMemberId,assigneeName,assigneeActive}`.
Type/parent/assignment fields can be null for historical/unassigned tasks.
Assignee identity is a **space membership UUID**, never a user UUID. Historical
assignees retain their name and ID with `assigneeActive=false` when membership,
user or role becomes inactive. No passwords, emails or entity graphs are exposed.
Detail adds `spaceId,description,efforts,plannedStartDate,plannedEndDate,actualStartDate,actualEndDate,createdAt,updatedAt`.
Clients use task UUIDs for navigation; there is no project key/issue number.
The GET response is `{items,page,size,totalItems,totalPages,groups}`; page defaults
to 0 (0–100000), size to 25 (1–100). Sort is fixed: stage position, item position, UUID.
With `groupBy=stage`, groups are ordered `{stage,items}` objects, including empty
columns, and contain only items on the current page. Totals describe the full
filtered result; grouping does not bypass pagination. Without grouping, groups
is empty. Deleted items/stages and descendants of deleted tasks are excluded. Other groupBy values return 400.
The optional stageId must identify an active stage of the requested project.
`q` is trimmed, literal case-insensitive title/description search (maximum 100
characters). Type/member/parent UUID filters are scoped by the project query and
can match historical references; unrelated IDs return an empty result. Children
are discovered using `parentId`, with the same paging limits. Unknown query/body
fields, including `sprintId` and the ambiguous `assigneeId`, return 400.

Create/PATCH/delete require `space.update`; reads require active account,
membership and role. Invisible resources/ancestors return 404, missing grants
403 and inactive/missing authentication 401. Relationships on writes require
an active same-project type/stage/parent and active same-space assignee. Cycles,
invalid relationships and malformed fields return 400 VALIDATION_ERROR.

Create requires trimmed nonblank title (maximum 255 characters); description is
nullable (maximum 500), efforts is a nonnegative integer (default 1). Omitted
type/stage select the first active type by name/UUID and stage by position/UUID.
V27 provisions Task for existing and newly inserted projects without active
types; it does not retype historical tasks or replace existing custom types.
PATCH omission preserves fields. Null clears description, parentId,
assigneeMemberId and dates; title/efforts/typeId/stageId cannot be cleared.
Dates are ISO-8601 instants in years 0001–9999; when both endpoints exist, end
must not precede start. Planned and actual ranges are checked independently,
including against preserved PATCH values. Sprint relations are not mapped and
are explicitly unsupported in this revision. Comments/uploads remain #11/#28.

Move position is the zero-based destination index after removing the moving
item. Omission appends and oversized values clamp to the end. Both affected
columns become contiguous; same-column moves use the same rule. Successful
requests serialize on space then project rows before rereading board data.
Concurrent requests apply in lock-acquisition order; repeated moves to the same
explicit position preserve order. There is no optimistic stale-board rejection.
Create/delete/reparent/PATCH share these locks. Stage PATCH appends to the new
column; stage PATCH and delete compact the source column. Deleting a parent
requires removing or reparenting its active direct children first; soft-deleted
tasks and their historical comments/attachments remain stored.

```json
// CreateWorkItemRequest
{
  "title": "Create and set up navbar component",
  "description": "...",
  "typeId": "uuid",
  "parentId": null,
  "assigneeMemberId": "uuid",
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

Task writes verify scoped relationships under the space/project locks before
committing; concurrent parent changes cannot create a cycle. No sprint
assignment or fabricated issue numbering is accepted.

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
2. Consume the #45 stage/move contract when building Project Details drag-and-drop.
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
- Project board columns are supported by V26; general task CRUD remains #69.
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
loading descendants. #68 implements project CRUD; #69 supplies remaining work-item CRUD.

PATCH is included in the CORS method allow-list. Configured frontend origins may preflight authenticated metadata updates; untrusted origins remain rejected.

## Issue #67 membership contract (implemented on issue branch)

`memberId` is the membership UUID, never the user UUID. All six endpoints resolve
JWT email to an active user, space, membership, and same-space active role. Missing
or deleted callers return 401; invisible spaces and scoped resources return 404.
Authorized callers with insufficient action/role grants receive 403. Errors use
`{code,message,fieldErrors}`. Existing authentication text and GET spaces arrays
remain compatible.

| Action | Permission | Success |
| --- | --- | --- |
| GET members, roles, permissions | `space.members.read` | 200 |
| POST members | `space.members.add` | 201 |
| PATCH membership | `space.members.change-role` | 200 |
| DELETE membership, including self | `space.members.remove` | 204 |

POST body is `{email,roleId}`. Email is trimmed, syntactically validated (maximum
255 characters), and matched exactly to an existing active account, consistently
with current authentication. There is no invitation, outgoing email or pending
membership. Unknown/deleted accounts return the same 404. PATCH accepts only
`{roleId}`; missing/null/invalid role IDs and unknown fields return 400. Role and
member IDs must belong to the selected space. The caller's effective grants must
contain every active grant of the requested role and, for update/removal, of the
member's current role. Knowing an ID cannot grant authority.

Member results are `{id,user:{id,name,email},role:{id,name}}`; passwords and other
account fields are excluded. Member lists use the standard page envelope. Defaults
are page=0, size=25, sort=name,asc, q empty. Size is 1..100, page 0..100000; q is at
most 100 characters and performs case-insensitive literal substring matching on
name/email (percent/underscore are not wildcards). Sort accepts name, email, role,
createdAt, updatedAt with asc/desc; membership UUID ascending breaks ties. Invalid
bounds/sorts return 400. Deleted users, roles, spaces and memberships are excluded
from both items and totals. User/role fetches are bounded per page, not per item.
Roles/permissions accept the same page/size bounds, ordered by name then UUID;
items are `{id,name}`. The permissions endpoint is the space catalogue, not an
assertion that the caller has every listed grant.

An administrator is an active user/membership/role with all three legacy effective
grants: space.update, space.delete, space.members.manage. Names do not establish
administrator status. Removing/demoting the final administrator returns
409 LAST_ADMINISTRATOR. Self-removal and self-demotion follow the same permissions,
grant-subset, and last-administrator checks. Mutations serialize on the space row
before rechecking caller access; two concurrent requests cannot remove/demote the
last administrator. Unknown/deleted targets return 404. Roles and users cannot be
edited through this API; future deletion/grant-edit APIs must coordinate this lock.

Active duplicate membership returns 409 DUPLICATE_MEMBERSHIP, even if the active
row has a deleted role. Rejoining a soft-deleted membership returns 201 and restores
its original UUID/createdAt with the explicitly requested active role. The unique
(user_id,space_id) constraint is retained and concurrent add/rejoin requests produce
one success and one 409. Removal updates only membership deletion/timestamp;
historical task assignees and comments remain attached to that UUID and confer no
access. Future assignment APIs must offer only active members and display inactive
historical assignees appropriately. Reactivation restores access under the new role.

V25 adds the four action permissions to existing active spaces and grants them to
active roles with space.members.manage. It does not revive deleted grants. New
space bootstrap creates Administrator with all seven catalogue grants and Member
with read only. Member provides a safe role for selectors; custom role editing and
invitations remain future work. Existing spaces retain their roles. Project #68 now consumes the merged #45 completion semantics.

## Project CRUD and summaries (#68)

Implemented on `issue/68_project-crud`; PR review/merge pending. ProjectSummary is
returned by create/read/update and in the standard project-list page envelope.
POST returns 201 with Location; reads/PATCH return 200; DELETE returns 204.
Active JWT-email account, space, membership and same-space role are required.
Reading requires membership; create/PATCH require `space.update`; delete requires
`space.delete`. No new implicit role grants are introduced. Missing/deleted/foreign
resources return 404, missing/inactive authentication 401, insufficient grants 403,
and invalid input 400 using `{code,message,fieldErrors}`. Legacy auth text and
GET spaces array responses remain unchanged.

Create/PATCH accept only `name`, `description`, `sprintCycleDays`. Name is trimmed,
nonblank, at most 255 Unicode characters and required for create. Description is
at most 500 characters; explicit null clears it. Sprint cycle must be a positive
32-bit integer and defaults to 7 on create; null is invalid when supplied. Legacy
null cycles remain readable. PATCH omission preserves values, empty PATCH preserves
timestamps, and ownership/audit/spaceId fields are rejected. No reparenting is allowed.

List defaults: page=0, size=25, sort=name,asc, q empty. Page range 0..100000, size
1..100, q at most 100 Unicode characters. Search is a trimmed case-insensitive
literal substring of name or description; `%` and `_` are literal. Sort accepts
name, createdAt, updatedAt with asc/desc; UUID ascending always breaks ties.
Totals exclude deleted projects/spaces. Metadata page, optional count and one
aggregate query supply results without per-project child loads. Reads use a
repeatable-read snapshot for authorization, metadata, totals and aggregates.

Metric denominator: every active work-item row in an active stage of the project,
including parents, epics and children once each. This is item completion, not effort
or leaf-only completion. Type/assignee deletion does not delete the item; neither
relationship changes the denominator. Completion uses only the stage `is_complete`
flag from #45, never its name or actualEndDate. Deleted items/stages/projects/spaces
are excluded. progressPercent is 100 * completed / total rounded to nearest integer
(half up); no items means 0. openTaskCount is total minus completed. Stage changes
are reflected on the next read. updatedAt describes project metadata, not task activity.

Project deletion retains stored descendants but makes all existing board, stage and
work-item move/read paths inaccessible. Mutations lock the space before the project,
coordinating with membership changes, space deletion and board writes. V26 provisions
the default six-stage board transactionally when the project is created.

Project detail/PATCH/DELETE verify account activity before resolving ownership,
so inactive accounts receive 401 for both existing and unknown project IDs.
