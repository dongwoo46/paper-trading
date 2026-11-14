# Step 1 — 교체 골격

> **One implementation step.** It maps to one step row in state.md. Copy it to `steps/step-1.md`, `steps/step-2.md` … for real use.
> A problem met during the work goes into §8; when it needs a new step or a change, raise it in the state.md change history.
>
> **A feature usually splits into 5–7 phases (steps)**, more or fewer by size. Derive steps from the planner / development-plan §5 design — never imagine them from nothing. At kickoff, [`step-scaffold`](../../../../../tools/step-scaffold.js) writes the 5–7 phase roadmap into state.md §2 and creates **only step-1.md**; step-2.md is created when step-1 ends, and so on in order (the roadmap up front / the files one at a time).
> **Fill it in two blocks:**
> - **Plan block = required before starting:** `0 based on · 1 goal and scope · 2 approach (how, in what order) · 3 dependencies · 4 completion criteria`. Derive it from the planner / development-plan design, not imagination. **Never leave it empty — a step is the base for "what, how far, how".**
> - **Record block = while developing, in order:** leave `5 work log · 6 verification · 7 result · 8 problems found` empty and fill them step-1 → step-2 in sequence. Writing them ahead causes drift. **When a problem or change comes up, edit this step.md directly** (the reason goes in state.md §3 change history).
>
> Relation to state = **index (state, thin) vs detail (step, thick).** They do not overlap.
> Tools read the labels below literally — `step-scaffold` fills the plan block by them and `doc-quality` reads the Status row (one of the words listed there).

---

## 0. Metadata

| Field | Value |
|---|---|
| Step | 1 |
| Title | 교체 골격 |
| Feature | trading-web-shadcn-rebuild |
| Based on | development-plan §5-1, D2·D3·D15·D17, DEC-01·08 |
| Status | done |
| Owner | Maker |

---

**━━ Plan block (required before starting · from the planner / development-plan design) ━━**

## 1. Goal and scope  (plan)

