# Step 2 — Implementation (fullstack-dev)

## Agent
fullstack-dev

## Working Directory
`.worktrees/market-bars-chart-ui`

## Files to Read First
1. `CODEX.md`
2. `docs/phase/front/market-bars-chart-ui/spec.md`
3. `frontend/trading-web/src/App.tsx`
4. `frontend/trading-web/src/shared/api/index.ts`
5. `frontend/trading-web/src/pages/historical/ui/HistoricalPage.tsx`

## Tasks

### 1. API client 추가
파일: `frontend/trading-web/src/shared/api/marketBarsApi.ts` (신규)

- `MarketBarsInterval = "1m" | "5m" | "10m"` 타입 정의
- `MarketBar` 인터페이스 정의 (`startedAt`, `open`, `high`, `low`, `close`, `volume`, `tradeValue`, `vwap`, `tickCount`)
- `fetchMarketBars(params)` 구현:
  - 입력: `symbol`, `interval`, `limit`
  - 호출: `GET /api/market/bars/{symbol}?interval={interval}&limit={limit}`
  - `fetchJson` 사용 (직접 `fetch` 금지)

### 2. 차트 페이지 구현
파일: `frontend/trading-web/src/pages/market-bars/ui/MarketBarsChartPage.tsx` (신규)

- 컨트롤:
  - symbol 입력 + 조회 버튼
  - interval 탭 (`1m`, `5m`, `10m`)
  - limit 선택 (`50`, `100`, `200`, `500`)
- query key: `["market-bars", symbol, interval, limit]`
- 상태 UX:
  - 로딩: "분봉 데이터를 불러오는 중..."
  - 빈 상태: "해당 조건의 분봉 데이터가 없습니다."
  - 에러 상태:
    - `400`: "조회 조건을 확인해 주세요."
    - `404`: 빈 상태와 동일 처리
    - 기타: "일시적인 오류가 발생했습니다. 다시 시도해 주세요."
  - 재시도 버튼 제공
- 성공 상태:
  - 차트 렌더(기존 프로젝트 차트 컴포넌트 재사용 우선)
  - 마지막 갱신 시각 표시
- 모바일 대응:
  - 상단 컨트롤 2행 배치
  - 차트 최소 높이 보장

### 3. 라우팅 연결
파일: `frontend/trading-web/src/App.tsx` (수정)

- `/market-bars` 라우트 추가
- 페이지 타이틀 매핑 추가 (예: `"분봉 히스토리 차트"`)

파일: `frontend/trading-web/src/shared/ui/Sidebar.tsx` (수정)
- 사이드바 메뉴에 `/market-bars` 링크 추가

### 4. 스타일 보완
파일: `frontend/trading-web/src/pages/market-bars/ui/MarketBarsChartPage.css` (신규 또는 기존 스타일 파일에 추가)

- 모바일(360px 이상)에서 입력/탭/limit 컨트롤이 겹치지 않도록 구성
- 탭/버튼 터치 영역 최소 높이 40px 보장

## Acceptance Criteria
- `interval` 탭 전환 시 요청 파라미터가 `1m/5m/10m`으로 정확히 변경됨
- `limit` 변경 시 요청이 재실행됨
- `400/404/기타` 오류 UX가 spec과 일치함
- 아래 명령으로 빌드 성공:

```bash
cd .worktrees/market-bars-chart-ui/frontend/trading-web && npm run build
```

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
