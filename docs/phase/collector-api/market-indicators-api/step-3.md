# Step 3: Testing and QA Verification
Assigned agent: test-engineer

## Working Directory
.worktrees/collector-api-market-indicators-api

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CODEX.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-indicators-api/spec.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-indicators-api/step-2.md
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/market/MarketIndicatorsController.kt
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/application/market/service/MarketIndicatorsQueryService.kt
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/test/kotlin/com/papertrading/collector

## Tasks
1. Verify functional correctness against spec.
- Confirm formulas and parameter handling for BB/RSI/MACD.
- Confirm missing-data policy outputs null-until-window-ready.
- Confirm multi-indicator response alignment by timestamp.

2. Verify interval and period contract.
- Intervals: `1m/5m/10m/1d/1w` all covered by tests.
- Period query: `limit` and `from~to` success/failure cases.
- Error contract: 400/404/422 and stable error code values.

3. Add/adjust QA-focused tests only where gaps remain.
- Add test cases if step-2 coverage is missing critical path.
- Do not change business logic unless required to unblock testability and report it explicitly.

4. Report residual risks.
- Redis data sparsity and period boundary edge cases.
- Weekly bar composition behavior when source data is partial.

## Acceptance Criteria
```bash
cd backend/collector-api && ./gradlew test --tests "*MarketIndicators*" --tests "*Indicator*"
cd backend/collector-api && ./gradlew test
```
- All tests pass.
- Spec contract violations are either fixed or listed as blockers.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
