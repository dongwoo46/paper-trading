# Step 2: Implement Market Microstructure + RS API (TDD)
Assigned agent: fullstack-dev

## Working Directory
.worktrees/collector-api-market-microstructure-rs-api

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CODEX.md
- C:/Users/dw/Desktop/paper-trading/docs/ADR.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-microstructure-rs-api/spec.md
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/market/MarketIndicatorsController.kt
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/marketbar/MarketBarController.kt
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/application/market/service/MarketIndicatorsQueryService.kt
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/application/marketfeature/service/MarketFeatureQueryService.kt

## Tasks
1. Add microstructure + RS endpoint contract and DTOs.
- Create/modify:
  - `backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/market/MarketMicrostructureController.kt`
  - `backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/market/dto/MarketMicrostructureDto.kt`

2. Implement application orchestration service and validation.
- Create/modify:
  - `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/market/service/MarketMicrostructureQueryService.kt`
  - `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/market/service/MarketMicrostructureExceptions.kt`

3. Implement/extend domain VO and calculators for RS + flow metrics.
- Create/modify:
  - `backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/market/analytics/MarketAnalyticsQuery.kt`
  - `backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/market/analytics/MarketMicrostructureSnapshot.kt`
  - `backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/market/analytics/RelativeStrengthCalculator.kt`

4. Add/extend infra query adapters.
- Reuse existing repositories where possible and add minimal new adapters under:
  - `backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/market/query/`
- Ensure interval/session/range rules and missing-data policy from `spec.md` are enforced.

5. TDD requirements.
- Add tests first for:
  - validation failures (interval/session/period)
  - microstructure field mapping and null policy
  - RS ratio/returnDelta calculations and baseline fallback
  - contract stability for 200 response shape

## Acceptance Criteria
```bash
cd backend/collector-api && ./gradlew test --tests "*MarketMicrostructure*" --tests "*RelativeStrength*"
cd backend/collector-api && ./gradlew compileKotlin
```
- Endpoint supports `1m/5m/10m/1d/1w`, `session`, `limit/from~to`, and unified schema.
- Numeric/validation behavior matches `spec.md`.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---