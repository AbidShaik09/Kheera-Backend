# Backend Engineering Standards

## Mandatory Delivery Flow

1. Create or confirm a GitHub issue before changing a feature, bug, security
   concern, migration, API behavior, or production configuration. The issue
   must state the problem, scope, acceptance criteria, security impact, and
   relevant links, screenshots, API examples, or design attachments.
2. Add the issue to `docs/planning/IMPLEMENTATION_TODO.md` in dependency order.
   Do not start implementation if the issue has no acceptance criteria.
3. Read the full issue and all attached references before coding. Verify the
   API contract, Flyway schema, affected consumers, existing test coverage, and
   external links rather than guessing requirements.
4. Start only from an up-to-date, clean local `develop` branch. Create a branch
   named `issue/<number>_<short-kebab-title>`, for example
   `issue/51_fix-password-reset-otp`. Never use `main` as a feature base.
5. Create a clean loosely coupled plan to implement the feature/ Fix Bug. Think in terms of whole Application and not just current Feature
6. Maintain notes of plan based on github issue naming
7. Follow Test Driven Development, Write Unit Tests or update existing Tests Based on the requirement and development plan. 
8. For a bug, first write a failing regression test. For a feature, write or
   update the relevant unit tests before implementation. Mock collaborators
   outside the unit under test; use PostgreSQL Testcontainers for repository
   queries, mappings, migrations, or PostgreSQL-specific behavior. 
9. Implement the smallest complete change that meets the acceptance criteria.
   Keep naming explicit, remove dead code, and update tests whenever the
   contract or expected behavior changes.
10. Before pushing any issue branch, run the backend locally using the appropriate development configuration `.\mvnw.cmd spring-boot:run` . A successful build alone is not sufficient.  
    Verify that:
    - the application starts successfully and remains healthy;
    - /api/health responds successfully;
    - the API behavior affected by the issue is exercised locally using appropriate requests;
    - expected success and relevant failure/authorization cases behave according to the acceptance criteria;
    - Swagger/OpenAPI reflects API changes where applicable.
      If local verification cannot be completed, do not push or merge. Document the blocker and ask the repository owner for guidance. Missing packages or tooling may be installed only after receiving permission.
11. If relevant, Visit `/api/swagger-ui/index.html` or raw swagger docs to verify API controller reflects changes
2Update every relevant document under `docs/workspace/` and the backend
   `docs/` directory according to
   `../workspace/governance/DOCUMENTATION_STANDARDS.md`. Update architecture,
   API/schema/design records during implementation; update progress and history
   after a material merge or deployment. 
13. During implementation, run the tests relevant to the changed behavior frequently. Before a PR is considered ready, run the entire backend unit test suite and ./mvnw verify.  
    Tests may only be skipped when the repository owner explicitly identifies the specific test or test category that may be skipped. An instruction to skip a test never means skip build, compilation, local startup, smoke testing, security checks, self-review, or other validation.  
    Any skipped test must be listed in the PR with the reason it was skipped.
14. Before requesting a push, self-review the diff for correctness, edge cases,
   authorization, validation, errors, concurrency, logging, secrets, and API
   compatibility. Run the relevant test suite and the full backend build. 
15. When asked to push, push only the issue branch and create a pull request to
   `develop`, never `main`. After checking the diff, if the change contains
   only documentation or workflow-rule text and was explicitly requested by the
   repository owner, update `docs/workspace/progress/CHANGE_HISTORY.md`, commit
   the docs-only change, and push it directly to `develop`. Docs-only direct
   pushes must not contain code, schema, runtime configuration, dependency, or
   generated artifact changes. Do not merge before code review and required
   checks pass. After the PR is created, mark the GitHub issue closed and keep
   any remaining review, CI, merge, or deployment follow-up on the pull request.
16. Everytime there is a change made to the CORE schema, create a new relevant migration
17. Validation rules take precedence over delivery instructions. An instruction such as “push,” “create PR,” or “merge” does not imply permission to bypass any validation gate. If a required gate fails, stop the delivery process, report the failure, and fix it or request guidance.


18. If a required networked command fails because of sandbox or environment
   restrictions, retry the same command with the proper escalation/permission
   request before changing the implementation plan. Examples include
   `git fetch`, `git push`, Maven dependency downloads, and required local
   verification that cannot complete without network access. Record the reason
   in the progress update or PR verification notes when it affects delivery.
