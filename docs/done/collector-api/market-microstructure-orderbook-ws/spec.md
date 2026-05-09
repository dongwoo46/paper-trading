# Market Microstructure — Orderbook WebSocket Ingest & Mapping

## Core Feature
Add KIS H0STASP0 (실시간 호가잔량) WebSocket channel parsing, store top-N depth to a dedicated `orderbook:{symbol}` Redis Hash, map real bid/ask/spread/depth data into the existing `GET /api/market/microstructure/{symbol}` response, and close the known stability gaps (1d/1w DB-source branching, RS timestamp ordering, ingest health metrics).

---

## Considerations
- The existing pipeline (`KisRawEventParser` + `RawEventPipeline`) only handles `H0STCNT0` (체결가). `bestBid`, `bestAsk`, `spread`, `bidDepthTopN`, `askDepthTopN`, `depthImbalance` are hardcoded null in `MarketMicrostructureQueryService.loadSnapshot()`.
- `KisProperties.trIds` already supports a list; adding `H0STASP0` requires no structural change to the WebSocket subscription flow — only a new TR_ID entry in config and a new parser branch in `KisRawEventParser`.
- The `quote:{symbol}` Hash already stores `askp1/bidp1` from H0STCNT0 (single-level, from trade event). These are insufficient for depth. A separate `orderbook:{symbol}` key is required.
- Health/metrics for the ingest pipeline are currently zero-instrumentation. Step A (from TODO) requires received count, parse failure rate, and latency to be observable.
- `RelativeStrengthCalculator` does not validate that input timestamp arrays are sorted. An unsorted RS series is a silent correctness bug.
- `MarketBarSourceResolver` separates intraday (Redis) vs. daily/weekly (Postgres). The `MarketMicrostructureQueryService` uses `MarketBarRepository` directly (not `MarketBarSourceResolver`), meaning there is no enforced 1d/1w DB-source branch for the microstructure API. This regression path needs a test.

---

## Trade-offs

### A: Orderbook key — reuse `quote:{symbol}` vs. separate `orderbook:{symbol}`
- Option A: Store depth fields in the existing `quote:{symbol}` Hash.
  - Pro: One key, one TTL.
  - Con: Conflates two different event sources (H0STCNT0 vs H0STASP0), different update frequencies, schema bloat, harder to reason about staleness separately.
- Option B: Separate `orderbook:{symbol}` Hash.
  - Pro: Clean separation, independent TTL, explicit schema, easier to extend to more levels.
  - Con: One more Redis key per symbol.
- **Chosen: Option B.** The schema contract for depth data is distinct from the quote scalar contract. Independent TTL (30s for orderbook, 60s for quote) is necessary since the two streams update at different rates.

### B: H0STASP0 parser — add to existing `KisRawEventParser` vs. new class
- Option A: Add an `if (trId == "H0STASP0")` branch inside `KisRawEventParser`.
  - Pro: Minimizes file count.
  - Con: Parser class grows with heterogeneous responsibilities; two unrelated formats in one file.
- Option B: Separate `KisOrderbookEventParser` class handling only H0STASP0.
  - Pro: Single-responsibility, independently testable, easier to find.
  - Con: One more class.
- **Chosen: Option B.** Each KIS TR_ID has its own field layout; keeping parsers separate prevents field-index coupling errors.

### C: Depth levels to store (topN) — fixed 5 vs. configurable
- Option A: Store all 10 KIS levels in Redis.
  - Pro: Maximum fidelity.
  - Con: 20 fields per symbol, more Redis memory, current API contract only exposes a scalar summary.
- Option B: Store top-5 per side as a JSON array, plus scalar aggregates.
  - Pro: Bounded size, matches current API contract (topN as a single decimal), extensible later to return array.
- **Chosen: Option B — top-5 per side stored as JSON array in a single field, plus scalar aggregate (sum of top-5 quantities).** The API response field `bidDepthTopN`/`askDepthTopN` remains a scalar (total top-5 qty) for backward compatibility.

### D: Ingest metrics — in-memory AtomicLong vs. Micrometer
- Option A: Micrometer counters/timers (requires Micrometer dep, actuator config).
- Option B: In-memory AtomicLong counters in a new `OrderbookIngestMetrics` component, exposed via existing `KisWsHealthController` extension or a new endpoint.
- **Chosen: Option B** for MVP. Micrometer is not yet wired. An in-memory counter is sufficient and zero-infra-dependency. A dedicated endpoint `GET /api/internal/kis/orderbook/ingest-health` exposes it.

---

## DDD Model

