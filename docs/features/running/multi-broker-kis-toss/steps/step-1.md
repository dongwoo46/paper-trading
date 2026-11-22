# Step 1 — 기준선 기록

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
| Title | 기준선 기록 |
| Feature | multi-broker-kis-toss |
| Based on | development-plan §5-1, §6 기준선, RULE-10 |
| Status | todo / in progress / done / blocked |
| Owner |  |

---

**━━ Plan block (required before starting · from the planner / development-plan design) ━━**

## 1. Goal and scope  (plan)

> What this step (phase) **builds (what)** + **how far it goes (this step's boundary — name what is handed to the next step)**.

- Builds: 변경 전 trading-api 전체 테스트 결과와 실패 목록을 남겨 이후 회귀 판정의 기준으로 삼는다
- Scope (where this step stops): 테스트 실행과 기록만. 코드 변경 없음

---

## 2. Approach (how, in what order)  (plan)

> **By what method and in what order** this step (= phase) is solved — briefly (one line to a few). Based on the feature-spec / development-plan design.
> A rough level is enough (the detailed work log is §5). **Never leave it empty — the step's base is here.**

- Docker 기동 확인
- cd backend/trading-api && ./gradlew test 실행
- 통과 수, 실패 목록, 실행 커밋을 이 step 문서에 기록

---

## 3. Dependencies  (plan)

- Previous step: 없음
- Prerequisites: Docker 실행 중

---

## 4. Completion criteria  (plan)

- [ ] 기준선 테스트 결과와 실패 목록이 커밋 해시와 함께 기록됨

---

**━━ Record block (filled in order while developing · never ahead) ━━**

## 5. Work log  (record)

> Fill it by spelling out the §2 approach as the actual work (concretely, what was done).

- 2026-11-15: Docker 29.5.2 실행 확인
- 기준 커밋 `e2904c8`(코드 변경 전, 문서만 미커밋)에서 `cd backend/trading-api && ./gradlew test --continue` 실행
- 결과는 `build/test-results/test/*.xml` 57개를 집계해 기록

---

## 6. Verification  (record)

| What | How it was checked | Result |
|---|---|---|
| trading-api 전체 테스트 기준선 | `./gradlew test --continue`, JUnit XML 집계 | 57개 파일, 393개 테스트, 실패 0, 에러 0, 건너뜀 0. `BUILD SUCCESSFUL in 9m 29s` |

---

## 7. Result  (record)

- Done: 기준선 = 393개 전부 통과, 실패 목록 없음. 이후 단계는 «새 실패 0»을 이 수치와 비교한다(RULE-10)
- Changed files / PR: 코드 변경 없음. 이 문서만 갱신

---

## 8. Problems found  (record)

> Found during the work — it can cause a new step, a change to an existing one, or a flag. Edit this step.md directly then (the reason goes in state.md §3).

| Problem | How it was handled |
|---|---|
| 테스트 종료 후 로그에 `Connection to localhost:58021 refused`(Hikari 종료 시 DDL drop 시도) | ignore: 테스트가 끝나고 Testcontainers DB가 내려간 뒤의 종료 로그. 결과 XML에 실패 0 |
