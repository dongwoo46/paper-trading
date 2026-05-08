# Market Microstructure + Relative Strength API

## Core Feature
Expand collector-api market query surface with a unified API contract for microstructure (quote/depth + trade-intensity metrics) and relative strength (RS) versus benchmark/sector, excluding news/disclosure sources.

## Considerations
- Priority is unblocking `front/market-unified-indicators-ui` with a stable schema the client can consume directly.
- Existing features already provide bar history and indicators; this phase extends read APIs and must not break existing contracts.
- Domestic/overseas symbols must share one contract with explicit `session` and timezone normalization.
- Missing-data behavior must be explicit for sparse intraday snapshots and unavailable benchmark series.

## Trade-offs
- Option A: Separate APIs per market (KR/US) and per metric family.
- Option B: Single contract with source-specific optional fields and normalized metadata.
- Chosen: Option B for MVP consistency, lower frontend branching cost, and easier future expansion.

## Implementation Approach
- Presentation: add market microstructure + RS query endpoint(s) with one response schema.
- Application: orchestration service validating query, resolving data sources, assembling normalized response.
- Domain: VO set for session/timezone normalization, microstructure snapshot, RS series point.
- Infra: compose from existing Redis latest/bar data + persisted daily/weekly OHLCV + symbol metadata/catalog.

## Workflow
1. Client calls `GET /api/market/microstructure/{symbol}` with market/session/range options.
2. Service validates symbol, interval/session, timezone, and requested metric set.
3. Service loads quote/depth + buy/sell volume/vwap/rvol metrics from latest feature sources.
4. Service resolves benchmark/sector baseline and computes RS ratio/relative return for same time grid.
5. Service returns unified payload with deterministic null/missing policy.

## Functional Requirements
- Microstructure metrics:
  - best bid/ask, spread, bid/ask imbalance, depth summary
  - buyVolume, sellVolume, tradeIntensity, vwap, rvol
- Relative strength metrics:
  - symbol vs benchmark/sector ratio
  - period return delta (symbol return - baseline return)
- Cross-market contract:
  - session (`regular|pre|after`) and timezone normalization in response metadata
  - consistent error contract regardless of market source

## Non-Functional Requirements
- BigDecimal-based numeric calculations for price/ratio fields.
- Validation-first error handling (400/404/422 stable codes).
- Bounded query cost with explicit limit cap.

## Clarified Assumptions
- If `session` is omitted: default `regular`.
- If benchmark is omitted: use market default benchmark (KR: KOSPI200 proxy, US: SPY proxy) configured server-side.
- If both `limit` and `from/to` are missing: default `limit=200`.

## DDD Model
- Bounded Context: `market-analytics` in collector-api.
- Value Objects:
  - `MarketSession` (`regular|pre|after`)
  - `QueryInterval` (`1m|5m|10m|1d|1w`)
  - `RangePolicy` (`limit` xor `from/to`)
  - `MicrostructureSnapshot`
  - `RelativeStrengthPoint`
- Aggregate:
  - `MarketAnalyticsQuery` validates invariants and parameter combinations.
- Domain Event:
  - `MarketAnalyticsQueried` (internal observability event only).

## API Contract
### GET /api/market/microstructure/{symbol}
Query parameters:
- `interval`: `1m|5m|10m|1d|1w` (required)
- `session`: `regular|pre|after` (optional)
- `limit`: integer (optional, min 1, max 2000)
- `from`, `to`: ISO-8601 (optional, pair-only)
- `benchmark`: string (optional)
- `sector`: string (optional)

Response 200 shape:
- `symbol`, `interval`, `session`, `timezone`
- `range`: `{ from, to, requestedLimit, actualCount }`
- `microstructure`:
  - `bestBid`, `bestAsk`, `spread`, `bidAskImbalance`
  - `depth`: `{ bidDepthTopN, askDepthTopN, depthImbalance }`
  - `flow`: `{ buyVolume, sellVolume, tradeIntensity, vwap, rvol }`
- `relativeStrength`:
  - `baseline`: `{ benchmark, sector }`
  - `series`: `[{ timestamp, ratio, returnDelta }]`
- `meta`: `{ missingPolicy, warnings }`

Errors:
- 400 `INVALID_INTERVAL`
- 400 `INVALID_PERIOD_QUERY`
- 400 `INVALID_SESSION`
- 404 `SYMBOL_NOT_FOUND_OR_NO_DATA`
- 422 `INSUFFICIENT_DATA_FOR_RS`

## Data Source and Policy
- Redis/latest + feature keys provide intraday microstructure snapshots.
- PostgreSQL daily/weekly bars provide baseline return calculations.
- Missing policy:
  - snapshot fields unavailable at timestamp -> null field values, not dropped points.
  - RS unavailable due to baseline shortage -> `relativeStrength.series` null values with warning.

## Out of Scope
- News/disclosure ingestion and sentiment signals.
- Long-term tick raw storage redesign.