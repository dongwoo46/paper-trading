# Step 13 — 체결 입력 공통화

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
| Step | 13 |
| Title | 체결 입력 공통화 |
| Feature | multi-broker-kis-toss |
| Based on | development-plan.md §5 2단계 6번, §10.2 D27, §10.3 체결 서비스·KisExecutionNotice·archunit_store 행, RISK-11(PR #53 머지 486116c 뒤 main 합쳐 기준선 재기록) |
| Status | in progress |
| Owner |  |

---

**━━ Plan block (required before starting · from the planner / development-plan design) ━━**

## 1. Goal and scope  (plan)

> What this step (phase) **builds (what)** + **how far it goes (this step's boundary — name what is handed to the next step)**.

- Builds: application 이 infrastructure 타입 KisExecutionNotice 를 import 하지 않도록, 체결 통지 입력을 application 명령 BrokerExecutionNotice(모드 포함)로 바꾸고 KisExecutionNoticeService 를 BrokerExecutionNoticeService 로 이름을 바꾼다
- Scope (where this step stops): main: application/order/command/BrokerExecutionNotice.kt 신설, application/order/KisExecutionNoticeService.kt → BrokerExecutionNoticeService.kt, infrastructure/kis/KisExecutionNotice.kt(BrokerExecutionNotice 로 바꾸는 함수), KisExecutionWebSocketManager.kt(서비스 이름·입력 교체). test: KisExecutionNoticeServiceTest → BrokerExecutionNoticeServiceTest, KisExecutionWebSocketManagerTest 의 서비스 mock. archunit_store 해결된 동결 줄 제거(사람 승인 뒤). 범위 밖: ExecutionProcessor, KIS 주문 찾기 쿼리, 파서 동작, characterization/kis 시험, 통지 경계 예외 범위(TODO C3-01), 토스 체결(3단계)

---

## 2. Approach (how, in what order)  (plan)

> **By what method and in what order** this step (= phase) is solved — briefly (one line to a few). Based on the feature-spec / development-plan design.
> A rough level is enough (the detailed work log is §5). **Never leave it empty — the step's base is here.**

- 시험 먼저: BrokerExecutionNoticeServiceTest 를 새 명령 타입으로 옮기고, KisExecutionNotice → BrokerExecutionNotice 변환 시험(accountScope 포함)을 더해 컴파일 실패 확인
- BrokerExecutionNotice 신설, 서비스 이름 바꿈과 입력 교체, 웹소켓 관리자가 변환 뒤 호출
- ArchUnit 동결 파일에서 해결된 KisExecutionNoticeService 줄 확인, 삭제는 사람 승인 뒤
- characterization/kis 무수정 통과와 전체 시험 확인

---

## 3. Dependencies  (plan)

- Previous step: step 14 커밋 a49e74a, PR #57 머지 52af70f
- Prerequisites: PR #53 머지 486116c(브랜치 feature/multi-broker-kis-toss-step13 = origin/main 486116c 기준), Docker(Testcontainers). 기준선: 486116c 와 백엔드 같은 트리(9fdbd37, 뒤 커밋은 문서뿐)에서 전체 92 xml 1216건 실패 0(2026-11-22, RISK-11 재기록)

---

## 4. Completion criteria  (plan)

- [ ] characterization/kis 10건 고치지 않고 통과(--rerun, 실행 뒤 생긴 xml)
- [ ] application 패키지에서 infrastructure.kis.KisExecutionNotice import 0
- [ ] BrokerExecutionNoticeServiceTest·변환 시험 통과
- [ ] 전체 시험 새 실패 0(기준선 재기록 값 기준)
- [ ] compileKotlin·compileTestKotlin·ktlintCheck·detekt·verifyLintBaselinesNotGrown exit 0

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
