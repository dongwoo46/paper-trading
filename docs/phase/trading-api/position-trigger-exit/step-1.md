# Step 1: Feature Spec and API Spec
Assigned agent: service-planner

## Working Directory
.worktrees/trading-api-position-trigger-exit

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CODEX.md
- C:/Users/dw/Desktop/paper-trading/docs/ADR.md
- C:/Users/dw/Desktop/paper-trading/docs/PRD.md
- C:/Users/dw/Desktop/paper-trading/docs/TODO.md
- C:/Users/dw/Desktop/paper-trading/backend/trading-api/graphify-out/graph.json

## Tasks
1. Clarify and structure requirements for position percent-triggered auto-exit (stop-loss / take-profit).
2. Define DDD model and aggregate boundaries for trigger settings, trigger evaluation, and OCO guard behavior.
3. Design API contract for trigger configuration (account/position level), validation, and idempotency inputs.
4. Design execution flow for realtime quote-based trigger checks, auto sell order creation, duplicate prevention, and failure/retry notifications.
5. Design persistence schema updates and indexes.
6. Write spec.md.
7. Generate step-2.md to step-N.md with concrete implementation directives, file paths, class/method signatures, and test scope.

## Acceptance Criteria
- spec.md created for trading-api/position-trigger-exit.
- step-2.md to step-N.md created with explicit file paths and actionable directives.
- Each generated step file includes "## Agent Return Protocol" section.
- All docs paths point to main repo root docs, not worktree-local docs.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---