# Step 5: Summary + PR Creation
Assigned agent: orchestrator

## Working Directory
.worktrees/collector-api-ws-reconnect-stability

## Files to Read
- CLAUDE.md
- docs/phase/collector-api/ws-reconnect-stability/spec.md
- docs/phase/collector-api/ws-reconnect-stability/index.json

## Tasks

### Task 1 — Verify Final State

Run the full build and test suite one last time:
```bash
cd .worktrees/collector-api-ws-reconnect-stability/backend/collector-api && ./gradlew compileKotlin && ./gradlew test
```

If any failure, report BLOCKED and stop.

### Task 2 — Write Feature Summary

Write file (in main repo, not worktree):
`docs/done/collector-api/ws-reconnect-stability/ws-reconnect-stability-summary.md`

Contents:
- Feature name and one-line description
- What was implemented (bullet list of new/modified files)
- API endpoint added
- Config keys added
- Test coverage summary (count of test cases)

### Task 3 — Update docs/TODO.md

In the main repo (`docs/TODO.md`), find the `ws-reconnect-stability` entry and mark it `[x]`.

### Task 4 — Update index.json

In the main repo, update `docs/phase/collector-api/ws-reconnect-stability/index.json`:
- Set `"status": "done"` at the top level
- Set step 5 `"status": "done"`

### Task 5 — Update docs/state.md

In the main repo, update `docs/state.md`:
- Set the active feature to the next pending feature from TODO.md, or mark as idle if none

### Task 6 — Create Pull Request

Push the worktree branch and open a PR:

```bash
cd .worktrees/collector-api-ws-reconnect-stability && git push origin feature/collector-api-ws-reconnect-stability
```

PR title: `feat(collector-api): ws-reconnect-stability — heartbeat timeout + health endpoint`

PR body format:
```
## Summary
- Heartbeat timeout detection (60s silence → reconnect) via Reactor `.timeout()` on receive flux
- Per-mode connection state registry (CONNECTED / DISCONNECTED / RECONNECTING)
- `GET /api/kis/ws/health` returns live status for each configured KIS mode
- Re-subscription of active tickers on reconnect is handled by existing `connectOnce()` flow

## Config
`collector.source.kis.heartbeat-timeout: 60s` (new, default 60s)

## Test plan
- [ ] `KisWsConnectionRegistryTest` — state transitions + concurrency
- [ ] `KisWsHealthServiceTest` — delegation + sort order
- [ ] `KisWsHealthControllerTest` — HTTP shape + null handling
- [ ] `./gradlew test` passes in CI

🤖 Generated with Claude Code
```

## Acceptance Criteria
- Build passes
- Tests pass
- Summary file written
- TODO.md marked `[x]`
- PR created and URL returned

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Test result: <passed N/N | failed N — list failing cases>
- Blockers: <none | description>
- PR URL: <url or "not created">
---
