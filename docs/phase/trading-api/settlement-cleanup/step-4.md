# Step 4: Regression QA for Mode-Specific Settlement/Tax Behavior
Assigned agent: test-engineer

## Working Directory
.worktrees/trading-api-settlement-cleanup

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/phase/trading-api/settlement-cleanup/spec.md
- docs/phase/trading-api/settlement-cleanup/step-2.md
- docs/phase/trading-api/settlement-cleanup/step-3.md
- backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/ExecutionProcessorTest.kt
- backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/tax
- backend/trading-api/src/test/kotlin/com/papertrading/api/application/settlement

## Tasks
1. Rebaseline tests for removed receivable settlement path.
- Remove or rewrite tests that directly depend on deleted `ReceivableSettlement` flow.
- Ensure test suite no longer expects KIS internal receivable settlement lifecycle.

2. Add/adjust execution regression tests.
- LOCAL sell: verify settlement creation includes 0.2% tax and expected PnL math.
- KIS_PAPER/KIS_LIVE sell: verify no internal receivable settlement/cash settlement mutation path is executed.

3. Add/adjust tax summary scope tests.
- LOCAL account recalculate/list/get succeeds.
- KIS account recalculate/list/get returns contract-defined client error.

4. Run focused and broad verification.
- Run targeted tests for execution and tax summary.
- Run full test command once to detect cross-module regressions.

## Acceptance Criteria
```bash
cd .worktrees/trading-api-settlement-cleanup/backend/trading-api
./gradlew test --tests "*ExecutionProcessorTest" --tests "*TaxSummary*"
./gradlew test
```

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---