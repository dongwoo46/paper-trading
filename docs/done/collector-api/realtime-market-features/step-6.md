# Step 6: Rework — Minute Rollover Atomicity Fix
Assigned agent: fullstack-dev

## Working Directory
`.worktrees/collector-api-realtime-market-features`

## Files to Read
- `CODEX.md`
- `docs/phase/collector-api/realtime-market-features/spec.md`
- `docs/phase/collector-api/realtime-market-features/step-2.md`
- `docs/phase/collector-api/realtime-market-features/step-5.md`
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/marketfeature/service/MarketFeatureAggregationService.kt`
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/redis/MarketFeatureRedisStore.kt`
- `backend/collector-api/src/test/kotlin/com/papertrading/collector/application/marketfeature/service/MarketFeatureAggregationServiceTest.kt`
- `backend/collector-api/src/test/kotlin/com/papertrading/collector/infra/redis/MarketFeatureRedisStoreTest.kt`

## Tasks
1. Code-review must-fix 반영:
- 분 경계 동시 tick 처리에서 `appendBar`가 중복 기록되지 않도록 rollover를 원자적으로 보장한다.

2. 구현 방향:
- symbol+minute 기준으로 rollover가 한 번만 실행되도록 동기화/원자성 전략을 적용한다.
- 기존 quote publish 경로와 feature snapshot 계약(1m/5m/10m)은 유지한다.

3. 테스트:
- 동시성 상황에서 duplicate append가 재현되는 실패 테스트를 먼저 작성한다(Red).
- 수정 후 동일 테스트 및 기존 feature 테스트가 통과하도록 한다(Green).

## Acceptance Criteria
```bash
cd .worktrees/collector-api-realtime-market-features/backend/collector-api && ./gradlew test --tests "*MarketFeature*" --tests "*RawEventPipeline*"
```

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Test result: <passed N/N | failed N — list failing cases> (if applicable)
- Blockers: <none | description>
---
