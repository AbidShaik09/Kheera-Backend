# Kheera Documentation

This directory contains workspace-wide references that do not belong to one
backend package alone. Its structure and mandatory update rules are defined in
`governance/DOCUMENTATION_STANDARDS.md`.

| Location | Purpose |
| --- | --- |
| `product/PROJECT_REFERENCE.md` | Product model, repository overview, and Penpot notes. |
| `api/API_CONTRACT.md` | Shared frontend/backend API contract. |
| `architecture/APPLICATION_ARCHITECTURE.md` | Whole-system structure, data flow, and ownership boundaries. |
| `architecture/DECISION_LOG.md` | Append-only record of cross-cutting technical decisions. |
| `progress/APPLICATION_PROGRESS.md` | Whole-application delivery status and current next steps. |
| `progress/CHANGE_HISTORY.md` | Dated record of completed material changes. |
| `governance/DOCUMENTATION_STANDARDS.md` | Required documentation updates for every delivery change. |
| `operations/CONTABO_VM.md` | Read-only deployment and server inventory; never add secrets. |
| `../` | Backend architecture, API, testing, planning, and standards. |
| [Kheera-Frontend docs](https://github.com/AbidShaik09/Kheera-Frontend/tree/develop/docs) | Frontend design, planning, and standards. |

Do not put passwords, private keys, API tokens, database dumps, SMTP settings,
or production `.env` values in these documents.
