# 테스트 증거 — trading-web visual redesign

> 독립 Tester와 최종 검증자가 `feature-spec.md` §10 및 최신 working tree를 기준으로 실행했다. 실행하지 못한 시각 검증은 통과로 간주하지 않는다.

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 기능 | `trading-web-visual-redesign` |
| 날짜 | 2026-08-16 KST |
| 대상 | `feature/trading-web-visual-redesign`, `origin/main` 대비 staged·unstaged·untracked 변경 |
| 역할 | Codex 독립 Tester + 최종 검증자 |

## 1. 결정적 실행 기록

| 명령 / 검사 | exit code | 결과 |
|---|---:|---|
| `npm test -- --run --maxWorkers=1 --testTimeout=15000 --hookTimeout=15000` | 0 | PASS — Vitest 34 files, 152 tests passed, 0 failed |
| 반응형·라우팅 집중 4파일 verbose Vitest | 0 | PASS — 17 tests; React `act(...)` 경고 없음 |
| 기능 집중 10파일 Vitest | 0 | PASS — 38 tests |
| `npm run lint` | 0 | PASS — ESLint 오류 없음 |
| `npm run build` | 0 | PASS — TypeScript + Vite production build, 2,068 modules transformed |
| baseline/current `<Route path>` 정렬 diff | 0 | PASS — `/`, 기존 10개 하위 route, `*` wildcard 동일 |
| changed path API·entity·OpenAPI·Swagger 검사 | 0 | PASS — 계약 경로 변경 없음 |
| App/page API·data-flow 식별자 변경 검사 | 0 | PASS — fetch/query/mutation/subscription/`/api/` 변경 없음 |
| `verify-web` browser preflight | blocked | 브라우저 실행면 0개 — desktop/mobile screenshot 미실행 |

공통 비차단 로그: Vitest/build에서 Node `[DEP0205] module.register()` deprecation warning이 출력됐다.

## 2. 완료 기준 커버리지

| 완료 기준 | 검증 층 | 결과 |
|---|---|---|
| 4개 업무 그룹·기존 11개 목적지 | navigation/Sidebar unit·component + route diff | PASS |
| 11개 화면의 공통 PageHeader·유일한 h1 | parameterized source contract + component | PASS — 11/11 |
| 홈의 실제 작업 진입점·가짜 KPI 제거 | HomePage component | PASS — 하위 목적지 10개 |
| 모바일 drawer·알림 동작 보존 | Sidebar/TopBar component | PASS — inert, focus 진입·복귀, Escape, destination close, `9+` |
| 1024px 경계·mobile→desktop resize | App/Sidebar integration + CSS source contract | PASS — JS와 Tailwind `lg` 일치, main inert 해제 |
| unknown URL·lazy route 실패 회복 | App integration | PASS — `/` replace 및 오류 후 홈 이동 |
| route 오류 UI·reset | RouteErrorPage component/integration | PASS |
| 디자인 시스템·reduced motion·긴 메뉴 | static contract | PASS |
| API·금융 계산·표시 로직 무변경 | diff contract | PASS |
| 타입·lint·bundle | deterministic | PASS |
| desktop/mobile 시각 정합 | browser screenshot | **BLOCKED — 증거 없음** |

## 3. 엣지·실패 모드

| 엣지 | 결과 |
|---|---|
| 390px 닫힌 drawer가 focus tree에서 제외 | PASS — `inert`, `aria-hidden` |
| 열린 drawer에서 main 격리·Escape·opener focus 복귀 | PASS |
| 열린 mobile drawer를 1024px로 resize | PASS — main `inert` 제거 |
| 정확히 1024px에서 desktop keyboard 동작 | PASS — sidebar 노출, Tab 이탈, Escape 무시 |
| 알림 10개 이상 | PASS — badge `9+`, panel open, unread 0 |
| 미등록 route·lazy module failure | PASS — 셸 유지 및 홈 회복 |
| reduced motion | PASS at static layer; 실제 OS preference 렌더는 미실행 |

## 4. 잔여 리스크

- 결정적 제품 실패는 발견되지 않았다.
- 브라우저 도구 부재 때문에 overflow, focus ring 가시성, heading 간격, desktop/mobile 실제 레이아웃을 캡처하지 못했다.
- 실제 Tab/Shift+Tab의 mobile first↔last wrap, 기본 `window.location.reload()` 재시도, 실제 네트워크 chunk failure는 미실행이다.

## 5. 판정

- 코드·계약 QA: **PASS**
- 전체 QA: **VISUAL BLOCKED** — 스펙 §10의 browser screenshot 증거만 남았다.
