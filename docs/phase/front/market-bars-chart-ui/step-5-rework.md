# Step 5 — Rework for Review Findings (fullstack-dev)

## Agent
fullstack-dev

## Working Directory
`.worktrees/market-bars-chart-ui`

## Files to Read First
1. `docs/phase/front/market-bars-chart-ui/step-4.md`
2. `frontend/trading-web/src/pages/market-bars/ui/MarketBarsChartPage.tsx`
3. `frontend/trading-web/src/pages/market-bars/ui/MarketBarsChartPage.test.tsx`

## Must Fix
1. Ensure loading/empty/error/success states are mutually exclusive.
- Current issue: in error cases (`400`, `5xx`) empty state can render together.
- Fix guidance:
  - Restrict empty condition to non-error state (example: `!isError && !isLoading && ...`), or
  - Refactor rendering into explicit `if / else if` single-state branches.

## Verification
```bash
cd .worktrees/market-bars-chart-ui/frontend/trading-web && npm test -- --run src/pages/market-bars/ui/MarketBarsChartPage.test.tsx
cd .worktrees/market-bars-chart-ui/frontend/trading-web && npm run build
```

## Acceptance Criteria
- Error and empty UIs are never displayed at the same time.
- Related tests pass.
- Frontend build succeeds.

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
