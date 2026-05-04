# Step 4: Trading Journal UI Code Review
Assigned agent: code-reviewer

## Working Directory
.worktrees/front-trading-journal-ui

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/phase/front/trading-journal-ui/spec.md
- docs/phase/front/trading-journal-ui/step-2.md
- docs/phase/front/trading-journal-ui/step-3.md
- frontend/trading-web/src/entities/trading-journal/**
- frontend/trading-web/src/features/trading-journal/**
- frontend/trading-web/src/pages/trading-journal/**
- frontend/trading-web/src/App.tsx
- frontend/trading-web/src/shared/ui/Sidebar.tsx

## Tasks
1. 스펙 정합성 검토.
   - API contract, 필터 UX, 수정 범위(`title/content/sentiment`) 일치 여부 확인.
2. 위험 중심 리뷰.
   - must-fix: 데이터 무결성/오류 처리/상태 동기화 문제
   - should-fix: 접근성/성능/테스트 누락
3. 테스트 품질 검토.
   - 실패 케이스(400/404/409) 커버 여부
   - 비동기 UI 전환 시 race condition 가능성 확인
4. 결과를 severity 기준으로 정리하고 재작업 필요 시 구체 파일 경로와 수정 방향 제시.

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

