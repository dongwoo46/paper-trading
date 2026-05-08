# Step 5 — Cleanup and PR (orchestrator)

## Agent
orchestrator

## Working Directory
`.worktrees/market-bars-chart-ui`

## Tasks

### 1. 최종 빌드 검증

```bash
cd .worktrees/market-bars-chart-ui/frontend/trading-web && npm run build
```

실패 시 즉시 중단 후 step-2로 반환.

### 2. 최종 테스트 검증

```bash
cd .worktrees/market-bars-chart-ui/frontend/trading-web && npm test -- --run
```

실패 시 step-3 재실행 요청.

### 3. Summary 문서 작성
파일: `docs/done/front/market-bars-chart-ui/market-bars-chart-ui-summary.md`

포함:
- 구현 기능 요약
- 신규/수정 파일 목록
- API 연동 요약 (`GET /api/market/bars/{symbol}?interval=...&limit=...`)
- 오류 처리 UX 요약(400/404/5xx)
- 테스트 결과 및 알려진 제한

### 4. 커밋 및 PR 생성

```bash
git add frontend/trading-web/src/shared/api/marketBarsApi.ts
git add frontend/trading-web/src/pages/market-bars/
git add frontend/trading-web/src/App.tsx
git add frontend/trading-web/src/shared/ui/Sidebar.tsx
git commit -m "feat(trading-web): 분봉 히스토리 차트 UI 추가"
git push origin feature/front-market-bars-chart-ui
gh pr create \
  --title "feat(trading-web): market bars chart UI" \
  --body "collector-api market bars API 연동, interval/limit 제어, loading/empty/error 상태 UX, 모바일 레이아웃 적용." \
  --base main
```

### 5. docs 상태 정리 (main repo 기준)
- `docs/phase/front/market-bars-chart-ui/index.json`
  - `current_step: 5`
  - `status: "done"`
  - 모든 step 상태 `done`
- `docs/TODO.md`
  - `front | phase: market-bars-chart-ui` 항목 `[x]` 처리 + done 날짜
- `docs/state.md`
  - 활성 phase 정보 정리 또는 완료 반영

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
