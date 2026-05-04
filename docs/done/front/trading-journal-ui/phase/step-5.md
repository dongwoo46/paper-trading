# Step 5: Review Rework Implementation
Assigned agent: fullstack-dev

## Working Directory
.worktrees/front-trading-journal-ui

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/phase/front/trading-journal-ui/spec.md
- docs/phase/front/trading-journal-ui/step-2.md
- docs/phase/front/trading-journal-ui/step-4.md
- frontend/trading-web/src/entities/trading-journal/**
- frontend/trading-web/src/features/trading-journal/**
- frontend/trading-web/src/pages/trading-journal/**

## Tasks
1. API 계약을 spec과 일치시킨다.
   - list endpoint/query: `/api/trading-journals?accountId=...&ticker=...&from=...&to=...&page=...&size=...`
   - detail endpoint: `/api/trading-journals/{journalId}`
   - update endpoint: `/api/trading-journals/{journalId}` (PATCH)
2. 타입/응답 모델을 spec 기준으로 정렬한다.
   - 편집 필드: `title`, `content`, `sentiment`
   - list/detail response shape을 spec 문서와 맞춘다.
3. UI 편집 폼과 상태 동기화를 수정한다.
   - note 중심 구현을 title/content/sentiment 중심으로 변경
   - 저장 성공 시 목록/상세 invalidate 유지
4. 테스트 보강.
   - `tradingJournalApi.test.ts`에 400/404/409 에러 전파 케이스 추가
   - 필요 시 `TradingJournalPage.test.tsx`를 spec 동작과 맞게 보강

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
