# Issue Implementation Plans

The [engineering standards](../../engineering/ENGINEERING_STANDARDS.md) are
authoritative. Copy the outline below to
`issue-<number>_<short-kebab-title>.md` in this folder, for example
`issue-123_fix-session-expiry.md`. Commit the completed initial plan before
application code or test changes. Include it and subsequent updates in the PR.

Replace every placeholder and expand each execution step with actual files,
test names, commands, expected results, and prerequisites. Generic statements
such as "implement feature" or "run tests" are not a complete plan. Include
additional applicable rules from the repository. Use "N/A: <reason>" for steps
that do not apply; do not check unfinished steps. Keep old issue plans at their
existing paths unless the issue requires a move.

## Copyable outline

```markdown
# Issue #<number>: <title>

## Scope and baseline
- Issue URL:
- Branch and synchronized develop commit:
- Problem and intended behavior:
- In scope / out of scope:
- Dependencies and blockers:
- References read: issue attachments, contracts, designs, implementation, rules.
- Open decisions: resolve implementation-blocking questions before coding.

## Acceptance criteria and tests
| Criterion | Named test / manual check | Expected result |
| --- | --- | --- |
| <observable requirement> | <file and test name or precise manual steps> | <result> |

## Design and affected files
- Existing behavior and proposed change:
- Files/layers to change and responsibility of each:
- API, schema, consumers, authorization, validation, and error impacts:
- Compatibility, concurrency, data risks, and migration/forward-fix approach:
- Documentation paths to update:
- Alternatives considered and reason for chosen approach:

## Ordered execution checklist
- [ ] Confirm issue requirements, dependencies, references, and latest develop.
- [ ] Create issue branch; update TODO; complete and commit this initial plan.
- [ ] Write and run failing regression/unit/PostgreSQL tests; record expected failures.
- [ ] Add required Flyway migration and compatible mappings, with database tests.
- [ ] Implement repository methods; run repository tests.
- [ ] Implement services and transaction/authorization rules; run service tests.
- [ ] Implement controllers, DTOs, and errors; run controller tests.
- [ ] Run targeted regression after each phase.
- [ ] Update API, schema, architecture, testing, README, and TODO documents.
- [ ] Start locally; verify health, affected API success/failure/auth, and OpenAPI.
- [ ] Run docker info and full ./mvnw clean verify; record counts and zero skips.
- [ ] Fix failures and rerun failed/affected checks plus full clean verification.
- [ ] Self-review diff against all criteria, scope, security, and documentation.
- [ ] Commit/push issue branch with plan; create PR to develop with evidence.
- [ ] Post exactly @codex review; verify accepted comment and record URL.
- [ ] Follow repository issue-closure policy; keep remaining work on the PR.
- [ ] Inspect CI/reviews; fix valid findings, rerun required validation, and push.
- [ ] Resolve addressed threads with evidence; post fresh @codex review.
- [ ] Review blockers and update rules where needed.
- [ ] Verify checks/review and applicable authorization before merge.
- [ ] After merge/deployment, update progress/history and run applicable smoke checks.

## Validation evidence
| Step | Command or manual procedure | Expected | Actual result / counts / evidence | Tested commit or working-tree state |
| --- | --- | --- | --- | --- |
| <step> | <exact command and configuration> | <pass condition> | Pending | <state> |

Record initial expected failing tests, subsequent passing tests, full regression,
and browser/API smoke checks. Distinguish blocked, skipped, failed, and passed.
After code changes or upstream integration, mark invalidated evidence stale and
record fresh results. Do not change expected behavior merely to pass tests.

## Plan changes and resume notes
| Date | New evidence / deviation | Reason and scope decision | Steps/checks to repeat |
| --- | --- | --- | --- |
| <date> | <finding> | <decision or clarification link> | <steps> |

Before each phase or after an interruption, compare this plan with the issue,
current diff, and completed evidence. Update the plan before following a new
direction. Preserve the original decisions and explain revisions.

## Delivery
- PR URL:
- Initial Codex request URL:
- Follow-up review URLs and findings:
- CI results:
- Merge/deployment status and evidence:
- Remaining steps or blockers:
```
