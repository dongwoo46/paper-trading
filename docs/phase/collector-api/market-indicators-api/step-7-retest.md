# Step 7: Re-test After Rework
Assigned agent: test-engineer

## Working Directory
.worktrees/collector-api-market-indicators-api

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CODEX.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-indicators-api/spec.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-indicators-api/step-3.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-indicators-api/step-3-rework.md
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/application/market/service/MarketIndicatorsQueryService.kt
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/common/GlobalExceptionHandler.kt
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/test/kotlin/com/papertrading/collector

## Tasks
1. Re-verify previously blocked contracts are fixed.
- 400/404/422 status mapping and stable error code values.
- default `limit=200` when period params omitted.

2. Run targeted + service full test gates.
- Ensure no regression on indicator warm-up, timestamp alignment, interval coverage.

## Acceptance Criteria
```bash
cd backend/collector-api && ./gradlew test --tests "*MarketIndicators*" --tests "*Indicator*"
cd backend/collector-api && ./gradlew test
```
- All tests pass.
- Previously blocked items are resolved or explicitly blocked with evidence.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
