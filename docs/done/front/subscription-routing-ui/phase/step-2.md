# Step 2: Implementation (TDD, blocked-safe)
Assigned agent: fullstack-dev

## Working Directory
.worktrees/front-subscription-routing-ui

## Files to Read
- docs/phase/front/subscription-routing-ui/spec.md
- docs/phase/front/subscription-routing-ui/index.json
- C:/Users/dw/Desktop/paper-trading/frontend/trading-web/src/pages/realtime/ui/RealtimePage.tsx
- C:/Users/dw/Desktop/paper-trading/frontend/trading-web/src/features/kis-management/ui/KisPanel.tsx

## Preconditions (Gate)
Proceed with full implementation only if backend external API contracts are frozen for:
1. favorites CRUD,
2. strategy-priority symbol CRUD,
3. routing status query.

If not frozen:
- implement only safe subset (manual subscribe/unsubscribe UX hardening using existing `/api/kis/symbols/subscriptions`),
- record missing contracts as blocker notes in completion report,
- return `BLOCKED` for full-scope completion.

## Tasks
1. Write tests first for routing write actions (success, validation failure, duplicate/in-flight guard).
2. Implement manual routing section refinement (clear write-centric UX boundaries).
3. If preconditions satisfied, implement favorites/strategy/routing-status sections.
4. Add API error surface + retry behavior per frozen contract.
5. Ensure visual separation from monitor concerns.

## Acceptance Criteria
- TDD evidence: failing tests first then passing.
- Manual routing flow works with current canonical API.
- If contracts frozen: full routing-ui scope implemented.
- If contracts unfrozen: subset shipped + explicit BLOCKED note for remaining scope.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
