# Step 3 — Testing and QA (test-engineer)

## Agent
test-engineer

## Working Directory
`.worktrees/market-bars-chart-ui`

## Files to Read First
1. `docs/phase/front/market-bars-chart-ui/spec.md`
2. `frontend/trading-web/src/shared/api/marketBarsApi.ts`
3. `frontend/trading-web/src/pages/market-bars/ui/MarketBarsChartPage.tsx`
4. `frontend/trading-web/src/App.tsx`
5. `frontend/trading-web/package.json`

## Tasks

### 1. API client 테스트
파일: `frontend/trading-web/src/shared/api/marketBarsApi.test.ts` (신규)

- `fetchJson` mock 기반으로 endpoint/query 조합 검증
- 검증 항목:
  - symbol path 적용 (`/api/market/bars/{symbol}`)
  - `interval` query 반영
  - `limit` query 반영

### 2. 페이지 상태 테스트
파일: `frontend/trading-web/src/pages/market-bars/ui/MarketBarsChartPage.test.tsx` (신규)

- API 모듈 mock으로 UI 상태 검증
- 케이스:
  - 로딩 상태 문구 노출
  - 성공 시 차트/데이터 영역 렌더
  - 404 또는 빈 배열 응답 시 빈 상태 문구 노출
  - 400 응답 시 입력 오류 문구 노출
  - 5xx/네트워크 오류 시 일반 오류 문구 + 재시도 버튼 노출
  - interval 탭 클릭 시 요청 인자 변경 확인
  - limit 변경 시 요청 인자 변경 확인

### 3. 라우팅 스모크 테스트
파일: `frontend/trading-web/src/App.test.tsx` 또는 기존 라우팅 테스트 파일 (수정)

- `/market-bars` 경로 접근 시 페이지 헤더가 렌더되는지 확인

### 4. 실행

```bash
cd .worktrees/market-bars-chart-ui/frontend/trading-web && npm test -- --run
```

## Acceptance Criteria
- 신규/수정 테스트 전체 통과
- `400/404/5xx` 상태 분기 테스트 통과
- interval/limit 변경에 따른 재요청 테스트 통과

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
