# Step 2: 포트폴리오 차트 구현 (TDD)
Assigned agent: fullstack-dev

## Goal
`portfolio-chart` 기능을 TDD로 구현해 일별 평가금액 추이와 KOSPI 대비 수익률 비교 차트를 화면에 노출한다.

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/phase/front/portfolio-chart/spec.md
- docs/phase/front/portfolio-chart/index.json
- frontend/trading-web/src/pages/account/ui/AccountDashboardPage.tsx

## Tasks
- TDD 순서를 반드시 따른다: 실패 테스트 작성 -> 최소 구현 -> 테스트 통과 -> 리팩터링.
- 생성/수정 대상(경로는 실제 프로젝트 구조에 맞춰 조정 가능):
  - `frontend/trading-web/src/entities/portfolio/model/types.ts`
  - `frontend/trading-web/src/shared/api/portfolioApi.ts`
  - `frontend/trading-web/src/features/portfolio-chart/model/normalizeSeries.ts`
  - `frontend/trading-web/src/features/portfolio-chart/ui/PortfolioChartPanel.tsx`
  - `frontend/trading-web/src/pages/portfolio/ui/PortfolioChartPage.tsx`
  - `frontend/trading-web/src/App.tsx` (route 추가)
  - `frontend/trading-web/src/widgets/sidebar/Sidebar.tsx` (nav 추가)
- 시그니처 지침:
  - `fetchDailyBalances(accountId: number, from: string, to: string): Promise<DailyBalancePoint[]>`
  - `fetchKospiBenchmark(from: string, to: string): Promise<BenchmarkPoint[]>`
  - `buildReturnSeries(input: BuildReturnSeriesInput): ReturnSeriesPoint[]`
- 제약:
  - API 응답 숫자 문자열을 안전하게 변환하고 `NaN`을 허용하지 않는다.
  - 기준일 수익률은 0%로 고정한다.
  - 벤치마크 누락 날짜는 해당 포인트를 skip하고 warning 상태를 상위 컴포넌트로 전달한다.

## Acceptance Criteria
```bash
cd frontend/trading-web
npm run test -- portfolio-chart
npm run build
```
- 실패 테스트가 먼저 존재하고, 최종적으로 전부 통과해야 한다.
- `/portfolio` 페이지에서 계좌/기간 선택 시 차트가 갱신되어야 한다.

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
