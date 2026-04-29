# Step 2 — Implementation (fullstack-dev)

## Working Directory
`.worktrees/front-order-management-ui`

## Files to Read First
1. `CLAUDE.md` — 절대 원칙 확인
2. `frontend/trading-web/CLAUDE.md` — FSD 아키텍처 규칙, 코드 규칙
3. `docs/phase/front/order-management-ui/spec.md` — 컴포넌트 트리, TypeScript 인터페이스, API 매핑, 라우팅
4. `frontend/trading-web/src/App.tsx` — 라우팅 패턴 참조
5. `frontend/trading-web/src/shared/ui/Sidebar.tsx` — NavLink 추가 위치 확인
6. `frontend/trading-web/src/shared/api/index.ts` — fetchJson 재사용 패턴
7. `frontend/trading-web/src/shared/ui/index.tsx` — SectionCard, StatusBar 등 공유 컴포넌트 확인
8. `frontend/trading-web/src/features/kis-management/ui/KisPanel.tsx` — useQuery/useMutation 패턴 참조

## Build Prerequisite
`cd .worktrees/front-order-management-ui/frontend/trading-web && npm install` (node_modules 없을 경우)

## Tasks

### 1. 타입 정의 — entities/order/model/types.ts

파일 경로: `frontend/trading-web/src/entities/order/model/types.ts`

정의할 타입:
- `MarketType`, `OrderType`, `OrderSide`, `OrderCondition`, `OrderStatus`, `TradingMode` — string literal union
- `PlaceOrderRequest` interface — spec.md 참조
- `OrderResponse` interface — spec.md 참조
- `AccountResponse` interface — spec.md 참조 (OrderPage 범위 내 최소 필드만)

규칙:
- `any` 금지, BigDecimal 수신 필드는 `string` 타입
- `quantity`, `limitPrice`, `avgFilledPrice`, `fee` → `string`
- `limitPrice`, `avgFilledPrice`, `expireAt` → nullable (`string | null`)

### 2. API 함수 — entities/order/api/orderApi.ts

파일 경로: `frontend/trading-web/src/entities/order/api/orderApi.ts`

함수 시그니처:
- `fetchListAccounts(): Promise<AccountResponse[]>`
  - GET `/api/v1/accounts`
- `fetchPlaceOrder(accountId: number, req: PlaceOrderRequest): Promise<OrderResponse>`
  - POST `/api/v1/accounts/{accountId}/orders`
- `fetchListOrders(accountId: number): Promise<OrderResponse[]>`
  - GET `/api/v1/accounts/{accountId}/orders`
- `fetchCancelOrder(accountId: number, orderId: number): Promise<void>`
  - DELETE `/api/v1/accounts/{accountId}/orders/{orderId}` (204 No Content)

규칙:
- 모든 함수는 `fetchJson`을 사용 (`shared/api/index.ts`)
- 직접 `fetch` 호출 금지

### 3. OrderFormPanel — features/order-form/ui/OrderFormPanel.tsx

파일 경로: `frontend/trading-web/src/features/order-form/ui/OrderFormPanel.tsx`

Props 시그니처: `OrderFormPanelProps { accountId: number; onSuccess: () => void }`

처리 흐름:
- `useMutation` 으로 `fetchPlaceOrder` 호출
- 폼 상태: `ticker`, `marketType`, `orderType`, `orderSide`, `orderCondition`, `quantity`, `limitPrice`, `expireAt`
- `idempotencyKey`는 submit 시 `crypto.randomUUID()` 로 생성 (상태 저장 불필요)
- `orderType === 'LIMIT'` 인 경우만 `LimitPriceInput` 렌더링
- `orderCondition === 'GTD'` 인 경우만 `ExpireAtInput` 렌더링
- 클라이언트 유효성 검증:
  - `ticker`: 비어있으면 안 됨, 대문자 변환
  - `quantity`: 양수 숫자 문자열, `parseFloat > 0`
  - `limitPrice` (LIMIT 시): 양수 숫자 문자열
  - `expireAt` (GTD 시): 미래 시각
- 에러 상태: mutation.isError 시 에러 메시지 표시
- 성공 시: `onSuccess()` 호출 + 폼 리셋

### 4. OrderTable + OrderTablePanel — features/order-list/

