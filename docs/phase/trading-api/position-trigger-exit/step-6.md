# Step 6: Rework for QA Failures from Step 3
Assigned agent: fullstack-dev

## Working Directory
.worktrees/trading-api-position-trigger-exit

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CODEX.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/position-trigger-exit/spec.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/position-trigger-exit/step-3.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/position-trigger-exit/index.json

## Rework Objectives
Address all Step 3 failing points without broad refactor.

## Must-fix Items
1. Closed position semantics:
- Ensure API behavior returns 422 for closed position trigger updates.
- Ensure orchestration path guards closed positions before trigger/order flow.

2. Concurrent duplicate prevention:
- Guarantee duplicate quote/concurrent trigger attempts for same position do not create duplicate auto-exit orders.
- Preserve idempotency key format: auto-exit:{positionId}:{triggerVersion}:{triggerType}.

3. Persistence constraint test stability:
- Fix Spring test context/bean conflict in `ExitTriggerPersistenceConstraintTest` so unique constraint assertions can execute.

4. Keep monetary and threshold logic in BigDecimal.

## Verification Commands
- `cd backend/trading-api && ./gradlew test --tests "*PositionExitTrigger*"`
- `cd backend/trading-api && ./gradlew test --tests "*QuoteEventListener*"`
- `cd backend/trading-api && ./gradlew compileKotlin`

## Deliverables
- All targeted trigger-related tests pass.
- Rework changes are minimal and scoped to failing behavior.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---