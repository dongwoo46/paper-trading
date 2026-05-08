# Step 2: Implement Market Indicators API (TDD)
Assigned agent: fullstack-dev

## Working Directory
.worktrees/collector-api-market-indicators-api

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CODEX.md
- C:/Users/dw/Desktop/paper-trading/docs/ADR.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-indicators-api/spec.md
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/application/market/service/MarketDailyOhlcvQueryService.kt
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/market/MarketDailyOhlcvController.kt
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/market/persistence/MarketDailyOhlcvRepository.kt

## Tasks
1. Add indicator query API contract and controller endpoint.
- Create/modify files under:
  - `backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/market/MarketIndicatorsController.kt`
  - `backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/market/dto/MarketIndicatorsDto.kt`
- Required endpoint signature:
  - `fun indicators(symbol: String, interval: String, limit: Int?, from: String?, to: String?, indicators: String, bbPeriod: Int?, bbStdDev: BigDecimal?, rsiPeriod: Int?, macdFast: Int?, macdSlow: Int?, macdSignal: Int?): ResponseEntity<MarketIndicatorsResponse>`

2. Implement application service and request validation.
- Create/modify:
  - `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/market/service/MarketIndicatorsQueryService.kt`
  - `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/market/service/MarketBarSourceResolver.kt`
- Required method signatures:
  - `fun query(request: MarketIndicatorsQuery): MarketIndicatorsResult`
  - `fun resolve(interval: Interval): MarketBarSource`

3. Add domain model and indicator calculators (directive-only signature constraints).
- Create/modify:
  - `backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/market/indicator/IndicatorQuery.kt`
  - `backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/market/indicator/IndicatorParams.kt`
  - `backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/market/indicator/IndicatorPoint.kt`
  - `backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/market/indicator/BollingerBandsCalculator.kt`
  - `backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/market/indicator/RsiCalculator.kt`
  - `backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/market/indicator/MacdCalculator.kt`
- Required calculator signatures:
  - `fun calculate(closes: List<BigDecimal>, params: BollingerBandParams): List<BollingerBandValue?>`
  - `fun calculate(closes: List<BigDecimal>, params: RsiParams): List<RsiValue?>`
  - `fun calculate(closes: List<BigDecimal>, params: MacdParams): List<MacdValue?>`

4. Add infra adapters for interval-based bar loading.
- Create/modify:
  - `backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/market/query/RedisIntradayBarQueryRepository.kt`
  - `backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/market/query/PostgresDailyWeeklyBarQueryRepository.kt`
- Enforce key/range rules from `spec.md`:
  - Intervals: `1m/5m/10m/1d/1w`
  - Period policy: `limit` xor `from~to`
  - Missing data policy: BB/RSI/MACD null-until-window-ready

5. TDD requirements.
- Add tests first for:
  - parameter validation failure cases
  - BB/RSI/MACD warm-up null behavior
  - multi-indicator response alignment on same timestamp list
  - interval switch behavior (`1m`,`5m`,`10m`,`1d`,`1w`)
- Test file targets:
  - `backend/collector-api/src/test/kotlin/com/papertrading/collector/domain/market/indicator/*Test.kt`
  - `backend/collector-api/src/test/kotlin/com/papertrading/collector/application/market/service/MarketIndicatorsQueryServiceTest.kt`
  - `backend/collector-api/src/test/kotlin/com/papertrading/collector/presentation/market/MarketIndicatorsControllerTest.kt`

## Acceptance Criteria
```bash
cd backend/collector-api && ./gradlew test --tests "*MarketIndicators*" --tests "*Indicator*"
cd backend/collector-api && ./gradlew compileKotlin
```
- API supports `1m/5m/10m/1d/1w`, `limit/from~to`, and multi-indicator query.
- Indicator formulas/parameters and missing-data policy match `spec.md`.
- No implementation shortcuts that violate DDD layering.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
