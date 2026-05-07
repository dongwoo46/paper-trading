# Step 7: Rework for Code Review Findings (Retry 2)
Assigned agent: fullstack-dev

## Working Directory
.worktrees/trading-api-trading-journal-service

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/phase/trading-api/trading-journal-service/spec.md
- docs/phase/trading-api/trading-journal-service/step-4.md
- docs/phase/trading-api/trading-journal-service/index.json
- backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/TradingJournalController.kt
- backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/TradingJournalControllerIntegrationTest.kt

## Tasks
1. Fix paging validation contract mismatch.
- For `GET /api/trading-journals`, invalid `size` must return `400` (no silent `coerceIn` fallback).
- Keep invalid `page` handling as `400`.

2. Add regression test.
- Add test case verifying invalid `size` (e.g. 0 or negative) returns 400.

3. Verify.
- `./gradlew test --tests "*TradingJournal*"`
- `./gradlew compileKotlin`

## Acceptance Criteria
- Paging invalid param contract matches spec (`400`).
- Regression test added and passing.

## Agent Return Protocol
---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences>
- Files modified: <list of paths relative to main repo root>
- Test result: <passed/failed with commands>
- Blockers: <none | description>
---
