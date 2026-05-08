# Step 3 Rework: Fix Contract Gaps Found by QA
Assigned agent: fullstack-dev

## Working Directory
.worktrees/collector-api-market-indicators-api

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CODEX.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-indicators-api/spec.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-indicators-api/step-2.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-indicators-api/step-3.md
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/market/MarketIndicatorsController.kt
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/application/market/service/MarketIndicatorsQueryService.kt

## Tasks
1. Align API error contract to spec.
- Ensure stable error code mapping:
  - 400: `INVALID_INTERVAL`, `INVALID_PERIOD_QUERY`
  - 404: `SYMBOL_NOT_FOUND_OR_NO_BARS`
  - 422: `INSUFFICIENT_BARS_FOR_REQUESTED_RANGE`
- Add/update exception types and global exception mapping as needed.

2. Implement period default behavior.
- When both `limit` and `from/to` are omitted, apply default `limit=200`.
- Keep xor policy for explicit inputs (`limit` xor `from~to`).

3. Add/adjust tests for contract guarantees.
- Controller/service tests must assert HTTP status + stable error code values.
- Add tests for default `limit=200` behavior.

4. Preserve previous passing behavior.
- Do not regress BB/RSI/MACD warm-up null policy.
- Keep multi-indicator timestamp alignment.

## Acceptance Criteria
```bash
cd backend/collector-api && ./gradlew test --tests "*MarketIndicators*" --tests "*Indicator*"
cd backend/collector-api && ./gradlew test
cd backend/collector-api && ./gradlew compileKotlin
```
- Error contract 400/404/422 and code values are enforced by tests.
- Default `limit=200` works when period params are omitted.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
