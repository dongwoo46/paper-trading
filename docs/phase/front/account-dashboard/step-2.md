# Step 2 — Implementation (fullstack-dev)

## Agent
fullstack-dev

## Working Directory
`.worktrees/front-account-dashboard`

## Files to Read First
1. `CLAUDE.md` (루트)
2. `docs/ADR.md`
3. `docs/phase/front/account-dashboard/spec.md`
4. `frontend/trading-web/src/shared/api/index.ts` — fetchJson 패턴 확인
5. `frontend/trading-web/src/shared/ui/index.tsx` — 재사용 가능한 공유 컴포넌트 확인
6. `frontend/trading-web/src/App.tsx` — 라우팅 패턴 및 getPageTitle 확인
7. `frontend/trading-web/src/shared/ui/Sidebar.tsx` — NavLink 추가 위치 확인
8. `frontend/trading-web/src/pages/realtime/ui/RealtimePage.tsx` — 페이지 구조 패턴 참조

## Tasks

### 1. entities 레이어 — 타입 정의
파일: `frontend/trading-web/src/entities/account/model/types.ts` (신규)

`AccountResponse`, `PositionResponse`, `AccountType`, `TradingMode`, `MarketType`, `PriceSource` 인터페이스를 spec.md의 TypeScript Interfaces 섹션 기준으로 정의한다.
- BigDecimal 필드는 모두 `string` 타입
- nullable 필드는 `string | null`

### 2. shared/api 레이어 — API 함수
파일: `frontend/trading-web/src/shared/api/accountApi.ts` (신규)

- `fetchAccounts(): Promise<AccountResponse[]>` — `GET /api/v1/accounts`
- `fetchPositions(accountId: number): Promise<PositionResponse[]>` — `GET /api/v1/accounts/{accountId}/positions`

`fetchJson`을 import하여 사용. 직접 fetch 호출 금지.

### 3. features/account-overview 레이어

파일: `frontend/trading-web/src/features/account-overview/ui/AccountCard.tsx` (신규)

Props 시그니처:
```
AccountCard({ account: AccountResponse, isSelected: boolean, onClick: () => void }): JSX.Element
```

표시 항목:
- accountName (제목)
- tradingMode 배지 (LOCAL / KIS_PAPER / KIS_LIVE — 각각 다른 색상)
- accountType 배지
- deposit, availableDeposit (formatAmount 유틸로 포맷)
- isActive 여부 표시 (비활성 계좌는 dimmed 처리)
- isSelected 시 선택 강조 스타일 적용

### 4. features/position-table 레이어

파일: `frontend/trading-web/src/features/position-table/ui/PositionTable.tsx` (신규)

Props 시그니처:
```
PositionTable({ positions: PositionResponse[] }): JSX.Element
```

테이블 컬럼 순서: 종목(ticker) | 시장(marketType) | 수량(quantity) | 평균단가(avgBuyPrice) | 현재가(currentPrice) | 평가금액(evaluationAmount) | 평가손익(unrealizedPnl) | 수익률(returnRate) | 가격소스(priceSource)

- `currentPrice`, `evaluationAmount`, `unrealizedPnl`, `returnRate`가 null이면 `"-"` 표시
- `returnRate`: 양수는 green(`var(--color-profit, #10b981)`), 음수는 red(`var(--color-loss, #ef4444)`), null은 neutral
- `unrealizedPnl`: 양수 앞에 `+` 접두사 표시
- 빈 배열이면 빈 상태 메시지("포지션 없음") 행 표시

### 5. pages/account 레이어

파일: `frontend/trading-web/src/pages/account/ui/AccountDashboardPage.tsx` (신규)

- `useQuery(["accounts", "list"], fetchAccounts, { staleTime: 30_000 })` 로 계좌 목록 조회
- `selectedAccountId` state로 선택된 계좌 관리 (초기값: 첫 계좌 id, 없으면 null)
- `useQuery(["positions", selectedAccountId], () => fetchPositions(selectedAccountId!), { enabled: selectedAccountId != null, staleTime: 0 })` 로 포지션 조회
- `useQueryClient`로 포지션 쿼리 invalidate (새로고침 버튼)
- 로딩/에러/빈 상태 각각 처리 (계좌, 포지션 모두)
- `<section className="panel">` 구조 사용

구조:
```
<section className="panel">
  <div className="panel-header">
    <h2>계좌·포지션</h2>
    <p className="lead">...</p>
  </div>
  <AccountListSection>   // 계좌 카드 목록
  <PositionSection>      // 포지션 테이블 (selectedAccountId 기반)
</section>
```

### 6. 라우팅 추가

파일: `frontend/trading-web/src/App.tsx` (수정)

- `import { AccountDashboardPage }` 추가
- `<Route path="/account" element={<AccountDashboardPage />} />` 추가
- `getPageTitle` 스위치에 `case "/account": return "계좌·포지션";` 추가

파일: `frontend/trading-web/src/shared/ui/Sidebar.tsx` (수정)

- `Wallet` icon import (lucide-react)
- `/account` NavLink 추가 (기존 `Home` NavLink 아래, `Zap` 위)

### 7. 포맷 유틸리티

파일: `frontend/trading-web/src/shared/utils/format.ts` (신규)

- `formatAmount(value: string | null, currency?: string): string` — null이면 "-", 아니면 `parseFloat(value).toLocaleString('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })` + 통화 접미사
- `formatRate(value: string | null): string` — null이면 "-", 아니면 소수점 2자리 + "%"

## Acceptance Criteria

빌드 성공:
```
cd .worktrees/front-account-dashboard/frontend/trading-web && npm run build
```

- 빌드 오류 0건
- TypeScript `any` 타입 사용 금지
- `shared/api/accountApi.ts` 에서만 API 호출 (feature에서 직접 fetch 금지)
- null 값 처리: currentPrice null 시 "-" 렌더 확인

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
