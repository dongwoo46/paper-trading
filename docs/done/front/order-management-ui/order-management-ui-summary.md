# 주문 관리 UI (order-management-ui)

## Core Feature
신규 라우트 `/orders` — 계좌 선택 → 주문 생성(매수/매도) → 주문 내역 조회 및 취소를 단일 페이지에서 처리하는 운영 대시보드.

## Considerations
- trading-api 주문은 accountId에 귀속 → 계좌 선택이 폼과 목록 모두의 선행 조건
- 주문 모드(LOCAL / KIS_PAPER / KIS_LIVE)는 ADR-005 기준 폼에서 선택, 계좌 타입과 독립
- 가격·수량 필드는 BigDecimal → string 직렬화 → `parseFloat` 처리 필요
- 주문 취소는 204 No Content 응답 → 별도 타입 불필요, raw fetch 예외 허용
- 주문 목록은 서버 사이드 accountId 필터링 사용 (`GET /api/v1/orders?accountId=N`)
- FSD 레이어 규칙 준수: pages → features → entities → shared

## Trade-offs
- 계좌 선택: Tabs vs Select → **AccountSelector(Select)** 선택. 주문 폼과 목록 모두 accountId 종속이므로 상위 공유 Select가 더 적합. 계좌 수 무제한 가정.
- 주문 목록 필터링: 클라이언트 vs 서버 → **서버 사이드**(`?accountId=`) 선택. 주문 수 증가 대비 클라이언트 부하 최소화.
- 가격 입력: number input vs text → **text** 선택. BigDecimal string 처리 일관성 + 소수점 정밀도 보존.

## Implementation Approach

| Layer | 파일 | 역할 |
|---|---|---|
| entities/order/model | `types.ts` | OrderResponse, OrderRequest, OrderSide, OrderStatus, TradingMode 인터페이스 |
| entities/account/model | 기존 types.ts 재사용 | AccountResponse |
| shared/api | `orderApi.ts` | fetchOrders, createOrder, cancelOrder |
| shared/api | 기존 `accountApi.ts` 재사용 | fetchAccounts |
| features/order-form/ui | `AccountSelector.tsx` | 계좌 선택 드롭다운 |
| features/order-form/ui | `OrderFormPanel.tsx` | 주문 생성 폼 (종목/수량/가격/매수매도/모드) |
| features/order-list/ui | `OrderTable.tsx` | 주문 행 렌더링 |
| features/order-list/ui | `OrderTablePanel.tsx` | 상태 필터 + 취소 버튼 + 테이블 조합 |
| pages/order/ui | `OrderPage.tsx` | 전체 조합 및 selectedAccountId 상태 관리 |
| App.tsx | 수정 | `/orders` 라우트 추가, getPageTitle 케이스 추가 |
| Sidebar.tsx | 수정 | ShoppingCart icon + `/orders` NavLink 추가 |

## Key APIs

| Method | Path | 컴포넌트 |
|---|---|---|
| GET | `/api/v1/accounts` | AccountSelector — 계좌 목록 |
| GET | `/api/v1/orders?accountId={id}` | OrderTablePanel — 주문 내역 |
| POST | `/api/v1/orders` | OrderFormPanel — 주문 생성 |
| DELETE | `/api/v1/orders/{id}` | OrderTablePanel — 주문 취소 |

Query Keys:
- `["accounts", "list"]` (staleTime: 30_000)
- `["orders", accountId]` (staleTime: 0)

## Test Results
- 29/29 tests passed (5 test files)
- Build: 0 TypeScript errors

## Known Limitations / Future Work
- 종목 검색: 현재 text input 직접 입력 → 자동완성(ticker search API) 미구현
- 시장가/지정가 주문 타입 구분 UI 미구현 (현재 가격 필드 단순 입력)
- 주문 체결 실시간 알림 미구현 (P1: realtime-execution phase에서 처리 예정)

## Completed / PR
2026-04-29 / PR 생성 예정
