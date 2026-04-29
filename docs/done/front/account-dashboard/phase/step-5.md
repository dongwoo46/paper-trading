# Step 5 — Cleanup & PR (orchestrator)

## Agent
orchestrator

## Working Directory
`.worktrees/front-account-dashboard`

## Tasks

### 1. 최종 빌드 검증

```
cd .worktrees/front-account-dashboard/frontend/trading-web && npm run build
```

빌드 실패 시 즉시 중단, 이전 단계로 피드백.

### 2. 최종 테스트 검증

```
cd .worktrees/front-account-dashboard/frontend/trading-web && npm test -- --run
```

실패 케이스가 있으면 step-3 재실행 요청.

### 3. summary.md 작성

파일: `docs/done/front/account-dashboard/account-dashboard-summary.md` (메인 레포 기준)

포함 내용:
- 구현된 기능 목록
- 신규 파일 목록 (상대 경로)
- 수정된 파일 목록
- API 엔드포인트 매핑 요약
- 테스트 결과 요약
- 알려진 제한 사항 또는 후속 작업

### 4. 워크트리 커밋 및 PR 생성

```
git add frontend/trading-web/src/entities/account/
git add frontend/trading-web/src/shared/api/accountApi.ts
git add frontend/trading-web/src/shared/utils/format.ts
git add frontend/trading-web/src/features/account-overview/
git add frontend/trading-web/src/features/position-table/
git add frontend/trading-web/src/pages/account/
git add frontend/trading-web/src/App.tsx
git add frontend/trading-web/src/shared/ui/Sidebar.tsx
git commit -m "feat(trading-web): 계좌·포지션 대시보드 페이지 추가"
git push origin feature/front-account-dashboard
gh pr create \
  --title "feat(trading-web): 계좌·포지션 대시보드" \
  --body "계좌 목록(예수금·평가금액·수익률) + 포지션 테이블 구현. trading-api GET /api/v1/accounts, GET /api/v1/accounts/{accountId}/positions 연동." \
  --base main
```

### 5. docs 정리

메인 레포(`C:\Users\dw\Desktop\paper-trading`) 기준으로 실행:

- `docs/phase/front/account-dashboard/index.json` — `current_step: 5`, `status: "done"`, 모든 step `status: "done"` 처리
- `docs/TODO.md` — `account-dashboard` 항목 `[x]` 처리
- `docs/state.md` — `front/account-dashboard` 활성 Phase 항목 제거 또는 완료 표시

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
