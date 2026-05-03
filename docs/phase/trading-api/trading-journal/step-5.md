# Step 5: 코드 리뷰 및 리스크 점검
Assigned agent: code-reviewer

## Working Directory
.worktrees/trading-api-trading-journal

## Files to Read
- CODEX.md
- docs/phase/trading-api/trading-journal/spec.md
- docs/phase/trading-api/trading-journal/step-2.md
- docs/phase/trading-api/trading-journal/step-3.md
- docs/phase/trading-api/trading-journal/step-4.md
- backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/TradingJournal.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/application/journal/TradingJournalAutoGenerationService.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/TradingJournalController.kt

## Tasks
1. 도메인 규칙 리뷰:
   - `orderId + journalType` 멱등 upsert가 실제로 보장되는지 확인.
   - `BigDecimal` 외 수치형 사용 여부 점검.
2. API 계약 리뷰:
   - spec.md와 실제 endpoint/request/response 일치성 확인.
   - 에러 코드(400/404/409) 매핑 누락 여부 확인.
3. 성능/운영 리뷰:
   - N+1 가능성, 인덱스 미사용 가능성, 트랜잭션 경계 점검.
4. 결과 분류:
   - must-fix / should-fix / note 로 분리해 코멘트.

## Acceptance Criteria
```bash
cd backend/trading-api
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
