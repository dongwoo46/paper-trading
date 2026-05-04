# Step 5: Phase Wrap-up and PR Preparation
Assigned agent: orchestrator

## Working Directory
.worktrees/front-subscription-routing-ui

## Files to Read
- docs/phase/front/subscription-routing-ui/index.json
- docs/phase/front/subscription-routing-ui/spec.md
- docs/phase/front/subscription-routing-ui/step-2.md
- docs/phase/front/subscription-routing-ui/step-3.md
- docs/phase/front/subscription-routing-ui/step-4.md
- docs/state.md
- docs/TODO.md

## Tasks
1. Consolidate step outcomes and blockers.
2. If all required scope completed, mark phase done and update root docs state/TODO.
3. If blocked by backend contract freeze, keep phase status as blocked/paused with explicit dependency notes.
4. Prepare PR summary:
   - implemented scope,
   - excluded scope and why,
   - test/lint/build evidence.

## Acceptance Criteria
- Orchestration state is consistent (`docs/state.md`, phase `index.json`, `docs/TODO.md` as applicable).
- Completion status reflects real dependency state (no false PASS).
- PR summary is reproducible and concise.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
