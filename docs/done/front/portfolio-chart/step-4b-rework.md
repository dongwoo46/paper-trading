# Step 5: 코드리뷰 MUST FIX 재작업
Assigned agent: fullstack-dev

## Goal
Step 4 코드리뷰에서 식별된 MUST FIX 3건을 반영하고 관련 테스트를 통과시킨다.

## Working Directory
.worktrees/front-portfolio-chart

## Files to Read
- CODEX.md
- docs/phase/front/portfolio-chart/spec.md
- docs/phase/front/portfolio-chart/step-2.md
- docs/phase/front/portfolio-chart/step-4.md
- .worktrees/front-portfolio-chart/frontend/trading-web/src/features/portfolio-chart/model/normalizeSeries.ts
- .worktrees/front-portfolio-chart/frontend/trading-web/src/shared/api/portfolioApi.ts
- .worktrees/front-portfolio-chart/frontend/trading-web/src/pages/portfolio/ui/PortfolioChartPage.tsx
- .worktrees/front-portfolio-chart/frontend/trading-web/src/features/portfolio-chart/model/normalizeSeries.test.ts
- .worktrees/front-portfolio-chart/frontend/trading-web/src/pages/portfolio/ui/PortfolioChartPage.test.tsx

## Tasks
1. benchmark 404(전체 부재) 상황에서도 portfolio-only 시리즈가 정상 렌더되도록 graceful degrade 구현.
2. `buildReturnSeries`에서 base 값 0 또는 비정상값인 경우 0-division을 방어하고 테스트 추가.
3. 날짜 중복 데이터 처리 규칙(예: 동일 날짜 latest 우선)을 명시적으로 구현하고 테스트로 고정.
4. TDD 순서 준수: 실패 테스트 -> 구현 -> 통과.

## Acceptance Criteria
```bash
cd .worktrees/front-portfolio-chart/frontend/trading-web
npm run test -- portfolio-chart
npm run test -- PortfolioChartPage.test.tsx
npm run build
```
- MUST FIX 3건이 모두 해결되어야 함.

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
