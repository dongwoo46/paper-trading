# Step 7: Re-verify QA After Rework
Assigned agent: test-engineer

## Working Directory
.worktrees/trading-api-position-trigger-exit

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CODEX.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/position-trigger-exit/spec.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/position-trigger-exit/step-3.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/position-trigger-exit/step-6.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/position-trigger-exit/index.json

## Objective
Verify that Step 3 failures are fully resolved after Step 6 rework.

## Verification Scope
- `cd backend/trading-api && ./gradlew test --tests "*PositionExitTrigger*Test"`
- `cd backend/trading-api && ./gradlew test --tests "*QuoteEventListenerTest"`
- `cd backend/trading-api && ./gradlew compileKotlin`

## Assertions to Confirm
- Closed position update path returns 422 semantics.
- Orchestrator guard prevents closed-position auto-exit flow.
- Concurrent duplicate attempts do not create duplicate orders.
- Persistence constraint checks execute without prior Spring context conflict.

## Deliverables
- PASS/FAIL with concrete failed case names if any.
- Residual risk list.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---