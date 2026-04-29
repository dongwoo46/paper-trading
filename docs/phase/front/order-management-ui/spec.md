# Order Management UI

## Core Feature
주문 생성(종목 검색·수량·가격·방향·모드 선택) + 주문 내역 조회(상태 필터·취소 버튼)를 단일 페이지에서 운영.

## Considerations
- 주문 API는 계좌 범위(account-scoped): `/api/v1/accounts/{accountId}/orders` — 페이지 진입 시 accountId 선택이 선행되어야 함
- `PlaceOrderRequest`에 `idempotencyKey` 필수 → 프론트엔드에서 `crypto.randomUUID()` 로 자동 생성
- `OrderType.LIMIT` 선택 시 `limitPrice` 필수, `MARKET` 시 null — 조건부 필드 표시/검증 필요
- `OrderCondition.GTD` 선택 시 `expireAt` 필수 — 추가 날짜시각 입력 필요
- 거래 모드는 `TradingMode` enum: `LOCAL | KIS_PAPER | KIS_LIVE | UPBIT_LIVE` — ADR-005에서 필수 선택
- 모드는 계좌(`AccountResponse.tradingMode`)와 주문 요청(`PlaceOrderRequest`에는 `marketType`) 개념이 다름:
  - `marketType`: KOSPI/KOSDAQ/NASDAQ/NYSE/CRYPTO (시장 구분)
  - 계좌의 `tradingMode`: LOCAL/KIS_PAPER/KIS_LIVE (실행 모드) → **계좌 선택으로 tradingMode를 결정**
  - 주문 폼에는 `marketType` + `orderType` + `orderSide` + `orderCondition` 선택
- `OrderStatus`: PENDING / PARTIAL / FILLED / CANCELLED / REJECTED — PARTIAL은 부분 체결
- 취소는 `PENDING` 상태 주문만 가능 — UI에서 status !== PENDING이면 취소 버튼 비활성화
- 수량·가격은 BigDecimal 직렬화 → string으로 수신, 입력 시 string → number 변환 후 검증

## Trade-offs
- 계좌 선택 UI: 드롭다운 vs. 별도 계좌 페이지 → 드롭다운 선택. 이유: MVP 단계, 별도 계좌 페이지는 account-dashboard feature 범위
- 주문 목록 폴링 vs. WebSocket → 폴링(`refetchInterval: 5000`). 이유: WS 연결 인프라 없음, 5초 폴링으로 충분
- 종목 검색: 자유 입력 vs. 카탈로그 조회 → 자유 입력(ticker 직접 입력). 이유: collector-api 카탈로그는 구독 관리 전용, 주문은 ticker 알고 있음을 전제
- 상태 필터: 서버 측 필터링 vs. 클라이언트 측 → 클라이언트 측. 이유: `GET /orders`는 필터 파라미터 없음, 전체 조회 후 로컬 필터링

## Implementation Approach
- **pages/order**: `OrderPage` — 계좌 선택 + 탭(주문 생성 / 주문 내역)
- **features/order-form**: `OrderForm` — 주문 입력 폼 컴포넌트
- **features/order-list**: `OrderTable` — 주문 목록 + 필터 + 취소
- **entities/order**: TypeScript 타입 정의 + `orderApi` 함수
- **shared/api**: 기존 `fetchJson` 재사용

## Workflow
1. 사용자가 `/orders` 진입 → `GET /api/v1/accounts` 로 계좌 목록 조회 → 드롭다운에 표시
2. 계좌 선택 → `accountId` 상태 저장 → `GET /api/v1/accounts/{accountId}/orders` 주문 목록 조회
3. 주문 생성 탭: 폼 입력 → 유효성 검증 → `POST /api/v1/accounts/{accountId}/orders` → 성공 시 목록 invalidate
4. 주문 내역 탭: 상태 필터 버튼 클릭 → 클라이언트 필터링 → 테이블 렌더링
5. 취소 버튼 클릭 → 확인 dialog → `DELETE /api/v1/accounts/{accountId}/orders/{orderId}` → 목록 invalidate

## Component Tree

