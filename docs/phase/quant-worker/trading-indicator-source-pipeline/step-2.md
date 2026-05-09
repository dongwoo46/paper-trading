# Step 2: Implementation (TDD)
Assigned agent: quant-dev

## Working Directory
.worktrees/quant-worker-trading-indicator-source-pipeline

## Files to Read
- CODEX.md
- docs/phase/quant-worker/trading-indicator-source-pipeline/spec.md
- backend/quant-worker/src/collectors/
- backend/quant-worker/src/repositories/
- backend/quant-worker/src/jobs/
- backend/quant-worker/src/application/
- backend/collector-api/src/main/resources/db/migration/

## Tasks
1. Write tests first for each source domain (Red): microstructure/session-ohlcv/rs/alternative-flow/metadata.
2. Implement collectors and normalization contracts for KR/US source feeds.
3. Implement Decimal-safe repositories with idempotent upsert keys.
4. Implement batch job/service orchestration for provider별 수집 실행.
5. Add collector-api Flyway migration scripts for source tables/indexes/partition basics.
6. Verify tests pass after implementation.

## Verification
- `cd backend/quant-worker && pytest tests/ -k "indicator or microstructure or flow or metadata" -v`
- `cd backend/collector-api && ./gradlew compileKotlin`

## Acceptance Criteria
- Source collectors/repositories/jobs/services implemented.
- Migration scripts added with indexes and partition policy reflected.
- All new targeted tests pass.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Test result: <passed N/N | failed N — list failing cases>
- Blockers: <none | description>
---
