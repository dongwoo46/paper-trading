# Step 12 — 계좌 응답 출처와 ADR 갱신

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
| Step | 12 |
| Title | 계좌 응답 출처와 ADR 갱신 |
| Feature | multi-broker-kis-toss |
| Based on | development-plan.md §5 2단계 5번, §10.2 D29·D25·D26, §10.3 AccountSource·AccountResponse·docs/ADR.md 행, §10.4 AE20 396행(계좌 응답 source LOCAL → KIS·TOSS·UPBIT), 가정 1(AccountSource 에 UPBIT 추가) |
| Status | done |
| Owner |  |

---

**━━ Plan block (required before starting · from the planner / development-plan design) ━━**

## 1. Goal and scope  (plan)

> What this step (phase) **builds (what)** + **how far it goes (this step's boundary — name what is handed to the next step)**.

- Builds: AccountSource 에 모드 → 출처 함수 하나(전수 when)를 두고 AccountResponse.from 과 BrokerAccountQueryService 가 그것을 쓰게 해 계좌 응답 source 를 계좌 모드에서 산출한다. 루트 docs/ADR.md 의 ADR-005 를 D25(포트별 supportedModes 표와 기동 검사)·D26(BrokerOrderPort·BrokerOrderSubmitter) 기준으로 갱신한다
- Scope (where this step stops): main: domain/enums/AccountSource(UPBIT 추가, fromTradingMode 전수 when: LOCAL→LOCAL, KIS_PAPER·KIS_LIVE→KIS, TOSS_LIVE→TOSS, UPBIT_LIVE→UPBIT, else 금지), presentation/dto/account/AccountResponse(source 기본값 LOCAL 제거, from 이 계좌 모드로 산출), application/account/BrokerAccountQueryService(Route 별 AccountSource 하드코딩 대신 같은 함수). test: AccountSource 모드 표 시험(TradingMode.entries 전부), AccountResponse.from 모드 표, 계좌 API 응답 source 를 단언하는 기존 시험 갱신. docs: 루트 docs/ADR.md ADR-005 갱신(OrderExecutionPort 이력 남김). 범위 밖: backend/trading-api/docs/ADR.md, 체결(step 13), trading-web(D20), characterization/kis 시험

---

## 2. Approach (how, in what order)  (plan)

> **By what method and in what order** this step (= phase) is solved — briefly (one line to a few). Based on the feature-spec / development-plan design.
> A rough level is enough (the detailed work log is §5). **Never leave it empty — the step's base is here.**

- 시험 먼저: AccountSource.fromTradingMode 를 TradingMode.entries 전부로, AccountResponse.from 이 KIS_PAPER·KIS_LIVE→KIS, TOSS_LIVE→TOSS, UPBIT_LIVE→UPBIT, LOCAL→LOCAL 을 내는지, 계좌 API(목록·단건) 응답 JSON source 단언 → 실패 확인
- AccountSource 에 UPBIT 와 함수 추가, AccountResponse·BrokerAccountQueryService 가 함수 사용(BrokerAccountQueryService 의 Route 와 출처가 어긋나지 않게)
- 응답 source 가 LOCAL 에서 바뀌는 시험만 새 값으로 옮기고 목록을 §5 에 기록(AE20 396행, trading-web src 의 source 읽는 곳 재검색 결과 기록)
- docs/ADR.md ADR-005 를 D25·D26 으로 갱신: 결정(포트마다 supportedModes, 서비스가 List<포트> 로 표를 만들고 전수 when 과 기동 검사, 주문은 BrokerOrderPort·BrokerOrderSubmitter), 이유, 기각안, 이력(옛 OrderExecutionPort·실행기 둘)

---

## 3. Dependencies  (plan)

- Previous step:
- Prerequisites: Previous step: step 11 커밋 c1153de, Prerequisites: Docker(Testcontainers)

---

## 4. Completion criteria  (plan)

- [x] characterization/kis 10건 고치지 않고 통과(--rerun, 실행 뒤 생긴 xml)
- [x] AccountSource 모드 표·AccountResponse.from 모드 표·계좌 API source 시험 통과
- [x] 전체 시험 새 실패 0(직전 1176건 기준)
- [x] compileKotlin·compileTestKotlin·ktlintCheck·detekt·verifyLintBaselinesNotGrown exit 0
- [x] AccountSource.LOCAL 기본값 사용처 0, trading-web source 호출부 재점검 결과 기록
- [x] ADR-005 갱신

---

**━━ Record block (filled in order while developing · never ahead) ━━**

## 5. Work log  (record)

- 시험 먼저: `domain/enums/AccountSourceTest`(모드 표가 TradingMode.entries 전부를 덮는지, 모드마다 출처), `presentation/dto/AccountResponseTest`(from 이 5개 모드에서 LOCAL·KIS·KIS·TOSS·UPBIT), `AccountControllerIntegrationTest` 에 «단건·목록 source 가 모드에서 나옴»(5개 모드 POST 생성 응답·GET 단건·GET 목록) 과 생성 응답 `$.source` 단언 추가. 구현 전 compileTestKotlin 이 `fromTradingMode` 미정의로 실패(Red)
- main: `AccountSource` 에 UPBIT 와 `fromTradingMode`(전수 when, else 없음). `AccountResponse` source 기본값 LOCAL 제거, `from` 이 `account.tradingMode` 를 지역 변수 1회만 읽어 산출(ArchUnit freeze 가 `Account.getTradingMode()` 호출 1건을 기존 위반으로 고정해 둬서 두 번 읽으면 새 위반이 됨, 첫 구현이 ArchitectureRulesTest 에서 실패해 고침). `BrokerAccountQueryService` 는 Route 별 AccountSource 하드코딩과 `fetch` 의 source 인자를 지우고 같은 함수 사용
- 옮긴 기존 시험: 0건. 계좌 API 응답 source 를 단언하던 기존 시험이 없었음(src/test 에서 `"source"`·`$.source` 단언은 characterization/kis 의 KIS 잔고뿐이고 그쪽은 BrokerAccountBalanceResponse 의 source). 기존 `BrokerAccountQueryServiceTest`(KIS·TOSS 출처 단언)는 그대로 통과
- AE20(396행) 호출부: trading-web `frontend/trading-web/src` 를 `source` 단어·`AccountSource` 로 읽기 전용 재검색, 계좌 응답 source 를 읽는 곳 0건(일치는 `src/test/proxy-table.test.ts`, `src/test/guards/rules.ts` 의 함수 매개변수 이름 `source` 뿐, 계좌와 무관). 같은 저장소 안 다른 소비자는 AccountResponse 를 쓰는 AccountController 세 곳(생성·목록·단건·수정)
- `AccountSource.LOCAL` 기본값 사용처: `grep -rn "AccountSource\." src` 에서 AccountResponse 의 LOCAL 기본값·하드코딩 0건(남은 `AccountSource.KIS` 는 `KisAccountBalanceResult` 의 기존 기본값, 범위 밖)
- ktlint baseline: AccountResponse·AccountControllerIntegrationTest 는 baseline 이 줄·열로 고정돼 있어 줄이 밀리면 새 위반으로 잡힘. 항목 수는 그대로 두고 줄 번호만 밀린 만큼 옮김(`config/ktlint/baseline.xml`, 14줄 변경). 새로 만든 코드는 trailing comma 규칙 준수
- ADR: 루트 `docs/ADR.md` ADR-005 를 «거래 모드 → 브로커 포트 선택 (포트별 supportedModes 표)» 로 갱신. 결정(D25·D26·D29), 이유, 기각안, 트레이드오프, 이력(옛 OrderExecutionPort 와 KisPaper·KisLive 실행기 둘) 포함. `backend/trading-api/docs/ADR.md` 는 안 건드림

---

## 6. Verification  (record)

| What | How it was checked | Result |
|---|---|---|
| 특성화 characterization/kis | `./gradlew test --rerun --offline` 후 표식 파일보다 새 xml 3개(Account 3건·ExecutionNotice 3건·OrderIntake 4건), 파일은 미수정 | 10건 실패 0·오류 0·건너뜀 0 |
| 새 시험 | AccountSourceTest 2건, AccountResponseTest 1건, AccountControllerIntegrationTest 신규 1건(전체 실행에 포함) | 실패 0 |
| 전체 시험 | 표식 `touch` 후 `./gradlew test --rerun --offline`(BUILD SUCCESSFUL, 9m51s, exit 0), 표식보다 새 xml 89개 합산 | tests 1180, failures 0, errors 0, skipped 0(직전 1176 + 새 4) |
| 린트 | `./gradlew compileKotlin compileTestKotlin ktlintCheck detekt verifyLintBaselinesNotGrown -PlintBaselineRef=origin/main --offline` | exit 0 |
| ArchUnit | 전체 실행의 ArchitectureRulesTest, archunit_store 미수정 | 통과 |

---

## 7. Result  (record)

- Done: 계좌 응답 source 가 계좌 모드에서 산출되고(LOCAL·KIS_PAPER·KIS_LIVE→KIS·TOSS_LIVE→TOSS·UPBIT_LIVE→UPBIT), 잔고 경로도 같은 함수를 씀. ADR-005 갱신
- Changed files / PR: backend/trading-api: `domain/enums/AccountSource.kt`, `presentation/dto/account/AccountResponse.kt`, `application/account/BrokerAccountQueryService.kt`, `config/ktlint/baseline.xml`(줄 번호 이동만). 시험: `domain/enums/AccountSourceTest.kt`(신규), `presentation/dto/AccountResponseTest.kt`(신규), `presentation/controller/AccountControllerIntegrationTest.kt`. 문서: 루트 `docs/ADR.md`, 이 파일. 커밋은 메인이 함

---

## 8. Problems found  (record)

| Problem | How it was handled |
|---|---|
| `from` 에서 `account.tradingMode` 를 두 번 읽으면 ArchUnit freeze(presentation → domain 엔티티 호출)가 새 위반으로 실패 | 지역 변수로 1회만 읽음. presentation 이 도메인 엔티티를 직접 읽는 기존 위반 자체는 범위 밖 |
| 기존 UPBIT_LIVE 계좌 응답은 지금까지 source LOCAL 이었음(AE20 396행 변경) | trading-web 이 source 를 읽는 곳 0건이라 호출부 영향 없음, 외부 소비자 확인은 메인 판단 |
| `KisAccountBalanceResult.source` 의 기본값 `AccountSource.KIS` 는 남아 있음 | 계좌 응답이 아니고 step 범위 밖이라 그대로 둠 |
