# Step 2: Tax Summary UI 구현 (TDD)
Assigned agent: fullstack-dev

## 목표
`frontend/trading-web`에 세금 요약 조회/재계산 UI를 구현하고, 반드시 TDD 순서(실패 테스트 -> 구현 -> 통과)를 지켜 기능을 완성한다.

## Files to Read
- `C:/Users/dw/Desktop/paper-trading/CODEX.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/front/tax-summary-ui/spec.md`
- `C:/Users/dw/Desktop/paper-trading/docs/done/front/account-dashboard/account-dashboard-summary.md`
- `C:/Users/dw/Desktop/paper-trading/docs/done/front/order-management-ui/order-management-ui-summary.md`
- `C:/Users/dw/Desktop/paper-trading/frontend/trading-web/src/pages`
- `C:/Users/dw/Desktop/paper-trading/frontend/trading-web/src/features`
- `C:/Users/dw/Desktop/paper-trading/frontend/trading-web/src/entities`

## 입력
- tax-summary backend API 계약 (`GET tax summary`, `POST recalculate`)
- 기존 대시보드 UX 패턴(계좌 선택/패널 배치/명시적 새로고침)

## 출력
- Tax Summary 페이지 라우트/사이드바 진입점
- Tax Summary 도메인 타입 및 API 클라이언트
- 계좌/연도 선택 + 요약 카드 + 수동 재계산 UI
- 기능 단위 테스트(페이지/핵심 컴포넌트/API 훅)

## Tasks
1. 테스트 먼저 작성한다.
- Tax Summary 페이지 초기 렌더/계좌 선택/연도 선택/로딩/에러/빈 상태 시나리오 테스트를 먼저 추가.
- 재계산 버튼 동작(요청 호출, RUNNING 중복 클릭 방지, 성공 후 재조회 트리거) 테스트를 먼저 추가.
- 기존 라우팅/사이드바에 `tax-summary` 메뉴가 노출되는 테스트를 필요 범위에서 추가.

2. 실패 테스트를 확인한다.
- 새 테스트가 현재 코드에서 실패함을 확인한다.
- 실패 원인이 기대 동작 부재임을 확인하고 기록한다.

3. 구현한다.
- `entities/tax-summary`에 API/타입/상태 표시 매핑을 추가.
- `features/tax-summary`에 선택 패널, 요약 패널, 재계산 액션 패널을 추가.
- `pages/tax-summary`를 생성해 선택 상태와 API 호출 오케스트레이션을 구현.
- `App` 라우트와 `Sidebar` 내비게이션을 기존 스타일에 맞게 확장.

4. 테스트를 통과시킨다.
- 새 테스트와 기존 영향 범위 테스트를 통과시켜 회귀가 없음을 확인.

## 검증 명령
```bash
cd C:/Users/dw/Desktop/paper-trading/frontend/trading-web
npm run test -- tax-summary
npm run test -- AccountDashboardPage OrderPage
npm run lint -- src/pages src/features src/entities
```

## 완료 조건
- TDD 순서가 작업 로그에 명시됨(실패 테스트 -> 구현 -> 통과).
- Tax Summary UI가 계좌/연도 선택 및 재계산 UX를 제공함.
- 새 테스트가 안정적으로 통과하고 기존 핵심 페이지 회귀가 없음.

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
