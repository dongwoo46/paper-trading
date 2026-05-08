# Market Indicators API

## Core Feature
Provide BB/RSI/MACD indicator series computed server-side from market bars and expose them through a unified query API for chart clients.

## Considerations
- Priority is API contract stability for `front/market-unified-indicators-ui`.
- Existing collector-api already has daily OHLCV persistence and Redis-based short-interval bars/features; this feature must not break those paths.
- Indicator calculations must support `1m/5m/10m/1d/1w` with `limit` and `from~to` period filters.
- Missing-data policy must be explicit and deterministic for warm-up windows and sparse bars.
- Monetary/price math must use BigDecimal.

## Trade-offs
- Option A: Persist full indicator history in PostgreSQL.
- Option B: Compute on read using Redis for intraday + PostgreSQL for daily/weekly.
- Chosen: Option B for MVP, because it avoids schema bloat and keeps recalculation logic centralized. Redis TTL + backfill contract controls cache growth.

## Implementation Approach
- Presentation layer: Add a market indicators controller and DTOs for multi-indicator query.
- Application layer: Add indicator query service coordinating bar loading, parameter validation, and indicator engine.
- Domain layer: Add indicator parameter/value VO set and pure calculation components (BB/RSI/MACD).
- Infra layer: Add bar-source abstraction over Redis time-series keys for intraday and existing JPA repository for daily/weekly.

## Workflow
1. Client calls `GET /api/market/indicators/{symbol}` with interval, period, and indicator selection.
2. Service validates symbol/interval/period/indicator params.
3. Service resolves bar source by interval:
   - `1m/5m/10m` -> Redis bars key space.
   - `1d/1w` -> PostgreSQL OHLCV repository (source-specific mapping).
4. Service computes requested indicators on the aligned bar timeline.
5. Service returns series points with null-safe warm-up handling and metadata.

## Requirements and Ambiguities
- Functional requirements:
  - Multi-indicator single call (BB, RSI, MACD combinable).
  - Interval support: `1m`, `5m`, `10m`, `1d`, `1w`.
  - Period support: `limit` or `from`+`to` (mutually exclusive).
  - Chart-friendly response: timestamp-aligned values with nulls for unavailable windows.
- Non-functional requirements:
  - Deterministic formula behavior across intervals.
  - Validation-first error responses (400) for unsupported combinations.
  - Query-timeouts bounded by max period limit.
- Clarified assumptions (to unblock implementation):
  - Timezone is KST for interval boundary interpretation.
  - `1w` bars use week start Monday.
  - If both `limit` and `from/to` are missing, default `limit=200`.

## DDD Model
- Bounded Context: `market-indicators` inside collector-api market domain.
- Entity:
  - `IndicatorSeriesSnapshot` (identity: symbol + interval + indicatorSet + queryRange hash) for response assembly only (non-persistent aggregate output).
- Value Objects:
  - `Interval` (`1m|5m|10m|1d|1w`)
  - `Period` (`limit` or `from/to`)
  - `BollingerBandParams` (`period`, `stdDevMultiplier`)
  - `RsiParams` (`period`)
  - `MacdParams` (`fastPeriod`, `slowPeriod`, `signalPeriod`)
  - `IndicatorPoint` (timestamp + optional values)
- Aggregate:
  - `IndicatorQuery` aggregate validates full request invariants before execution.
- Domain Event:
  - `IndicatorSeriesComputed` (internal app event for observability/audit, no external broker dependency).

## API
GET /api/market/indicators/{symbol} - Query indicator series for one symbol.

Request query params:
- `interval`: `1m|5m|10m|1d|1w` (required)
- `limit`: integer (optional, min 1, max 2000)
- `from`: ISO-8601 datetime/date depending on interval (optional)
- `to`: ISO-8601 datetime/date depending on interval (optional)
- `indicators`: comma-separated subset of `bb,rsi,macd` (required)
- `bbPeriod`: integer optional, default 20
- `bbStdDev`: decimal optional, default 2.0
- `rsiPeriod`: integer optional, default 14
- `macdFast`: integer optional, default 12
- `macdSlow`: integer optional, default 26
- `macdSignal`: integer optional, default 9

Response 200 shape:
- `symbol`: string
- `interval`: string
- `range`: `{ from, to, requestedLimit, actualCount }`
- `series`: array of
  - `timestamp`: string
  - `close`: string
  - `bb`: `{ middle, upper, lower } | null`
  - `rsi`: `{ value } | null`
  - `macd`: `{ macd, signal, histogram } | null`
- `meta`:
  - `missingPolicy`: `null_until_window_ready`
  - `warnings`: string[]

Errors:
- 400 `INVALID_INTERVAL`
- 400 `INVALID_PERIOD_QUERY` (both limit and from/to provided, or malformed range)
- 400 `INVALID_INDICATOR_PARAM` (e.g., macdFast >= macdSlow)
- 404 `SYMBOL_NOT_FOUND_OR_NO_BARS`
- 422 `INSUFFICIENT_BARS_FOR_REQUESTED_RANGE`

## Indicator Formula and Missing-data Policy
- Bollinger Bands (BB):
  - Middle = SMA(close, bbPeriod)
  - StdDev = population stddev on same window
  - Upper = Middle + bbStdDev * StdDev
  - Lower = Middle - bbStdDev * StdDev
  - Missing policy: first `bbPeriod-1` points are `bb=null`.
- RSI:
  - Wilder smoothing RSI with period `rsiPeriod`.
  - Missing policy: first `rsiPeriod` points are `rsi=null`.
- MACD:
  - EMA(fast)-EMA(slow), signal=EMA(macd, signalPeriod), hist=macd-signal
  - Missing policy: null until slow EMA and signal EMA warm-up satisfied.
- Cross-indicator alignment:
  - Return all requested timestamps.
  - Indicator field null independently if only that indicator lacks enough lookback.

## Redis and DB Schema / Access Policy
- Redis key naming:
  - Intraday bars source: `bars:1m:{symbol}`, `bars:5m:{symbol}`, `bars:10m:{symbol}`
  - Optional cached indicators: `indicator:{interval}:{symbol}:{indicator}:{paramHash}`
- Redis TTL:
  - `bars:1m:*` keep 7d equivalent maxlen/ttl.
  - `bars:5m:*`, `bars:10m:*` keep 30d equivalent maxlen/ttl.
  - `indicator:*` TTL 10m (read-through cache).
- Backfill contract:
  - On Redis miss/insufficient bars for intraday, return 422 without silent backfill.
  - For `1d/1w`, query PostgreSQL via existing market daily repository; weekly can be composed from daily if dedicated weekly source unavailable.
- PostgreSQL:
  - Reuse `market_daily_ohlcv` as authoritative store for daily/weekly source data.
  - No new persistent table in this phase.

## External Dependencies
- RedisTemplate access for intraday bars.
- Existing `MarketDailyOhlcvRepository` for daily/weekly.
- No new third-party indicator library; use domain pure calculators for deterministic tests.
