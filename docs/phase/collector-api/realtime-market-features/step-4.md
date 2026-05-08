# Step 4: QA Verification for Realtime Market Features
Assigned agent: test-engineer

## Working Directory
`.worktrees/collector-api-realtime-market-features`

## Files to Read
- `CODEX.md`
- `docs/phase/collector-api/realtime-market-features/spec.md`
- `docs/phase/collector-api/realtime-market-features/step-2.md`
- `docs/phase/collector-api/realtime-market-features/step-3.md`
- `backend/collector-api/src/test/kotlin/com/papertrading/collector/infra/redis/QuoteRedisPublisherTest.kt`
- `backend/collector-api/src/test/kotlin/com/papertrading/collector/presentation/subscriptions/SubscriptionStatusControllerTest.kt`

## Tasks
1. Aggregation unit test 검증:
- minute rollover 경계
- 5m/10m snapshot 계산 정합성
- tradeImbalance, vwap 계산식 검증

2. Redis persistence test 검증:
- key naming 정책 일치
- TTL/maxlen 적용
- debug ring buffer flag on/off 동작

3. API contract test 검증:
- `GET /api/market/features/{symbol}` 200/400/404/409 케이스
- windows query 파싱/기본값 검증

4. 회귀 검증:
- 기존 `RawEventPipeline` quote publish 동작 불변

## Acceptance Criteria
```bash
cd .worktrees/collector-api-realtime-market-features/backend/collector-api && ./gradlew test --tests "*MarketFeature*" --tests "*RawEventPipeline*" --tests "*QuoteRedisPublisher*"
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
