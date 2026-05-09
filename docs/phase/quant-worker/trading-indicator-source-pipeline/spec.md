# quant-worker trading indicator source pipeline

## Strategy Overview
- Objective: build source-data ingestion for trading indicators required by unified chart UX (excluding news/disclosures).
- Scope focus: data correctness, schema standardization, and collector-api-ready storage contract.
- Dependency alignment: this phase must complete before `front/market-unified-indicators-ui` investor-flow panel and advanced microstructure overlays.

## Data Domains
### KR market source set
- Orderbook depth snapshot and spread source data.
- Trade intensity inputs (`buyVolume`, `sellVolume`, `tradeCount`) and VWAP inputs (`tradeValue`, `volume`).
- Relative-strength baseline inputs (symbol return vs benchmark/sector return series).

### US market source set
- Session-aware OHLCV (`regular`, `pre`, `after`) with timezone-normalized timestamps.
- Bid/ask/spread/depth where provider supports it; nullable policy when unavailable.
- VWAP/RVOL source inputs.

### Cross-market alternative flow inputs
- `short_interest`, `days_to_cover`, `shares_outstanding`, `float_shares` collection pipeline.
- Symbol metadata normalization (`exchange`, `currency`, `timezone`, `market_cap`, `free_float`).

## Storage Contract
- collector-api Flyway-owned schema is retained; quant-worker writes to PostgreSQL directly.
- Source tables (proposed):
  - `market_microstructure_source`
  - `market_session_ohlcv_source`
  - `market_relative_strength_source`
  - `market_flow_alternative_source`
  - `market_symbol_metadata`
- Shared constraints:
  - `source`, `symbol`, `as_of` (or session date/time) as canonical identity.
  - Decimal-safe monetary/volume fields only.
  - Unique key + upsert policy for idempotent reruns.
  - Timestamp columns: `collected_at`, `created_at`, `updated_at`.

## Index / Partition Strategy
- Time-series dominant tables index by `(symbol, as_of DESC)` and `(source, as_of DESC)`.
- Session table index by `(symbol, session, as_of DESC)`.
- RS table index by `(symbol, benchmark_symbol, as_of DESC)`.
- Partition guidance:
  - Monthly partitions for high-volume intraday source tables.
  - Yearly partitions for daily/weekly alternative-flow tables.

## API/Consumer Readiness Contract
- This phase delivers source ingestion + DB contract; consumer API expansion stays in collector-api follow-up steps.
- Output schema must be directly mappable to existing collector-api query DTO rules (BigDecimal/ISO datetime/null-safe).

## Risk / Constraints
- Provider capability differs by market; unsupported fields must be persisted as null with provenance marker.
- No float persistence; use Decimal conversion with string-safe casting.
- Retry-safe ingestion required for scheduled reruns.

## Deliverables
1. Spec-defined schema + migration plan.
2. quant-worker collectors/repositories/jobs/services for the 5 source domains.
3. Targeted tests for conversion/validation/idempotent upsert.
4. collector-api handoff notes in summary step.