```
OrderPage (pages/order/ui/OrderPage.tsx)
├── AccountSelector (features/order-form/ui/AccountSelector.tsx)
│   └── <select> with AccountResponse[]
├── OrderFormPanel (features/order-form/ui/OrderFormPanel.tsx)
│   ├── TickerInput — 종목코드 직접 입력
│   ├── <select> marketType  — KOSPI | KOSDAQ | NASDAQ | NYSE | CRYPTO
│   ├── <select> orderType   — MARKET | LIMIT
│   ├── <select> orderSide   — BUY | SELL
│   ├── <select> orderCondition — DAY | GTC | IOC | FOK | GTD
│   ├── QuantityInput        — BigDecimal 검증
│   ├── LimitPriceInput      — orderType === LIMIT 시만 표시
│   ├── ExpireAtInput        — orderCondition === GTD 시만 표시
│   └── SubmitButton
└── OrderTablePanel (features/order-list/ui/OrderTablePanel.tsx)
    ├── StatusFilterBar       — PENDING | PARTIAL | FILLED | CANCELLED | REJECTED | ALL
    └── OrderTable (features/order-list/ui/OrderTable.tsx)
        └── OrderRow[]
            └── CancelButton  — status === PENDING 시만 활성화
```

## TypeScript Interfaces

```typescript
// entities/order/model/types.ts

type MarketType = 'KOSPI' | 'KOSDAQ' | 'NASDAQ' | 'NYSE' | 'CRYPTO';
type OrderType = 'MARKET' | 'LIMIT';
type OrderSide = 'BUY' | 'SELL';
type OrderCondition = 'DAY' | 'GTC' | 'IOC' | 'FOK' | 'GTD';
type OrderStatus = 'PENDING' | 'PARTIAL' | 'FILLED' | 'CANCELLED' | 'REJECTED';
type TradingMode = 'LOCAL' | 'KIS_PAPER' | 'KIS_LIVE' | 'UPBIT_LIVE';

interface PlaceOrderRequest {
  ticker: string;
  marketType: MarketType;
  orderType: OrderType;
  orderSide: OrderSide;
  orderCondition: OrderCondition;
  quantity: string;        // BigDecimal → string
  limitPrice: string | null;
  expireAt: string | null; // ISO 8601
  idempotencyKey: string;  // crypto.randomUUID()
}

interface OrderResponse {
  orderId: number;
  ticker: string;
  marketType: MarketType;
  orderType: OrderType;
  orderSide: OrderSide;
  orderCondition: OrderCondition;
  orderStatus: OrderStatus;
  quantity: string;          // BigDecimal → string
  filledQuantity: string;
  limitPrice: string | null;
  avgFilledPrice: string | null;
  fee: string;
  createdAt: string;         // ISO 8601
}

interface AccountResponse {
  id: number;
  accountName: string;
  tradingMode: TradingMode;
  deposit: string;
  availableDeposit: string;
  isActive: boolean;
}

// entities/order/api/orderApi.ts
// fetchPlaceOrder(accountId: number, req: PlaceOrderRequest): Promise<OrderResponse>
// fetchListOrders(accountId: number): Promise<OrderResponse[]>
// fetchCancelOrder(accountId: number, orderId: number): Promise<void>
// fetchListAccounts(): Promise<AccountResponse[]>
```

## API Mapping

| Component | Method | Endpoint | Key Fields |
|---|---|---|---|
| AccountSelector | GET | `/api/v1/accounts` | `id`, `accountName`, `tradingMode`, `isActive` |
| OrderFormPanel | POST | `/api/v1/accounts/{accountId}/orders` | `ticker`, `marketType`, `orderType`, `orderSide`, `orderCondition`, `quantity`, `limitPrice?`, `expireAt?`, `idempotencyKey` |
| OrderTablePanel | GET | `/api/v1/accounts/{accountId}/orders` | `orderId`, `ticker`, `orderStatus`, `orderSide`, `quantity`, `filledQuantity`, `limitPrice`, `avgFilledPrice`, `createdAt` |
| CancelButton | DELETE | `/api/v1/accounts/{accountId}/orders/{orderId}` | 204 No Content |

## Routing

`App.tsx`에 추가:
- Route path: `/orders`
- Page title: `주문 관리` (getPageTitle 스위치 케이스 추가)
- Sidebar NavLink: `ClipboardList` 아이콘, 레이블 `주문 관리`

## Order Status Flow

```
PENDING ──[체결 엔진]──► PARTIAL ──[완전 체결]──► FILLED
PENDING ──[취소 요청]──► CANCELLED
PENDING ──[리스크/검증 실패]──► REJECTED
PARTIAL ──[취소 요청]──► CANCELLED (부분 취소)
```

UI 반영:
- `PENDING`: 노란색 badge, 취소 버튼 활성화
- `PARTIAL`: 파란색 badge, 부분 체결 수량 표시, 취소 버튼 활성화
- `FILLED`: 초록색 badge, 취소 버튼 비활성화
- `CANCELLED`: 회색 badge, 취소 버튼 비활성화
- `REJECTED`: 빨간색 badge, 취소 버튼 비활성화
