# Step 1: Feature Spec and API Spec
Assigned agent: service-planner

## Working Directory
.worktrees/market-bars-chart-ui

## Files to Read
- CODEX.md
- docs/TODO.md
- docs/phase/front/market-bars-chart-ui/index.json

## Tasks
1. Define detailed spec for `front/market-bars-chart-ui` based on TODO requirement.
2. Fix API contract usage for `GET /api/market/bars/{symbol}?interval=1m|5m|10m&limit={n}` and error handling UX.
3. Design UI behavior for interval tabs (1m/5m/10m), limit control, loading/empty/error states, and mobile layout.
4. Write `spec.md`.
5. Create `step-2.md` to `step-5.md` with concrete implementation/testing/review/cleanup instructions and explicit file paths.

## Acceptance Criteria
- `spec.md` created.
- `step-2.md` to `step-5.md` created with actionable directives.
- Each step file includes `## Agent Return Protocol`.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
