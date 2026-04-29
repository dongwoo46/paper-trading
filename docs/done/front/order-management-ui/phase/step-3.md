# Step 3 — Testing (test-engineer)

## Working Directory
`.worktrees/front-order-management-ui`

## Files to Read First
1. `docs/phase/front/order-management-ui/spec.md`
2. `frontend/trading-web/src/entities/order/model/types.ts`
3. `frontend/trading-web/src/entities/order/api/orderApi.ts`
4. `frontend/trading-web/src/features/order-form/ui/OrderFormPanel.tsx`
5. `frontend/trading-web/src/features/order-list/ui/OrderTablePanel.tsx`
6. `frontend/trading-web/src/features/order-list/ui/OrderTable.tsx`
7. `frontend/trading-web/src/pages/order/ui/OrderPage.tsx`
8. `frontend/trading-web/package.json` — 테스트 도구 확인 (vitest 여부)

## Test Setup Check
- `package.json`에 `vitest` 또는 `@testing-library/react` 확인
- 없으면 `npm install -D vitest @testing-library/react @testing-library/user-event jsdom @testing-library/jest-dom` 설치 후 `vite.config.ts`에 `test: { environment: 'jsdom' }` 추가
- `vitest.setup.ts` 파일 생성 후 `import '@testing-library/jest-dom'`

## Tasks

### 1. 타입 단위 테스트 — entities/order/model/types.test.ts

파일 경로: `frontend/trading-web/src/entities/order/model/types.test.ts`

검증 항목:
- `OrderStatus` 리터럴 유니온이 5개 값(`PENDING | PARTIAL | FILLED | CANCELLED | REJECTED`) 모두 커버
- `PlaceOrderRequest`의 `limitPrice` 필드가 `string | null` 타입
- `PlaceOrderRequest`의 `expireAt` 필드가 `string | null` 타입
- `OrderResponse`의 `quantity`, `fee` 필드가 `string` 타입 (BigDecimal 직렬화)

방법: TypeScript 타입 단언(타입 에러가 발생해야 할 곳에서 컴파일 에러 유도) 또는 `satisfies` 연산자로 타입 체크

### 2. API 함수 테스트 — entities/order/api/orderApi.test.ts

파일 경로: `frontend/trading-web/src/entities/order/api/orderApi.test.ts`

테스트 케이스:
- `fetchListAccounts`: GET `/api/v1/accounts` 호출 검증, 응답 파싱 확인
- `fetchPlaceOrder`: POST `/api/v1/accounts/1/orders` 호출 검증, 요청 body에 `idempotencyKey` 포함 확인
- `fetchListOrders`: GET `/api/v1/accounts/1/orders` 호출 검증
- `fetchCancelOrder`: DELETE `/api/v1/accounts/1/orders/42` 호출 검증, void 반환 확인
- 에러 응답(4xx, 5xx): `fetchJson` 에러 throw 전파 확인

방법: `vi.stubGlobal('fetch', ...)` 또는 `msw` 핸들러로 mock

### 3. OrderFormPanel 컴포넌트 테스트 — features/order-form/ui/OrderFormPanel.test.tsx

파일 경로: `frontend/trading-web/src/features/order-form/ui/OrderFormPanel.test.tsx`

테스트 케이스:
- 초기 렌더링: 모든 필수 입력 필드 존재 확인
- MARKET 선택 시 `limitPrice` 입력 필드 미표시 확인
- LIMIT 선택 시 `limitPrice` 입력 필드 표시 확인
- DAY 선택 시 `expireAt` 입력 필드 미표시 확인
- GTD 선택 시 `expireAt` 입력 필드 표시 확인
- ticker 빈 값으로 submit → 에러 메시지 표시 (submit 차단)
- quantity 0 또는 음수로 submit → 에러 메시지 표시
- 유효한 MARKET 주문 submit → `fetchPlaceOrder` 호출 확인, `idempotencyKey` UUID 형식 포함 확인
- 유효한 LIMIT 주문 submit → `limitPrice` 포함하여 `fetchPlaceOrder` 호출 확인
- submit 성공 시 `onSuccess` 콜백 호출 확인
- submit 성공 후 폼 리셋 확인 (ticker 입력 비워짐)
- API 오류 시 에러 메시지 표시 확인

Mock: `entities/order/api/orderApi` 모듈 전체 vi.mock

### 4. OrderTable 컴포넌트 테스트 — features/order-list/ui/OrderTable.test.tsx

파일 경로: `frontend/trading-web/src/features/order-list/ui/OrderTable.test.tsx`

테스트 케이스:
- 주문 목록이 빈 배열일 때 빈 테이블 또는 안내 메시지 렌더링
- 주문 목록에 PENDING 주문 포함 시 취소 버튼 활성화 확인
- 주문 목록에 FILLED 주문 포함 시 취소 버튼 비활성화 확인
- 주문 목록에 CANCELLED 주문 포함 시 취소 버튼 비활성화 확인
- 취소 버튼 클릭 시 `window.confirm` 호출 확인
- `window.confirm` 반환 true 시 `onCancel` 콜백 호출, orderId 전달 확인
- `window.confirm` 반환 false 시 `onCancel` 미호출 확인
- `createdAt` 필드가 한국 시간(`ko-KR`) 형식으로 표시 확인
- BUY 주문 행에 파란색 계열 클래스 또는 스타일 적용 확인
- SELL 주문 행에 빨간색 계열 클래스 또는 스타일 적용 확인

### 5. OrderTablePanel 통합 테스트 — features/order-list/ui/OrderTablePanel.test.tsx

파일 경로: `frontend/trading-web/src/features/order-list/ui/OrderTablePanel.test.tsx`

테스트 케이스:
- `accountId` prop으로 `fetchListOrders` 호출 확인
- 로딩 상태 표시 확인
- 주문 목록 렌더링 후 상태 필터 버튼 클릭 → 필터링 결과 확인
  - PENDING 필터 선택 시 FILLED 주문 미표시
  - ALL 필터 선택 시 전체 주문 표시
- 취소 mutation 성공 시 query invalidation 발생 확인

Mock: `entities/order/api/orderApi` 모듈 vi.mock, `@tanstack/react-query` QueryClient wrapper 설정

## Acceptance Criteria
- `cd .worktrees/front-order-management-ui/frontend/trading-web && npm test -- --run` 모든 테스트 통과
- 커버리지 기준: 폼 유효성 검증 분기, 조건부 필드 표시 분기, 취소 confirm 분기 모두 커버
- TypeScript 에러 없음

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
