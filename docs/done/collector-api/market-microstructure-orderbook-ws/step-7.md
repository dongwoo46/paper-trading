# Step 7: Cleanup + Summary + PR
Assigned agent: orchestrator

## Files to Read
- CLAUDE.md
- docs/state.md
- docs/TODO.md
- docs/phase/collector-api/market-microstructure-orderbook-ws/index.json
- docs/phase/collector-api/market-microstructure-orderbook-ws/spec.md

## Tasks

### 1. Final compile + test gate
```bash
cd backend/collector-api && ./gradlew test && ./gradlew compileKotlin
```
Must pass before proceeding.

### 2. Write summary
File: `docs/done/collector-api/market-microstructure-orderbook-ws/market-microstructure-orderbook-ws-summary.md`

Summary must cover:
- What was built (H0STASP0 parser, OrderbookRedisStore, ingest metrics, API mapping, RS sort fix, 1d/1w branch fix)
- Redis schema finalized (`orderbook:{symbol}` with 30s TTL)
- New endpoint: `GET /api/internal/kis/orderbook/ingest-health`
- Test coverage added (list test class names)
- Known limitations (domestic KR symbols only, no 10-level depth history)

### 3. Move phase folder
Move `docs/phase/collector-api/market-microstructure-orderbook-ws/` → `docs/done/collector-api/market-microstructure-orderbook-ws/`

### 4. Mark TODO complete
In `docs/TODO.md`, mark the `market-microstructure-orderbook-ws` entry as `[x]`.

### 5. Update `docs/state.md`
```
상태: idle
활성 Phase: 없음
마지막 액션: 2026-05-09: collector-api/market-microstructure-orderbook-ws 완료
다음 액션: docs/TODO.md 미완료 항목 중 다음 phase 선택
```

### 6. Create PR
```bash
git add -p
git commit -m "feat(collector-api): H0STASP0 호가잔량 파이프라인 + orderbook Redis 적재 + 마이크로구조 API 실데이터 매핑"
gh pr create --title "feat(collector-api): market-microstructure-orderbook-ws" --body "..."
```

## Acceptance Criteria
```bash
cd backend/collector-api && ./gradlew test && ./gradlew compileKotlin
```
PR created. `docs/state.md` set to idle. `docs/TODO.md` entry marked complete.
