# Kheera Backend Database Design Findings

## Purpose and Scope

This document records the database design implemented by the Kheera Java backend,
compares it with the `kheera_ER` dbdiagram.io model reviewed on 2026-09-11, and
identifies schema drift that should be resolved before using the diagram as an
implementation contract.

The Flyway SQL migrations under `Kheera-Backend/src/main/resources/db/migration`
are the source of truth for the database currently created by the backend. JPA
entities describe the runtime mapping, but a few mappings do not fully agree with
those migrations.

No database, server, or application files were changed while making these notes.

## Domain Intent

Kheera is structured as a collaborative project-workspace application.

```text
User
  -> SpaceMembership
      -> Space
          -> Project
              -> Work item / Sprint / Workflow / Project link / Work-item type
```

Access control is scoped to a space:

```text
User -> SpaceMember -> SpaceRole -> SpaceRolePermission -> SpacePermission
```

A work item can belong to a project, be a child of another work item, have a
type, be assigned to a member of the relevant space, and contain comments and
attachments.

## Conventions in the Implemented Schema

### Identifiers and audit columns

- All primary keys are UUIDs generated in PostgreSQL with `gen_random_uuid()`.
- The migration set enables the `pgcrypto` extension for UUID generation.
- Most collaborative-domain tables inherit the BaseEntity shape:
  `id`, `is_deleted`, `created_at`, and `updated_at`.
- `is_deleted` implements soft deletion. Queries must consistently filter it,
  otherwise soft-deleted records remain visible.
- The timestamps default in PostgreSQL when inserted. There is no database
  trigger shown to automatically refresh `updated_at` on every SQL update.

### Authentication and mail tables

`users`, `emails`, and `one_time_passwords` are separate from the workspace
domain. `users` holds application credentials and profile data. `emails` appears
to support queued or tracked outbound mail. `one_time_passwords` supports the
signup OTP flow.

## Implemented Table Inventory

### Identity and communication

| Table | Purpose | Important columns / rules |
| --- | --- | --- |
| `users` | Application identity | `email` is unique and non-null; `password` is non-null; optional `name`, `profile_pic`; plus audit/soft-delete fields. |
| `emails` | Outbound email record | Recipient, urgency, subject/body, send status, retry count, grouping, and timestamps. |
| `one_time_passwords` | OTP verification | Email, numeric OTP, retry count, expiry timestamp, audit/soft-delete fields. |

### Spaces, roles, and permissions

| Table | Purpose | Important relationships / rules |
| --- | --- | --- |
| `spaces` | Collaborative workspace | Owns roles, permissions, projects, and memberships. |
| `space_roles` | Space-local role definition | `space_id -> spaces.id`; role name is required. |
| `space_permissions` | Space-local permission definition | `space_id -> spaces.id`; permission name is required. |
| `space_members` | User membership in a space | `user_id -> users.id`, `space_id -> spaces.id`, `space_role_id -> space_roles.id`; unique `(user_id, space_id)`. |
| `space_role_permissions` | Role-to-permission join table | Links role and permission; unique `(space_role_id, space_permission_id)`. |

The membership table is the critical authorization boundary. A user can have only
one membership record per space and receives their role through that record.

### Projects and planning

| Table | Purpose | Important relationships / rules |
| --- | --- | --- |
| `projects` | A project within one space | `space_id -> spaces.id`; required project name; optional description and sprint-cycle length. |
| `project_sprint_status` | Allowed sprint statuses for a project | `project_id -> projects.id`; required `status_name`. |
| `project_urls` | External links associated with a project | `project_id -> projects.id`; `url`, `display_name`, and `icon`. |
| `project_workflows` | Workflow definitions for a project | `project_id -> projects.id`; `workflow_name` and optional icon. |
| `sprints` | Planned/executed sprint instances | `project_id -> projects.id`; optional `sprint_status_id -> project_sprint_status.id`. |

### Work management

