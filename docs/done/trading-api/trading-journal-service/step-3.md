# Step 3: Trading Journal QA Verification
Assigned agent: test-engineer

## Working Directory
.worktrees/trading-api-trading-journal-service

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/phase/trading-api/trading-journal-service/spec.md
- docs/phase/trading-api/trading-journal-service/step-2.md
- docs/phase/trading-api/trading-journal-service/index.json
- backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/*
- backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/*TradingJournal*

## Tasks
1. Validate Step 2 behavior against spec:
- create/update/list/detail happy paths
- validation errors (blank title/content, invalid sentiment/type)
- 404 cases (account/journal not found)
- account/journal mismatch guard
- ticker filter + paging determinism

2. Add missing tests when gaps are found.
- Keep tests deterministic (seeded data / explicit sort assertions).
- Verify BigDecimal/string formatting consistency if exposed in response DTOs.

3. Execute targeted and broad verification.

## Acceptance Criteria
```bash
cd backend/trading-api
./gradlew test --tests "*TradingJournal*"
./gradlew test
./gradlew compileKotlin
```

## Agent Return Protocol
When finished, return:
- Validation checklist with PASS/FAIL per scenario
- Added/updated test file list
- Command outputs summary
- Defects found (if any) with reproduction notes
