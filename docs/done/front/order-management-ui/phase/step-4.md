# Step 4 — Code Review (code-reviewer)

## Working Directory
`.worktrees/front-order-management-ui`

## Files to Read First
1. `docs/phase/front/order-management-ui/spec.md`
2. `frontend/trading-web/src/entities/order/model/types.ts`
3. `frontend/trading-web/src/entities/order/api/orderApi.ts`
4. `frontend/trading-web/src/features/order-form/ui/OrderFormPanel.tsx`
5. `frontend/trading-web/src/features/order-form/ui/AccountSelector.tsx`
6. `frontend/trading-web/src/features/order-list/ui/OrderTablePanel.tsx`
7. `frontend/trading-web/src/features/order-list/ui/OrderTable.tsx`
8. `frontend/trading-web/src/pages/order/ui/OrderPage.tsx`
9. `frontend/trading-web/src/App.tsx`
10. `frontend/trading-web/src/shared/ui/Sidebar.tsx`
11. Step 3에서 생성된 테스트 파일 전체

## Review Checklist

각 항목에 대해 다음 마커 중 하나로 평가:
- 🟢 PASS — 기준 충족
- 🟡 WARNING — 개선 권장, 블로커 아님
- 🔴 MUST FIX — 머지 전 수정 필수

### 타입 안전성
- [ ] `any` 타입 사용 없음 (CLAUDE.md 절대 원칙)
- [ ] BigDecimal 수신 필드 (`quantity`, `fee`, `limitPrice`, `avgFilledPrice`) 모두 `string` 타입으로 처리
- [ ] `PlaceOrderRequest.quantity` string → 서버 전송 전 검증 (`parseFloat > 0`)
- [ ] `OrderResponse`, `AccountResponse` 응답 타입이 spec.md와 일치
- [ ] `unknown` + 타입 가드 패턴 (불명확 타입에 `any` 대신 사용 여부)

### 폼 유효성 검증
- [ ] `ticker` 빈 값 submit 차단
- [ ] `ticker` 자동 대문자 변환
- [ ] `quantity` 양수 검증 (`> 0`)
- [ ] `limitPrice` — `orderType === 'LIMIT'` 시 필수 검증, `MARKET` 시 null 전송
- [ ] `expireAt` — `orderCondition === 'GTD'` 시 필수 검증, 미래 시각 확인
- [ ] `idempotencyKey` — `crypto.randomUUID()` 로 생성, 상태에 저장하지 않음

### 조건부 필드 렌더링
- [ ] `limitPrice` 입력 필드: `orderType === 'MARKET'` 시 DOM에서 제거 또는 hidden, `LIMIT` 시만 표시
- [ ] `expireAt` 입력 필드: `orderCondition === 'GTD'` 시만 표시

### 취소 버튼 로직
- [ ] `PENDING` 상태만 취소 가능 (PARTIAL 포함 여부 spec과 일치 확인 — spec은 PENDING | PARTIAL 허용)
- [ ] `FILLED`, `CANCELLED`, `REJECTED` 상태에서 취소 버튼 비활성화
- [ ] 취소 전 `window.confirm()` 확인 절차 존재

### 거래 모드 (ADR-005 준수)
- [ ] 계좌 선택 드롭다운에서 `tradingMode` 표시 (`{accountName} ({tradingMode})`)
- [ ] `PlaceOrderRequest`에 `tradingMode` 필드 없음 (서버 API 계약: 계좌에 귀속, 요청에 불필요)
- [ ] 비활성 계좌 (`isActive: false`) 선택 불가 처리

### API 호출 규칙
- [ ] 직접 `fetch` 호출 없음, `fetchJson` 만 사용
- [ ] `fetchCancelOrder` 204 No Content 응답 처리 (빈 body 파싱 시도 없음)
- [ ] `fetchListOrders` `refetchInterval: 5000` 설정

### FSD 아키텍처 규칙
- [ ] `features`에서 `pages` 레이어 import 없음
- [ ] `entities`에서 `features` 또는 `pages` import 없음
- [ ] `shared`에서 상위 레이어 import 없음

### 에러·로딩·빈 상태
- [ ] 로딩 중 spinner 또는 상태 표시
- [ ] API 오류 시 사용자에게 에러 메시지 표시 (콘솔만 출력은 불충분)
- [ ] 주문 목록 비어있을 때 안내 메시지 표시
- [ ] `accountId === null` 상태에서 OrderFormPanel/OrderTablePanel 렌더링 차단

### 보안
- [ ] 시크릿·API 키 하드코딩 없음
- [ ] `idempotencyKey` 로그 출력 없음 (UUID이므로 민감하진 않지만 불필요한 로깅 없음)

### 라우팅
- [ ] `/orders` 라우트 `App.tsx`에 추가됨
- [ ] `Sidebar.tsx` NavLink 추가됨
- [ ] `getPageTitle('/orders')` 케이스 추가됨

## Output Format
각 체크리스트 항목에 대해 위 마커(🟢/🟡/🔴) 로 평가하고, 🔴 MUST FIX 항목은 수정 방법을 구체적으로 명시.

최종 판정:
- 🔴 MUST FIX 항목 0개: **APPROVED**
- 🔴 MUST FIX 항목 1개 이상: **REQUEST CHANGES** (목록 명시)

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