### Bounded Context
`market-ingest` (infra/application layer, collector-api). No new aggregate — orderbook data is infrastructure-layer state, not a domain entity.

### New Value Objects (domain layer)
- `KisOrderbookEvent` — parsed H0STASP0 result:
  - `ticker: String`
  - `askPrices: List<BigDecimal>` (top-5, index 0 = best ask)
  - `bidPrices: List<BigDecimal>` (top-5, index 0 = best bid)
  - `askQtys: List<BigDecimal>` (top-5 quantities)
  - `bidQtys: List<BigDecimal>` (top-5 quantities)
  - `receivedAt: Instant`

### Derived scalars (computed at save time)
- `bestAsk = askPrices[0]`
- `bestBid = bidPrices[0]`
- `spread = bestAsk - bestBid`
- `askDepthTopN = sum(askQtys[0..4])`
- `bidDepthTopN = sum(bidQtys[0..4])`
- `depthImbalance = (bidDepthTopN - askDepthTopN) / (bidDepthTopN + askDepthTopN)`, null if denominator = 0

### New Infrastructure Components
- `KisOrderbookEventParser` (application/kis/pipeline) — parses H0STASP0 payload
- `OrderbookRedisStore` (infra/redis) — saves `orderbook:{symbol}` Hash, reads back `OrderbookSnapshot`
- `OrderbookIngestMetrics` (application/kis/service) — AtomicLong counters: receivedCount, parseFailCount, lastReceivedAtMs
- `OrderbookIngestHealthController` (presentation/kis) — GET endpoint for metrics

### Modified Components
- `KisRawEventParser` — route H0STASP0 messages to `KisOrderbookEventParser` (or `RawEventPipeline` dispatches based on TR_ID)
- `RawEventPipeline` — call `OrderbookRedisStore.save()` + `OrderbookIngestMetrics.record()` for H0STASP0 events
- `KisProperties.trIds` — default now includes `"H0STASP0"` (via config, not hardcoded)
- `MarketMicrostructureQueryService.loadSnapshot()` — read `bestBid/bestAsk/spread/depth` from `OrderbookRedisStore`
- `RelativeStrengthCalculator.calculate()` — validate timestamps are sorted ascending before computing

---

## Implementation Approach

### Layer summary
- **Infrastructure (infra/redis)**: `OrderbookRedisStore` — save/load `orderbook:{symbol}` Hash; expose `OrderbookSnapshot` data class.
- **Application (application/kis/pipeline)**: `KisOrderbookEventParser` — parse H0STASP0 fields, return `KisOrderbookEvent?`; update `RawEventPipeline` to branch on TR_ID.
- **Application (application/kis/service)**: `OrderbookIngestMetrics` — in-memory counters.
- **Application (application/market/service)**: `MarketMicrostructureQueryService.loadSnapshot()` — inject `OrderbookRedisStore`, map orderbook fields to `MarketMicrostructureSnapshot`.
- **Domain (domain/market/analytics)**: `RelativeStrengthCalculator` — add timestamp sort guard.
- **Presentation (presentation/kis)**: `OrderbookIngestHealthController` — exposes ingest metrics.

---

## Workflow

### Ingest path (H0STASP0)
1. `KisWebSocketCollector` receives raw WS message → `RawEventPipeline.publish(source, payload)`
2. `RawEventPipeline` inspects TR_ID in payload header section:
   - `H0STCNT0` → existing path (`KisRawEventParser` + `QuoteRedisPublisher` + feature aggregation)
   - `H0STASP0` → new path (`KisOrderbookEventParser` → `OrderbookRedisStore.save()` + `OrderbookIngestMetrics.record()`)
3. `OrderbookRedisStore` writes `orderbook:{symbol}` Hash with TTL=30s.

### Query path (GET /api/market/microstructure/{symbol})
1. `MarketMicrostructureController` → `MarketMicrostructureQueryService.query()`
2. `loadSnapshot()`: if interval is intraday, read `OrderbookRedisStore.load(symbol)` → populate `bestBid/bestAsk/spread/depth*`; also read `MarketFeatureStore` → populate `buyVolume/sellVolume/vwap/rvol`
3. If interval is `1d` or `1w`, `loadSnapshot()` returns all-null microstructure (existing behavior, now regression-tested).

---

## API

### Unchanged (same contract as previous phase)
`GET /api/market/microstructure/{symbol}` — same request/response shape as previous phase.

