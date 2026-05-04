# Step 4c: 코드리뷰 2차 검증
Assigned agent: code-reviewer

## 목표
Step 4에서 지적된 MUST FIX 3건이 정확히 해소되었는지 2차 리뷰로 검증한다.

## Working Directory
.worktrees/front-tax-summary-ui

## Files to Read
- `C:/Users/dw/Desktop/paper-trading/CODEX.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/front/tax-summary-ui/spec.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/front/tax-summary-ui/step-4.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/front/tax-summary-ui/step-4b-rework.md`
- `C:/Users/dw/Desktop/paper-trading/docs/done/trading-api/tax-summary/tax-summary-summary.md`
- rework 결과 코드/테스트 파일 전체

## Review Focus
1. API 경로 계약 정합성 (`/api/accounts/...`)
2. 오류 코드별 UX 분기 (`400/404/409`)
3. 세율 표시 정밀도 (부동소수 재계산 금지)

## 검증 명령
```bash
cd C:/Users/dw/Desktop/paper-trading/.worktrees/front-tax-summary-ui/frontend/trading-web
npm run test -- tax-summary
npm run test -- AccountDashboardPage OrderPage
npm run build
npm run lint -- src/pages src/features src/entities
```

## 완료 조건
- MUST FIX 3건이 모두 해소되었음을 확인한다.
- 검증 명령 결과를 보고한다.
- 승인 가능 여부(PASS/FAIL)를 명확히 제시한다.

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
