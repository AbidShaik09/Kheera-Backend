# Kheera Major Change History

This is an append-only record of material completed changes. Add a row after a
feature/fix, migration, security change, operational change, or release has
merged or been deployed. Do not record passwords, tokens, personal data, or
unredacted production output.

| Date | Change | Affected areas | Issue / reference | Impact |
| --- | --- | --- | --- | --- |
| 2026-09-11 | Established documentation governance, architecture tracking, application progress, and engineering workflow standards. | Workspace docs, backend docs, frontend docs. | [Backend #52](https://github.com/AbidShaik09/Kheera-Backend/issues/52), [frontend #66](https://github.com/AbidShaik09/Kheera-Frontend/issues/66). | Future changes have an issue-first, documented delivery path. |
| 2026-09-11 | Recorded password-reset OTP ordering and expiry defect for correction. | Backend OTP lookup and reset flow. | [Backend #51](https://github.com/AbidShaik09/Kheera-Backend/issues/51). | Valid recent reset codes can be rejected by legacy null-timestamp data until fixed. |
| 2026-09-11 | Implemented Angular authentication screens and reset-password flow against existing raw-text auth endpoints. | Angular routes, auth service, login, register, password recovery. | `../api/API_CONTRACT.md`. | Local frontend supports login, sign-up, forgotten-password, and reset flows. |
| 2026-09-11 | Audited Contabo deployment, Docker/Nginx topology, and TLS recovery. | Contabo VM, Nginx, frontend/backend containers, PostgreSQL. | `../operations/CONTABO_VM.md`. | Established a redacted infrastructure baseline for later operational work. |
