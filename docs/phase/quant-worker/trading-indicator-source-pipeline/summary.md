# trading-indicator-source-pipeline Summary

## Delivered Scope
- Implemented source ingestion pipeline for 5 domains:
  - market microstructure
  - session OHLCV (regular/pre/after)
  - relative strength
  - alternative flow
  - symbol metadata
- Added/updated migration for source tables, constraints, and indexes in `collector-api` Flyway scope.
- Implemented collector normalization for session canonical values (`regular|pre|after`).
- Enforced decimal-safe ingestion and idempotent upsert behavior.
- Aligned null-safe contract for `relative_strength` across collector/repository/schema.

## Verification Commands
- `cd backend/quant-worker && pytest tests/ -k "indicator or microstructure or flow or metadata" -v --tb=short`
- `cd backend/collector-api && ./gradlew test --tests "*Market*"`

## Latest Verification Result
- Quant worker targeted tests: passed (18 passed).
- Collector API market tests: BUILD SUCCESSFUL.

## Rollout / Backfill Notes
- Run ingestion in staged mode by market/provider batches to avoid write spikes.
- Backfill high-volume intraday tables by date windows (daily chunks), then run idempotent rerun once.
- Monitor invalid session alias reject counts and benchmark-return zero/null ratio.

## Known Limitations / Follow-ups
- Provider capability differs by market; unsupported bid/ask/depth-related fields remain nullable.
- Partition tuning (monthly/yearly boundary operations) should be validated with production-like volume.

## PR Draft
### Title
`quant-worker: add trading indicator source ingestion pipeline (KR/US/session/RS/flow/metadata)`

### Scope
- Add collectors/repositories/jobs/services and migration for indicator source domains.
- Add tests for collector normalization, decimal safety, idempotent upsert, and migration contract.
- Fix collector-api market test compile compatibility in KIS token client test.

### Validation
- `pytest tests/ -k "indicator or microstructure or flow or metadata" -v --tb=short`
- `./gradlew test --tests "*Market*"`

### Risks
- Provider schema drift can affect alias mapping and null-policy assumptions.
- Data quality variance may increase nullable fields for unsupported feeds.

### Follow-ups
- Add runtime metrics/alerts for null-rate and rejected-session counts.
- Evaluate additional benchmark policies for RS when benchmark return is zero.
