# Step 4: Code Review and Risk Assessment
Assigned agent: code-reviewer

## Working Directory
.worktrees/trading-api-position-trigger-exit

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CODEX.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/position-trigger-exit/spec.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/position-trigger-exit/step-2.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/position-trigger-exit/step-3.md

## Objective
Perform focused review for financial safety, idempotency, and OCO correctness.

## Review Checklist
1. Domain correctness:
- Trigger invariants enforced in aggregate methods.
- No floating-point arithmetic for money/percent.
- OCO state transition is atomic and auditable.

2. Idempotency and duplicate prevention:
- deterministic key generation.
- repository/DB unique constraints effectively prevent duplicate orders.
- retry paths are safe under repeated execution.

3. Transaction and concurrency:
- no race where two quotes create two exit orders.
- optimistic/pessimistic locking strategy is explicit.

4. API and error contract:
- request validation maps to spec codes.
- stale version and closed-position errors are explicit.

5. Regression and maintainability:
- new code does not bypass aggregate boundaries.
- logs/metrics/events include traceable identifiers.

## Deliverables
- Findings ordered by severity with file:line references.
- Required fixes list before merge approval.
- Explicit statement if no blocking issues found.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
