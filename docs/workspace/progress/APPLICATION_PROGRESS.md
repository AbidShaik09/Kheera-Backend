# Kheera Application Progress

**Last updated:** 2026-09-11. This is the live, whole-application delivery
view. Update it when work starts, becomes blocked, changes scope, merges, or is
deployed. Completed material changes also require a row in `CHANGE_HISTORY.md`.

| Workstream | Current status | Active work / source | Next milestone |
| --- | --- | --- | --- |
| Documentation and engineering workflow | Implemented | Backend [#52](https://github.com/AbidShaik09/Kheera-Backend/issues/52), frontend [#66](https://github.com/AbidShaik09/Kheera-Frontend/issues/66). | Adopt rules on every new issue and PR. |
| Authentication UI | Implemented locally | Login, sign-up, and password reset are integrated with existing auth endpoints. | Verify against corrected OTP behavior and deployment configuration. |
| Password-reset OTP integrity | Implemented | Backend [#51](https://github.com/AbidShaik09/Kheera-Backend/issues/51). | Add regression coverage in #47. |
| Schema and persistence alignment | In progress | Backend [#44](https://github.com/AbidShaik09/Kheera-Backend/issues/44). | Align Flyway, JPA mappings, and ER diagram before wider repository coverage. |
| Authenticated space list | Planned | Backend [#43](https://github.com/AbidShaik09/Kheera-Backend/issues/43). | Resolve JWT email subject to the current user correctly. |
| Test and CI baseline | Planned | Backend [#47](https://github.com/AbidShaik09/Kheera-Backend/issues/47), [#48](https://github.com/AbidShaik09/Kheera-Backend/issues/48), [#49](https://github.com/AbidShaik09/Kheera-Backend/issues/49), [#50](https://github.com/AbidShaik09/Kheera-Backend/issues/50). | Establish unit, PostgreSQL integration, MVC/security, worker, and CI coverage. |
| Backend design boundaries | Planned | Backend [#46](https://github.com/AbidShaik09/Kheera-Backend/issues/46). | Harden DTO/repository/service boundaries after tests protect current behavior. |
| Space module | Planned | Backend [#43](https://github.com/AbidShaik09/Kheera-Backend/issues/43), frontend [#63](https://github.com/AbidShaik09/Kheera-Frontend/issues/63). | Deliver authorization-scoped space APIs and Space Details UI. |
| Project board | Planned | Backend [#45](https://github.com/AbidShaik09/Kheera-Backend/issues/45), [#10](https://github.com/AbidShaik09/Kheera-Backend/issues/10), frontend [#64](https://github.com/AbidShaik09/Kheera-Frontend/issues/64). | Define ordered workflow stages, then build project APIs and board UI. |
| Task details and attachments | Planned | Backend [#11](https://github.com/AbidShaik09/Kheera-Backend/issues/11), [#28](https://github.com/AbidShaik09/Kheera-Backend/issues/28), frontend [#65](https://github.com/AbidShaik09/Kheera-Frontend/issues/65). | Complete work items/comments before attachment UX. |
| Production operations | Observed; needs ongoing validation | Contabo deployment inventory and TLS history. | Keep TLS, exposed ports, backups, updates, and CI/CD status verified. |
