# Step 1: Feature Spec and API Spec
Assigned agent: service-planner

## Working Directory
.worktrees/trading-api-tax-summary-service/backend/trading-api

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/PRD.md
- docs/TODO.md
- backend/trading-api/graphify-out/graph.json

## Tasks
1. Structure requirements and identify missing decisions for tax-summary-service.
2. Confirm DDD model updates (Entity, VO, Aggregate, Domain Event).
3. Design API spec (endpoints, request/response, validation, error mapping).
4. Design DB schema changes (tables/indexes/constraints/migration plan).
5. Write docs/phase/trading-api/tax-summary-service/spec.md.
6. Generate step-2.md to step-N.md with concrete implementation directives.
- Each step file must include explicit Working Directory and Files to Read.
- Every path must reference the main repo root docs and code locations.

## Acceptance Criteria
- spec.md created for tax-summary-service.
- step-2.md to step-N.md created with concrete classes/files/tests.
- Every generated step includes the Agent Return Protocol section.
- Scope explicitly excludes strategy domain work under current hold policy.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
