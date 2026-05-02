# Step 1 — service-planner

## Goal
`trading-api/kis-account-sync` phase의 구현 준비를 위해 `spec.md`와 실행 step 문서(step-2~step-5)를 생성한다.

## Context
- 선택된 TODO 항목:
  - KIS 잔고조회 API 연동 (TTTC8434R live / VTTC8434R paper) → 보유주식·평가금액·수익률
  - KisAccountQueryService: KIS 계좌 잔고 + 포지션 동기화
  - GET /api/kis/account/balance — 실계좌 잔고·평가손익 응답
  - trading-web 대시보드에 KIS 실계좌 잔고 반영 (KIS/LOCAL 구분)
- 프로젝트: `trading-api` (Kotlin/Spring Boot)
- 금융 안전 규칙: 금액 계산은 `BigDecimal`, 상태 변경은 감사 가능해야 함

## Files to Read
- `C:/Users/dw/Desktop/paper-trading/docs/TODO.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-account-sync/index.json`

## Required Outputs
1. `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-account-sync/spec.md`
2. `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-account-sync/step-2.md`
3. `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-account-sync/step-3.md`
4. `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-account-sync/step-4.md`
5. `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-account-sync/step-5.md`

## Writing Rules
- 각 step 파일은 담당 agent, 목표, 입력/출력, 검증 명령, 완료 조건을 명시.
- Step 2는 반드시 TDD 순서(실패 테스트 → 구현 → 통과)를 강제.
- Step 3은 feature-scope 테스트 및 컴파일 검증을 포함.
- Step 4는 리뷰 체크리스트(보안/회귀/재무정합성) 중심.
- Step 5는 summary 작성, TODO/state 갱신, PR 준비 절차 포함.
- 모든 step 파일 끝에 아래 `Agent Return Protocol` 섹션을 동일하게 포함.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to worktree root>
- Test result: <passed N/N | failed N — list failing cases> (if applicable)
- Blockers: <none | description>
---
