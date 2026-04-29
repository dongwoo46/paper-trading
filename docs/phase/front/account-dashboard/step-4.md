# Step 4 — Code Review (code-reviewer)

## Agent
code-reviewer

## Working Directory
`.worktrees/front-account-dashboard`

## Files to Read
1. `docs/phase/front/account-dashboard/spec.md`
2. `frontend/trading-web/src/entities/account/model/types.ts`
3. `frontend/trading-web/src/shared/api/accountApi.ts`
4. `frontend/trading-web/src/shared/utils/format.ts`
5. `frontend/trading-web/src/features/account-overview/ui/AccountCard.tsx`
6. `frontend/trading-web/src/features/position-table/ui/PositionTable.tsx`
7. `frontend/trading-web/src/pages/account/ui/AccountDashboardPage.tsx`
8. `frontend/trading-web/src/App.tsx`
9. `frontend/trading-web/src/shared/ui/Sidebar.tsx`

## Review Checklist

각 항목에 대해 결과를 아래 형식으로 출력:
- 🟢 PASS — 문제 없음
- 🟡 WARNING — 개선 권장 (블로커 아님)
- 🔴 MUST FIX — 머지 전 수정 필수

### 타입 안전성
- [ ] `any` 타입 사용 여부 — CLAUDE.md 규칙: `any` 금지
- [ ] `unknown` + 타입 가드 적용 여부 (불명확 타입)
- [ ] API 응답 타입과 `AccountResponse`, `PositionResponse` 인터페이스 일치 여부
- [ ] `string | null` 필드에 null 가드 없이 접근하는 코드 존재 여부

### 비즈니스 로직 / 금융 데이터 무결성
- [ ] `deposit`, `availableDeposit`, `unrealizedPnl`, `returnRate` 등 금액·수익률 필드를 UI에서 재계산하는 코드 없는지 (spec: 서버 값 우선)
- [ ] BigDecimal 문자열 → 숫자 변환 시 `parseFloat` 사용 (Number() 오버플로우 가능성 주의)
- [ ] `returnRate` 부호 판별 로직의 정확성 (parseFloat 후 > 0 / < 0 비교)
- [ ] currentPrice null 케이스 처리 누락 여부

### 아키텍처 / FSD 규칙
- [ ] features 레이어에서 직접 `fetch` 호출 없는지 (shared/api 경유 필수)
- [ ] pages → features → shared 방향 의존성만 존재하는지 (역방향 import 금지)
- [ ] `shared/utils/format.ts`가 도메인 의존성 없이 순수 유틸인지

### API 연동
- [ ] `fetchAccounts` URL이 `/api/v1/accounts`인지
- [ ] `fetchPositions` URL이 `/api/v1/accounts/{accountId}/positions`인지
- [ ] query key가 spec 명세와 일치하는지 (`["accounts", "list"]`, `["positions", accountId]`)
- [ ] `staleTime: 0` (positions), `staleTime: 30_000` (accounts) 적용 여부

### UX / 접근성
- [ ] 로딩 상태 표시 (accounts, positions 각각)
- [ ] 에러 상태 표시 (에러 메시지 사용자에게 노출)
- [ ] 빈 상태 표시 (포지션 없음 메시지)
- [ ] 테이블에 `<thead>` / `<th scope="col">` 시맨틱 마크업 사용 여부
- [ ] 버튼에 `aria-label` 또는 명시적 레이블 존재 여부

### 라우팅 및 사이드바
- [ ] `/account` 라우트가 App.tsx에 추가되었는지
- [ ] `getPageTitle` 케이스가 추가되었는지
- [ ] Sidebar NavLink가 올바른 순서로 추가되었는지

### 보안
- [ ] 시크릿·토큰 하드코딩 없는지
- [ ] API 에러 응답에서 민감정보 로그 출력 없는지

## Output Format

리뷰 결과를 항목별로 출력하고, MUST FIX 항목이 있으면 수정 방법도 함께 기술한다.
MUST FIX 항목이 0건이면 최종 판정 `APPROVED`. 1건 이상이면 `CHANGES REQUIRED`.

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
