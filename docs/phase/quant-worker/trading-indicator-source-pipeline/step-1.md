# Step 1: Feature Spec and API Spec
Assigned agent: quant-planner

## Working Directory
.worktrees/quant-worker-trading-indicator-source-pipeline

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/PRD.md
- docs/TODO.md
- docs/phase/quant-worker/trading-indicator-source-pipeline/index.json

## Tasks
1. Define detailed spec for `quant-worker/trading-indicator-source-pipeline` from TODO scope.
2. Design data pipelines for KR/US trading indicator source datasets (excluding news/disclosures).
3. Define storage/index/partition and normalization contracts for collector-api consumption.
4. Write `spec.md`.
5. Create `step-2.md` to `step-5.md` with concrete implementation/testing/review/cleanup instructions and explicit file paths.

## Acceptance Criteria
- `spec.md` created.
- `step-2.md` to `step-5.md` created with actionable directives.
- Each step file includes `## Agent Return Protocol`.
- All doc paths in step files reference main repo root docs.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
