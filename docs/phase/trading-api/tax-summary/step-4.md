# Step 4: 테스트 및 회귀 검증
Assigned agent: test-engineer

## Working Directory
.worktrees/trading-api-tax-summary

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/phase/trading-api/tax-summary/spec.md
- docs/phase/trading-api/tax-summary/step-2.md
- docs/phase/trading-api/tax-summary/step-3.md
- backend/trading-api/src/test/kotlin

## Tasks
1. 세금 정산 규칙 테스트 보강.
- 계산 규칙 단위 테스트: 손익/수수료/세율/경계연도.
- RUNNING 중복 차단, FAILED 전이, force 재계산 케이스.

2. API 통합 테스트 추가.
- 조회 200/404
- 재계산 202/409
- year range 400

3. 회귀 검증.
- 주문/정산 관련 기존 핵심 테스트 스모크 실행.

## Acceptance Criteria
```bash
cd .worktrees/trading-api-tax-summary/backend/trading-api
./gradlew compileTestKotlin
./gradlew test --tests "*TaxSummary*" --tests "*Settlement*"
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
