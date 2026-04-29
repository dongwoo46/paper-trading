# 계좌·포지션 대시보드 (account-dashboard)

## Core Feature
계좌 목록(예수금·평가금액·수익률)과 계좌별 포지션 테이블(종목·수량·평균단가·현재가·평가손익)을 `/account` 단일 페이지에서 조회하는 운영 대시보드.

## Considerations
- trading-api 포지션은 accountId에 귀속 → 계좌 선택 → 포지션 조회 2-step 흐름
- `currentPrice`, `unrealizedPnl`, `returnRate`는 서버 계산값 — UI 재계산 금지
- BigDecimal → string 직렬화 → `parseFloat` 처리 필요
- `currentPrice`는 nullable (Redis 시세 없으면 null) → UI에서 `"-"` 표시
- 포지션은 실시간성 중요 → staleTime: 0, 계좌는 staleTime: 30_000
- FSD 레이어 규칙 준수: pages → features → entities → shared

## Trade-offs
- 계좌 선택 UI: Tabs vs Select → **Tabs** 선택. 운영 대시보드 특성상 계좌 수 적고 맥락 전환 직관적.
- 포지션 갱신: 자동 폴링 vs 수동 새로고침 → **수동 Refresh 버튼**. trading-api 부하 최소화.
- FSD 위반 수정(rework): `accountApi.ts`를 `shared/api/` → `entities/account/api/`로 이동. shared는 도메인 의존성 금지.

## Implementation Approach

| Layer | 파일 | 역할 |
|---|---|---|
| entities/account/model | `types.ts` | AccountResponse, PositionResponse, AccountType, TradingMode 등 |
| entities/account/api | `accountApi.ts` | fetchAccounts, fetchPositions (FSD 수정 후 위치) |
| shared/utils | `format.ts` | formatAmount, formatRate (순수 유틸, 도메인 의존 없음) |
| features/account-overview/ui | `AccountCard.tsx` | 계좌 카드 (예수금·배지·선택 상태) |
| features/position-table/ui | `PositionTable.tsx` | 포지션 데이터 테이블 (null 처리·색상) |
| pages/account/ui | `AccountDashboardPage.tsx` | 조합 + selectedAccountId 상태 관리 |
| App.tsx | 수정 | `/account` 라우트 + getPageTitle 케이스 추가 |
| Sidebar.tsx | 수정 | Wallet icon + `/account` NavLink 추가 |

## Key APIs

| Method | Path | 컴포넌트 |
|---|---|---|
| GET | `/api/v1/accounts` | AccountDashboardPage — 계좌 목록 |
| GET | `/api/v1/accounts/{accountId}/positions` | AccountDashboardPage — 포지션 목록 |

Query Keys:
- `["accounts", "list"]` (staleTime: 30_000)
- `["positions", accountId]` (staleTime: 0)

## Test Results
- 26/26 tests passed (4 test files)
- Build: 0 TypeScript errors

## Known Limitations / Future Work
- 전체 수익률 집계 미표시 (서버 계산 API 없음 → 포지션 배열 합산 금지)
- 실시간 포지션 갱신 미구현 (P1: realtime-execution phase 예정)

## Completed / PR
2026-04-29 / PR 생성 예정
