# Step 3: Trading Journal UI Testing and QA
Assigned agent: test-engineer

## Working Directory
.worktrees/front-trading-journal-ui

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/phase/front/trading-journal-ui/spec.md
- docs/phase/front/trading-journal-ui/step-2.md
- frontend/trading-web/src/entities/trading-journal/api/tradingJournalApi.ts
- frontend/trading-web/src/pages/trading-journal/ui/TradingJournalPage.tsx

## Tasks
1. API client 단위 테스트 추가/보강.
   - Create or Modify: `frontend/trading-web/src/entities/trading-journal/api/tradingJournalApi.test.ts`
   - 검증 포인트:
     - 목록 조회 query string(accountId, ticker, from, to, page, size)
     - 상세 조회 URL
     - PATCH payload/HTTP method
     - 400/404/409 에러 전파
2. 페이지/피처 통합 테스트 추가.
   - Create or Modify: `frontend/trading-web/src/pages/trading-journal/ui/TradingJournalPage.test.tsx`
   - 검증 포인트:
     - 초기 목록 로딩/렌더링
     - ticker 필터 적용 시 재조회
     - row 선택 시 상세 조회
     - 수정 저장 성공 시 invalidate/재렌더링
     - API 실패 시 에러 메시지 표시
3. 회귀 영향 확인.
   - 기존 `tax-summary`, `orders`, `portfolio` 페이지 smoke 실행으로 라우팅 회귀 점검.

## Acceptance Criteria
```bash
cd frontend/trading-web
npm run test -- tradingJournalApi.test.ts TradingJournalPage.test.tsx
npm run test -- TaxSummaryPage.test.tsx OrderTablePanel.test.tsx PortfolioChartPage.test.tsx
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

