# Step 8 — 2단계 기준선과 특성화 시험

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
| Step | 8 |
| Title | 2단계 기준선과 특성화 시험 |
| Feature | multi-broker-kis-toss |
| Based on | development-plan.md §5 2단계 1번, §10 D21, DEC-05 A, RULE-10 |
| Status | todo / in progress / done / blocked |
| Owner |  |

---

**━━ Plan block (required before starting · from the planner / development-plan design) ━━**

## 1. Goal and scope  (plan)

> What this step (phase) **builds (what)** + **how far it goes (this step's boundary — name what is handed to the next step)**.

- Builds: 리팩터링 전 KIS 동작을 경계 특성화 시험으로 고정해 이후 step 9~14가 KIS 동작 불변을 증명할 비교 기준을 만든다
- Scope (where this step stops): 시험 코드와 step 문서만 추가한다. main 소스는 고치지 않는다. 경계 셋(D21 a·b·c)의 특성화 시험과 착수 시점 기준선 기록까지

---

## 2. Approach (how, in what order)  (plan)

> **By what method and in what order** this step (= phase) is solved — briefly (one line to a few). Based on the feature-spec / development-plan design.
> A rough level is enough (the detailed work log is §5). **Never leave it empty — the step's base is here.**

- 착수 시점 main 기준 trading-api 전체 시험을 --rerun 으로 돌려 건수·실패 목록과 KIS 관련 시험 클래스 목록을 이 문서 §6에 기록
- (a) /api/kis/account/balance 를 서비스 mock 없이 끝까지 부르는 통합 시험: KIS 응답은 MockRestServiceServer fixture, 토큰 관리자만 mock. 응답 JSON 전체와 KIS 호출 URL·tr_id·헤더를 단언
- (b) KIS_PAPER·KIS_LIVE 주문 접수 특성화 시험: KIS 요청 경로·tr_id·본문, externalOrderId 저장, 원장·잠금 행, Slack 문구 단언
- (c) KIS 체결 통지 1건이 만드는 체결·포지션·원장 행 단언
- 새 시험이 옛 코드에서 통과함을 확인하고, 이후 step 에서 이 시험은 고치지 않는다는 규칙을 §4에 적음

---

## 3. Dependencies  (plan)

- Previous step: step 7 코드 머지(PR #55, 263f047)
- Prerequisites: 설계 승인(tech-plan-approval.md, 2026-11-16)

---

## 4. Completion criteria  (plan)

- [x] 경계 셋 특성화 시험이 옛 코드에서 모두 통과
- [x] 기준선 전체 시험 건수와 실패 목록, KIS 시험 클래스 목록이 §6에 기록됨
- [x] main 소스 변경 0, 린트 exit 0

**고정 규칙 (D21, DEC-05 A):** `backend/trading-api/src/test/kotlin/com/papertrading/api/characterization/kis/` 아래 시험 파일 4개는 이후 step 9~14 에서 고치지 않는다. 리팩터링은 이 시험을 그대로 통과시켜야 하고, 빨갛게 되면 시험이 아니라 코드를 고친다. 시험을 바꿔야 한다고 판단되면 멈추고 사람 결정을 받는다(바꿀 때는 `// devkit:test-change <근거>` 필요). 응답 변경(예: D28 KIS 에러 응답)이 허용돼 있어도 이 시험이 단언하는 성공 응답·KIS 호출·행 값은 AE20 목록 없이 바꾸지 않는다.

---

**━━ Record block (filled in order while developing · never ahead) ━━**

## 5. Work log  (record)

> Fill it by spelling out the §2 approach as the actual work (concretely, what was done).

- 착수 시점 main(브랜치 feature/multi-broker-kis-toss-phase2, 새 시험 추가 전)에서 `./gradlew test --rerun --offline` 로 trading-api 전체 시험을 돌려 기준선을 만듦. 집계는 실행 시작 시각 뒤에 생긴 xml 82개만 셈(시작 전 xml 0개). Docker Desktop 기동 확인 뒤 실행
- 경계 (a) `KisAccountBalanceBoundaryCharacterizationTest` 3건: `/api/kis/account/balance` 를 서비스 mock 없이 호출. KIS 응답은 `MockRestServiceServer`, `KisTokenManager` 만 `@MockitoBean`. KIS 호출 URL(쿼리 전체)·`tr_id`·`authorization`·`appkey`·`appsecret`·`custtype`·`Content-Type` 와 응답 JSON 전체(글자 그대로, 금액 자릿수 포함)를 단언. 호출 시각이라 못 박을 수 없는 `asOf` 는 «UTC 오프셋 ISO 문자열이고 1분 안»만 단언
  - PAPER(VTTC8434R, 모의 주소, 로컬 포지션과 대사 3종 목록이 모두 채워지는 입력), LIVE(TTTC8434R, 실전 주소), KIS_PAPER 계좌를 `mode=LIVE` 로 조회(trId·주소는 계좌 모드가 아니라 `mode` 파라미터가 정함, 가정 2) 
- 경계 (b) `KisOrderIntakeBoundaryCharacterizationTest` 4건: HTTP `POST /api/v1/accounts/{id}/orders` 부터 끝까지. KIS_PAPER 지정가 매수(VTTC0802U), KIS_LIVE 지정가 매수(TTTC0802U), KIS_PAPER 시장가 매도(VTTC0801U, `ORD_DVSN=01`, `ORD_UNPR=0`), KIS_LIVE 지정가 매도(TTTC0801U). KIS 요청은 주소·헤더·본문 전체 문자열, 저장은 `externalOrderId`·주문 `lockedAmount`·계좌 예수금/주문가능/잠금·원장 행(BUY_LOCK 금액·잔액·참조·멱등키)·매도 포지션 잠금 수량, 밖으로 나가는 것은 `SlackNotificationRequestedEvent`(ORDER_CREATED, 문구 전체)와 collector 구독 호출. collector 구독 포트는 외부 호출 차단용으로만 mock
- 경계 (c) `KisExecutionNoticeBoundaryCharacterizationTest` 3건: 통지 원문 → `KisExecutionNoticeParser` → `KisExecutionNoticeService.handle`. 접수 직후 상태(주문 PENDING, 외부 주문번호, 매수 잠금 원장 또는 보유 전량 잠금)는 도메인 메서드로 DB 에 미리 둠. 수수료 정책(0.015%, 최소 100원, 거래세 0.18%)을 KIS_PAPER·KIS_LIVE 에 심음
  - KIS_PAPER 매수 전량 체결(체결·포지션·원장 BUY_LOCK/BUY_EXECUTE/FEE/BUY_UNLOCK 금액·잔액·멱등키, 계좌 3종 금액, 구독 해제, 체결 이벤트·Slack 문구)
  - KIS_PAPER 매수 부분 체결 + 같은 통지 재수신(최소 수수료 100 적용, 잠금 잔존, 재수신은 행 안 늘어남)
  - KIS_LIVE 매도 전량 체결(포지션 0, 예수금 불변, SELL_EXECUTE/FEE 원장, 정산 행 수수료·세금·손익)
- 공통 지원 `KisBoundaryEventCapture` 1개: 발행된 Slack 이벤트와 `ExecutionFilledEvent` 를 모아 단언(전송기·DB 알림 저장은 건드리지 않음)
- 린트 정리: 줄 길이 120·함수 매개변수 6 한도 때문에 매개변수 묶음 데이터 클래스로 줄이고, 스프링 생성자 주입 시험 클래스 2개에만 `@Suppress("LongParameterList")` 를 사유 주석과 함께 둠(baseline 파일은 건드리지 않음)

---

## 6. Verification  (record)

| What | How it was checked | Result |
|---|---|---|
| 기준선: trading-api 전체 시험(새 시험 추가 전) | `./gradlew test --rerun --offline`(backend/trading-api), 종료 코드 0, 실행 시작 시각 뒤에 생긴 `build/test-results/test/TEST-*.xml` 만 집계 | xml 82개(시작 전 xml 0개), 시험 1073건, 실패 0, 오류 0, 건너뜀 0 |
| 기준선 KIS 이름 시험 클래스 7개 | 같은 xml | `KisAccountQueryServiceTest` 4, `KisExecutionNoticeServiceTest` 3, `KisAccountBalanceAdapterTest` 5, `KisExecutionNoticeParserTest` 3, `KisExecutionWebSocketManagerTest` 6, `KisOrderRestClientTest` 1, `KisAccountBalanceControllerIntegrationTest` 2 (합 24건, 실패 0) |
| 기준선 KIS 모드·KIS 클라이언트를 쓰는 그 밖의 시험 클래스 10개 | 같은 xml | `OrderCommandServiceTest` 37, `ExecutionProcessorTest` 15, `OrderControllerIntegrationTest` 17(KisOrderRestClient mock), `BrokerAccountQueryServiceTest` 23, `BrokerAccountBalanceControllerIntegrationTest` 3, `AccountTest` 58, `PositionCommandServiceTest` 5, `TaxSummaryCommandServiceTest` 8, `TaxSummaryQueryServiceTest` 4, `RedisMarketQuoteAdapterTest` 2 (실패 0). 이 중 시그니처에 묶여 step 10~12 에서 옮겨질 후보는 DEC-05 A 목록(`KisAccountQueryServiceTest`, `KisAccountBalanceAdapterTest`, `OrderCommandServiceTest`)이고, 옮길 때 이 표를 근거로 목록을 남김 |
| 특성화 시험 3개 클래스 10건이 옛 코드에서 통과 | `./gradlew test --rerun --offline --tests 'com.papertrading.api.characterization.kis.*'`, 종료 코드 0, 시작 뒤 생긴 xml 3개만 집계. 린트 정리 뒤 한 번 더 같은 명령으로 재실행 | 첫 실행·재실행 모두 10건, 실패 0, 오류 0, 건너뜀 0 (잔고 3, 주문 접수 4, 체결 통지 3) |
| 특성화 시험이 주문 접수·체결 통지를 우연히 지나치지 않음 | 시험 안에서 `MockRestServiceServer.verify()`(기대한 KIS 호출이 실제로 일어났는지)와 응답 JSON 글자 그대로 비교, 행 값 `isEqualByComparingTo` | 통과(기대 호출이 안 일어나면 verify 가 깨짐). 코드 수정 뒤 «시험이 빨개지는지» 되돌림 실험은 main 무수정 범위라 하지 않음 |
| 린트 | `./gradlew compileKotlin compileTestKotlin ktlintCheck detekt verifyLintBaselinesNotGrown -PlintBaselineRef=origin/main --offline` (backend/trading-api) | 종료 코드 0. ktlint·detekt baseline 늘어난 항목 0건 |
| main 소스 변경 0 | `git status --short backend/trading-api/src/main` | 출력 0줄 |

---

## 7. Result  (record)

- Done: (완료 선언은 사람이 커밋 확인 뒤 기록)
- 결과 요약: 경계 셋 특성화 시험 10건이 옛 코드에서 통과하고 기준선(1073건 실패 0)이 §6에 기록됨. main 소스 변경 0, 린트 exit 0
- Changed files / PR: 커밋 전(메인이 확인 뒤 커밋)
  - `backend/trading-api/src/test/kotlin/com/papertrading/api/characterization/kis/KisAccountBalanceBoundaryCharacterizationTest.kt` (3건)
  - `backend/trading-api/src/test/kotlin/com/papertrading/api/characterization/kis/KisOrderIntakeBoundaryCharacterizationTest.kt` (4건)
  - `backend/trading-api/src/test/kotlin/com/papertrading/api/characterization/kis/KisExecutionNoticeBoundaryCharacterizationTest.kt` (3건)
  - `backend/trading-api/src/test/kotlin/com/papertrading/api/characterization/kis/KisBoundaryEventCapture.kt` (지원 클래스, 시험 아님)
  - `docs/features/running/multi-broker-kis-toss/steps/step-8.md`

---

## 8. Problems found  (record)

> Found during the work — it can cause a new step, a change to an existing one, or a flag. Edit this step.md directly then (the reason goes in state.md §3).

| Problem | How it was handled |
|---|---|
| 시작 때 Docker 데몬이 꺼져 있어 Testcontainers 시험을 못 돌림 | 멈추고 보고, 사용자가 Docker Desktop 을 켠 뒤(2026-11-16) 이어서 진행 |
| 주문 접수 fixture 의 주문번호 키가 `ORNO`. `KisOrderRestClient.placeOrder` 가 응답 `output.ORNO` 를 읽는데(`outputValue(response, "ORNO")`), KIS 공식 주문 응답은 `ODNO` 인 것으로 알고 있어 실제 응답과 키가 다를 수 있음(확신 못 함) | 시험은 지금 코드가 읽는 키(`ORNO`)로 고정. 실제 응답 키는 §10.6 선택 수동 확인(RISK-16)에서 사람이 확인하고, 다르면 별도 결함으로 올림. 이 step 에서는 고치지 않음 |
| 이 시험이 고정한 «지금 동작» 중 이후 step 에서 건드릴 때 AE20 목록이 필요한 것: (1) 옛 잔고 응답 `asOf` 는 UTC 오프셋 ISO 문자열인데 D22 는 어댑터 `asOf` 를 KST `LocalDateTime` 으로 바꿈. D23 이 성공 응답 불변이므로 서비스에서 UTC 오프셋으로 되돌려야 시험이 통과함 (2) 체결 수수료 FEE 원장 행은 예수금을 줄이지 않고 행만 남김 (3) 정산 행의 `realizedPnl` 은 체결가×수량(원가 차감 아님), 세금은 총매도대금×거래세율 | 모두 고정만 하고 고치지 않음(동작 불변이 2단계 원칙, DEC-08 A). 바꾸려면 시험을 고치는 게 아니라 사람 결정 + AE20 목록이 먼저 |
| 새 시험 클래스 3개가 각자 PostgreSQL·Redis 컨테이너와 스프링 컨텍스트를 띄워 특성화 시험만 돌려도 약 3분 걸림 | 기존 통합 시험과 같은 방식(클래스별 컨테이너)을 따랐음. 공용 컨테이너 통합은 별도 개선으로 두고 이 step 에서는 안 함 |
| detekt `LongParameterList` 를 스프링 생성자 주입 시험 클래스 2개에서 피할 수 없음 | 두 클래스에만 `@Suppress("LongParameterList")` + 사유 주석. baseline 은 늘리지 않음(`verifyLintBaselinesNotGrown` 통과) |
