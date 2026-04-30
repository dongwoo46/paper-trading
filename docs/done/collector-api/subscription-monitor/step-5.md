# Step 5: Summary + PR

**Agent**: orchestrator/cleanup
**Branch**: feature/collector-api-subscription-monitor
**Working Directory**: C:\Users\dw\Desktop\paper-trading\.worktrees\collector-api-subscription-monitor

---

## Files to Read (in order)

1. `C:\Users\dw\Desktop\paper-trading\docs\state.md`
2. `C:\Users\dw\Desktop\paper-trading\docs\phase\collector-api\subscription-monitor\index.json`
3. `C:\Users\dw\Desktop\paper-trading\docs\phase\collector-api\subscription-monitor\spec.md`
4. `C:\Users\dw\Desktop\paper-trading\docs\TODO.md`

---

## Tasks

### Task 1: Final Build + Test Verification

```bash
cd backend/collector-api && ./gradlew compileKotlin
cd backend/collector-api && ./gradlew test
```

Confirm:
- 0 compile errors
- All tests pass (no failures)

### Task 2: Write Done Summary

Create:
`C:\Users\dw\Desktop\paper-trading\docs\done\collector-api\subscription-monitor\subscription-monitor-summary.md`

Include:
- What was built
- New files (with paths relative to repo root)
- API endpoint added
- Test counts
- Design decisions made

### Task 3: Move Phase Folder

Move:
`docs/phase/collector-api/subscription-monitor/` → `docs/done/collector-api/subscription-monitor/`

All files (spec.md, index.json, step-1.md through step-5.md) move together.

Command:
```bash
mkdir -p C:/Users/dw/Desktop/paper-trading/docs/done/collector-api/subscription-monitor
cp -r C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/subscription-monitor/* \
      C:/Users/dw/Desktop/paper-trading/docs/done/collector-api/subscription-monitor/
rm -rf C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/subscription-monitor
```

### Task 4: Update TODO.md

Mark the subscription-monitor item as done in `docs/TODO.md`:

```
- [x] 구독 상태 모니터링 API | project: collector-api | phase: subscription-monitor | priority: P1 | done: 2026-04-30 | pr: #{PR_NUMBER}
```

Replace `{PR_NUMBER}` with the actual PR number after creation.

### Task 5: Update state.md

Update `docs/state.md`:
- 상태: `idle` (또는 다음 Phase가 있으면 `in_progress`)
- 활성 Phase: 완료된 항목 제거 또는 다음 항목으로 교체
- 마지막 액션: `2026-04-30: collector-api/subscription-monitor 완료 — PR #{PR_NUMBER} 생성`

### Task 6: Update index.json

Update `docs/done/collector-api/subscription-monitor/index.json`:
- `"status": "done"`
- `"current_step": 5`
- `"updated": "2026-04-30"`
- All steps: `"status": "done"`

### Task 7: Commit and Create PR

Stage and commit all changes from the worktree:

```bash
git add backend/collector-api/src/
git add docs/
git commit -m "feat(collector-api): subscription-monitor — GET /api/subscriptions/status

Adds SubscriptionStatusService + SubscriptionStatusController.
Assembles WS/REST subscription state and connection health
into a single status endpoint for the operations dashboard."
```

Create PR:
```bash
gh pr create \
  --title "feat(collector-api): subscription-monitor — GET /api/subscriptions/status" \
  --body "$(cat <<'EOF'
## Summary
- Adds `GET /api/subscriptions/status` endpoint to expose current KIS WebSocket/REST subscription state
- New `SubscriptionStatusService` assembles WS health, WS symbols, REST symbols per mode into a unified report
- Builds on top of ws-reconnect-stability infra (KisWsConnectionRegistry, KisWsHealthService)

## New Files
- `application/subscriptions/dto/SubscriptionModeStatus.kt`
- `application/subscriptions/dto/SubscriptionStatusReport.kt`
- `application/subscriptions/service/SubscriptionStatusService.kt`
- `presentation/subscriptions/dto/SubscriptionStatusResponse.kt`
- `presentation/subscriptions/SubscriptionStatusController.kt`

## Test Plan
- [ ] `SubscriptionStatusServiceTest` — 8 unit tests (disabled state, assembly, sorting, slot calc, edge cases)
- [ ] `SubscriptionStatusControllerTest` — 3 MockMvc tests (200 response, null lastConnectedAt, empty modes)
- [ ] Full test suite regression check passes

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

---

## Completion Criteria

- [ ] Build passes
- [ ] All tests pass
- [ ] done summary written
- [ ] phase folder moved to done/
- [ ] TODO.md updated with `[x]` + PR number
- [ ] state.md updated
- [ ] index.json updated to `"status": "done"`
- [ ] PR created

---

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to worktree root>
- Test result: <passed N/N | failed N — list failing cases>
- Blockers: <none | description>
---
