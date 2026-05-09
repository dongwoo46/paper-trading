# Step 3: Testing and QA Verification
Assigned agent: test-engineer

## Working Directory
.worktrees/quant-worker-trading-indicator-source-pipeline

## Files to Read
- CODEX.md
- docs/phase/quant-worker/trading-indicator-source-pipeline/spec.md
- docs/phase/quant-worker/trading-indicator-source-pipeline/step-2.md
- backend/quant-worker/tests/
- backend/collector-api/src/main/resources/db/migration/

## Tasks
1. Validate schema/migration consistency with spec (keys/indexes/nullable policy).
2. Execute targeted test suites and capture failures with root cause.
3. Validate Decimal conversion and idempotent upsert behavior.
4. Validate session/timezone normalization for US regular/pre/after data.

## Verification
- `cd backend/quant-worker && pytest tests/ -k "indicator or microstructure or flow or metadata" -v --tb=short`
- `cd backend/collector-api && ./gradlew test --tests "*Market*"`

## Acceptance Criteria
- Targeted tests green or explicit failing list with reproducible command.
- QA notes include residual risks and data-quality gaps.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Test result: <passed N/N | failed N — list failing cases>
- Blockers: <none | description>
---