파일 경로:
- `frontend/trading-web/src/features/order-list/ui/OrderTable.tsx`
- `frontend/trading-web/src/features/order-list/ui/OrderTablePanel.tsx`

`OrderTablePanel` Props: `{ accountId: number }`

처리 흐름:
- `useQuery` 로 `fetchListOrders(accountId)` — `refetchInterval: 5000`
- 상태 필터 버튼: ALL | PENDING | PARTIAL | FILLED | CANCELLED | REJECTED
- 필터링은 클라이언트 측 (`orders.filter(o => selectedStatus === 'ALL' || o.orderStatus === selectedStatus)`)
- 취소 `useMutation`: `fetchCancelOrder` → 성공 시 `invalidateQueries`
- 취소 버튼: `status === 'PENDING' || status === 'PARTIAL'` 시만 활성화
- 취소 전 `window.confirm()` 확인

`OrderTable` Props: `{ orders: OrderResponse[]; onCancel: (orderId: number) => void; isCancelling: boolean }`

컬럼 정의 (표시 순서):
1. 주문ID (`orderId`)
2. 종목 (`ticker`)
3. 시장 (`marketType`)
4. 구분 (`orderSide`) — BUY: 파란색, SELL: 빨간색
5. 유형 (`orderType`)
6. 조건 (`orderCondition`)
7. 수량 / 체결수량 (`quantity / filledQuantity`)
8. 지정가 (`limitPrice`) — null이면 "-"
9. 평균 체결가 (`avgFilledPrice`) — null이면 "-"
10. 수수료 (`fee`)
11. 상태 (`orderStatus`) — 컬러 badge
12. 등록일 (`createdAt`) — 한국 시간 `toLocaleString('ko-KR')`
13. 취소 버튼

Status badge 색상:
- PENDING: `var(--brand-warning)` 또는 노란색 계열
- PARTIAL: `var(--brand-primary)` 또는 파란색 계열
- FILLED: 초록색
- CANCELLED: 회색
- REJECTED: 빨간색

### 5. AccountSelector — features/order-form/ui/AccountSelector.tsx

Props: `{ value: number | null; onChange: (accountId: number) => void }`

처리 흐름:
- `useQuery` 로 `fetchListAccounts()` — `queryKey: ['accounts']`
- `<select>` 에 계좌 목록 렌더링: `{accountName} ({tradingMode})`
- 비활성 계좌 (`isActive: false`) 는 disabled option

### 6. OrderPage — pages/order/ui/OrderPage.tsx

파일 경로: `frontend/trading-web/src/pages/order/ui/OrderPage.tsx`

처리 흐름:
- `accountId: number | null` 상태 관리
- `activeTab: 'form' | 'list'` 상태 관리
- `accountId === null` 이면 `OrderTablePanel`, `OrderFormPanel` 렌더링 불가 — 안내 메시지 표시
- 탭 전환 시 리렌더링 — 폼 제출 성공 시 자동으로 `list` 탭으로 전환

레이아웃:
- `<section className="panel">` 최상위 래퍼
- 상단: AccountSelector + 탭 버튼 (주문 생성 | 주문 내역)
- 탭 콘텐츠 영역

### 7. 라우팅 및 사이드바 수정

수정 파일:
- `frontend/trading-web/src/App.tsx`
- `frontend/trading-web/src/shared/ui/Sidebar.tsx`

`App.tsx` 변경사항:
- `import { OrderPage }` 추가
- `<Route path="/orders" element={<OrderPage />} />` 추가
- `getPageTitle` 스위치에 `case '/orders': return '주문 관리'` 추가

`Sidebar.tsx` 변경사항:
- `ClipboardList` lucide 아이콘 import 추가
- `<NavLink to="/orders" ...>` + `<ClipboardList size={18} />` + `주문 관리` 추가
- 위치: `거시경제 지표` 링크 아래

## Acceptance Criteria
- `cd .worktrees/front-order-management-ui/frontend/trading-web && npm run build` 빌드 통과
- TypeScript 에러 없음 (`any` 사용 금지)
- FSD 의존 방향 위반 없음 (pages → features → entities → shared)
- `fetchJson` 외 직접 `fetch` 호출 없음

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
