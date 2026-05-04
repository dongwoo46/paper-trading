# Step 7: Phase Finalization and PR Preparation
Assigned agent: cleanup

## Working Directory
.worktrees/front-trading-journal-ui

## Files to Read
- CODEX.md
- docs/state.md
- docs/TODO.md
- docs/phase/front/trading-journal-ui/index.json
- docs/phase/front/trading-journal-ui/spec.md
- docs/phase/front/trading-journal-ui/step-2.md
- docs/phase/front/trading-journal-ui/step-3.md
- docs/phase/front/trading-journal-ui/step-4.md
- docs/phase/front/trading-journal-ui/step-5.md
- docs/phase/front/trading-journal-ui/step-6.md

## Tasks
1. phase 상태/결과 문서 반영.
   - Modify: `docs/phase/front/trading-journal-ui/index.json`
   - Modify: `docs/state.md`
2. 완료 문서 작성.
   - Create: `docs/done/front/trading-journal-ui/trading-journal-ui-summary.md`
3. 백로그 갱신.
   - Modify: `docs/TODO.md` (`front / trading-journal-ui` -> `[x]`)
4. PR 준비 체크리스트 작성.

## Acceptance Criteria
```bash
cd frontend/trading-web
npm run build
```

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
