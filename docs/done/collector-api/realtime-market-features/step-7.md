# Step 7: Re-Review After Rollover Rework
Assigned agent: code-reviewer

## Working Directory
`.worktrees/collector-api-realtime-market-features`

## Files to Read
- `CODEX.md`
- `docs/phase/collector-api/realtime-market-features/spec.md`
- `docs/phase/collector-api/realtime-market-features/step-5.md`
- `docs/phase/collector-api/realtime-market-features/step-6.md`
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/marketfeature/service/MarketFeatureAggregationService.kt`
- `backend/collector-api/src/test/kotlin/com/papertrading/collector/application/marketfeature/service/MarketFeatureAggregationServiceTest.kt`

## Tasks
1. Step 5 must-fix(분 경계 rollover race)가 해결됐는지 확인한다.
2. 동시성 시나리오에서 duplicate append 방지 검증 로직/테스트의 충분성을 리뷰한다.
3. 기존 400/404/409 에러 계약과 Redis 정책 회귀 여부를 확인한다.
4. 이상 없으면 phase wrap handoff 가능 상태로 승인한다.

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
