# Step 1 — 공용 정보 구조와 테스트 골조

> **구현 단계 하나.** state.md의 한 step에 대응한다. 실제 파일은 `steps/step-1.md`, `steps/step-2.md` … 로 복사해 쓴다.
> 작업 중 문제를 만나면 8번에 적고, 새 step·변경이 필요하면 state.md 변경 이력으로 올린다.
>
> **기능 하나를 보통 5~7개 phase(step)로 쪼갠다**(규모 따라 가감). step은 무에서 상상하지 않고 **planner·development-plan §5 설계에서 도출**한다. → 착수 시 [`step-scaffold`](../../../../../tools/step-scaffold.js)로 **state.md §2에 5~7 phase 로드맵을 적고 step-1.md만 생성**, 이후 **step-1 끝나면 step-2.md 생성**…식으로 *순차로* 만든다(로드맵은 미리 / 파일은 하나씩).
> **두 블록으로 나눠 채운다:**
> - **① 계획 = 착수 전 필수:** `0근거 · 1목표·범위 · 2접근(어떻게·순서) · 3의존 · 4완료기준`. planner·development-plan 설계에서 도출(상상 아님). **텅 비우지 않는다 — step은 "무엇을·어디까지·어떻게"의 기초 기반.**
> - **② 기록 = 개발하면서 (순차):** `5작업기록 · 6검증 · 7결과 · 8발견한문제`는 비워두고 step-1→step-2 순차로 채운다. 미리 쓰면 drift. **문제·변경이 생기면 이 step.md를 직접 수정**(사유는 state.md §3 변경이력).
>
> state와 관계 = **인덱스(state, 얇음) vs 상세(step, 두꺼움).** 안 겹친다.

---

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| step | 1 |
| 제목 | 공용 정보 구조와 테스트 골조 |
| 기능 | trading-web-visual-redesign |
| 근거 | development-plan.md §2, §5 |
| 상태 | 완료 |
| 담당 | Codex |

---

**━━ ① 계획 (착수 전 필수 · planner·development-plan 설계 기반) ━━**

## 1. 목표 · 범위  ① 골조

> 이 step(phase)이 **만드는 기능(무엇을)** + **어디까지(이 step 경계 — 다음 step으로 넘기는 건 명시)**.

- 만드는 기능: 4개 업무 그룹·11개 route SSoT와 공통 PageHeader, route 오류 UI를 테스트 우선으로 구현한다.
- 이 step 범위(어디까지): src/shared/model/navigation.ts, src/shared/ui/PageHeader.tsx, src/shared/ui/RouteErrorPage.tsx 및 단위 테스트

---

## 2. 접근 (어떻게 · 순서)  ① 골조

> 이 step(=phase)을 **어떤 방법으로, 어떤 순서로** 풀지 — 간략하게(한두 줄~몇 줄). feature-spec/development-plan 설계 기반.
> 거친 수준이면 된다(상세 작업 기록은 §5). **비워두지 않는다 — step의 기초 기반이 여기다.**

- 실패하는 행동 테스트로 그룹·링크·h1·재시도 계약을 먼저 고정한다.
- 기존 shared/model·shared/ui 경계와 shadcn 프리미티브를 재사용해 최소 구현한다.

---

## 3. 의존  ① 골조

- 앞 step:
- 선행 조건: confirmed feature-spec과 development-plan

---

## 4. 완료 기준  ① 골조

- [x] 공용 테스트가 통과한다.
- [x] 11개 route path와 label이 한 SSoT에 있다.
- [x] 공통 헤더와 오류 UI가 접근 가능한 이름을 제공한다.

---

**━━ ② 기록 (개발하면서 순차로 채움 · 미리 X) ━━**

## 5. 작업 기록  ② 하면서

> §2 접근을 실제 작업으로 풀어쓰며 채운다(구체적으로 무엇을 했나).

- `navigation.test.ts`로 4개 업무 그룹·11개 route·고유 label/description 계약을 작성했다.
- `PageHeader.test.tsx`로 유일한 h1·설명·page action 계약을 작성했다.
- `RouteErrorPage.test.tsx`로 접근 가능한 오류 안내·재시도 계약을 작성했다.
- 최신 `origin/main`을 fast-forward해 기준 commit을 `dafe3d7`로 맞췄다(웹 변경 없음).
- 내비게이션 SSoT, `PageHeader`, `RouteErrorPage`와 `RouteErrorBoundary`를 구현했다.
- 선언형 `<Routes>`의 `errorElement`가 오류를 포착하지 못한 Red 결과에 따라 실제 React 오류 경계로 계획을 교정했다.

---

## 6. 검증  ② 하면서

| 무엇 | 어떻게 확인 | 결과 |
|---|---|---|
| 공용 컴포넌트 Red | 대상 Vitest 3개 | exit 1 — 구현 모듈 3개 부재 확인 |
| 공용 컴포넌트 Green | `npm test -- --run src/shared/model/navigation.test.ts src/shared/ui/PageHeader.test.tsx src/shared/ui/RouteErrorPage.test.tsx` | exit 0 — 3 files, 6 tests passed |

---

## 7. 결과  ② 하면서

- 한 일: 실패 테스트 골조, 4그룹·11 route SSoT, 공통 h1 헤더, route render 오류 fallback 구현
- 변경 파일 / PR: `navigation.ts`, `navigation.test.ts`, `PageHeader.tsx`, `PageHeader.test.tsx`, `RouteErrorPage.tsx`, `RouteErrorPage.test.tsx`

---

## 8. 발견한 문제  ② 하면서

> 작업 중 발견 — 새 step·기존 변경·flag를 유발할 수 있다. 그때 이 step.md를 직접 수정(사유는 state.md §3).

| 문제 | 어떻게 처리 |
|---|---|
| devkit 평가 DAG가 일반 기능에 과발화 | devkit 제안 PR #580으로 분리; 제품 구현 범위에는 영향 없음 |
| 선언형 `<Routes>`의 `errorElement`는 render 오류를 포착하지 않음 | 실제 실패 결과를 근거로 React 오류 경계로 변경하고 development-plan 결정 갱신 |
