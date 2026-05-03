# Step 1: Feature Spec and API Spec
Assigned agent: service-planner

## Working Directory
.worktrees/trading-api-tax-summary

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/PRD.md
- docs/TODO.md
- docs/phase/trading-api/tax-summary/index.json
- backend/trading-api/graphify-out/graph.json

## Tasks
1. Structure `tax-summary` requirements and clarify ambiguities with concrete questions.
2. Confirm DDD model (Entity, VO, Aggregate, Domain Event) for tax settlement automation.
3. Design API spec (endpoints, request/response, error cases).
4. Design DB schema (tables, indexes, relationships).
5. Write `docs/phase/trading-api/tax-summary/spec.md`.
6. Generate `step-2.md` to `step-N.md` with concrete implementation directives.
   - Explicitly list worktree path and required file paths in each step's `Files to Read` section.
   - Include validation commands per step.

## Acceptance Criteria
- `spec.md` created.
- `step-2.md` to `step-N.md` created (with file paths, class signatures, key rules).
- Each generated step includes `## Agent Return Protocol`.
- All docs paths in step files reference the main repo root (not worktree-relative docs path assumptions).

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
