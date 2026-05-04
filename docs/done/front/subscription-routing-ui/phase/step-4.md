# Step 4: Code Review
Assigned agent: code-reviewer

## Working Directory
.worktrees/front-subscription-routing-ui

## Files to Read
- docs/phase/front/subscription-routing-ui/spec.md
- docs/phase/front/subscription-routing-ui/step-2.md
- docs/phase/front/subscription-routing-ui/step-3.md
- frontend/trading-web changed files from Step 2

## Tasks
1. Review for behavioral regressions in existing KIS panel flows.
2. Verify contract-boundary handling:
   - no implicit assumptions beyond frozen API contracts,
   - correct BLOCKED treatment for unfrozen APIs.
3. Check UX safety for write actions:
   - duplicate submission prevention,
   - clear failure visibility,
   - mode/channel correctness.
4. Confirm tests meaningfully cover core write paths.

## Acceptance Criteria
- Findings prioritized by severity with concrete file references.
- PASS only when no blocking defects remain.
- Residual risk documented if accepted.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
