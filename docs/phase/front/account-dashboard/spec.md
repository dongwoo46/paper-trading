# Account·Position Dashboard

## Core Feature
계좌 목록(예수금·평가금액·수익률)과 계좌별 포지션 테이블(종목·수량·평균단가·현재가·평가손익)을 단일 페이지에서 조회하는 운영 대시보드.

## Considerations
- trading-api 기준 포지션은 accountId에 귀속됨 → 계좌 선택 → 해당 계좌 포지션 조회 2-step 흐름
- `currentPrice`와 `unrealizedPnl`, `returnRate`는 서버에서 계산하여 내려줌 — UI는 재계산 금지
- 숫자 포맷: 금액은 소수점 2자리 고정(KRW·USD), 수익률은 소수점 2자리 + % 표시, BigDecimal은 string으로 직렬화될 수 있음 → parseFloat 처리 필요
- `currentPrice`는 nullable (`BigDecimal?`) — Redis 시세 없는 경우 null로 내려옴, UI에서 "-" 표시
- `tradingMode` (LOCAL / KIS_PAPER / KIS_LIVE) 과 `accountType`을 배지로 시각화
- 로딩·에러·빈 상태 각각 별도 UI 처리 (ADR-003: Redis 장애 시 currentPrice null 가능)
- 데이터 fresh 유지: 포지션은 실시간성 중요 → `staleTime: 0`, 계좌는 `staleTime: 30_000`
- 기존 FSD 구조(pages → features → entities → shared) 준수, `shared/api`로 fetch 일원화

## Trade-offs
- 계좌 선택 UI: Tabs vs Select → Tabs 선택. 계좌 수가 적고 (운영 대시보드) 전환 맥락이 명확하여 탭이 직관적. 계좌가 많아지면 Select로 교체 고려.
- 포지션 자동 폴링 vs 수동 새로고침 → 수동 새로고침 버튼 + Refresh 아이콘. 폴링은 trading-api 부하 불필요. 사용자가 직접 갱신 의도를 가짐.
- 전체 수익률 집계(총 평가금액 합산) → 서버에서 계산하지 않으므로 포지션 배열 합산은 금지. 대신 `AccountResponse.deposit`(예수금) 기준 요약만 표시.

## Implementation Approach
- **entities layer**: `account/model/types.ts` — AccountResponse, PositionResponse 인터페이스 정의
- **shared/api layer**: `accountApi.ts`, `positionApi.ts` — fetchJson 래퍼 함수
- **features layer**:
  - `account-overview/ui/AccountCard.tsx` — 계좌 카드 1개
  - `position-table/ui/PositionTable.tsx` — 포지션 데이터 테이블
- **pages layer**: `pages/account/ui/AccountDashboardPage.tsx` — 조합 및 상태 관리
- **App.tsx**: `/account` 라우트 추가, Sidebar에 네비게이션 항목 추가

## Workflow
1. 페이지 마운트 → `GET /api/v1/accounts` 호출 → 계좌 목록 렌더
2. 첫 번째 계좌 자동 선택 (또는 탭 클릭으로 전환)
3. 선택된 계좌 ID로 `GET /api/v1/accounts/{accountId}/positions` 호출
4. 포지션 테이블 렌더 (currentPrice null이면 "-")
5. 새로고침 버튼 클릭 → positions 쿼리 invalidate

## Component Tree

```
AccountDashboardPage                         # pages/account/ui/
  ├── <section className="panel">
  │     ├── panel-header (제목 + lead 텍스트)
  │     ├── AccountListSection               # features/account-overview/ui/
  │     │     ├── [로딩 스피너 | 에러 메시지 | 빈 상태]
  │     │     └── AccountCard × N            # features/account-overview/ui/
  │     │           ├── accountName + tradingMode Badge + accountType Badge
  │     │           ├── 예수금 (deposit)
  │     │           ├── 가용 예수금 (availableDeposit)
  │     │           └── 활성/비활성 표시
  │     └── PositionSection                  # features/position-table/ui/
  │           ├── SectionCard (헤더: 계좌명 + 새로고침 버튼)
  │           ├── [로딩 스피너 | 에러 메시지 | 빈 상태]
  │           └── PositionTable              # features/position-table/ui/
  │                 └── <table>
  │                       ├── thead (종목·시장·수량·평균단가·현재가·평가금액·평가손익·수익률·가격소스)
  │                       └── tbody
  │                             └── PositionRow × N
```

## TypeScript Interfaces

```typescript
// entities/account/model/types.ts

type AccountType = "PAPER" | "LIVE" | "VIRTUAL";
type TradingMode = "LOCAL" | "KIS_PAPER" | "KIS_LIVE";

interface AccountResponse {
  id: number;
  accountName: string;
  accountType: AccountType;
  tradingMode: TradingMode;
  deposit: string;           // BigDecimal → string
  availableDeposit: string;
  lockedDeposit: string;
  baseCurrency: string;
  externalAccountId: string | null;
  isActive: boolean;
  createdAt: string | null;  // Instant → ISO string
  updatedAt: string | null;
}

type MarketType = "KOSPI" | "KOSDAQ" | "NYSE" | "NASDAQ" | string;
type PriceSource = "REDIS" | "NONE" | string;

interface PositionResponse {
  ticker: string;
  marketType: MarketType;
  quantity: string;
  orderableQuantity: string;
  lockedQuantity: string;
  avgBuyPrice: string;
  currentPrice: string | null;
  evaluationAmount: string | null;
  unrealizedPnl: string | null;
  returnRate: string | null;
  priceSource: PriceSource;
  priceUpdatedAt: string | null;
}
```

## API Mapping

| Component | Method | Endpoint | Key Fields Used |
|---|---|---|---|
| AccountListSection | GET | `/api/v1/accounts` | id, accountName, tradingMode, accountType, deposit, availableDeposit, isActive |
| AccountListSection (filter) | GET | `/api/v1/accounts?isActive=true` | 활성 계좌만 표시 옵션 |
| PositionSection | GET | `/api/v1/accounts/{accountId}/positions` | ticker, marketType, quantity, avgBuyPrice, currentPrice, evaluationAmount, unrealizedPnl, returnRate, priceSource |

Query Keys:
- `["accounts", "list"]` — 계좌 목록 (staleTime: 30_000)
- `["positions", accountId]` — 포지션 (staleTime: 0)

## Routing

App.tsx에 추가할 라우트:
- path: `/account`
- element: `<AccountDashboardPage />`
- title: `"계좌·포지션"` (getPageTitle 스위치 케이스 추가)

Sidebar.tsx에 추가할 NavLink:
- to: `/account`
- icon: `Wallet` (lucide-react)
- label: `계좌·포지션`
