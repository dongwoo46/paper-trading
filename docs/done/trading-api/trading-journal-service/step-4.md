# Step 4: Code Review and Risk Sign-off
Assigned agent: code-reviewer

## Working Directory
.worktrees/trading-api-trading-journal-service

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/phase/trading-api/trading-journal-service/spec.md
- docs/phase/trading-api/trading-journal-service/step-2.md
- docs/phase/trading-api/trading-journal-service/step-3.md
- docs/phase/trading-api/trading-journal-service/index.json
- All files changed in Step 2 and Step 3

## Tasks
1. Review for behavioral correctness against spec.
2. Review DDD boundary consistency:
- entity invariants vs service-level validation
- account linkage and consistency checks
3. Review API contract stability for frontend integration.
4. Review persistence/index impact and query risks.
5. Confirm tests cover critical regressions and edge cases.

## Acceptance Criteria
```bash
cd backend/trading-api
./gradlew test --tests "*TradingJournal*"
./gradlew compileKotlin
```

## Agent Return Protocol
When finished, return:
- Findings ordered by severity (Critical/High/Medium/Low)
- File and line references for each finding
- Required fixes before merge
- Optional improvements after merge
- Final verdict: APPROVE | REQUEST_CHANGES
