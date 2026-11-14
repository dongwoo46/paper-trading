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
| Status | todo / in progress / done / blocked |
| Owner |  |

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

> Fill it by spelling out the §2 approach as the actual work (concretely, what was done).

-

---

## 6. Verification  (record)

| What | How it was checked | Result |
|---|---|---|
|  | test / screenshot / API contract / manual |  |

---

## 7. Result  (record)

- Done:
- Changed files / PR:

---

## 8. Problems found  (record)

> Found during the work — it can cause a new step, a change to an existing one, or a flag. Edit this step.md directly then (the reason goes in state.md §3).

| Problem | How it was handled |
|---|---|
|  | new step / change an existing step / open-questions flag / ignore |
