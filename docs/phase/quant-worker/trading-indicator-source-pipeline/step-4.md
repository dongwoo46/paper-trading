# Step 4: Code Review
Assigned agent: code-reviewer

## Working Directory
.worktrees/quant-worker-trading-indicator-source-pipeline

## Files to Read
- CODEX.md
- docs/phase/quant-worker/trading-indicator-source-pipeline/spec.md
- docs/phase/quant-worker/trading-indicator-source-pipeline/step-2.md
- docs/phase/quant-worker/trading-indicator-source-pipeline/step-3.md
- backend/quant-worker/src/
- backend/collector-api/src/main/resources/db/migration/

## Tasks
1. Review correctness of pipeline design vs spec (KR/US/session/RS/flow/metadata).
2. Review Decimal safety, idempotency keys, and null-policy handling.
3. Review migration/index/partition choices for operational risk.
4. Produce must-fix vs optional findings with file references.

## Acceptance Criteria
- No unresolved critical/high findings.
- Review output contains concrete fix actions when needed.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
