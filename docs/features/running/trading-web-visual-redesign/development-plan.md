# Development Plan — trading-web visual redesign

> devkit v0.47.0 · Tech Planner 산출물. 기반: `feature-spec.md` · 다음: `state.md` + `steps/step-N.md`

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 기능 | trading-web visual redesign |
| 기반 feature-spec | `docs/features/running/trading-web-visual-redesign/feature-spec.md` (confirmed) |
| 작성자 | Codex |
| 날짜 | 2026-08-15 |
| tier | T1 · 반자동 |
| 대상 repo / 브랜치 | `frontend/trading-web` / `feature/trading-web-visual-redesign` |
| 기준 commit | `5125d03` |
| 상태 | in-review |

## 1. 구현 가능성 판단 (착수 전 점검)

| 항목 | 값 |
|---|---|
| feature-spec 준비됨? | ready |
| 막힌 ASK (open-questions) | none |
| **판정** | 🟢 다 가능 |
| 코멘트 | 기존 11개 route, shadcn 프리미티브, 알림 store와 반응형 셸을 그대로 활용한다. 신규 API·패키지·도메인 계산은 필요 없다. |

| 부분 | 가능? | 필요한 선행 작업 |
|---|---|---|
| 그룹 내비게이션·작업 중심 홈 | 🟢 가능 | 기존 route를 공용 내비게이션 메타데이터로 정리 |
| 단일 PageHeader | 🟢 가능 | `shared/ui` 합성 컴포넌트 추가 후 11개 page 적용 |
| 셸 내부 route 오류 복구 | 🟢 가능 | 기존 React Router route의 `errorElement` 사용 |
| 반응형·접근성·reduced motion | 🟢 가능 | 기존 Tailwind·의미 토큰·CSS media query 활용 |
| 회귀·시각 검증 | 🟢 가능 | Vitest/Testing Library와 로컬 Vite 렌더 사용 |

## 2. 아키텍처·통합 결정

| 결정 | 선택 | 근거 | 기각한 대안 |
|---|---|---|---|
| 내비게이션 SSoT | `shared/model/navigation.ts`에 4개 그룹·11개 목적지 메타데이터 정의 | Sidebar와 Home의 경로·라벨 drift 방지, 기존 FSD 경계 재사용 | Sidebar/Home에 각각 배열 복제 — 변경 누락 위험 |
| 제목 계층 | `shared/ui/PageHeader`가 화면별 유일한 `h1`; TopBar는 유틸리티 전용 | RULE-03과 키보드/스크린리더 계층을 동시에 충족 | TopBar 제목 유지 — 페이지 제목과 경쟁 |
| route 오류 | 콘텐츠 route 영역을 `RouteErrorBoundary`로 감싸고 함수형 `RouteErrorPage`를 fallback으로 사용 | 선언형 `<Routes>`는 `errorElement`를 소비하지 않으므로 lazy/render 오류를 실제로 포착하는 React 경계가 필요 | data router 전환 — route 계약 대비 과도; 동작하지 않는 `errorElement` 속성 — 회귀 테스트에서 기각 |
| 홈 정보 구조 | 내비게이션 SSoT에서 실제 route만 렌더 | 가짜 KPI 없이 모든 업무 목적지 제공 | 서버 상태 요약 카드 — 신규 API/가짜 상태 위험 |
| 시각 체계 | 기존 light semantic token과 shadcn Card/Button/Tabs 유지, 전역 hover 이동 축소 | UI_GUIDE 계약과 reduced-motion 요구 충족 | 다크 테마·glassmorphism — 명시적 non-goal |

## 3. 바꿀 파일·모듈

| 경로 / 모듈 | 변경 종류 | 목적 | 위험 |
|---|---|---|---|
| `src/shared/model/navigation.ts` | 신설 | 그룹·route·label·description·icon SSoT | 낮음 |
| `src/shared/ui/PageHeader.tsx` | 신설 | 공통 제목·설명·행동 계층 | 낮음 |
| `src/shared/ui/RouteErrorPage.tsx` | 신설 | 셸 내부 오류 안내·재시도 | 중간 |
| `src/App.tsx` | 수정 | layout route·오류 경계·콘텐츠 폭 정리 | 중간 |
| `src/shared/ui/Sidebar.tsx`, `TopBar.tsx` | 수정 | 그룹 내비게이션·유틸리티 TopBar | 중간 |
| `src/pages/*/ui/*Page.tsx` 11개 | 수정 | 공통 PageHeader 및 작업 중심 홈 | 중간 |
| `src/shared/ui/index.tsx`, `src/app/styles/index.css` | 수정 | 표면 경쟁·불필요 motion 축소 | 낮음 |
| 관련 `*.test.tsx`, 디자인 시스템 정적 테스트 | 신설/수정 | 요구사항·회귀 결정적 검증 | 낮음 |

