# Step 5: Summary and PR
Assigned agent: Orchestrator (cleanup)

## Working Directory
`.worktrees/trading-api-settlement-service`

## Tasks

1. Run full test suite one final time:
```bash
cd .worktrees/trading-api-settlement-service/backend/trading-api && ./gradlew test
```

2. Write `docs/phase/trading-api/settlement-service/settlement-service-summary.md` (to main repo root):
   - What was built
   - Key design decisions (Option C — LOCAL immediate, KIS T+2)
   - Files created/modified
   - Test results

3. Commit all changes in the worktree:
```bash
cd .worktrees/trading-api-settlement-service
git add -A
git commit -m "feat: T+2 정산 처리 서비스 구현 (settlement-service)"
```

4. Create PR:
```bash
gh pr create \
  --title "feat: T+2 정산 처리 서비스 (settlement-service)" \
  --base main \
  --head feature/trading-api-settlement-service \
  --body "..."
```

5. Report PR URL.

## Acceptance Criteria

- All tests pass
- PR created and URL returned
- summary.md written to `docs/phase/trading-api/settlement-service/`

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences>
- Files modified: <list>
- Blockers: <none | description>
---
