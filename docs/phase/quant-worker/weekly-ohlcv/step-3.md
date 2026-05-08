# Step 3: Testing and QA Verification
Assigned agent: test-engineer

## Working Directory
.worktrees/weekly-ohlcv

## Files to Read
- docs/phase/quant-worker/weekly-ohlcv/spec.md
- docs/phase/quant-worker/weekly-ohlcv/step-2.md
- backend/quant-worker/tests/**
- backend/collector-api/src/main/resources/db/migration/V13__create_market_weekly_ohlcv.sql

## Tasks
1. Validate weekly API contracts against spec:
- `POST /collect/weekly`
- `GET /market/weekly/{symbol}`
2. Validate boundary/error cases:
- invalid date range (`start > end`, `from > to`)
- blank symbol
- invalid limit (below/above bounds)
3. Validate Decimal-safe handling:
- repository input/output conversion checks for monetary and volume fields
4. Validate idempotent upsert behavior:
- repeated insert for same `(source, symbol, trade_date)` updates without duplicate rows
5. Validate migration quality:
- unique constraint and required indexes exist
6. Execute regression checks:
- quant-worker existing daily tests still pass
- collector-api compile remains green

## Verification
- `cd backend/quant-worker && pytest`
- `cd backend/collector-api && ./gradlew test --tests \"*MarketWeekly*\"` (if tests exist)
- `cd backend/collector-api && ./gradlew compileKotlin`

## Acceptance Criteria
- Spec-defined weekly behaviors verified.
- Error handling paths verified.
- No regression in existing daily flow.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