삭제 파일은 없다.

## 4. 데이터·API 작업

| 항목 | 필요한 변경 | 소유 | 막는가(blocking)? |
|---|---|---|---|
| 기존 page API | 없음 | frontend/backend | 아니오 |
| route 계약 | 11개 path와 unknown redirect 유지 | frontend | 아니오 |
| 알림 store/SSE | 동작 변경 없음 | frontend | 아니오 |

## 5. 구현 순서 개요

1. 내비게이션 SSoT·PageHeader·route 오류 UI의 행동 테스트와 공용 구현
2. App shell·Sidebar·TopBar 정보 구조 정리
3. Home과 10개 하위 화면의 단일 PageHeader 적용
4. 반응형·focus·reduced-motion 및 정적 계약 보강
5. 전체 회귀·빌드·desktop/mobile 시각 검증과 리뷰 수정

## 6. 테스트 계획

| 테스트 | 종류 | 명령 / 방법 | 기대 결과 |
|---|---|---|---|
| navigation·Sidebar·Home·PageHeader·TopBar | component/integration | `npm test -- --run` | 4개 그룹·11개 링크, 유일 h1, 모바일 닫기, `9+`, 가짜 상태 부재 |
| route 오류·unknown URL | integration | MemoryRouter/Routes 렌더 | 셸 유지 오류 UI·재시도, `/` replace redirect |
| 디자인 시스템 계약 | static unit | `app-design-system.test.ts` | 원시 컨트롤·하드코딩 팔레트·legacy surface 없음 |
| 타입·lint·bundle | deterministic | `npm run lint`, `npm run build` | exit 0 |
| desktop/mobile | visual | 로컬 Vite + 브라우저 스크린샷 | 그룹 내비게이션, 제목 계층, overflow/focus 이상 없음 |

## 7. 도메인 제약 반영 (어떻게 지킬 것인가)

| 차원 | 적용 | 구현·검증 방법 | review routing |
|---|---|---|---|
| 수치 무결성 | n | API·금액·수익률 계산 파일 diff 부재 확인 | 수치 변경 시 중단 |
| 단위·기준치 | n | 기존 페이지 값 표시 로직 유지 | 단위 변경 시 중단 |
| 결측·LOD/LOQ | n | 기존 화면 상태 분기 유지 | 정책 변경 시 중단 |
| 규제·표현 | n | 내부 paper-trading UI 카피만 정리 | 외부 표현 변경 시 검토 |
| PHI | n | 관련 데이터 없음 | PHI 신호 발생 시 중단 |

## 8. 리스크·막힘

| ID | 리스크 / 막힘 | impact | 완화 / 필요한 결정 |
|---|---|---|---|
| RISK-01 | route 오류 경계가 lazy chunk render 오류를 놓침 | 중간 | 실제 throw 컴포넌트 통합 테스트로 오류 경계 fallback 렌더를 증명 |
| RISK-02 | 11개 page의 기존 행동/테스트가 헤더 변경으로 회귀 | 중간 | 페이지별 heading 정적 검색 + 전체 Vitest 실행 |
| RISK-03 | 긴 모바일 메뉴·콘텐츠 overflow | 중간 | 390px 렌더와 메뉴 이동 자동 닫기 테스트 |
| RISK-04 | CSS motion 축소가 toast/spinner 피드백을 제거 | 낮음 | reduced-motion에서 비필수 이동만 제거하고 상태 피드백은 즉시 전환 |

막힌 항목은 없다.

## 9. 구현자에게 넘기기 (Handoff)

**이것만 만들어라 (승인된 범위):**

- 확정된 라이트 워크스테이션 정보 구조, 공통 PageHeader, 작업 중심 홈, 셸 내부 route 오류 복구
- 기존 11개 route와 모바일 메뉴·알림 동작을 보존하는 반응형/접근성 개선
- 요구사항을 증명하는 component/integration/static/visual 검증

**이건 하지 마라 (범위 밖):**

- API·도메인 계산·인증·route path·차트 엔진·테마 체계 변경
- 가짜 KPI·가짜 운영 상태·신규 데이터 의존성 추가

**이때는 멈추고 물어봐라:**

- 기존 route/API/금융 계산을 바꿔야만 완료 가능한 충돌이 발견될 때
- 인증·권한·PII·외부 발행·데이터 손실 경계가 새로 생길 때
