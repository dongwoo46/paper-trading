# Step 2: Implementation (TDD)
Assigned agent: quant-dev

## Working Directory
.worktrees/weekly-ohlcv

## Files to Read
- docs/phase/quant-worker/weekly-ohlcv/spec.md
- backend/quant-worker/src/interfaces/api/app.py
- backend/quant-worker/src/collectors/yfinance_daily_collector.py
- backend/quant-worker/src/jobs/catalog_daily_fetch_job.py
- backend/quant-worker/src/repositories/market_daily_ohlcv_repository.py
- backend/collector-api/src/main/resources/db/migration/V8__create_market_daily_ohlcv.sql

## Tasks
1. Write tests first for weekly flow (Red):
- `backend/quant-worker/tests/collectors/test_yfinance_weekly_collector.py`
- `backend/quant-worker/tests/repositories/test_market_weekly_ohlcv_repository.py`
- `backend/quant-worker/tests/jobs/test_catalog_weekly_fetch_job.py`
- `backend/quant-worker/tests/application/test_weekly_fetch_service.py`
- `backend/quant-worker/tests/interfaces/test_weekly_api.py`
2. Implement yfinance weekly collector with `interval="1wk"`:
- `backend/quant-worker/src/collectors/yfinance_weekly_collector.py`
3. Implement Decimal-safe weekly repository upsert:
- `backend/quant-worker/src/repositories/market_weekly_ohlcv_repository.py`
4. Implement catalog weekly fetch job:
- `backend/quant-worker/src/jobs/catalog_weekly_fetch_job.py`
5. Implement weekly application service:
- `backend/quant-worker/src/application/weekly_fetch_service.py`
6. Extend FastAPI with:
- `POST /collect/weekly`
- `GET /market/weekly/{symbol}`
- File: `backend/quant-worker/src/interfaces/api/app.py`
7. Add collector-api migration for weekly table:
- `backend/collector-api/src/main/resources/db/migration/V13__create_market_weekly_ohlcv.sql`
8. Add collector-api weekly query stack:
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/entity/market/MarketWeeklyOhlcv.kt`
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/market/persistence/MarketWeeklyOhlcvRepository.kt`
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/market/service/MarketWeeklyOhlcvQueryService.kt`
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/market/MarketWeeklyOhlcvController.kt`
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/market/dto/MarketWeeklyOhlcvDto.kt`
9. Ensure monetary and volume conversions use `Decimal` (Python) / `BigDecimal` (Kotlin) only.

## Verification
- Run:
- `cd backend/quant-worker && pytest`
- `cd backend/quant-worker && python -m py_compile src/interfaces/api/app.py`
- `cd backend/collector-api && ./gradlew compileKotlin`
- Confirm tests created in step 1 fail before implementation and pass after.

## Acceptance Criteria
- Weekly collector/repository/job/service/API implemented.
- Weekly table migration added with daily-aligned schema and indexes.
- `POST /collect/weekly` and `GET /market/weekly/{symbol}` operational.
- All newly added tests pass.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
