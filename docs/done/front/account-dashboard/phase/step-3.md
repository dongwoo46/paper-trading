# Step 3 — Testing (test-engineer)

## Agent
test-engineer

## Working Directory
`.worktrees/front-account-dashboard`

## Files to Read First
1. `docs/phase/front/account-dashboard/spec.md`
2. `frontend/trading-web/src/entities/account/model/types.ts`
3. `frontend/trading-web/src/shared/api/accountApi.ts`
4. `frontend/trading-web/src/features/account-overview/ui/AccountCard.tsx`
5. `frontend/trading-web/src/features/position-table/ui/PositionTable.tsx`
6. `frontend/trading-web/src/pages/account/ui/AccountDashboardPage.tsx`
7. `frontend/trading-web/src/shared/utils/format.ts`
8. `frontend/trading-web/package.json` — 테스트 프레임워크 확인

## Tasks

### 테스트 파일 및 대상

#### 1. 유틸리티 테스트
파일: `frontend/trading-web/src/shared/utils/format.test.ts` (신규)

검증 케이스:
- `formatAmount(null)` → `"-"`
- `formatAmount("10000.5")` → 한국 로케일 포맷 문자열
- `formatAmount("0.00")` → `"0.00"` 형식
- `formatRate(null)` → `"-"`
- `formatRate("12.34")` → `"12.34%"`
- `formatRate("-5.67")` → `"-5.67%"`

#### 2. AccountCard 컴포넌트 테스트
파일: `frontend/trading-web/src/features/account-overview/ui/AccountCard.test.tsx` (신규)

검증 케이스:
- accountName이 렌더링됨
- tradingMode 배지가 올바른 레이블로 렌더링됨 (LOCAL / KIS_PAPER / KIS_LIVE)
- isActive=false인 경우 비활성 표시 요소 존재
- isSelected=true인 경우 선택 스타일 클래스 적용
- onClick 핸들러가 클릭 시 호출됨
- deposit, availableDeposit 값이 포맷되어 표시됨

#### 3. PositionTable 컴포넌트 테스트
파일: `frontend/trading-web/src/features/position-table/ui/PositionTable.test.tsx` (신규)

검증 케이스:
- positions 배열이 빈 경우 빈 상태 메시지("포지션 없음") 렌더
- ticker 컬럼이 올바르게 렌더링됨
- currentPrice가 null인 경우 해당 셀에 "-" 표시
- unrealizedPnl이 양수인 경우 "+" 접두사 표시
- unrealizedPnl이 null인 경우 "-" 표시
- returnRate 양수/음수에 따른 CSS 클래스 또는 color 스타일 차이 확인

#### 4. AccountDashboardPage 통합 테스트 (MSW 또는 vi.mock 사용)
파일: `frontend/trading-web/src/pages/account/ui/AccountDashboardPage.test.tsx` (신규)

`shared/api/accountApi`를 mock 처리하여 실제 HTTP 요청 없이 테스트.

검증 케이스:
- 계좌 목록 로딩 중 로딩 상태 UI 표시
- 계좌 목록 로드 완료 후 AccountCard 렌더
- 계좌 클릭 시 포지션 쿼리 실행 (fetchPositions 호출 여부)
- fetchAccounts가 빈 배열 반환 시 빈 상태 메시지 렌더
- fetchAccounts 에러 시 에러 메시지 렌더
- 새로고침 버튼 클릭 시 포지션 재조회 트리거

### 테스트 실행 명령

```
cd .worktrees/front-account-dashboard/frontend/trading-web && npm test -- --run
```

## Acceptance Criteria

- 모든 테스트 PASS
- `formatAmount(null)` → `"-"` 검증 통과
- `currentPrice null` 렌더 케이스 통과
- 빈 포지션 빈 상태 케이스 통과

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
