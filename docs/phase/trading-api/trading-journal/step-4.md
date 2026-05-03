# Step 4: TradingJournal QA/회귀 검증
Assigned agent: test-engineer

## Working Directory
.worktrees/trading-api-trading-journal

## Files to Read
- CODEX.md
- docs/phase/trading-api/trading-journal/spec.md
- docs/phase/trading-api/trading-journal/step-2.md
- docs/phase/trading-api/trading-journal/step-3.md
- backend/trading-api/src/test/kotlin/com/papertrading/api/application/journal/TradingJournalAutoGenerationServiceTest.kt
- backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/TradingJournalControllerIntegrationTest.kt

## Tasks
1. 신규 테스트 실행 및 실패 원인 분석.
2. 아래 회귀 영역 검증:
   - 기존 주문 체결/정산 플로우 회귀 (`ExecutionProcessorTest`, `SettlementCommandServiceTest`)
   - 계좌/포지션 조회 영향 없음 확인.
3. 비기능 체크:
   - 같은 idempotencyKey 재요청 시 중복 생성 차단 확인.
   - account+ticker 필터 조회 인덱스 사용 가능성 확인(실행계획 또는 로그 근거).
4. 결과를 phase 문서에 남길 수 있도록 실패/리스크 목록 정리.

## Acceptance Criteria
```bash
cd backend/trading-api
./gradlew compileTestKotlin
./gradlew test --tests "*TradingJournalAutoGenerationServiceTest"
./gradlew test --tests "*TradingJournalControllerIntegrationTest"
./gradlew test --tests "*ExecutionProcessorTest"
./gradlew test --tests "*SettlementCommandServiceTest"
```

## Agent Return Protocol
---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
