# Step 4: API Mapping — bestBid/bestAsk/spread/depth + 1d/1w DB-Source Branch Fix + RS Timestamp Sort
Assigned agent: fullstack-dev

## Files to Read
- CLAUDE.md
- docs/ADR.md
- docs/phase/collector-api/market-microstructure-orderbook-ws/spec.md
- backend/collector-api/src/main/kotlin/com/papertrading/collector/application/market/service/MarketMicrostructureQueryService.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/application/market/service/MarketBarSourceResolver.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/market/analytics/MarketMicrostructureSnapshot.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/market/analytics/RelativeStrengthCalculator.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/application/marketbar/port/MarketBarRepository.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/redis/OrderbookRedisStore.kt
- backend/collector-api/src/test/kotlin/com/papertrading/collector/application/market/service/MarketMicrostructureQueryServiceTest.kt

## Tasks

### 1. Fix `RelativeStrengthCalculator.calculate()` — timestamp sort guard
File: `backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/market/analytics/RelativeStrengthCalculator.kt`

- At entry to `calculate()`, after the size equality check, validate that `timestamps` is sorted in ascending order:
  - `timestamps.zipWithNext().forEach { (a, b) -> if (a > b) throw IllegalArgumentException("INVALID_PERIOD_QUERY") }`
- This must be the first validation after size checks.

### 2. Fix `MarketMicrostructureQueryService` — 1d/1w DB-source branching
File: `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/market/service/MarketMicrostructureQueryService.kt`

- Constructor-inject `MarketBarSourceResolver` (in addition to existing `MarketBarRepository` and `MarketFeatureStore`)
- In `query()`, replace the direct `marketBarRepository.findBars(...)` call:
  - Resolve source: `val barSource = marketBarSourceResolver.resolve(interval)` — use the domain `Interval` enum (from `MarketBarSourceResolver`)
  - For symbol bars: `barSource.load(query.symbol, interval.toIndicatorInterval(), resolvedRequest)` where `resolvedRequest` maps `MarketMicrostructureQuery` fields to `MarketIndicatorsQuery`
  - For baseline bars: same source resolver with baseline symbol
- `MarketBarRepository` is no longer needed in this service if all bar loads go through the resolver; remove it or keep it if the port is separate — prefer removal to avoid dead injection.
- Note: `MarketBarSource.load()` signature is `(symbol, Interval, MarketIndicatorsQuery)`. Map `MarketMicrostructureQuery.limit/from/to` to `MarketIndicatorsQuery` fields.
- `QueryInterval` → `Interval` mapping: add `fun QueryInterval.toIndicatorInterval(): Interval` as an extension function in the same file or in a dedicated mapper file (whichever is minimal).

### 3. Map orderbook fields in `loadSnapshot()`
File: `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/market/service/MarketMicrostructureQueryService.kt`

- Constructor-inject `OrderbookRedisStore`
- In `loadSnapshot(symbol, interval)`:
  - For intraday intervals (M1, M5, M10):
    - Call `orderbook = orderbookRedisStore.load(symbol)` (nullable)
    - Map: `bestBid = orderbook?.bestBid`, `bestAsk = orderbook?.bestAsk`, `spread = orderbook?.spread`, `bidDepthTopN = orderbook?.bidDepthTopN`, `askDepthTopN = orderbook?.askDepthTopN`, `depthImbalance = orderbook?.depthImbalance`
  - For 1d/1w: all depth fields remain null (existing behavior)
  - `buyVolume`, `sellVolume`, `tradeIntensity`, `vwap`, `rvol` continue to come from `MarketFeatureStore` (unchanged)

### 4. Write Tests (TDD — write tests first)

File: `backend/collector-api/src/test/kotlin/com/papertrading/collector/domain/market/analytics/RelativeStrengthCalculatorTest.kt` (extend existing)

Test cases to add:
- `timestamp 역순 입력 시 IllegalArgumentException`
- `timestamp 정순 입력 시 정상 계산` (verify no exception, verify result count)

File: `backend/collector-api/src/test/kotlin/com/papertrading/collector/application/market/service/MarketMicrostructureQueryServiceTest.kt` (extend existing)

Test cases to add:
- `1d 요청 시 Postgres 소스 사용 검증`
  - Mock `MarketBarSourceResolver` to return a mock `MarketBarSource`
  - Verify the Postgres-side mock is called, not the Redis mock
- `1w 요청 시 Postgres 소스 사용 검증` (same pattern)
- `intraday 요청 시 orderbook null이어도 나머지 필드 정상 반환` (bestBid null, buyVolume non-null)
- `intraday 요청 시 orderbook 있을 때 bestBid/bestAsk/spread/depth 매핑됨`
- `1d/1w 요청 시 microstructure 전체 null` (existing behavior regression)

## Acceptance Criteria
```bash
cd backend/collector-api && ./gradlew test --tests "*.RelativeStrengthCalculatorTest" --tests "*.MarketMicrostructureQueryServiceTest" && ./gradlew compileKotlin
```
All target tests pass. `compileKotlin` succeeds with no errors.