| Table | Purpose | Important relationships / rules |
| --- | --- | --- |
| `work_item_types` | Per-project work-item classification | `project_id -> projects.id`; name required by migration; icon optional. |
| `work_items` | Project task / issue / hierarchical work item | `project_id -> projects.id`; optional self-reference `parent_item_id -> work_items.id`; optional type and assignee relationships in SQL. |
| `work_item_comments` | Comment made on a work item | `work_item_id -> work_items.id`; optional `space_member_id -> space_members.id`. |
| `work_item_attachments` | Attachment attached directly to a work item | `work_item_id -> work_items.id`; URL capacity is `VARCHAR(2048)`. |
| `work_item_comment_attachments` | Attachment attached to a comment | `work_item_comment_id -> work_item_comments.id`; URL capacity is `VARCHAR(2048)`. |

The work-item hierarchy allows arbitrary nesting because `parent_item_id` points
back to the same table. This makes subtask-like structures possible, but the
application should prevent cycles such as a work item becoming its own ancestor.

## Relationships as Implemented

```text
users 1 --- * space_members * --- 1 spaces
space_roles 1 --- * space_members
spaces 1 --- * space_roles
spaces 1 --- * space_permissions
space_roles 1 --- * space_role_permissions * --- 1 space_permissions

spaces 1 --- * projects
projects 1 --- * project_sprint_status
projects 1 --- * project_urls
projects 1 --- * project_workflows
projects 1 --- * sprints
projects 1 --- * work_item_types
projects 1 --- * work_items

work_items 1 --- * work_items              (parent / child)
work_item_types 1 --- * work_items
space_members 1 --- * work_items           (assignee)
work_items 1 --- * work_item_comments
space_members 1 --- * work_item_comments   (author)
work_items 1 --- * work_item_attachments
work_item_comments 1 --- * work_item_comment_attachments
```

## dbdiagram.io Comparison

The reviewed diagram captures the main collaboration hierarchy correctly. It
shows Users, Spaces, roles, permissions, memberships, projects, sprint data,
workflows, work items, comments, attachments, and work-item types.

It is currently a logical model, not an exact physical model of the backend.

### Diagram corrections needed

| Diagram item | Backend source of truth | Recommended diagram update |
| --- | --- | --- |
| Missing `emails` table | Flyway includes `emails` | Add it if the diagram intends to show the complete backend schema. |
| Missing OTP table | Flyway creates `one_time_passwords` | Add it if authentication/signup data belongs in scope. |
| Missing user password/audit fields | `users` includes `password`, `created_at`, `updated_at`, `is_deleted` | Include them or label the diagram as intentionally logical-only. |
| Missing audit fields on most domain tables | BaseEntity-backed migrations add them | Include `is_deleted`, `created_at`, and `updated_at` on each BaseEntity table. |
| `Space_Members.role_id` | Physical column is `space_role_id` | Rename the diagram column and relationship. |
| `Project_Sprint_Status.name` | Physical column is `status_name` | Rename it. |
| `Project_Urls.name` | Physical column is `display_name` | Rename it. |
| `Project_Workflows.name` | Physical column is `workflow_name` | Rename it. |
| `Sprints.status_id varchar` | `sprint_status_id UUID -> project_sprint_status.id` | Rename, change type to UUID, and retain the FK. |
| `Work_Items.workflow_id` | No migration or JPA mapping exists | Remove it from the diagram, or add the missing migration/entity relationship to the backend. |
| Comment `space_member_id` required | SQL and JPA permit it to be null | Make it nullable in the diagram, or tighten the backend deliberately. |
| Attachment URL length 255 | SQL uses `VARCHAR(2048)` | Update both attachment tables to `VARCHAR(2048)`. |
| Missing unique constraints | SQL enforces member and role-permission uniqueness | Annotate unique `(user_id, space_id)` and `(space_role_id, space_permission_id)`. |

## Backend Schema and ORM Drift

These are implementation inconsistencies independent of the dbdiagram model.

### 1. OTP table mapping name

The `OneTimePassword` entity uses `@Table(name = "OneTimePasswords")`, while
Flyway creates `one_time_passwords`. In PostgreSQL, these are different
unquoted identifiers after case folding. The entity should map to
`one_time_passwords` unless a separate table was intentionally created.

### 2. Work-item type nullability

