# Kheera Major Change History

This is an append-only record of material completed changes. Add a row after a
feature/fix, migration, security change, operational change, or release has
merged or been deployed. Do not record passwords, tokens, personal data, or
unredacted production output.

| Date | Change | Affected areas | Issue / reference | Impact |
| --- | --- | --- | --- | --- |
| 2026-09-12 | Added email worker unit tests, made retry handling null-safe, and required deployment workflows to run tests before packaging. | Email worker, deployment workflows, application smoke test, testing documentation. | [Backend #50](https://github.com/AbidShaik09/Kheera-Backend/issues/50). | Email delivery retry behavior is protected, and failing tests now stop development and production deployments. |
| 2026-09-12 | Updated the delivery workflow so GitHub issues are closed after PR creation, with review, CI, merge, and deployment follow-up continuing on the PR. | Backend engineering governance. | Direct repository-owner request. | Future issue branches have a single PR-centered follow-up path after the pull request is opened. |
| 2026-09-12 | Added workflow rules for sandbox escalation, docs-only direct pushes, post-PR blocker review, and misunderstanding root-cause updates. | Backend engineering and documentation governance. | Direct repository-owner request. | Future issue work has clearer guidance for permission failures, PR follow-up, and correcting ambiguous instructions. |
| 2026-09-11 | Aligned OTP and work-item JPA mappings with the existing Flyway schema. | OneTimePassword, work-item entities, database design record. | [Backend #44](https://github.com/AbidShaik09/Kheera-Backend/issues/44). | Fresh deployments use the intended OTP table and JPA nullability matches the physical schema. |
| 2026-09-11 | Corrected password-reset OTP persistence, ordering, and expiry validation. | OTP repository and verification service. | [Backend #51](https://github.com/AbidShaik09/Kheera-Backend/issues/51). | New reset OTPs are timestamped; legacy null-timestamp rows cannot supersede them; expired OTPs are rejected. Regression coverage is deferred to #47. |
| 2026-09-11 | Established documentation governance, architecture tracking, application progress, and engineering workflow standards. | Workspace docs, backend docs, frontend docs. | [Backend #52](https://github.com/AbidShaik09/Kheera-Backend/issues/52), [frontend #66](https://github.com/AbidShaik09/Kheera-Frontend/issues/66). | Future changes have an issue-first, documented delivery path. |
| 2026-09-11 | Recorded password-reset OTP ordering and expiry defect for correction. | Backend OTP lookup and reset flow. | [Backend #51](https://github.com/AbidShaik09/Kheera-Backend/issues/51). | Valid recent reset codes can be rejected by legacy null-timestamp data until fixed. |
| 2026-09-11 | Implemented Angular authentication screens and reset-password flow against existing raw-text auth endpoints. | Angular routes, auth service, login, register, password recovery. | `../api/API_CONTRACT.md`. | Local frontend supports login, sign-up, forgotten-password, and reset flows. |
| 2026-09-11 | Audited Contabo deployment, Docker/Nginx topology, and TLS recovery. | Contabo VM, Nginx, frontend/backend containers, PostgreSQL. | `../operations/CONTABO_VM.md`. | Established a redacted infrastructure baseline for later operational work. |
