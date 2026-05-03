# Step 3: TradingJournal API/영속성/예외 처리 구현 (TDD)
Assigned agent: fullstack-dev

## Working Directory
.worktrees/trading-api-trading-journal

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/PRD.md
- docs/phase/trading-api/trading-journal/spec.md
- docs/phase/trading-api/trading-journal/step-2.md
- backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/exception/GlobalExceptionHandler.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/exception/ApiErrorResponse.kt

## Tasks
1. 테스트 먼저 작성:
   - `backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/TradingJournalControllerIntegrationTest.kt`
2. API 계층 구현:
   - `TradingJournalController` 생성
   - DTO 생성
     - `TradingJournalAutoGenerateRequest/Response`
     - `TradingJournalUpdateRequest/Response`
     - `TradingJournalItemResponse`, `TradingJournalPageResponse`
3. 엔드포인트 구현:
   - `POST /api/trading-journals/auto-generate`
   - `GET /api/trading-journals`
   - `GET /api/trading-journals/{journalId}`
   - `PATCH /api/trading-journals/{journalId}`
4. 예외/검증 매핑:
   - 400: 유효성 실패(기간 역전, 빈 본문, 사이즈 제한 초과)
   - 404: account/journal/execution 미존재
   - 409: idempotency 충돌 또는 optimistic lock 충돌
5. DB 마이그레이션 추가:
   - `backend/trading-api/src/main/resources/db/migration/V{next}__trading_journal_automation.sql`
   - `summary_json`, `entry_source_execution_id`, `last_auto_generated_at`, unique/조회 인덱스 반영

## Acceptance Criteria
```bash
cd backend/trading-api
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