Fields now populated (previously null for intraday):
- `microstructure.bestBid` — from `orderbook:{symbol}.bestBid`
- `microstructure.bestAsk` — from `orderbook:{symbol}.bestAsk`
- `microstructure.spread` — from `orderbook:{symbol}.spread`
- `microstructure.depth.bidDepthTopN` — from `orderbook:{symbol}.bidDepthTopN`
- `microstructure.depth.askDepthTopN` — from `orderbook:{symbol}.askDepthTopN`
- `microstructure.depth.depthImbalance` — from `orderbook:{symbol}.depthImbalance`

### New endpoint
`GET /api/internal/kis/orderbook/ingest-health`

Response 200:
```json
{
  "receivedCount": 1240,
  "parseFailCount": 2,
  "parseFailRatePct": 0.16,
  "lastReceivedAtMs": 1746791234567,
  "lastReceivedAgeMs": 312
}
```

Fields:
- `receivedCount: Long` — total H0STASP0 frames received since startup
- `parseFailCount: Long` — frames that failed parsing
- `parseFailRatePct: Double` — (parseFailCount / receivedCount * 100), 0.0 if receivedCount=0
- `lastReceivedAtMs: Long?` — epoch ms of last received frame, null if none
- `lastReceivedAgeMs: Long?` — ms since last received frame, null if none

Errors: none (always 200; returns zero state if no frames received)

---

## Redis Schema

### `orderbook:{symbol}` (Hash)
Written by: `OrderbookRedisStore.save()`
Read by: `OrderbookRedisStore.load()` → `MarketMicrostructureQueryService.loadSnapshot()`

| Field | Type (stored) | Description |
|---|---|---|
| `bestBid` | String (BigDecimal plain) | Best bid price (bidPrices[0]) |
| `bestAsk` | String (BigDecimal plain) | Best ask price (askPrices[0]) |
| `spread` | String (BigDecimal plain) | bestAsk - bestBid |
| `bidDepthTopN` | String (BigDecimal plain) | Sum of top-5 bid quantities |
| `askDepthTopN` | String (BigDecimal plain) | Sum of top-5 ask quantities |
| `depthImbalance` | String (BigDecimal plain) or absent | (bid - ask) / (bid + ask); absent if zero denominator |
| `bidLevels` | String (JSON array) | Top-5 bid `[{price, qty}]` |
| `askLevels` | String (JSON array) | Top-5 ask `[{price, qty}]` |
| `source` | String | Fixed "H0STASP0" |
| `timestamp` | String (ISO-8601 Instant) | receivedAt from `KisOrderbookEvent` |

TTL: **30 seconds** (independent from `quote:{symbol}` 60s TTL)
Key function: `RedisKeyPolicy.orderbookKey(symbol: String): String = "orderbook:$symbol"`

### Missing/null policy
- If `orderbook:{symbol}` key does not exist (no data received or TTL expired): `bestBid/bestAsk/spread/depth*` fields in response are null (not error).
- `depthImbalance` absent from hash → null in response.

### Existing keys (unchanged)
| Key pattern | Purpose | TTL |
|---|---|---|
| `quote:{symbol}` | Latest tick scalar (price, askp1, bidp1) | 60s |
| `agg:1m:{symbol}:current` | Current 1m bar aggregation state | 180s |
| `bars:1m:{symbol}` | List of recent 1m bars | 12h |
| `feature:{symbol}:{window}` | Feature snapshot (vwap, imbalance, etc.) | 30m |

---

## RS Timestamp Sort Validation

`RelativeStrengthCalculator.calculate()` must validate that `timestamps` list is sorted in ascending order before computing. If not sorted, throw `IllegalArgumentException("INVALID_PERIOD_QUERY")`. This guards against upstream data ordering bugs surfaced in the previous phase's code review.

---

## 1d/1w DB-Source Branch Regression

`MarketMicrostructureQueryService.query()` calls `marketBarRepository.findBars(symbol, interval.value, limit)`. For `1d`/`1w`, `MarketBarRepository` is implemented by `MarketBarRedisRepository` which returns `emptyList()` for intervals not in `{1m, 5m, 10m}`. This means a `1d`/`1w` request will throw `SymbolNotFoundOrNoDataException` regardless of whether DB data exists — which is incorrect behavior (the query never reaches Postgres).

Resolution: `MarketMicrostructureQueryService` must inject and use `MarketBarSourceResolver` to route `1d`/`1w` to `PostgresDailyWeeklyBarQueryRepository`. A regression test must be added confirming `1d`/`1w` hits the Postgres source path, not the Redis path.

---

## Out of Scope
- Storing full 10-level orderbook history (only latest snapshot)
- WebSocket reconnection/subscription changes (existing retry logic is sufficient)
- Overseas symbol orderbook (KIS domestic only; H0STASP0 is KR market)
- Micrometer/Prometheus integration
