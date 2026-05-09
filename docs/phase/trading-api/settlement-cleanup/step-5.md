# Step 5: Final Review, Phase Docs Closure, and PR Preparation
Assigned agent: orchestrator

## Working Directory
.worktrees/trading-api-settlement-cleanup

## Files to Read
- CODEX.md
- docs/phase/trading-api/settlement-cleanup/index.json
- docs/phase/trading-api/settlement-cleanup/spec.md
- docs/phase/trading-api/settlement-cleanup/step-2.md
- docs/phase/trading-api/settlement-cleanup/step-3.md
- docs/phase/trading-api/settlement-cleanup/step-4.md
- docs/TODO.md

## Tasks
1. Validate phase outcomes against TODO scope.
- Confirm all four scope items are implemented and tested:
  - receivable settlement removal
  - KIS branch simplification
  - LOCAL 0.2% sell tax fix
  - TaxSummary LOCAL-only scope

2. Run code-review gate.
- Review for regressions in account balance mutation paths and mode separation.
- Review migration safety (table drop impact and startup migration behavior).

3. Update phase index and TODO.
- Mark completed steps status in `index.json`.
- Move phase status to `done` only when all acceptance commands passed.
- Update `docs/TODO.md` target item to done with date/PR.

4. Prepare PR summary artifacts.
- Produce concise summary with changed modules, risk notes, and rollback notes.
- Include exact verification commands and results in PR body.

## Acceptance Criteria
```bash
cd .worktrees/trading-api-settlement-cleanup/backend/trading-api
./gradlew compileKotlin
./gradlew test
```

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---