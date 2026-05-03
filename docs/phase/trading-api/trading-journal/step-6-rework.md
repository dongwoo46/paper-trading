# Step 6: 리뷰 반영 재작업 (must-fix)
Assigned agent: fullstack-dev

## Working Directory
.worktrees/trading-api-trading-journal

## Files to Read
- CODEX.md
- docs/phase/trading-api/trading-journal/spec.md
- docs/phase/trading-api/trading-journal/step-3.md
- docs/phase/trading-api/trading-journal/step-5.md
- backend/trading-api/src/main/kotlin/com/papertrading/api/application/journal/TradingJournalAutoGenerationService.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/TradingJournalController.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/TradingJournalRepository.kt
- backend/trading-api/src/main/resources/db/migration/V1__trading_journal_automation.sql

## Tasks
1. `idempotencyKey` 중복 충돌(409) 경로 구현.
- 저장/조회/유니크 제약 기반으로 동일 키 재요청 시 명시적으로 409 반환.
- 관련 테스트 추가 또는 기존 통합테스트 확장.

2. 목록 API `ticker` 필터 반영.
- `GET /api/trading-journals`에 `ticker` query param 지원.
- repository 조회 경로를 `account + ticker + 기간` 조건으로 확장(옵션 ticker).
- 인덱스 사용 가능한 쿼리 형태 유지.

3. `orderId + journalType` 업서트 동시성 충돌 처리 강화.
- 경쟁 요청에서 unique 충돌 발생 시 정책을 명확히 처리(멱등 성공 또는 409).
- 관련 테스트로 재현/검증.

4. 기존 동작 회귀 방지.
- 기존 TradingJournal 테스트/컨트롤러 테스트 회귀 통과 확인.

## Acceptance Criteria
```bash
cd backend/trading-api
./gradlew test --tests "*TradingJournalAutoGenerationServiceTest"
./gradlew test --tests "*TradingJournalControllerIntegrationTest"
./gradlew compileKotlin
```

## Agent Return Protocol
---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
