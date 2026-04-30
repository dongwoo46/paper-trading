# Step 5 — orchestrator: Summary + PR Creation

## Context
- Phase: front/realtime-execution
- Branch: feature/front-realtime-execution
- Main repo root: C:\Users\dw\Desktop\paper-trading

## Files to Read First

1. `C:\Users\dw\Desktop\paper-trading\docs\phase\front\realtime-execution\spec.md`
2. `C:\Users\dw\Desktop\paper-trading\docs\phase\front\realtime-execution\index.json`
3. `C:\Users\dw\Desktop\paper-trading\docs\state.md`
4. `C:\Users\dw\Desktop\paper-trading\docs\TODO.md`

---

## Tasks

### 1. Final Build Verification

Run all builds from the worktree:
```
cd backend/trading-api && ./gradlew compileKotlin && ./gradlew test
cd frontend/trading-web && npm run build && npm run test -- --run
```

If any build fails, STOP and report BLOCKED.

---

### 2. Write Done Summary

**Create** `C:\Users\dw\Desktop\paper-trading\docs\done\front\realtime-execution\realtime-execution-summary.md`

Include:
- Feature description (1 paragraph)
- Transport decision rationale (SSE vs WebSocket)
- Backend changes summary (new files, modified files)
- Frontend changes summary (new files, modified files)
- Test coverage: which tests were added
- Any deferred items (e.g., production CORS restriction, tickerName mapping)

---

### 3. Move Phase Folder

Move: `C:\Users\dw\Desktop\paper-trading\docs\phase\front\realtime-execution\`
To: `C:\Users\dw\Desktop\paper-trading\docs\done\front\realtime-execution\`

(Summary file created in step 2 goes here as well)

---

### 4. Update TODO.md

**Modify** `C:\Users\dw\Desktop\paper-trading\docs\TODO.md`

Change the realtime-execution entry from:
```
- [ ] 실시간 체결 알림 | project: front | phase: realtime-execution | priority: P1
```
To:
```
- [x] 실시간 체결 알림 | project: front | phase: realtime-execution | priority: P1 | done: 2026-04-30 | pr: #{PR_NUMBER}
```

(Fill in actual PR number after PR is created)

---

### 5. Update state.md

**Modify** `C:\Users\dw\Desktop\paper-trading\docs\state.md`

- Change `상태` to `idle`
- Clear `활성 Phase`
- Update `마지막 액션` to: `2026-04-30: front/realtime-execution 완료 — PR #{PR_NUMBER} 생성`
- Update `다음 액션` to: (next item from TODO.md backlog, or `없음 — /orchestrate 실행`)

---

### 6. Commit and Push

Stage all modified and new files in the feature branch:
```
git add backend/trading-api/src/main/kotlin/com/papertrading/api/domain/event/
git add backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/sse/
git add backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/ExecutionSseController.kt
git add backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/sse/
git add backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt
git add backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/
git add backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/dto/sse/
git add frontend/trading-web/src/entities/execution/
git add frontend/trading-web/src/shared/lib/sse/
git add frontend/trading-web/src/shared/ui/Toast/
git add frontend/trading-web/src/features/execution-toast/
git add frontend/trading-web/src/App.tsx
git add docs/
```

Commit message:
```
feat(front/realtime-execution): SSE 체결 알림 — backend SSE endpoint + frontend toast

- trading-api: ExecutionFilledEvent, SseRegistry, SseController, TransactionalEventListener handler
- trading-web: useExecutionSse hook, zustand toast store, ToastItem/ToastContainer components
- tests: SseRegistry unit tests, ToastStore vitest, ToastItem render tests
```

Push: `git push origin feature/front-realtime-execution`

---

### 7. Create Pull Request

Use `gh pr create`:

```
gh pr create \
  --title "feat(front/realtime-execution): 실시간 체결 SSE 알림" \
  --base main \
  --body "..."
```

PR body must include:
- Summary: what was built
- Backend changes list
- Frontend changes list
- Test coverage
- How to test manually (start trading-api + trading-web, place a LOCAL order, check toast appears)
- Deferred: production CORS restriction, tickerName KRX mapping

---

### 8. Update index.json

**Modify** `C:\Users\dw\Desktop\paper-trading\docs\done\front\realtime-execution\index.json`

Set:
```json
"status": "done",
"current_step": 5,
"updated": "2026-04-30"
```
Set all steps to `"status": "completed"`.

---

## Agent Return Protocol

Return a completion report in this exact format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <phase completed, PR created>
- PR URL: <url>
- Files modified: docs/state.md, docs/TODO.md, docs/done/front/realtime-execution/realtime-execution-summary.md
- Blockers: <none | description>
---
