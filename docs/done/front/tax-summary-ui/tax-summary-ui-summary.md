# Tax Summary UI

## Core Feature
`/tax-summary` 페이지에서 계좌/연도 기준 세금 요약 조회와 수동 재계산 요청을 수행하는 운영 UI를 구현했다.

## Considerations
- 기존 대시보드 패턴(계좌 선택, 명시적 새로고침, 상태 패널 구성)을 유지했다.
- 금액/세율은 서버 string 값을 그대로 표시하고 프런트에서 재계산하지 않도록 고정했다.
- 재계산 비동기 상태(RUNNING)와 오류 코드(400/404/409)에 맞는 사용자 피드백 UX를 반영했다.

## Trade-offs
- 자동 폴링 대신 수동 재조회 + 재계산 후 invalidate 방식을 선택해 API 부하를 낮추고 사용자 의도를 명확히 했다.
- 연도 입력을 자유 입력 대신 선택 기반으로 제한해 운영 입력 오류를 줄였다.

## Implementation Approach
- `entities/tax-summary`: API 클라이언트, 타입, 상태/표시 유틸 추가.
- `features/tax-summary`: 선택 패널, 요약 패널, 재계산 액션 패널 구성.
- `pages/tax-summary`: 선택 상태와 조회/재계산 오케스트레이션, 로딩/에러/빈 상태 처리.
- 라우팅/사이드바에 `tax-summary` 진입점을 추가하고 기존 주문 화면 테스트 영향 범위를 함께 보정했다.

## Workflow
1. `/tax-summary` 진입 후 계좌/연도 선택.
2. `GET /api/v1/accounts/{accountId}/tax-summaries/{taxYear}`로 요약 조회.
3. 상태/금액/세율/계산 시각 렌더링.
4. 재계산 클릭 시 `POST /api/v1/accounts/{accountId}/tax-summaries/{taxYear}/recalculate` 호출.
5. 성공 시 관련 쿼리 invalidate 후 최신 상태 재조회.

## Key APIs
- `GET /api/v1/accounts/{accountId}/tax-summaries/{taxYear}`
- `POST /api/v1/accounts/{accountId}/tax-summaries/{taxYear}/recalculate`

## Test / Build Result
- `npm run test -- tax-summary` PASS (3 files, 11 tests)
- `npm run build` PASS

## Residual Tasks
- PR 생성 후 `docs/TODO.md`의 `pr: #TBD`를 실제 PR 번호로 갱신.
- 후속 phase(`trading-journal-ui`) 착수 시 공통 패널 패턴 재사용 범위 점검.

## PR Draft
Title: `feat(front): tax-summary UI 추가 및 재계산 UX 반영`

Body:
- 변경 요약
  - `/tax-summary` 페이지/라우트/사이드바 메뉴 추가
  - tax-summary 엔티티(API/타입) 및 선택/요약/재계산 패널 구현
  - 코드리뷰 MUST FIX 반영: API 경로 정합, 400/404/409 오류 UX, 세율 string-safe 표시
  - 관련 테스트 보강 및 주문/사이드바 영향 테스트 보정
- 변경 파일
  - `frontend/trading-web/src/App.tsx`
  - `frontend/trading-web/src/shared/ui/Sidebar.tsx`
  - `frontend/trading-web/src/shared/ui/Sidebar.test.tsx`
  - `frontend/trading-web/src/shared/ui/index.tsx`
  - `frontend/trading-web/src/entities/tax-summary/**`
  - `frontend/trading-web/src/features/tax-summary/**`
  - `frontend/trading-web/src/pages/tax-summary/**`
  - `frontend/trading-web/src/features/order-list/ui/OrderTable.test.tsx`
  - `frontend/trading-web/src/features/order-list/ui/OrderTablePanel.test.tsx`
- 검증 명령/결과
  - `npm run test -- tax-summary` (PASS)
  - `npm run build` (PASS)
- 리뷰 포인트
  - API 계약(`accountId`, `taxYear`, 응답 필드명) 정합성
  - 금액/세율 string 기반 표시와 반올림/포맷 일관성
  - 재계산 RUNNING 상태 중복 클릭 방지와 오류 메시지 분기

## PR Checklist
- [x] 기능: 계좌/연도 조회 및 재계산 플로우 동작
- [x] 테스트: tax-summary 테스트 통과
- [x] 빌드: production build 통과
- [x] 문서: summary/TODO/state/index 갱신
- [x] 리스크: 잔여 리스크 및 후속 과제 기록

## Completed / PR
2026-05-04 / #TBD
