# Step 6: Rework Code Review
Assigned agent: code-reviewer

## Working Directory
.worktrees/front-trading-journal-ui

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/phase/front/trading-journal-ui/spec.md
- docs/phase/front/trading-journal-ui/step-4.md
- docs/phase/front/trading-journal-ui/step-5.md
- frontend/trading-web/src/entities/trading-journal/**
- frontend/trading-web/src/features/trading-journal/**
- frontend/trading-web/src/pages/trading-journal/**

## Tasks
1. Step-4 리뷰 fail 항목이 모두 해소되었는지 확인한다.
2. API contract, 편집 필드(title/content/sentiment), 에러 케이스 테스트(400/404/409)를 집중 검토한다.
3. must-fix가 없으면 PASS, 있으면 FAIL로 구체 파일/수정지시를 남긴다.

## Acceptance Criteria
```bash
cd frontend/trading-web
npm run test -- tradingJournalApi.test.ts TradingJournalPage.test.tsx
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