19. After every PR is created, identify any blockers, friction, or repeated
   failure pattern from the issue. Decide whether a new or updated rule would
   make similar future work easier, safer, or less ambiguous. If yes, update
   the relevant engineering, documentation, testing, or issue-creation rule
   before considering the task complete.
20. After fixing and resolving GitHub PR review comments, always add a new PR
   comment containing exactly `@codex review` so Codex performs a fresh review
   of the updated pull request.
21. If the user stops execution and says the AI was not on the right track,
   pause implementation work and identify the root cause of the
   misunderstanding. Before resuming similar implementation work, update the
   relevant documentation, GitHub issue creation rules, or implementation rules
   so future issues carry clearer scope, sequencing, branch, PR, or validation
   instructions.
22. resulting pipeline becomes:
Issue → TODO → Plan → tests/TDD → implementation → targeted tests → local backend startup → health/API smoke verification → full unit suite → mvnw verify → self-review → docs → push → PR → close issue → blocker/rule review → CI → review → resolve comments → @codex review → merge to develop → dev deployment smoke check.

If the worktree is dirty or the repository has no `develop` branch, stop before
switching branches. Preserve existing work and resolve the branch baseline with
the repository owner first.

## GitHub Issue Creation Rules

- Each issue must state whether it needs its own branch and PR, or whether it
  may be grouped with another issue. If separate PRs are required, say so in
  the issue before implementation begins.
- Each issue must define acceptance criteria, affected contracts/docs, required
  verification, and known blockers or dependencies.
- If a previous attempt was stopped because the implementation direction was
  wrong, the follow-up issue or rule update must name the misunderstanding and
  the instruction that would have prevented it.
- Issue descriptions should call out workflow exceptions explicitly, including
  docs-only direct-to-`develop` updates approved by the repository owner.

## Design and Code Rules

- Apply SOLID principles without speculative abstraction. Controllers handle
  HTTP concerns, services own business rules and transaction boundaries, and
  repositories encapsulate persistence queries.
- Use request/response DTOs at API boundaries. Do not expose JPA entities or
  accept client-controlled ownership, role, permission, or audit fields.
- Prefer constructor injection, cohesive classes, explicit names, and small
  methods. Depend on interfaces only when there is a real substitution seam.
- Keep modules loosely coupled. Avoid circular dependencies, static mutable
  state, controller-to-repository shortcuts, and cross-domain writes without a
  service boundary.
- Flyway owns schema changes. Every persistence change needs a reviewed
  migration, compatible mapping, rollback/forward-fix plan, and repository
  coverage where behavior depends on PostgreSQL.

## Security and Reliability Rules

- Authenticate every protected request and authorize from JWT email to the
  server-side user, membership, role, and permission. Never trust browser IDs
  for ownership or access decisions.
- Validate and normalize all request input. Use allow-lists for enums, sort
  fields, upload types, and state transitions. Return safe client errors and
  keep internal stack traces out of API responses.
- Store secrets only in environment/secret managers. Never commit passwords,
  keys, JWT secrets, tokens, certificates, `.env` files, SMTP credentials, or
  real production configuration. Provide redacted examples only.
- Review dependencies and transitive vulnerabilities before release. Keep
  Spring, Maven plugins, container bases, and GitHub Actions pinned and updated.
- Consider rate limiting, replay protection, expiry, audit logging, and generic
  user-facing messages for authentication, password-reset, and email flows.
- Test boundary conditions: missing/invalid input, expired data, duplicate
  requests, authorization failures, soft-deleted records, timezone handling,
  races, and downstream failures.

## Definition of Done

- Issue, acceptance criteria, TODO state, API contract, architecture/progress
  records, and all relevant docs are current.
- Unit/repository/controller coverage reflects the intended behavior.
- Relevant tests and `./mvnw verify` pass.
- Code is self-reviewed for cleanup, security, and edge cases.
- Run all Unit Tests and ensure no other functionality is Broken, If unit test in other features fail, reach out to application docs to figur out intended behaviour and fix the broken code or update Unit Tests, whichever meets the application requirements
- PR targets `develop`, documents verification, and awaits review before merge.