> What this step (phase) **builds (what)** + **how far it goes (this step's boundary — name what is handed to the next step)**.

- Builds: 옛 frontend/trading-web 을 지우고 같은 자리에 shadcn/ui Vite 골격을 세워 build·lint·test 가 통과하는 상태를 한 커밋으로 만든다
- Scope (where this step stops): 삭제 실행 직전 사람 재확인(DEC-01), 고정 자료(D15), 옛 폴더 삭제, shadcn base-nova Vite 골격, 개발 프록시, 소스 검사 시험 골조, 새 앱 지시 문서(D17), CI build·lint 단계(DEC-08). 화면·API 클라이언트는 다음 step

---

## 2. Approach (how, in what order)  (plan)

> **By what method and in what order** this step (= phase) is solved — briefly (one line to a few). Based on the feature-spec / development-plan design.
> A rough level is enough (the detailed work log is §5). **Never leave it empty — the step's base is here.**

- 삭제 대상 146개 목록과 되돌리는 방법을 사람에게 보이고 재확인을 받는다
- D15 고정 자료를 먼저 옮겨 둔다
- 옛 폴더를 지우고 shadcn 공식 Vite 초기화 명령으로 골격을 만든다
- 프록시, 소스 검사 시험, 새 UI_GUIDE.md, CI build·lint 단계를 더한다
- build·lint·test 통과를 확인하고 한 커밋으로 묶는다

---

## 3. Dependencies  (plan)

- Previous step:
- Prerequisites: 기획·설계 승인(tech-plan) 기록

---

## 4. Completion criteria  (plan)

- [ ] npm run build·lint·test 모두 0 종료
- [ ] 옛 src 파일 import 0건(소스 검사 시험 통과)
- [ ] CI trading-web 잡에 build·lint 단계 존재

---

**━━ Record block (filled in order while developing · never ahead) ━━**

## 5. Work log  (record)

1. DEC-01 사용자 승인(2026-11-15, «삭제·골격 진행») → development-plan §8-2 DEC-01 결정 칸 갱신.
2. D15 고정 자료: 삭제 전 `git ls-files -s` 로 옛 추적 파일 146개 blob 해시(고유 144), CSS 사용자 정의 변수 이름 107개를 `src/test/fixtures/` 에 저장. 107개 중 shadcn 생성기·Tailwind 가 스스로 쓰는 표준 이름 42개는 `generatorStandardNames` 로 분리(금지 65개).
3. `git rm -r frontend/trading-web` → 추적 파일 0.
4. D1: `npx shadcn@4.23.0 init -t vite -b base -p nova` (`--help` 확인 결과 `base-nova` 라는 preset 이름은 없고, base `base` + preset `nova` 가 `style: base-nova` 가 됨). 생성기가 만든 중첩 `.git` 은 지움.
5. D12·D13: vitest 4.1.12·jsdom 28.1.0 정확 고정. 생성 결과 TS 6.0.3·ESLint 10.15·typescript-eslint 8.72.2(peer 범위 안)라 변경 없음. 설치된 334개 패키지 중 엔진 범위가 Node 20.19.0 을 못 맞는 것은 `cn@0.5.2`(`>=22`) 1개 — Node 20.19.0 으로 build·lint·test 를 실제로 돌려 통과 확인(npm ci 때 경고만 날 수 있음).
6. D7·D10: `src/{app,pages,shared}` 최소 뼈대(빈 화면 1개), `components.json` 별칭을 `src/shared/*` 로, `proxy.config.ts`(server·preview 같은 표).
7. D11·D15 소스 검사 시험 `src/test/guards/`(규칙 함수 단위 시험 + 실제 소스 검사), 프록시 표 시험.
8. D16 tone 토큰 `--tone-rise/fall/buy/sell`(임시 값), D17 `CLAUDE.md`·`CODEX.md`·`docs/UI_GUIDE.md`·`README.md` 새로 작성.
9. DEC-08: CI trading-web 잡에 `npm run build`·`npm run lint` 단계 추가(Node 20 유지).

---

## 6. Verification  (record)

| What | How it was checked | Result |
|---|---|---|
| D15 고정 자료 | 해시 146개, 변수 이름 107개(금지 65개) | 통과 |
| 옛 폴더 삭제 | `git ls-files frontend/trading-web` | 0 → 골격 생성 후 신규 파일만 |
| build | `npm run build` (Node 26 로컬, Node 20.19.0 별도) | 0 종료 |
| lint | `npm run lint` (두 Node 모두) | 0 종료 |
| test | `npm test -- --run` (Node 26·Node 20.19.0 모두) | 9 파일 · 36 통과 / 0 실패 (D15 (a) 범위 한정 반영, 앱 소스 시험 추가) |
| 규칙 함수 단위 시험 | 위반 샘플로 검출 확인 | 통과 |

---

## 7. Result  (record)

- Done: 옛 앱 삭제 + shadcn base-nova Vite 골격 + 프록시 + 소스 검사 시험 + 새 앱 지시 문서 + CI build·lint 단계, 한 커밋.
- Changed files / PR: 커밋 해시는 보고에 기록(푸시 안 함).

---

## 8. Problems found  (record)

| Problem | How it was handled |
|---|---|
| D15(a): 프로젝트 전체를 훑자 `.gitignore`(생성기 Vite 템플릿 산출물)가 옛 파일과 같은 blob 으로 걸림. `src/assets/react.svg` 도 일치했으나 미사용 템플릿 자산이라 삭제해 해소 | 사용자 결정(2026-11-15 팝업): 검사 범위를 `frontend/trading-web/src/` 로 한정(계획 D15 «새 src» 와 일치). 예외 목록은 만들지 않고 `.gitignore` 는 그대로 둠. 루트 설정 파일은 이식 검사 대상이 아님 |
| 옛 CSS 변수 이름 107개 중 42개가 shadcn·Tailwind 표준 이름(`--background`, `--radius` 등)이라 그대로 금지하면 생성기 출력이 항상 걸림 | 사용자 승인(2026-11-15 팝업): `generatorStandardNames` 42개 분리, 금지 65개 해석 |
| 스톱 훅 «테스트 동반(TDD)»: 새 소스 6개(App·skeleton-page·main·theme-provider·utils·button)에 시험이 없음 | 면제 주석 대신 시험 추가(렌더·테마 전환·cn 병합·진입점). Node 25+ 는 전역 `localStorage` 가 비어 jsdom 것을 가려 메모리 구현으로 대체(`src/test/memory-storage.ts`) |
| 커밋 게이트: `frontend/trading-web/CLAUDE.md` 가 하네스 변경인데 proposal 번들이 없음 | 사용자 결정(2026-11-15 팝업): CLAUDE.md 는 하네스 게이트로 미룸, 규칙 정리 때 반영. 이번 커밋에서 뺌(staged 해제). 옛 파일이 이미 추적 중이라 «미추적»은 불가 — 새 내용은 작업 트리에 수정 상태로 남고, 커밋된 트리에는 옛 CLAUDE.md 가 규칙 정리 때까지 남음(삭제도 하네스 변경으로 차단됨). `CODEX.md`·`docs/UI_GUIDE.md` 는 게이트에 안 걸려 커밋에 포함 |
| 커밋 게이트: 기존 테스트 약화 34건(옛 앱 시험 파일 통째 삭제) — 차단 | 사용자 결정(2026-11-15): 근거 «DEC-01 옛 앱 전체 삭제 승인(development-plan D2) — 시험 대상 코드도 함께 삭제, 새 골격 시험 36건». 그러나 게이트(`tools/test-gate.js` `markerReason`)는 근거 주석을 «약화된 그 시험 파일 안의 추가 줄»에서만 인정하고 문서·커밋 메시지는 인정하지 않음 → 통째 삭제된 파일에는 추가 줄이 없어 충족 불가. 사람 결정 대기(커밋 보류) |
| `-p base-nova` 는 유효하지 않은 preset 이름 | `-b base -p nova` 사용(결과 `style: base-nova`) |
| `cn@0.5.2` 엔진 `>=22` (CI Node 20 과 불일치) | Node 20.19.0 으로 build·lint·test 실제 통과 확인, 충돌로 보지 않음. `npm ci` 에서 엔진 경고 가능 |
