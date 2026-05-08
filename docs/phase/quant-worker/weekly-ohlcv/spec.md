# quant-worker weekly OHLCV

## Strategy Overview
- Objective: extend existing daily ingestion so weekly bars can be collected and queried with identical operational patterns.
- Return/risk context: this is data infrastructure for downstream quant research; target is data correctness and reproducibility over speed.
- Investment universe: symbols already managed in collector-api catalogs (`yfinance_symbol_catalog`), default to yfinance provider for weekly.

## Alpha Factors
- Not in scope for this phase.
- Rationale: this phase only establishes weekly market data plumbing required before factor/backtest work.

## Trade-offs
- Option A: reuse `market_daily_ohlcv` with `interval='1wk'`.
- Option B: create dedicated `market_weekly_ohlcv` table and model.
- Decision: Option B.
- Why: clearer ownership for weekly-specific constraints, independent indexing, no accidental coupling with existing daily query/reporting paths.

## Backtesting Spec
- Not in scope for this phase.
- Constraint for future backtests: weekly bars must preserve deterministic date semantics (week anchor date from yfinance `1wk` output) and Decimal-safe numeric persistence.

## Risk Metrics
- Not in scope for this phase.
- Data quality constraints:
- No float/double for monetary values in storage or persistence conversion.
- Idempotent upsert on `(source, symbol, trade_date)`.
- API validation must reject invalid date windows (`from > to`) and invalid limits.

## Implementation Spec
### 1) Weekly DB schema and SQLAlchemy model
- Add table `market_weekly_ohlcv` in collector-api Flyway migration path:
- File: `backend/collector-api/src/main/resources/db/migration/V13__create_market_weekly_ohlcv.sql`
- Columns (daily-aligned):
- `id BIGSERIAL PRIMARY KEY`
- `source VARCHAR(16) NOT NULL` (`yfinance`)
- `symbol VARCHAR(32) NOT NULL`
- `market VARCHAR(32) NOT NULL`
- `trade_date DATE NOT NULL`
- `open_price NUMERIC(18, 6) NOT NULL`
- `high_price NUMERIC(18, 6) NOT NULL`
- `low_price NUMERIC(18, 6) NOT NULL`
- `close_price NUMERIC(18, 6) NOT NULL`
- `volume NUMERIC(20, 4) NOT NULL`
- `adj_close_price NUMERIC(18, 6) NULL`
- `provider VARCHAR(32) NOT NULL DEFAULT 'yfinance'`
- `interval VARCHAR(8) NOT NULL DEFAULT '1wk'`
- `is_adjusted BOOLEAN NOT NULL DEFAULT FALSE`
- `collected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`
- `created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`
- `updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`
- Unique key: `(source, symbol, trade_date)`
- Indexes:
- `(symbol, trade_date DESC)`
- `(source, trade_date DESC)`
- `(market, trade_date DESC)`
- `(provider)`

### 2) Weekly collector flow (`yfinance interval="1wk"`)
- Add collector module:
- `backend/quant-worker/src/collectors/yfinance_weekly_collector.py`
- Request model fields:
- `symbol`, `start_date`, `end_date`, `auto_adjust`
- Fetch:
- `yf.download(..., interval="1wk", start=..., end=end_date+1day, auto_adjust=..., threads=False)`
- Normalize output schema to match repository contract:
- `date`, `symbol`, `open`, `high`, `low`, `close`, `adj_close`, `volume`, `source`
- Ensure `date` is `datetime.date`, sorted ascending.

### 3) Decimal-safe persistence and weekly repository
- Add repository:
- `backend/quant-worker/src/repositories/market_weekly_ohlcv_repository.py`
- Follow `MarketDailyOhlcvRepository` pattern:
- Convert all numeric fields with `Decimal(str(value))`.
- Upsert into `market_weekly_ohlcv`.
- Context dataclass should include:
- `source`, `symbol`, `market`, `provider`, `interval='1wk'`, `is_adjusted`

### 4) Weekly batch job + application service
- Add job:
- `backend/quant-worker/src/jobs/catalog_weekly_fetch_job.py`
- Behavior:
- symbol iteration from existing catalog repository
- effective start = max(requested start, fetched_until_date+1day when available)
- call weekly collector
- upsert repository rows
- return result set with success/skipped/error
- Add app service:
- `backend/quant-worker/src/application/weekly_fetch_service.py`
- Add options dataclass and `execute(...)` returning summary:
- `provider, symbols, success_symbols, failed_symbols, total_rows_inserted, start, end`
- Provider support for weekly endpoint:
- `yfinance` and `all` accepted, but weekly collection executes yfinance branch only.

### 5) API contract
- Extend FastAPI app:
- `backend/quant-worker/src/interfaces/api/app.py`

- `POST /collect/weekly`
- Request body:
- `provider: Literal["yfinance","all"] = "yfinance"`
- `start: str = "2010-01-01"`
- `end: str = <today>`
- `only_default: bool = false`
- `auto_adjust: bool = false`
- Response body:
- `provider: str`
- `symbols: int`
- `success_symbols: int`
- `failed_symbols: int`
- `total_rows_inserted: int`
- `start: str`
- `end: str`
- Error policy:
- 400 for invalid input/date window
- 500 for unexpected runtime errors

- `GET /market/weekly/{symbol}`
- Query params:
- `source: str = "yfinance"`
- `from: date | null` (default: `to - 1 year`)
- `to: date | null` (default: today)
- `limit: int = 260` (safe clamp: 1..520)
- Response:
- ordered ascending weekly bars with fields:
- `source, symbol, market, trade_date, open_price, high_price, low_price, close_price, volume, adj_close_price, provider, interval, is_adjusted, collected_at`
- Validation:
- if trimmed symbol is blank -> 400
- if `from > to` -> 400

### 6) Collector-api weekly read model/API
- Add domain/JPA entity:
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/entity/market/MarketWeeklyOhlcv.kt`
- Add repository/query service/controller/dto mirroring daily stack:
- `.../infra/market/persistence/MarketWeeklyOhlcvRepository.kt`
- `.../application/market/service/MarketWeeklyOhlcvQueryService.kt`
- `.../presentation/market/MarketWeeklyOhlcvController.kt`
- `.../presentation/market/dto/MarketWeeklyOhlcvDto.kt`
- Endpoint contract:
- `GET /api/market/weekly/{symbol}?source=yfinance&from=YYYY-MM-DD&to=YYYY-MM-DD&limit=260`

### 7) Migration strategy
- Keep current pattern where collector-api owns schema via Flyway.
- quant-worker writes directly into collector-api-managed PostgreSQL tables.
- Do not alter existing `market_daily_ohlcv` behavior.
- Weekly addition must be fully backward-compatible.
