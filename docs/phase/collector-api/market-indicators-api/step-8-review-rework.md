# Step 8: Review Rework
Assigned agent: fullstack-dev

## Working Directory
.worktrees/collector-api-market-indicators-api

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CODEX.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-indicators-api/spec.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-indicators-api/step-4.md
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/market/dto/MarketIndicatorsDto.kt
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/market/query/RedisIntradayBarQueryRepository.kt
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/market/query/PostgresDailyWeeklyBarQueryRepository.kt

## Tasks
1. Fix daily/weekly period contract.
- Support `limit` query path for `1d/1w` and keep `limit` xor `from~to` contract.

2. Fix intraday range behavior.
- Apply `from~to` filtering for `1m/5m/10m` in repository query path.

3. Align response schema to spec.
- Add required `range` and `meta` fields (including missingPolicy/warnings) and update mapping/tests.

4. Add weekly fallback policy.
- If weekly source unavailable/empty, compose weekly bars from daily source.

5. Strengthen tests.
- Cover `1d/1w + limit`, intraday `from~to`, response schema fields, weekly fallback.

## Acceptance Criteria
```bash
cd backend/collector-api && ./gradlew test --tests "*MarketIndicators*" --tests "*Indicator*"
cd backend/collector-api && ./gradlew test
cd backend/collector-api && ./gradlew compileKotlin
```
- All must-fix review items resolved with tests.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
