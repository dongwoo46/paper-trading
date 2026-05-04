# Step 1 — service-planner

## Goal
`front/tax-summary-ui` phase 구현 준비를 위해 `spec.md`와 실행 step 문서(step-2~step-5)를 생성한다.

## Context
- 선택된 TODO 항목:
  - trading-api 세금 요약 조회/재계산 API 연동
  - 계좌/연도별 세금 요약 조회 및 수동 재계산 UX
- 프로젝트: `front` (React/TypeScript/Vite)
- 기존 대시보드(account-dashboard, order-management-ui)와 UX 일관성 유지

## Working Directory
.worktrees/front-tax-summary-ui

## Files to Read
- `C:/Users/dw/Desktop/paper-trading/CODEX.md`
- `C:/Users/dw/Desktop/paper-trading/docs/TODO.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/front/tax-summary-ui/index.json`
- `C:/Users/dw/Desktop/paper-trading/docs/done/trading-api/tax-summary/tax-summary-summary.md`
- `C:/Users/dw/Desktop/paper-trading/docs/done/front/account-dashboard/account-dashboard-summary.md`
- `C:/Users/dw/Desktop/paper-trading/docs/done/front/order-management-ui/order-management-ui-summary.md`

## Required Outputs
1. `C:/Users/dw/Desktop/paper-trading/docs/phase/front/tax-summary-ui/spec.md`
2. `C:/Users/dw/Desktop/paper-trading/docs/phase/front/tax-summary-ui/step-2.md`
3. `C:/Users/dw/Desktop/paper-trading/docs/phase/front/tax-summary-ui/step-3.md`
4. `C:/Users/dw/Desktop/paper-trading/docs/phase/front/tax-summary-ui/step-4.md`
5. `C:/Users/dw/Desktop/paper-trading/docs/phase/front/tax-summary-ui/step-5.md`

## Writing Rules
- 각 step 파일은 담당 agent, 목표, 입력/출력, 검증 명령, 완료 조건을 명시.
- Step 2는 반드시 TDD 순서(실패 테스트 -> 구현 -> 통과)를 강제.
- Step 3은 feature-scope 테스트 및 `frontend/trading-web` 빌드 검증을 포함.
- Step 4는 API 계약 정합성, 금액/세율 표시 정확성, 회귀 위험 중심 리뷰 체크리스트 포함.
- Step 5는 summary 작성, TODO/state/index 갱신, PR 준비 절차 포함.
- 모든 step 파일 끝에 아래 `Agent Return Protocol` 섹션을 동일하게 포함.

## Acceptance Criteria
- `spec.md` 생성 완료.
- `step-2.md` ~ `step-5.md` 생성 완료.
- 각 step 파일에 `## Agent Return Protocol` 포함.
- step 문서의 docs 경로는 모두 메인 레포 루트 경로를 참조.

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
