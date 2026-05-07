# Step 1: Feature Spec and API Spec
Assigned agent: service-planner

## Working Directory
.worktrees/trading-api-trading-journal-service

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/PRD.md
- docs/TODO.md
- backend/trading-api/graphify-out/graph.json

## Tasks
1. Structure requirements and clarify ambiguities with questions.
2. Confirm DDD model (Entity, VO, Aggregate, Domain Event).
3. Design API spec (endpoints, request/response, error cases).
4. Design DB schema (tables, indexes, relationships).
5. Write `spec.md`.
6. Generate `step-2.md` to `step-N.md` with concrete implementation directives.
- Explicitly list worktree path and required file paths in each step file's "Files to Read" section.

## Acceptance Criteria
- `spec.md` created.
- `step-2.md` to `step-N.md` created (file paths, class signatures, key rules included).
- Each step file includes the "## Agent Return Protocol" section.
- All doc paths in step files reference main repo root (not worktree).
- User approval received.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
