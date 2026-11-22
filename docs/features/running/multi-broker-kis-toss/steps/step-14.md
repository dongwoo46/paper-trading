# Step 14 — 2단계 최종 확인

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
| Step | 14 |
| Title | 2단계 최종 확인 |
| Feature | multi-broker-kis-toss |
| Based on | development-plan.md §5 2단계 7번(최종 확인), §10 RULE-10 판정(DEC-05 A), RISK-11(PR #53), step-8 §4(특성화 시험 무수정), state.md §3 2026-11-22 순서 변경(step 13 은 PR #53 머지 뒤 별도 PR) |
| Status | done |
| Owner |  |

---

**━━ Plan block (required before starting · from the planner / development-plan design) ━━**

## 1. Goal and scope  (plan)

> What this step (phase) **builds (what)** + **how far it goes (this step's boundary — name what is handed to the next step)**.

- Builds: step 8~12 로 끝난 2단계 변경분(origin/main..HEAD)을 최종 확인한다: 빌드·전체 시험·린트, step 8 기준선과 특성화 시험 비교, 2단계 전체 독립 QA, 그 뒤 PR(feature/multi-broker-kis-toss-phase2 → main)
- Scope (where this step stops): 확인과 QA 막는 지적 고침만. step 13(D27 체결 입력 공통화)은 PR #53 이 열려 있어 이 PR 에서 빼고 머지 뒤 별도 PR. KIS 모의 계좌 두 잔고 경로 값 대조(선택, 사람)와 1단계 실호출 대조는 사람 준비 뒤라 이 step 의 완료 조건이 아님. trading-web·토스 실주문·KIS 토큰 이전 제외

---

## 2. Approach (how, in what order)  (plan)

> **By what method and in what order** this step (= phase) is solved — briefly (one line to a few). Based on the feature-spec / development-plan design.
> A rough level is enough (the detailed work log is §5). **Never leave it empty — the step's base is here.**

- 순서 변경을 state.md §3 에 기록(사람 결정 2026-11-22 «step 14 먼저»)
- 특성화 시험 무수정 증거: git diff 338a5ad..HEAD -- src/test/kotlin/com/papertrading/api/characterization/kis 가 빔
- 메인에서 빌드·린트, 특성화 시험 --rerun, 전체 시험 --rerun(실행 뒤 생긴 xml 만 집계, 직전 1180건 기준 새 실패 0)
- 2단계 전체 독립 QA(devkit:tester, origin/main..HEAD 변경분, 명세 §10 완료 기준·AE20 목록 대조)
- QA 막는 지적은 이 step 에서 고치고 후속은 findings 원장
- PR 생성(본문에 AE20 응답 변경 목록, step 13 별도 PR 예고)

---

## 3. Dependencies  (plan)

- Previous step: step 12 커밋 d78dc5f
- Prerequisites: Docker(Testcontainers), PR #53 은 열린 채 둠(그 PR 은 고치지 않음)

---

## 4. Completion criteria  (plan)

- [x] characterization/kis 10건 고치지 않고 통과(diff 빔, --rerun xml)
- [x] 전체 시험 새 실패 0(직전 1180건 기준)
- [x] compileKotlin·compileTestKotlin·ktlintCheck·detekt·verifyLintBaselinesNotGrown exit 0
- [x] 2단계 독립 QA 막는 지적 0
- [x] PR 생성(#57), step 13 은 PR #53 머지 뒤 별도로 남김

---

**━━ Record block (filled in order while developing · never ahead) ━━**

## 5. Work log  (record)

> Fill it by spelling out the §2 approach as the actual work (concretely, what was done).

- 순서 변경: PR #53 OPEN·CONFLICTING(2026-11-22 `gh pr view 53`), 사용자 결정 «step 14 먼저». state.md §3 기록, step 13 은 blocked. origin/main 새 커밋 0건이라 기준선 재기록 불필요
- 특성화 시험 무수정: `git diff --stat 338a5ad..HEAD -- .../characterization/kis` 빔
- 1차 최종 확인(고치기 전, HEAD `34ae484`): 린트 exit 0, 전체 시험 새 xml 89개 1180건 실패 0
- 2단계 전체 독립 QA(devkit:tester, origin/main..HEAD): 막는 지적 0, should-fix Q-01~Q-04, nit Q-05·Q-06
- 사용자 결정 «Q-01·02·03 고침»:
  - Q-01: 잔고 5xx 본문 `rt_cd 1` 서버 에러(EGW00002)가 `KIS_AUTH_FAILED` 로 가던 오분류. `KisFailureClassifier.transportFailure` 가 5xx 본문은 한도·인증만 확정하고 나머지 `KIS_UNAVAILABLE`(주문 경로 R1-02 와 같은 꼴). 시험 먼저: `KisAccountBalanceAdapterTest` 에 5xx EGW00002·5xx 권한 문구(Red 2건 확인)·4xx rt_cd 1 유지 시험 추가. detekt ReturnCount 로 `when` 하나로 정리
  - Q-02: 계획서 D28 잔고 5xx 문구 축소, §10.4 AE20 잔고·주문 행 보충(연결 거부·DNS → KIS_UNAVAILABLE, 한도 → KIS_RATE_LIMITED 새 code, 본문 붙은 4xx·5xx 분류, 주문 경로 KIS_TIMEOUT 없음), step-11 §7 Done 줄 정정
  - Q-03: `TossFloatingPointScanTest` 대상에 application/order/port, BrokerOrderSubmitter, KisOrderAdapter 추가(위반 0). 새 `KisOrderSecretLeakTest`: 실제 KisOrderRestClient·KisOrderAdapter·BrokerOrderSubmitter 로 본문 없는 500·읽기 타임아웃·200 주문 거부 세 경로의 로그에 토큰·app secret·계좌번호 전체 0건. mockk 자체 DEBUG 로그는 호출 인자를 찍어 대상에서 뺌, 접수기 오류 로그가 찍혔는지 먼저 단언
- 고침분 리뷰: 메인 인라인(사용자 결정 «메인이 직접»), `review/step14-code-review.md` 발견 0

---

## 6. Verification  (record)

| What | How it was checked | Result |
|---|---|---|
| 특성화 시험 무수정 | git diff 338a5ad..HEAD | 빔 |
| 1차 전체 시험 | `./gradlew test --rerun --offline`, 실행 뒤 생긴 xml 만 | 89 xml 1180건 실패 0 |
| Q-01 Red→Green | 집중 실행 | 새 시험 2건 실패 확인 뒤 통과 |
| 고친 뒤 집중 시험 | 잔고·주문 어댑터·접수기·핸들러·스캔·누출·특성화 | 12 xml 145건 실패 0 |
| 고친 뒤 전체 시험 | `./gradlew test --rerun --offline` | 90 xml 1186건 실패 0·건너뜀 0 |
| 린트 | compileKotlin compileTestKotlin ktlintCheck detekt verifyLintBaselinesNotGrown | exit 0 |
| 독립 QA | devkit:tester | 막는 지적 0 |

---

## 7. Result  (record)

- Done: 2단계 step 8~12 최종 확인 끝, 독립 QA 막는 지적 0, Q-01~03 고침(`a49e74a`)
- Changed files / PR: PR #57(feature/multi-broker-kis-toss-phase2 → main)

---

## 8. Problems found  (record)

> Found during the work — it can cause a new step, a change to an existing one, or a flag. Edit this step.md directly then (the reason goes in state.md §3).

| Problem | How it was handled |
|---|---|
| Q-04 주문 HTTP 200 + 게이트웨이 오류 문구(EGW00001·00002)가 400 KIS_ORDER_REJECTED 로 나가 재주문 위험 가능(KIS 가 200 으로 보내는지 미확인) | KIS 실호출 대조 항목으로 넘김(코드 미변경) |
| Q-05 자동 청산 접수 실패 Slack reason 이 KIS 원문에서 고정 문구로 바뀜(원문은 로그) | PR 본문에 기록, findings 원장 |
| Q-06 주문 경로 KIS_TEMPORARY_UNAVAILABLE 분기 도달 불가 | findings 원장 |
| PR #53 OPEN 으로 step 13 착수 불가 | step 13 blocked, 머지 뒤 별도 PR(state.md §3 2026-11-22) |
