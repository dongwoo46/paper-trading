# Step 5 — Cleanup & PR (orchestrator)

## Working Directory
`.worktrees/front-order-management-ui`

## Files to Read First
1. `docs/phase/front/order-management-ui/spec.md`
2. `docs/state.md`
3. `docs/TODO.md`
4. Step 2~4 Completion Reports (이전 스텝 결과 확인)

## Tasks

### 1. 최종 빌드 및 테스트 확인

실행 순서:
1. `cd .worktrees/front-order-management-ui/frontend/trading-web && npm run build`
   - 빌드 실패 시 즉시 FAIL 보고, 수정 후 재실행
2. `cd .worktrees/front-order-management-ui/frontend/trading-web && npm test -- --run`
   - 테스트 실패 시 즉시 FAIL 보고

### 2. 코드 리뷰 결과 반영 확인

- Step 4 Completion Report에서 🔴 MUST FIX 항목이 있었다면 수정 완료 여부 확인
- 미해결 항목이 있으면 BLOCKED 보고

### 3. summary.md 작성

파일 경로: `docs/done/front/order-management-ui/order-management-ui-summary.md` (메인 레포)

포함 내용:
- 구현된 파일 목록 (worktree 상대 경로)
- 핵심 설계 결정 사항 (계좌 선택 방식, 폴링 주기, 클라이언트 필터링 선택 이유)
- API 엔드포인트 매핑 요약
- 테스트 결과 요약
- 알려진 제한사항 또는 향후 개선 포인트

### 4. PR 생성

브랜치: `feature/front-order-management-ui` → `main`

PR 제목: `feat(trading-web): 주문 관리 UI — 주문 생성·내역·취소`

PR Body 포함 항목:
- 변경 사항 요약 (신규 라우트 `/orders`, 구현 컴포넌트 목록)
- 스크린샷 또는 테스트 통과 증거 (테스트 결과 텍스트)
- 리뷰어 체크리스트 (ADR-005 모드 선택, BigDecimal string 처리, FSD 레이어 규칙)

명령: `git push -u origin feature/front-order-management-ui && gh pr create ...`

### 5. 문서 정리 (메인 레포에서 실행)

순서:
1. `docs/done/front/order-management-ui/` 디렉터리 생성
2. `order-management-ui-summary.md` 저장 (위에서 작성)
3. `docs/phase/front/order-management-ui/` 폴더를 `docs/done/front/order-management-ui/phase/` 로 이동
4. `docs/TODO.md` — `order-management-ui` 항목 `[x]` 처리
5. `docs/state.md` 업데이트:
   - `front/order-management-ui` 활성 phase 제거
   - 마지막 액션 업데이트
6. `docs/phase/front/order-management-ui/index.json` 업데이트: `status: "done"`, `current_step: 5`

### 6. 최종 커밋

메시지 형식:
```
docs: front/order-management-ui phase 완료 — summary 및 TODO 업데이트
```

## Acceptance Criteria
- 빌드 및 테스트 모두 통과
- PR 생성 완료 (URL 반환)
- `docs/done/front/order-management-ui/order-management-ui-summary.md` 존재
- `docs/TODO.md` 해당 항목 `[x]` 처리
- `docs/state.md` 최신화

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