The migration adds `work_items.work_item_type_id` as nullable. The `WorkItems`
entity declares `@JoinColumn(... nullable = false)`. New schema creation and
runtime behavior can therefore disagree. Choose one rule:

- Required type: backfill existing data and add a migration setting the column
  to `NOT NULL`.
- Optional type: change the JPA mapping and service validation to permit null.

### 3. Work-item type project nullability

The migration permits `work_item_types.project_id` to be null. The
`WorkItemTypes` JPA relationship says it is non-null. Again, choose one rule and
make Flyway and JPA agree.

### 4. Workflow relationship is designed but unimplemented

The diagram indicates `work_items.workflow_id -> project_workflows.id`, but the
backend table and entity do not include it. This is a meaningful product choice:
the project can define workflows, but a work item cannot currently be assigned
to one. Implement it only if workflows are intended to drive work-item state.

## Design Strengths

- Authorization is scoped through `space_members`, preventing roles from being
  global by default.
- The two join-table uniqueness constraints protect important duplicate cases.
- Project-owned status, workflow, and work-item type definitions support
  customization by project.
- UUID primary keys work well for distributed clients and opaque API IDs.
- Soft deletion preserves records for recovery/audit purposes.

## Recommended Next Steps

1. Decide whether the dbdiagram model will be logical-only or the physical
   schema contract. If it is the contract, synchronize it with Flyway first.
2. Correct the OTP table entity mapping before relying on it in fresh or
   validated deployments.
3. Resolve the two nullability differences between migrations and JPA.
4. Decide whether a work item needs a workflow foreign key; update both the
   migration and JPA entity together if yes.
5. Add indexes for frequently queried foreign keys and soft-delete filters after
   measuring application queries. PostgreSQL does not automatically create
   indexes for every foreign key.
6. Document deletion behavior for each relationship. No explicit `ON DELETE`
   actions are defined in the migrations, so PostgreSQL defaults to restrictive
   behavior and the application must orchestrate dependent soft deletions.

## Source Files Reviewed

- `Kheera-Backend/src/main/resources/db/migration/V001__Initial_schema.sql`
- `Kheera-Backend/src/main/resources/db/migration/V0004__add_is_deleted_and_profile_pic_to_users.sql`
- `Kheera-Backend/src/main/resources/db/migration/V0007__add_spaces_table.sql`
- `Kheera-Backend/src/main/resources/db/migration/V0008__add_space_roles_table.sql`
- `Kheera-Backend/src/main/resources/db/migration/V0009__add_space_permissions_table.sql`
- `Kheera-Backend/src/main/resources/db/migration/V0010__add_projects_table.sql`
- `Kheera-Backend/src/main/resources/db/migration/V0011__add_space_members_table.sql`
- `Kheera-Backend/src/main/resources/db/migration/V0013__add_space_role_permissions_table.sql`
- `Kheera-Backend/src/main/resources/db/migration/V0014__add_project_sprint_status.sql`
- `Kheera-Backend/src/main/resources/db/migration/V0015__add_project_urls.sql`
- `Kheera-Backend/src/main/resources/db/migration/V0016__add_projectWorkflows.sql`
- `Kheera-Backend/src/main/resources/db/migration/V0017__add_workitems.sql`
- `Kheera-Backend/src/main/resources/db/migration/V0018__add_work_item_types.sql`
- `Kheera-Backend/src/main/resources/db/migration/V0019__add_work_item_type_to_workitems.sql`
- `Kheera-Backend/src/main/resources/db/migration/V0020__add_assigned_to_id_to_workitems.sql`
- `Kheera-Backend/src/main/resources/db/migration/V0021__add_sprints.sql`
- `Kheera-Backend/src/main/resources/db/migration/V22__add_workitem_comments.sql`
- `Kheera-Backend/src/main/resources/db/migration/V0023__add_work_item_attachments.sql`
- `Kheera-Backend/src/main/resources/db/migration/V0024__add_work_item_comments_attachments.sql`
- JPA entities under `Kheera-Backend/src/main/java/com/knightdevelopers/kheerabackend/entity`
