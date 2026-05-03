# Step 5: 문서 정리 및 phase 완료 처리
Assigned agent: cleanup

## Working Directory
.worktrees/trading-api-tax-summary

## Files to Read
- CODEX.md
- docs/TODO.md
- docs/state.md
- docs/phase/trading-api/tax-summary/index.json
- docs/phase/trading-api/tax-summary/spec.md
- docs/phase/trading-api/tax-summary/step-2.md
- docs/phase/trading-api/tax-summary/step-3.md
- docs/phase/trading-api/tax-summary/step-4.md

## Tasks
1. 결과 요약 문서 작성.
- `docs/done/trading-api/tax-summary/tax-summary-summary.md` 생성.

2. phase 상태 마감.
- `docs/phase/trading-api/tax-summary/index.json` step 상태 반영.
- 완료 시 `status=completed`, `current_step=5`, 각 step result 입력.

3. TODO/state 반영.
- `docs/TODO.md`의 tax-summary 항목 `[x]` 처리.
- orchestrator 정책에 맞게 필요한 상태 필드만 업데이트.

## Acceptance Criteria
```bash
cd .worktrees/trading-api-tax-summary/backend/trading-api
./gradlew compileKotlin
./gradlew test --tests "*TaxSummary*"
```

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
