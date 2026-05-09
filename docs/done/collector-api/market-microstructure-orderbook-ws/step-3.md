# Step 3: OrderbookRedisStore + Ingest Metrics + Pipeline Wiring
Assigned agent: fullstack-dev

## Files to Read
- CLAUDE.md
- docs/ADR.md
- docs/phase/collector-api/market-microstructure-orderbook-ws/spec.md
- backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/redis/RedisKeyPolicy.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/redis/QuoteRedisPublisher.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/redis/MarketFeatureRedisStore.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/application/kis/pipeline/RawEventPipeline.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/entity/kis/KisOrderbookEvent.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/application/kis/pipeline/KisOrderbookEventParser.kt

## Tasks

### 1. Update `RedisKeyPolicy`
File: `backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/redis/RedisKeyPolicy.kt`

- Add constant: `val ORDERBOOK_TTL: Duration = Duration.ofSeconds(30)`
- Add function: `fun orderbookKey(symbol: String): String = "orderbook:$symbol"`
- Do NOT change any existing keys or TTLs

### 2. New Data Class: `OrderbookSnapshot`
File: `backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/redis/OrderbookSnapshot.kt`

- Data class with fields:
  - `bestBid: BigDecimal?`
  - `bestAsk: BigDecimal?`
  - `spread: BigDecimal?`
  - `bidDepthTopN: BigDecimal?`
  - `askDepthTopN: BigDecimal?`
  - `depthImbalance: BigDecimal?`
  - `timestamp: Instant?`
- All numeric fields `BigDecimal` (no Float/Double)

### 3. New Component: `OrderbookRedisStore`
File: `backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/redis/OrderbookRedisStore.kt`

- `@Component` class, constructor-inject `StringRedisTemplate` and `ObjectMapper`
- `fun save(event: KisOrderbookEvent)`:
  - Compute: `bestAsk = event.askPrices[0]`, `bestBid = event.bidPrices[0]`
  - Compute: `spread = bestAsk - bestBid` (BigDecimal subtraction)
  - Compute: `askDepthTopN = event.askQtys.fold(BigDecimal.ZERO, BigDecimal::add)`
  - Compute: `bidDepthTopN = event.bidQtys.fold(BigDecimal.ZERO, BigDecimal::add)`
  - Compute: `depthImbalance`: if `(bidDepthTopN + askDepthTopN) == 0` → null; else `(bidDepthTopN - askDepthTopN) / (bidDepthTopN + askDepthTopN)` with `scale=8, RoundingMode.HALF_UP`
  - Serialize `bidLevels` and `askLevels` as JSON arrays of `{price, qty}` objects (use `ObjectMapper`)
  - Write `orderbook:{symbol}` Hash via `opsForHash<String,String>().putAll(...)` with fields: `bestBid, bestAsk, spread, bidDepthTopN, askDepthTopN, depthImbalance` (only if non-null), `bidLevels, askLevels, source, timestamp`
  - Set TTL: `RedisKeyPolicy.ORDERBOOK_TTL`
- `fun load(symbol: String): OrderbookSnapshot?`:
  - Read hash at `RedisKeyPolicy.orderbookKey(symbol)`
  - If empty → return null
  - Parse fields with safe `?.toBigDecimalOrNull()` (no `!!`)
  - Return `OrderbookSnapshot(bestBid, bestAsk, spread, bidDepthTopN, askDepthTopN, depthImbalance, timestamp)`

### 4. New Component: `OrderbookIngestMetrics`
File: `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/kis/service/OrderbookIngestMetrics.kt`

- `@Component` class
- Fields (all thread-safe):
  - `receivedCount: AtomicLong`
  - `parseFailCount: AtomicLong`
  - `lastReceivedAtMs: AtomicLong` (0 = never received)
- `fun recordReceived()` — increment receivedCount, set lastReceivedAtMs = `System.currentTimeMillis()`
- `fun recordParseFail()` — increment parseFailCount
- `fun snapshot(): OrderbookIngestSnapshot` — returns a data class with:
  - `receivedCount: Long`
  - `parseFailCount: Long`
  - `parseFailRatePct: Double` — `(parseFailCount.toDouble() / receivedCount * 100).coerceAtLeast(0.0)`, 0.0 if receivedCount=0
  - `lastReceivedAtMs: Long?` — null if 0
  - `lastReceivedAgeMs: Long?` — `System.currentTimeMillis() - lastReceivedAtMs` if non-null, else null

### 5. Update `RawEventPipeline` to wire OrderbookRedisStore + Metrics
File: `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/kis/pipeline/RawEventPipeline.kt`

- Constructor-inject `OrderbookRedisStore` and `OrderbookIngestMetrics`
- In the H0STASP0 branch:
  - Call `OrderbookIngestMetrics.recordReceived()` BEFORE parse attempt
  - On null parse result: call `OrderbookIngestMetrics.recordParseFail()` and return
  - On success: call `OrderbookRedisStore.save(event)`
- Wrap the save call in `runCatching { ... }.onFailure { log.warn(...) }` (same pattern as feature aggregation)

### 6. Write Tests (TDD — write tests first)

File: `backend/collector-api/src/test/kotlin/com/papertrading/collector/infra/redis/OrderbookRedisStoreTest.kt`

Test cases (use mock `StringRedisTemplate` and `ObjectMapper`):
- `save — orderbook:{symbol} Hash에 bestBid/bestAsk/spread/depth 저장 검증`
  - Verify `putAll` called with correct field map
  - Verify `expire` called with `ORDERBOOK_TTL`
- `depthImbalance 분모 0일 때 저장 필드 없음`
- `load — Hash 데이터 있을 때 OrderbookSnapshot 반환`
- `load — Hash 비어있을 때 null 반환`

File: `backend/collector-api/src/test/kotlin/com/papertrading/collector/application/kis/service/OrderbookIngestMetricsTest.kt`

Test cases:
- `recordReceived → receivedCount 증가, lastReceivedAtMs 갱신`
- `recordParseFail → parseFailCount 증가`
- `parseFailRatePct 계산 — receivedCount=0이면 0.0`
- `parseFailRatePct 계산 — 정상 비율 검증`

## Acceptance Criteria
```bash
cd backend/collector-api && ./gradlew test --tests "*.OrderbookRedisStoreTest" --tests "*.OrderbookIngestMetricsTest" --tests "*.RawEventPipelineTest" && ./gradlew compileKotlin
```
All target tests pass. `compileKotlin` succeeds.
