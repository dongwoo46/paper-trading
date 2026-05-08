# Weekly OHLCV Phase Summary

Date: 2026-05-08  
Phase: `quant-worker/weekly-ohlcv`  
Branch: `feature/quant-worker-weekly-ohlcv`

## Implemented Scope
- Added weekly ingestion pipeline with yfinance `interval="1wk"`:
  - collector: `backend/quant-worker/src/collectors/yfinance_weekly_collector.py`
  - repository (Decimal-safe upsert): `backend/quant-worker/src/repositories/market_weekly_ohlcv_repository.py`
  - catalog job/service: `backend/quant-worker/src/jobs/catalog_weekly_fetch_job.py`, `backend/quant-worker/src/application/weekly_fetch_service.py`
- Added quant-worker API endpoints:
  - `POST /collect/weekly`
  - `GET /market/weekly/{symbol}`
- Added collector-api DB schema migration:
  - `backend/collector-api/src/main/resources/db/migration/V13__create_market_weekly_ohlcv.sql`
- Added weekly test coverage across collector/repository/job/service/API/migration checks.

## Schema and API Contract Decisions
- Chose dedicated table `market_weekly_ohlcv` (not `market_daily_ohlcv` reuse) for weekly ownership and indexing isolation.
- Enforced unique key `(source, symbol, trade_date)` for idempotent upsert.
- Added indexes aligned with query/access patterns:
  - `(symbol, trade_date DESC)`
  - `(source, trade_date DESC)`
  - `(market, trade_date DESC)`
  - `(provider)`
- Weekly API contract:
  - `POST /collect/weekly`: supports `provider=yfinance|all`, date window validation, summary response.
  - `GET /market/weekly/{symbol}`: validates blank symbol and date window, clamps limit into safe range, ascending result ordering.

## Test and Compile Evidence
- `cd backend/quant-worker && pytest` -> `39 passed`.
- `cd backend/quant-worker && python -m py_compile src/interfaces/api/app.py` -> success.
- `cd backend/collector-api && ./gradlew compileKotlin --no-daemon` -> `BUILD SUCCESSFUL`.

## Review Findings and Resolutions
- Step 4 review result: no critical/high findings.
- Residual risk noted: collector-api weekly endpoint-specific tests are still limited.
- Resolution in this phase: regression/contract guards are covered in quant-worker weekly API and migration tests; follow-up collector-api endpoint test expansion remains recommended.

## PR Details
- Suggested title: `feat(quant-worker): add weekly OHLCV collection/storage/query pipeline`
- Suggested description:
  - Add weekly yfinance collection flow (`interval=1wk`) and Decimal-safe persistence.
  - Add `POST /collect/weekly` and `GET /market/weekly/{symbol}` contracts with validation and bounded limits.
  - Add collector-api Flyway migration `V13__create_market_weekly_ohlcv.sql` with unique key and indexes.
  - Include weekly TDD/QA coverage for collector/repository/job/service/API/migration.
- Migration note:
  - New table only (`market_weekly_ohlcv`); no destructive change and no behavior change to existing daily flow.
- API contract changes:
  - New endpoints in quant-worker for weekly collect/query.
- Rollout/backfill notes:
  - Rollout is backward-compatible.
  - Backfill can be executed by calling `POST /collect/weekly` with default wide date range (or controlled start/end windows) after migration deployment.
