# Step 4 — 10개 하위 화면 PageHeader 통합

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| step | 4 |
| 제목 | 10개 하위 화면 PageHeader 통합 |
| 기능 | trading-web-visual-redesign |
| 근거 | feature-spec RULE-03, development-plan §2 |
| 상태 | 완료 |
| 담당 | Codex |

## 1. 목표 · 범위

- 홈을 제외한 10개 화면의 주 제목·설명·페이지 행동을 공통 PageHeader로 통합한다.
- TopBar 제목 제거와 합쳐 화면마다 h1이 정확히 한 곳에만 있도록 한다.

## 2. 접근

- 모든 page source가 PageHeader를 사용한다는 정적 계약 테스트를 먼저 추가한다.
- 기존 페이지 행동과 하위 section heading은 유지하고 중복 주 헤더만 교체한다.

## 3. 의존

- 앞 step: step-3 완료
- 선행 조건: 공통 PageHeader

## 4. 완료 기준

- [x] 11개 route page 전부 PageHeader를 사용한다.
- [x] 페이지 주 제목은 PageHeader의 h1 한 개다.
- [x] 차트 분석의 새로고침 action이 헤더에 유지된다.

## 5. 작업 기록

- 10개 하위 route 화면의 기존 주 헤더를 공통 `PageHeader`로 교체했다.
- 차트 분석의 새로고침 버튼은 `PageHeader.actions`로 옮겨 행동을 유지했다.
- 거래 일지의 고정 2열 그리드를 `lg` 이상 2열로 바꿔 작은 화면의 열 넘침을 제거했다.

## 6. 검증

| 무엇 | 어떻게 확인 | 결과 |
|---|---|---|
| PageHeader 정적 계약 Red | 대상 Vitest | exit 1 — 10/11 화면이 공통 PageHeader를 사용하지 않음 확인 |
| 영향 범위 Green | 대상 Vitest 11개 | exit 0 — 11 files, 39 tests passed |
| 타입·bundle 조기 검증 | `npm run build` | exit 0 — TypeScript·Vite build passed |

## 7. 결과

- 한 일: 홈 포함 11개 화면의 주 제목·설명·페이지 행동을 공통 헤더 계층으로 통일
- 변경 파일 / PR: `src/pages/*/ui/*Page.tsx`, `src/pages/page-header-contract.test.ts`

## 8. 발견한 문제

| 문제 | 어떻게 처리 |
|---|---|
| 없음 | - |
