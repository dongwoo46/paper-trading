# Step 6: Rework for TradingJournal Integration Test Failures
Assigned agent: fullstack-dev

## Working Directory
.worktrees/trading-api-trading-journal-service

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/phase/trading-api/trading-journal-service/spec.md
- docs/phase/trading-api/trading-journal-service/step-2.md
- docs/phase/trading-api/trading-journal-service/step-3.md
- docs/phase/trading-api/trading-journal-service/index.json
- backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/TradingJournalController.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/portfolio/TradingJournalCreateRequest.kt
- backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/TradingJournalControllerIntegrationTest.kt

## Tasks
1. Fix invalid sentiment handling to satisfy spec and test expectation.
- Ensure create API returns 400 for invalid sentiment input.
- Preserve existing valid enum paths.

2. Fix ticker filter paging determinism mismatch.
- Align sort/order behavior between API and integration test expectation (latest-first by createdAt, deterministic tie-breaker when needed).
- Update implementation or test setup/assertion so behavior is deterministic and spec-compliant.

3. Re-run targeted tests and compile.
- `./gradlew test --tests "*TradingJournalControllerIntegrationTest"`
- `./gradlew test --tests "*TradingJournal*"`
- `./gradlew compileKotlin`

## Acceptance Criteria
- Both previously failing cases pass.
- TradingJournal-related tests pass.
- compileKotlin passes.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences>
- Files modified: <list of paths relative to main repo root>
- Test result: <passed/failed with commands>
- Blockers: <none | description>
---
