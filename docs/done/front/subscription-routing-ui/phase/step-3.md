# Step 3: Testing and QA Verification
Assigned agent: test-engineer

## Working Directory
.worktrees/front-subscription-routing-ui

## Files to Read
- docs/phase/front/subscription-routing-ui/spec.md
- docs/phase/front/subscription-routing-ui/step-2.md
- docs/phase/front/subscription-routing-ui/index.json
- frontend/trading-web test files changed in Step 2

## Tasks
1. Verify Step 2 scope against gate condition:
   - full-scope implemented only if backend contracts frozen,
   - otherwise validate subset + BLOCKED rationale quality.
2. Run frontend verification commands for changed scope:
   - unit/integration tests for affected files,
   - lint for affected files,
   - build check.
3. Validate error handling and duplicate/in-flight write guard behavior.
4. Confirm monitor concerns are not mixed into routing write UI.

## Acceptance Criteria
- Tests/lint/build results are captured and consistent.
- QA verdict clearly states PASS or BLOCKED (contract-dependent).
- Any residual risk is explicit and actionable.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
