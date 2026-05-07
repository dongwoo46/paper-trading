# Step 5: Phase Wrap-up and Handoff
Assigned agent: orchestrator

## Working Directory
.worktrees/trading-api-trading-journal-service

## Files to Read
- CODEX.md
- docs/state.md
- docs/TODO.md
- docs/phase/trading-api/trading-journal-service/index.json
- docs/phase/trading-api/trading-journal-service/spec.md
- docs/phase/trading-api/trading-journal-service/step-2.md
- docs/phase/trading-api/trading-journal-service/step-3.md
- docs/phase/trading-api/trading-journal-service/step-4.md

## Tasks
1. Ensure step statuses/results are updated in phase index.
2. Confirm Step 2~4 outputs satisfy spec and no blocker remains.
3. Prepare done summary doc:
- `docs/done/trading-api/trading-journal-service/trading-journal-service-summary.md`
4. Update root state and TODO when phase is fully accepted:
- mark phase done in `docs/TODO.md`
- transition `docs/state.md` per orchestrator rules
5. Prepare PR message with scope/tests/risks.

## Acceptance Criteria
```bash
cd backend/trading-api
./gradlew test --tests "*TradingJournal*"
./gradlew compileKotlin
```

## Agent Return Protocol
When finished, return:
- State transition summary (`state.md`, `TODO.md`, phase index)
- Done summary path
- PR draft title/body
- Remaining follow-up items (if any)
