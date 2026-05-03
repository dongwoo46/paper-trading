# Step 8: 정리 및 완료 문서화
Assigned agent: cleanup

## Working Directory
`.worktrees/trading-api-ddd-aggregate-fix`

## Files to Read
- `docs/phase/trading-api/ddd-aggregate-fix/spec.md`
- `docs/phase/trading-api/ddd-aggregate-fix/step-2.md`
- `docs/phase/trading-api/ddd-aggregate-fix/step-3.md`
- `docs/phase/trading-api/ddd-aggregate-fix/step-4.md`
- `docs/phase/trading-api/ddd-aggregate-fix/step-5.md`
- `docs/phase/trading-api/ddd-aggregate-fix/step-6.md`
- `docs/phase/trading-api/ddd-aggregate-fix/step-7.md`

## Tasks
1. 최종 검증 실행
```bash
cd .worktrees/trading-api-ddd-aggregate-fix/backend/trading-api
./gradlew compileKotlin --no-daemon
./gradlew compileTestKotlin --no-daemon
./gradlew test --no-daemon
```
2. `docs/phase/trading-api/ddd-aggregate-fix/ddd-aggregate-fix-summary.md` 작성
3. 완료 보고 작성

## Acceptance Criteria
- 전체 테스트 통과
- summary 문서 작성 완료

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
