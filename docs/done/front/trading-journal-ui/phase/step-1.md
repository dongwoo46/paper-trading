# Step 1: Feature Spec and API Spec
Assigned agent: Service Planner

## Working Directory
.worktrees/front-trading-journal-ui

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/PRD.md
- docs/TODO.md
- frontend/trading-web/src

## Tasks
1. Structure requirements for trading journal UI and list ambiguities/questions.
2. Define UI flow and state model for list/detail/edit and ticker filter UX.
3. Design frontend API contract usage with trading-api trading journal endpoints.
4. Write `docs/phase/front/trading-journal-ui/spec.md`.
5. Generate `step-2.md` to `step-N.md` with concrete implementation/testing/review/cleanup directives.
   - Every step file must include explicit worktree path and required file paths.
   - Every step file must include the Agent Return Protocol section.

## Acceptance Criteria
- `spec.md` created for trading-journal-ui.
- `step-2.md` ~ `step-N.md` created with concrete file-level instructions.
- All step files include `## Agent Return Protocol`.
- All docs paths in step files reference main repo root docs.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
