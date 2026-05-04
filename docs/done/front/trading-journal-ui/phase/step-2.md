# Step 2: Trading Journal UI Implementation (TDD)
Assigned agent: fullstack-dev

## Working Directory
.worktrees/front-trading-journal-ui

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/phase/front/trading-journal-ui/spec.md
- frontend/trading-web/src/App.tsx
- frontend/trading-web/src/shared/ui/Sidebar.tsx
- frontend/trading-web/src/shared/api/http.ts
- frontend/trading-web/src/pages/tax-summary/ui/TaxSummaryPage.tsx

## Tasks
1. Trading Journal 엔티티 타입/API 클라이언트 추가.
   - Create: `frontend/trading-web/src/entities/trading-journal/model/types.ts`
     - `type JournalSentiment = "BULLISH" | "BEARISH" | "NEUTRAL" | "REFLECTIVE"`
     - `interface TradingJournalListItem`
     - `interface TradingJournalListResponse`
     - `interface TradingJournalDetailResponse`
     - `interface UpdateTradingJournalRequest`
   - Create: `frontend/trading-web/src/entities/trading-journal/api/tradingJournalApi.ts`
     - `fetchTradingJournalList(params: { accountId: number; ticker?: string; from: string; to: string; page: number; size: number }): Promise<TradingJournalListResponse>`
     - `fetchTradingJournalDetail(journalId: number): Promise<TradingJournalDetailResponse>`
     - `updateTradingJournal(journalId: number, body: UpdateTradingJournalRequest): Promise<{ journalId: number; status: "UPDATED" }>`
2. 거래 일지 페이지/피처 컴포넌트 구현.
   - Create: `frontend/trading-web/src/pages/trading-journal/ui/TradingJournalPage.tsx`
   - Create: `frontend/trading-web/src/features/trading-journal/ui/TradingJournalFilterPanel.tsx`
   - Create: `frontend/trading-web/src/features/trading-journal/ui/TradingJournalListPanel.tsx`
   - Create: `frontend/trading-web/src/features/trading-journal/ui/TradingJournalDetailPanel.tsx`
   - 처리 규칙:
     - 목록/상세/수정 각각 로딩/오류/빈 상태 표시
     - ticker/기간 필터 변경 시 목록 query key 갱신
     - 저장 성공 시 목록/상세 invalidate
     - 저장 중 중복 제출 방지(disabled)
3. 라우팅/네비게이션 연결.
   - Modify: `frontend/trading-web/src/App.tsx`
     - `/trading-journals` route 추가
   - Modify: `frontend/trading-web/src/shared/ui/Sidebar.tsx`
     - 거래 일지 메뉴 링크 추가
4. 스타일링은 기존 패턴을 재사용하고 신규 전역 스타일 변경은 금지.

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

