# Step 7: 코드 리뷰(2차) — 재작업 검증
Assigned agent: code-reviewer

## Working Directory
`.worktrees/trading-api-ddd-aggregate-fix`

## Files to Read
- `docs/phase/trading-api/ddd-aggregate-fix/spec.md`
- `docs/phase/trading-api/ddd-aggregate-fix/step-6.md`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/PendingSettlementQueryService.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PendingSettlementReadRepository.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PendingSettlementRepositoryImpl.kt`

## Tasks
1. DI 충돌 해소 여부 확인 (`PendingSettlementReadRepository` 단일 주입 보장)
2. 정책 준수 재확인
- Application 계층 내부 Entity Repository 비노출
- Command 경로 Root 강제 / Query 경로 ReadRepository 허용
3. 회귀 리스크 점검

## Verification Commands
```bash
cd .worktrees/trading-api-ddd-aggregate-fix/backend/trading-api
./gradlew test --tests "*PendingSettlementQueryServiceTest" --no-daemon
```

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
