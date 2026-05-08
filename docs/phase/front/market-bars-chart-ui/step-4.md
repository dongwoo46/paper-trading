# Step 4 — Code Review (code-reviewer)

## Agent
code-reviewer

## Working Directory
`.worktrees/market-bars-chart-ui`

## Files to Read
1. `docs/phase/front/market-bars-chart-ui/spec.md`
2. `frontend/trading-web/src/shared/api/marketBarsApi.ts`
3. `frontend/trading-web/src/pages/market-bars/ui/MarketBarsChartPage.tsx`
4. `frontend/trading-web/src/pages/market-bars/ui/MarketBarsChartPage.css`
5. `frontend/trading-web/src/App.tsx`
6. `frontend/trading-web/src/shared/ui/Sidebar.tsx`
7. `frontend/trading-web/src/shared/api/marketBarsApi.test.ts`
8. `frontend/trading-web/src/pages/market-bars/ui/MarketBarsChartPage.test.tsx`

## Review Checklist

각 항목에 대해 아래 포맷으로 결과 출력:
- PASS — 문제 없음
- WARNING — 개선 권장
- MUST FIX — 머지 전 수정 필수

### API Contract
- [ ] endpoint가 `/api/market/bars/{symbol}`로 고정되어 있는가
- [ ] `interval`이 `1m|5m|10m` enum으로 제한되는가
- [ ] `limit`이 숫자 검증 및 허용 범위 제한을 따르는가
- [ ] feature/page 레이어에서 직접 fetch 호출 없이 `shared/api`를 경유하는가

### Error Handling UX
- [ ] 400 오류가 입력 오류 메시지로 노출되는가
- [ ] 404가 빈 상태 UX로 처리되는가
- [ ] 5xx/네트워크 오류가 일반 오류 + 재시도로 처리되는가
- [ ] 로딩/빈/오류/성공 상태가 상호 배타적으로 렌더되는가

### UI Behavior
- [ ] interval 탭 전환 시 즉시 재조회되는가
- [ ] limit 변경 시 즉시 재조회되는가
- [ ] 빈 symbol 입력 시 요청이 차단되는가
- [ ] 모바일 레이아웃에서 컨트롤이 겹치지 않는가

### Type/Architecture
- [ ] `any` 타입 사용이 없는가
- [ ] query key가 `["market-bars", symbol, interval, limit]`를 따르는가
- [ ] 서버 OHLC 데이터 재계산 없이 표시만 수행하는가

### Routing
- [ ] `/market-bars` 라우트가 App에 추가되었는가
- [ ] Sidebar에 접근 링크가 추가되었는가

## Output Rule
- MUST FIX 0건: 최종 판정 `APPROVED`
- MUST FIX 1건 이상: 최종 판정 `CHANGES REQUIRED` + 수정 가이드 포함

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to worktree root>
- Test result: <passed N/N | failed N — list failing cases> (if applicable)
- Blockers: <none | description>
---
