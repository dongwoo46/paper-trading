# Step 4b: 코드리뷰 MUST FIX 재작업
Assigned agent: fullstack-dev

## 목표
Step 4 코드리뷰 FAIL 사유 3건을 모두 해소하고, 테스트/빌드 재검증까지 완료한다.

## Working Directory
.worktrees/front-tax-summary-ui

## Files to Read
- `C:/Users/dw/Desktop/paper-trading/CODEX.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/front/tax-summary-ui/spec.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/front/tax-summary-ui/step-2.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/front/tax-summary-ui/step-4.md`
- `C:/Users/dw/Desktop/paper-trading/docs/done/trading-api/tax-summary/tax-summary-summary.md`
- `C:/Users/dw/Desktop/paper-trading/.worktrees/front-tax-summary-ui/frontend/trading-web/src/entities/tax-summary/api/taxSummaryApi.ts`
- `C:/Users/dw/Desktop/paper-trading/.worktrees/front-tax-summary-ui/frontend/trading-web/src/pages/tax-summary/ui/TaxSummaryPage.tsx`
- `C:/Users/dw/Desktop/paper-trading/.worktrees/front-tax-summary-ui/frontend/trading-web/src/features/tax-summary/ui/TaxSummarySummaryPanel.tsx`
- Tax Summary 관련 테스트 파일 전체

## MUST FIX
1. API 경로 계약 정합성
- tax-summary API 경로를 backend 실제 계약과 일치시킨다.
- 문서/기존 API 모듈 패턴을 확인해 `/api/accounts/...` vs `/api/v1/accounts/...` 불일치를 제거한다.

2. 오류 코드별 UX 분기
- `400`, `404`, `409(RUNNING)`를 동일 문구로 처리하지 않는다.
- 409는 "이미 재계산 진행 중" 안내 및 중복 동작 제어를 보장한다.
- 400/404는 사용자 액션 가능한 메시지로 구분한다.

3. 세율 표시 정밀도
- `effectiveTaxRate`를 `parseFloat(...)*100`으로 재계산하지 않는다.
- 서버 계산값 기반 문자열 안전 포맷(비부동소수 재계산)으로 표기한다.

## 검증 명령
```bash
cd C:/Users/dw/Desktop/paper-trading/.worktrees/front-tax-summary-ui/frontend/trading-web
npm run test -- tax-summary
npm run test -- AccountDashboardPage OrderPage
npm run build
npm run lint -- src/pages src/features src/entities
```

## 완료 조건
- MUST FIX 3건이 코드/테스트로 해소된다.
- 상기 검증 명령이 모두 통과한다.
- 변경사항이 spec 요구사항(계약/오류 UX/정밀도)과 일치한다.

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
