# Step 10 — 브로커 선택과 KIS 잔고 이전

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
| Step | 10 |
| Title | 브로커 선택과 KIS 잔고 이전 |
| Feature | multi-broker-kis-toss |
| Based on | development-plan.md §5 2단계 3번, §10.2 D22·D23·D24·D25, §10.4 AE20 1~2행, §10.6 잔고 모드 표·선택 검사·KIS 어댑터 모드·옛 경로 mode 시험, DEC-05 A(시그니처에 묶인 KIS 단위 시험은 같은 커밋에서 새 포트 기준으로 옮기고 목록을 이 문서에 남김), 가정 2·3·4 |
| Status | done |
| Owner |  |

---

**━━ Plan block (required before starting · from the planner / development-plan design) ━━**

## 1. Goal and scope  (plan)

> What this step (phase) **builds (what)** + **how far it goes (this step's boundary — name what is handed to the next step)**.

- Builds: KIS 잔고 어댑터를 공통 BrokerAccountBalancePort 구현으로 옮기고, 포트마다 supportedModes 를 둔 모드→구현 표와 기동 때 검사로 브로커를 고른다. broker-balance 가 KIS_PAPER·KIS_LIVE 를 지원하고, /api/kis/account/balance 는 성공 응답 계약 그대로 공통 포트를 거친다
- Scope (where this step stops): main: BrokerAccountBalancePort(supportedModes), TossAccountBalanceAdapter(supportedModes=TOSS_LIVE), KisAccountBalanceAdapter(공통 포트 구현, ref.tradingMode 전수 when 으로 trId·REST 모드, 그 밖 모드는 범주 K 내부 오류이고 KIS 호출 0, 계좌 로드 제거, KRW 한 줄, cashBuyingPower null, asOf KST LocalDateTime), BrokerAccountQueryService(List<포트> 주입, 생성 때 표 검사, KIS_PAPER·KIS_LIVE → KIS 어댑터, externalAccountId 가 XXXXXXXX-XX 아니면 KIS_ACCOUNT_NOT_LINKED, source=KIS, UPBIT_LIVE 501, LOCAL 404), KisAccountQueryService(계좌를 읽고 mode 파라미터로 KIS_LIVE/KIS_PAPER ref 를 만듦, L35 의 !! 제거, 대사 그대로), KisAccountBalancePort·KisBalanceSnapshot·KisBalancePosition 삭제. test: KisAccountQueryServiceTest·KisAccountBalanceAdapterTest·BrokerAccountQueryServiceTest 새 포트 기준, 선택 검사 시험 신설. 범위 밖: 주문 포트(step 11), AccountResponse.source(step 12), 체결(step 13), KIS 0 채움 수정(가정 3), 대사의 broker-balance 추가(가정 4), characterization/kis 시험

---

## 2. Approach (how, in what order)  (plan)

> **By what method and in what order** this step (= phase) is solved — briefly (one line to a few). Based on the feature-spec / development-plan design.
> A rough level is enough (the detailed work log is §5). **Never leave it empty — the step's base is here.**

- 시험 먼저: BrokerAccountQueryServiceTest 를 TradingMode.entries 전부로(TOSS_LIVE → 토스, KIS 둘 → KIS 어댑터와 ref 모드 일치, UPBIT_LIVE 501, LOCAL 404, KIS 미연결 409), 선택 검사(구현 누락, 한 모드에 구현 둘, 정책이 어댑터로 안 보내는 모드를 구현이 지원 → 생성 때 예외, 메시지에 모드 이름), KIS 어댑터 모드(KIS_PAPER paper 주소·VTTC8434R, KIS_LIVE live 주소·TTTC8434R, 그 밖 내부 오류·호출 0회), 옛 경로 mode(KIS_PAPER 계좌를 mode=LIVE 로 → live 호출, 가정 2) → 실패 확인
- 포트에 supportedModes, 두 어댑터 구현. 서비스는 생성자에서 표를 만들고 전수 when 이 어댑터로 보내는 모드 집합과 표 키 집합 일치·모드마다 구현 1개를 검사(@Qualifier 금지, ArchUnit 5ffdb0db)
- KIS 어댑터를 BrokerAccountBalancePort 로: 계좌 로드·requireNotNull 제거(계좌 연결 검사는 서비스로, step 9 의 KIS_ACCOUNT_NOT_LINKED 재사용), 응답 해석·0 채움·수익률 계산은 그대로
- 옛 경로 KisAccountQueryService: 계좌를 읽어 ref 를 만들고 공통 스냅샷을 KisAccountBalanceResult 로 되돌림. asOf 는 KST LocalDateTime 을 UTC OffsetDateTime 으로 바꿔 지금 표기 유지(특성화 시험 assertAsOfIsRecentUtc). 금액은 JSON 숫자 그대로(FLAG-10 문자열은 새 API만)
- 옛 포트·스냅샷 타입 삭제, 옮긴 시험 목록을 §5 에 기록, ArchUnit 해결 줄 있으면 제거
- trading-web src 에 broker-balance·/api/kis/account/balance 호출부 재검색(읽기 전용) 결과 기록

---

## 3. Dependencies  (plan)

- Previous step: step 9 KIS 에러 정렬 커밋
- Prerequisites: Docker(Testcontainers)

---

## 4. Completion criteria  (plan)

- [ ] characterization/kis 10건 고치지 않고 통과(--rerun, 실행 뒤 생긴 xml)
- [ ] §10.6 잔고 모드 표·선택 검사·KIS 어댑터 모드·옛 경로 mode 시험 통과
- [ ] 전체 시험 새 실패 0(직전 1095건 기준)
- [ ] compileKotlin·compileTestKotlin·ktlintCheck·detekt·verifyLintBaselinesNotGrown exit 0
- [ ] KisAccountBalancePort·KisBalanceSnapshot·KisBalancePosition 사용처 0, 옮긴 시험 목록 기록
- [ ] trading-web 호출부 재점검 결과 기록

---

**━━ Record block (filled in order while developing · never ahead) ━━**

## 5. Work log  (record)

> Fill it by spelling out the §2 approach as the actual work (concretely, what was done).

1. 착수 재검색: trading-web `src` 에서 `broker-balance|api/kis/account` 0건(exit 1).
2. 시험 먼저(Red): 시험 5개를 새 포트 기준으로 바꾸거나 더한 뒤 `./gradlew compileTestKotlin --offline` 이 `supportedModes` 미해결·생성자 불일치로 실패함을 확인.
3. 포트: `BrokerAccountBalancePort` 에 `supportedModes: Set<TradingMode>` 추가, 토스 어댑터는 `{TOSS_LIVE}`.
4. 모드 → 구현 표: 새 `BrokerBalancePortTable`(application/account, 빈 아님)이 `List<포트>` 로 표를 만들고 한 모드에 구현 둘이면 생성 때 `IllegalStateException`(메시지에 모드 이름). `BrokerAccountQueryService` 는 `List<포트>` 주입, 전수 `when`(`routeOf`, else 없음)의 «어댑터로 보내는 모드 집합»과 표 키 집합을 `init` 에서 맞춰 보아 구현 누락·지원 초과면 기동 실패. 매개변수 이름·`@Qualifier` 의존 제거. KIS_PAPER·KIS_LIVE → KIS 어댑터(`source=KIS`, ref 모드 = 계좌 모드), UPBIT_LIVE 501, LOCAL 404 그대로.
5. 계좌 연결 검사: 새 `linkedKisAccountNumber`(application/account 내부 함수)가 null·형식 틀림(`XXXXXXXX-XX` 아님)을 `KisAccountNotLinkedException`(step 9) 으로, 값은 그대로 돌려줌. broker-balance 와 옛 경로가 같이 씀.
6. KIS 어댑터: `BrokerAccountBalancePort` 구현. 계좌 로드·`AccountRepository`·`requireNotNull` 제거. `ref.tradingMode` 전수 `when` 으로 KIS_PAPER → paper·`VTTC8434R`, KIS_LIVE → live·`TTTC8434R`, 그 밖 → `InternalErrorException(KIS_BALANCE_MODE_UNSUPPORTED)`(토큰·KIS 호출 0회). 응답 해석·0 채움·수익률 계산·Map → 문자열 → BigDecimal 경로는 손대지 않음. 스냅샷은 KRW 한 줄(`cashBuyingPower=null`), 종목 `currency=KRW`, `asOf = LocalDateTime.ofInstant(clock.instant(), Asia/Seoul)`.
7. 옛 경로 `KisAccountQueryService`: `AccountQueryService` 로 계좌를 읽고 `mode` 파라미터(LIVE → KIS_LIVE, PAPER → KIS_PAPER)로 ref 를 만들어 KIS 어댑터 호출(가정 2: 계좌 모드와 달라도 거부 안 함). 공통 스냅샷을 `KisAccountBalanceResult` 로 되돌리며 `asOf` 는 KST `LocalDateTime` → 같은 순간의 UTC `OffsetDateTime`. 대사는 `reconcile` 로 떼어 그대로 두고 `localQuantities[it]!!` 를 `?.compareTo(...)?.let` 으로 바꿔 `!!` 제거. KRW 행·금액이 없으면 0 으로 메우지 않고 `InternalErrorException(KIS_BALANCE_SNAPSHOT_INVALID)`.
8. 삭제: `KisAccountBalancePort.kt`(`KisBalanceSnapshot`·`KisBalancePosition` 포함). 사용처 0건(src 검색).
9. 동결 저장소 `archunit_store/bb05cc60…` 1줄: `KisAccountQueryService` 생성자 줄의 매개변수 목록 글자만 바뀜(같은 클래스·같은 `PositionRepository` 의존, 새 의존 아님). 로컬 `-Darchunit.freeze.store.default.allowStoreUpdate=true` 로 옛 줄 제거 → 새 위반이 정확히 이 1건임을 메시지로 확인 → 같은 자리에 새 줄 삽입.
10. 린트: 새 코드의 ktlint·detekt 지적을 고침(ktlint `function-signature`·`function-expression-body` 와 detekt 120자가 부딪히는 자리는 본문을 문장 둘로 나눔). ktlint baseline: 삭제한 `KisAccountBalancePort.kt` 2줄, 새로 쓴 `KisAccountQueryService.kt`(main 3줄)·`KisAccountQueryServiceTest.kt`(14줄)·`KisAccountBalanceAdapterTest.kt`(5줄) 낡은 항목 제거, `KisAccountBalanceAdapter.kt` 는 `positions = positions` 줄이 사라져 1줄 제거하고 나머지 6줄은 줄 번호만 맞춤. 새 위반은 baseline 에 넣지 않음.

옮긴 시험 목록(DEC-05 A, 새 포트 기준):

| 시험 | 바뀐 것 | 건수 |
|---|---|---|
| `application/account/KisAccountQueryServiceTest` | 목 `KisAccountBalancePort.fetchBalance(id, trId)` → 목 `BrokerAccountBalancePort` + `BrokerAccountRef`(모드로 단언). `mode=LIVE` 로 KIS_PAPER 계좌 조회(가정 2)·UTC 변환·계좌 없음·미연결·`id<=0`·KRW 행 없음 추가 | 4 → 11 |
| `infrastructure/kis/KisAccountBalanceAdapterTest` | `fetchBalance(1L, "VTTC8434R")` → `fetchBalance(BrokerAccountRef)`, `AccountRepository` 목 제거. 헤더 `tr_id`·`appkey` 단언, KRW 스냅샷 모양, `supportedModes`, 그 밖 모드 내부 오류·호출 0회 추가. 계좌 `externalAccountId` null 시험은 서비스 시험(`KisAccountQueryServiceTest`·`BrokerAccountQueryServiceTest`)으로 이동, 형식 틀린 ref 시험은 어댑터에 남김 | 10 → 11 |
| `application/account/BrokerAccountQueryServiceTest` | 포트 하나 → `List<포트>`, `TradingMode.entries` 전부 표(KIS 둘은 KIS 어댑터). «KIS_PAPER·KIS_LIVE 501» 단언은 D24 로 대체. KIS 미연결 409, 선택 검사 4종(누락, 한 모드 둘, UPBIT_LIVE·LOCAL 지원) 신설 | 35(파라미터 포함) |
| `infrastructure/toss/TossAccountBalanceAdapterTest` | 목록 밖: `supportedModes == {TOSS_LIVE}` 1건 추가 | +1 |
| `TradingApiApplicationTests` | 목록 밖: `getBean(BrokerAccountBalancePort)` 유일 빈 가정이 깨져 모드 표(`getBeansOfType`)로 단언, 이 기동이 RISK-17 선택 검사의 실행 증거 | 3(불변) |

시험 파일의 근거 주석: 각 파일에 `devkit:test-change` 한 줄(D22·D23·D24·D25).

---

## 6. Verification  (record)

| What | How it was checked | Result |
|---|---|---|
| Red 확인 | `./gradlew compileTestKotlin --offline`(구현 전) | 실패: `supportedModes` 미해결, `BrokerAccountQueryService`·`KisAccountQueryService`·`KisAccountBalanceAdapter` 생성자 불일치 |
| 집중 시험 | `./gradlew test --rerun --offline --tests '*BrokerAccountQueryService*' --tests '*KisAccountQueryService*' --tests '*KisAccountBalance*' --tests '*TossAccountBalance*' --tests 'com.papertrading.api.characterization.kis.*' --tests '*ArchitectureRulesTest' --tests '*ApplicationLayerRulesTest'`(backend/trading-api), 시작 전 타임스탬프 파일보다 새 xml 만 집계 | exit 0, BUILD SUCCESSFUL in 2m 55s. xml 10개, 141건 성공 / 실패 0 / 오류 0 / 건너뜀 0. 파일별: BrokerAccountQueryServiceTest 35, KisAccountQueryServiceTest 11, KisAccountBalanceAdapterTest 11, KisAccountBalanceControllerIntegrationTest 2, TossAccountBalanceAdapterTest 51, ArchitectureRulesTest 15, ApplicationLayerRulesTest 6, characterization/kis 3+4+3=10 |
| 특성화 시험 무변경 | `git diff --stat -- backend/trading-api/src/test/kotlin/com/papertrading/api/characterization` | 변경 0 (10건 고치지 않고 통과) |
| 전체 시험 | `./gradlew test --rerun --offline`(backend/trading-api), 시작 전 타임스탬프 파일보다 새 xml 만 집계 | exit 0, BUILD SUCCESSFUL in 9m 51s. xml 85개(오래된 xml 무시 0), 1116건 성공 / 실패 0 / 오류 0 / 건너뜀 0. 직전 1095건(xml 85개) 대비 +21건(신설·확장 시험), 새 실패 0. `TradingApiApplicationTests` 3건 통과(앱 컨텍스트 기동 = 선택 검사가 기동을 깨지 않음) |
| 린트 | `./gradlew compileKotlin compileTestKotlin ktlintCheck detekt verifyLintBaselinesNotGrown -PlintBaselineRef=origin/main --offline` | exit 0. ktlint baseline origin/main 대비 늘어난 항목 0건, detekt baseline 늘어난 항목 0건. 시험 소스 ktlint 는 `runKtlintCheckOverTestSourceSet --rerun` 으로 낡은 baseline 항목을 뺀 상태에서도 통과 확인 |
| 아키텍처 | `ArchitectureRulesTest`·`ApplicationLayerRulesTest` | 15 + 6건 통과, 동결 저장소는 `bb05cc60…` 1줄 교체(§5 9번) 외 불변 |
| 옛 타입 사용처 0 | `grep -rn "KisAccountBalancePort\|KisBalanceSnapshot\|KisBalancePosition(" backend/trading-api/src`(주석 제외) | 0건 |
| 규칙 지목 | `rule-pointer.js --list`(바뀐 main 파일 6개) | `diffChecks` 0건, `broken` 행 없음 |
| trading-web 호출부 | `grep -rn "broker-balance\|api/kis/account" frontend/trading-web/src` | 0건(exit 1) → 멈춤 조건 아님 |

---

## 7. Result  (record)

- Done: broker-balance 가 KIS_PAPER·KIS_LIVE 를 지원(200, 출처 KIS, 미연결 409, KIS 실패 step 9 의 503). 브로커 선택이 매개변수 이름 대신 `supportedModes` 표 + 생성 때 검사. `/api/kis/account/balance` 는 공통 포트를 거치되 성공 응답(JSON 숫자 표기, UTC `asOf`, 대사)은 특성화 시험 10건 그대로 통과. 옛 KIS 전용 포트·스냅샷 타입 삭제, `!!` 제거.
- Changed files / PR: (커밋 전, backend/trading-api 기준)
  - 신설 main: application/account/BrokerBalancePortTable.kt, application/account/KisAccountNumber.kt
  - 수정 main: application/account/BrokerAccountQueryService.kt, application/account/KisAccountQueryService.kt, application/account/port/BrokerAccountBalancePort.kt, infrastructure/kis/KisAccountBalanceAdapter.kt, infrastructure/toss/TossAccountBalanceAdapter.kt
  - 삭제 main: application/account/port/KisAccountBalancePort.kt
  - 수정 test: application/account/BrokerAccountQueryServiceTest.kt, application/account/KisAccountQueryServiceTest.kt, infrastructure/kis/KisAccountBalanceAdapterTest.kt, infrastructure/toss/TossAccountBalanceAdapterTest.kt, TradingApiApplicationTests.kt
  - 수정 설정: src/test/resources/archunit_store/bb05cc60-ae2b-4a57-8f2e-cec4890e2499(1줄 교체), config/ktlint/baseline.xml(낡은 항목 제거와 줄 번호 정렬)
  - 문서: 이 파일(§5~8)

---

## 8. Problems found  (record)

> Found during the work — it can cause a new step, a change to an existing one, or a flag. Edit this step.md directly then (the reason goes in state.md §3).

| Problem | How it was handled |
|---|---|
| 동결 저장소 1줄 교체: `KisAccountQueryService` 생성자의 매개변수 목록이 바뀌어 같은 위반(application → `PositionRepository`)의 글자가 달라짐. 멈춤 조건 «동결 목록 밖 새 위반»에 글자 그대로는 닿을 수 있음 | 새 위반 메시지가 정확히 이 1건이고 클래스·의존 대상이 같음을 확인하고 교체. 사람 확인이 필요하면 `git diff` 한 줄로 검토 가능 |
| 목록 밖 시험 2개를 고침: `TradingApiApplicationTests`(공통 포트 빈이 하나라는 가정), `TossAccountBalanceAdapterTest`(+1) | 구현이 둘이 되며 생긴 필연적 변경, 기대값을 코드에 맞춰 낮춘 것이 아니라 모드 표로 올림 |
| KST 가정이 두 곳: KIS 어댑터는 `Clock` 순간 → `Asia/Seoul`, 옛 경로는 KST → UTC 변환에 `Asia/Seoul`. 레포에 JVM 시간대 고정이 없고 토스 어댑터는 `systemDefaultZone` 에 기댐 | KIS 쪽은 명시적 KST 로 서로 일치시킴. 토스 어댑터는 범위 밖이라 안 건드림. 시간대 상수 통합은 후속 판단 |
| 계좌번호 형식 판정이 두 곳: `linkedKisAccountNumber`(서비스) 와 어댑터 `parseExternalAccountId` | infrastructure 가 application 내부 함수를 가져오지 않게 어댑터에 방어용으로 남김. 서비스가 먼저 막아 어댑터 분기는 평소 안 탐. 합칠지는 후속 |
| 가정(되돌리기 쉬움): 옛 경로는 계좌의 `tradingMode` 를 아예 안 보던 지금 동작을 유지(KIS_PAPER 만이 아니라 LOCAL 계좌도 `externalAccountId` 만 맞으면 조회됨) | 가정 2 의 연장. 거부 도입은 trading-web 재제작이 경로를 정한 뒤 |
| 가정(새 코드): 어댑터가 KIS 가 아닌 모드를 받거나 스냅샷에 KRW 행·금액이 없으면 `InternalErrorException`(code `KIS_BALANCE_MODE_UNSUPPORTED`·`KIS_BALANCE_SNAPSHOT_INVALID`, 응답은 500 고정 문구) | 서비스 표 검사 때문에 정상 경로에선 도달 불가인 방어. 응답에 내부 메시지 안 실림(핸들러) |
| step 9 후속 S9-06(계좌 모드 미검사) | broker-balance 는 이제 서비스가 계좌를 읽어 모드로 고르므로 해소. 옛 경로는 가정 2 대로 둠 |
| 멈춤 조건에 걸린 것 | 없음(특성화 시험 수정 0, trading-web 호출부 0, Docker 정상, 기준선 대비 새 실패 0) |
