# Step 1: Feature Spec and API Spec
Assigned agent: Service Planner

## Working Directory
.worktrees/collector-api-realtime-market-features

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/PRD.md
- docs/TODO.md
- backend/collector-api/graphify-out/graph.json

## Tasks
1. Structure requirements for `collector-api/realtime-market-features` and clarify ambiguities with questions.
2. Confirm DDD model (Entity, VO, Aggregate, Domain Event) for feature snapshot generation.
3. Design API/contract surfaces if needed (internal endpoints, Redis schema contracts, error cases).
4. Design data schema for Redis keys, rollover policy, maxlen/TTL, and debug ring buffer scope.
5. Write `docs/phase/collector-api/realtime-market-features/spec.md`.
6. Generate `step-2.md` to `step-N.md` with concrete implementation directives.
   - Explicitly list worktree path and required file paths in each step's "Files to Read" section.

## Acceptance Criteria
- `spec.md` created under `docs/phase/collector-api/realtime-market-features/`.
- `step-2.md` to `step-N.md` created with concrete implementation details.
- Each step file includes the "## Agent Return Protocol" section.
- All doc paths in step files reference main repo root docs (not worktree docs).
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
