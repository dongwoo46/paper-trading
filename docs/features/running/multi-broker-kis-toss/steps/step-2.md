# Step 2 — 모드 추가와 주문 안전장치

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
| Step | 2 |
| Title | 모드 추가와 주문 안전장치 |
| Feature | multi-broker-kis-toss |
| Based on | development-plan §2 D4, D6, §5-2, DEC-01, feature-spec RULE-03, RULE-04 |
| Status | todo / in progress / done / blocked |
| Owner |  |

---

**━━ Plan block (required before starting · from the planner / development-plan design) ━━**

## 1. Goal and scope  (plan)

> What this step (phase) **builds (what)** + **how far it goes (this step's boundary — name what is handed to the next step)**.

- Builds: TOSS_LIVE를 추가하고, TOSS_LIVE와 UPBIT_LIVE 주문을 자금 잠금 전에 501로 거부하며, 주문·체결 분기의 else를 없애 모든 모드를 명시한다
- Scope (where this step stops): TradingMode, AccountSource, Account 검증, OrderCommandService, ExecutionProcessor, RedisMarketQuoteAdapter, 브로커 예외. 토스 호출 코드는 없음

---

## 2. Approach (how, in what order)  (plan)

> **By what method and in what order** this step (= phase) is solved — briefly (one line to a few). Based on the feature-spec / development-plan design.
> A rough level is enough (the detailed work log is §5). **Never leave it empty — the step's base is here.**

- 실패하는 테스트 먼저: 모드 누락 표(주문, 체결), TOSS_LIVE·UPBIT_LIVE 거부 단위·통합 테스트, 계좌 검증
- enum과 검증 추가
- placeOrder 선행 거부와 전수 when
- ExecutionProcessor 분기 명시

---

## 3. Dependencies  (plan)

- Previous step: 1. 기준선 기록
- Prerequisites: 운영 ddl-auto 실제 값 확인(RISK-04)
  - RISK-04: 운영 ddl-auto는 기본값 create 그대로 — 사용자 확인 2026-11-15

---

## 4. Completion criteria  (plan)

- [ ] 모드 누락 표 테스트가 TradingMode.entries 전체를 덮음
- [ ] TOSS_LIVE·UPBIT_LIVE 주문 시 501, 주문·원장·잠금·구독 변화 없음
- [ ] 기준선 대비 새 실패 0

---

**━━ Record block (filled in order while developing · never ahead) ━━**

## 5. Work log  (record)

> Fill it by spelling out the §2 approach as the actual work (concretely, what was done).

1. Red: 테스트를 먼저 썼고 `compileTestKotlin`이 `TOSS_LIVE`, `BrokerOrderNotSupportedException` 미해결로 실패하는 것을 확인.
   - `AccountTest`: STOCK+TOSS_LIVE 허용, CRYPTO+TOSS_LIVE 거부, 계좌타입×모드 전수 표(키 집합이 `AccountType.entries`와 같은지, 모든 `TradingMode`에 대해 허용/거부 단언)
   - `OrderCommandServiceTest`: TOSS_LIVE·UPBIT_LIVE 거부 6종(지정가 매수, 시장가 매수, 매도, IOC·FOK, `createAutoExitSellOrder`, `createGroupedAutoExitSellOrder`)과 주문 모드 누락 표
   - `ExecutionProcessorTest`: TOSS_LIVE·UPBIT_LIVE 매도 체결 거부 2종과 체결 모드 누락 표
   - `RedisMarketQuoteAdapterTest`(새 파일): 시세 키 모드 누락 표, TOSS_LIVE 키와 대문자 변환
   - `OrderControllerIntegrationTest`: TOSS_LIVE 지정가·시장가·IOC·매도, UPBIT_LIVE 지정가. 501, code, 주문·원장 수, 예수금·잠금, 포지션 잠금 수량, 구독 mock 미호출
2. Green: `TradingMode.TOSS_LIVE`(끝에 추가), `AccountSource.TOSS`, `Account.validateTradingMode`의 STOCK에 TOSS_LIVE 추가.
3. 범주 I 기반 예외 `NotImplementedException`(501)을 `CommonApiExceptions.kt`에, `BrokerOrderNotSupportedException`(code `BROKER_ORDER_NOT_SUPPORTED`)을 새 파일 `BrokerApiExceptions.kt`에 추가. 모드는 문자열로 받음(common이 domain을 가리키면 순환).
4. `OrderCommandService.placeOrder`: `tradingMode` 확정 직후(매도 수량 잠금, 주문 저장, 매수 잠금, 구독 전)에 `rejectIfBrokerOrderNotSupported`(전수 `when`)로 TOSS_LIVE·UPBIT_LIVE 거부. `submitExternalOrderIfNeeded`의 `else -> {}`를 없애고 LOCAL, KIS_PAPER, KIS_LIVE, TOSS_LIVE·UPBIT_LIVE를 전수 명시.
5. `ExecutionProcessor`: 매도 체결 `when`의 `else`를 없애고 TOSS_LIVE·UPBIT_LIVE는 `BrokerOrderNotSupportedException`(입금 없음).
6. `RedisMarketQuoteAdapter`: `TOSS_LIVE -> quote:toss:live:{ticker}` 추가.
7. 변이 확인 1회(돈 경로): `placeOrder`의 선행 거부 호출을 빼면 `OrderCommandServiceTest` 14개 중 7개 실패, 복구 후 통과.

합동 리뷰 후속 수정(`review/panel.md`, 사용자가 범위 안 9건 지정, 2026-11-15):

- P-01: `ExecutionProcessor.processFill`의 미지원 모드 거부를 계좌·모드 확정 직후(체결 중복 확인·주문 상태 확인 다음, 수수료 조회·Execution 저장·포지션 차감 전)로 옮김. 매수·매도 모두 거부. 매도 분기 `when`의 TOSS_LIVE·UPBIT_LIVE는 `else` 없는 전수 `when`용 방어 분기로 남김. 테스트는 구현 전에 먼저 써서 6개 실패(Red)를 확인한 뒤 구현.
- P-02: `AccountTest`의 계좌 표를 `Map<TradingMode, Set<AccountType>>`로 뒤집고 키 집합을 `TradingMode.entries`와 비교. 모드별 `@ParameterizedTest`.
- P-05: 새 주석 6곳을 명사형 한 줄로 바꿈(`OrderCommandService.kt` 3곳, `ExecutionProcessor.kt` 2곳, `BrokerApiExceptions.kt` KDoc).
- P-08: `OrderCommandServiceTest` 자동 청산 테스트의 깨질 수 없던 `position.lockedQuantity == 0` 단언 제거(수량 잠금 부재는 조회 0회 단언이 증명).
- P-09: 주문 모드 표의 기대 호출 수를 `if (routing == …)` 대신 라우팅별 전수 `when`(`expectedCalls`)으로 명시. 거부 모드 목록을 표(`EXPECTED_ORDER_ROUTING`)에서 파생해 한 곳에서 관리. 체결 표도 같은 방식.
- P-10: `for` 루프 단언을 `@ParameterizedTest`(`@EnumSource`, `@MethodSource`)로 나눠 모드마다 따로 실행·보고. 키 집합 테스트는 표마다 1개 유지. 단언 메시지에도 모드 포함.
- P-03, P-04, P-11: 문서 기록(§3, §8, state.md §3 변경 이력 한 줄).
- 범위 밖으로 둠: P-06(자동 청산 재시도), P-07(프런트 타입).
- 도구 메모: serena Kotlin 언어 서버가 기동에 실패해(계획서 §2와 같은 증상) 내장 Edit로 편집.

---

## 6. Verification  (record)

| What | How it was checked | Result |
|---|---|---|
| Red 확인 | 구현 전 `./gradlew compileTestKotlin` | `TOSS_LIVE`, `BrokerOrderNotSupportedException` 미해결로 컴파일 실패 |
| 대상 테스트 | `./gradlew test --tests` OrderCommandServiceTest, ExecutionProcessorTest, AccountTest, RedisMarketQuoteAdapterTest, OrderControllerIntegrationTest, XML 집계 | 84개, 실패 0, 에러 0 |
| 변이 확인(돈 경로 1회) | `placeOrder`의 `rejectIfBrokerOrderNotSupported` 호출을 주석 처리 후 `OrderCommandServiceTest` | 14개 중 7개 실패(거부 6종과 주문 모드 누락 표). 복구 후 통과, 변이 표식 0건 |
| 컴파일 | `./gradlew compileKotlin` | 성공 |
| 전체 테스트 | `./gradlew test --continue`, `build/test-results/test/*.xml` 집계 | 58개 파일, 413개 테스트, 실패 0, 에러 0, 건너뜀 0 |
| 기준선 대비 | 1단계 기준선(e2904c8): 57개 파일, 393개 테스트, 실패 0 | 파일 +1(RedisMarketQuoteAdapterTest), 테스트 +20, 새 실패 0 |
| 완료 기준 1 | 주문, 체결, 계좌, 시세 키 4개 모드 누락 표 테스트가 `TradingMode.entries`와 표 키 집합이 같은지 단언. 정정(P-02): 최초 작성 때 계좌 표는 키가 `AccountType`이라 `AccountType.entries`와 비교했고 새 `TradingMode`를 표에 안 넣어도 통과했음. 리뷰 후 키를 `TradingMode`로 뒤집어 사실이 됨 | 통과 |
| 리뷰 후속 Red(P-01) | 구현 전 대상 3개 클래스 실행 | 97개 중 6개 실패(미지원 모드 SELL·BUY 체결 거부, 체결 모드 표의 TOSS_LIVE·UPBIT_LIVE). 구현 후 통과 |
| 리뷰 후속 대상 테스트 | ExecutionProcessorTest 14, OrderCommandServiceTest 27, AccountTest 56, RedisMarketQuoteAdapterTest 2, OrderControllerIntegrationTest 10 | 전부 통과 |
| 리뷰 후속 전체 테스트 | `./gradlew compileKotlin && ./gradlew test --continue`, `build/test-results/test/*.xml` 집계 (Docker Desktop은 꺼져 있어 다시 켬) | 58개 파일, 438개 테스트, 실패 0, 에러 0, 건너뜀 0. 직전 413개 대비 +25(모드 루프를 `@ParameterizedTest`로 나눈 것과 BUY 체결 거부 추가), 새 실패 0 |
| 완료 기준 2 | 통합 테스트 5종(TOSS_LIVE 지정가·시장가·IOC·매도, UPBIT_LIVE 지정가): 501, code `BROKER_ORDER_NOT_SUPPORTED`, 주문·원장 수 불변, 예수금·잠금 불변, 포지션 잠금 수량 불변, 구독 mock 미호출 | 통과 |

추가한 테스트 20개: AccountTest 3, OrderCommandServiceTest 7, ExecutionProcessorTest 3, RedisMarketQuoteAdapterTest 2(새 파일), OrderControllerIntegrationTest 5.

---

## 7. Result  (record)

- Done: TOSS_LIVE 추가, TOSS_LIVE·UPBIT_LIVE 주문을 자금 잠금 전에 501로 거부, 주문·체결 분기의 `else` 제거(전수 `when`), 시세 키 추가. 위 §6 증거. (완료 선언은 사람이 함)
- Changed files / PR: PR 없음, 커밋 안 함. `backend/trading-api/src/` 아래:
  - 수정(main): `domain/enums/TradingMode.kt`, `domain/enums/AccountSource.kt`, `domain/entity/account/Account.kt`, `application/order/OrderCommandService.kt`, `application/order/ExecutionProcessor.kt`, `infrastructure/redis/RedisMarketQuoteAdapter.kt`, `common/exception/CommonApiExceptions.kt`
  - 신설(main): `common/exception/BrokerApiExceptions.kt`
  - 수정(test): `application/order/OrderCommandServiceTest.kt`, `application/order/ExecutionProcessorTest.kt`, `domain/entity/account/AccountTest.kt`, `presentation/controller/OrderControllerIntegrationTest.kt`
  - 신설(test): `infrastructure/redis/RedisMarketQuoteAdapterTest.kt`

---

## 8. Problems found  (record)

> Found during the work — it can cause a new step, a change to an existing one, or a flag. Edit this step.md directly then (the reason goes in state.md §3).

| Problem | How it was handled |
|---|---|
| 계획서 §3은 `CommonApiExceptions.kt`에 범주 J, I, K 3개를 추가하라고 하나 이번 단계에서 쓰는 것은 I(501)뿐 | I(`NotImplementedException`)만 추가. J(503), K(500)는 처음 쓰는 단계(토큰·전송 계층)에서 추가. 계획 변경 아님, 시점만 미룸. 사람 확인 필요 |
| `BrokerOrderNotSupportedException`이 거래 모드를 `TradingMode`가 아닌 문자열로 받음 | `Account` 등 domain이 `common.exception`을 이미 가리켜서 common이 domain을 가리키면 패키지 순환. 호출부가 `tradingMode.name`을 넘김. ignore |
| 3단계(건드린 파일의 기존 `check`/`require` 범주 정렬, DEC-02) 미착수 | 지시대로 이번에 하지 않음. `Account.validateTradingMode`의 `require`(CRYPTO+TOSS_LIVE 거부)는 기존 IllegalArgumentException(400 `BAD_REQUEST`) 그대로이고, 다음 정렬 단계 대상 |
| (해결, P-01) ExecutionProcessor의 TOSS_LIVE·UPBIT_LIVE 예외가 Execution 저장·포지션 차감 뒤 매도 분기에만 있었음 | 진입부(`processFill`, 계좌·모드 확정 직후)로 옮겨 매수·매도 모두 거부. 단위 테스트가 Execution 저장, 포지션 조회·저장·원자 차감, 원장, 정산 호출 0회를 단언. 주문 선행 거부로 도달 불가(DEC-01)는 그대로 |
| (해결, P-04) RISK-04(운영 ddl-auto 실제 값) 확인 | 사용자 확인 2026-11-15: 기본값 create 그대로. §3에 기록 |
| AE20 점검(P-03): UPBIT_LIVE·TOSS_LIVE 주문 응답이 201(자금만 잠기고 외부 제출 없음)에서 501 `BROKER_ORDER_NOT_SUPPORTED`로 바뀜. trading-web 호출부 읽기 전용 점검 | 결과: 깨지는 코드 없음. `entities/order/api/orderApi.ts:9` `fetchPlaceOrder`는 `shared/api/index.ts`의 `fetchJson`을 쓰고, 2xx가 아니면 상태 코드나 code로 분기하지 않고 `Error("501 Not Implemented: <응답 JSON 원문>")`을 던짐. `features/order-form/ui/OrderFormPanel.tsx:97-98`은 그 메시지를 경고창에 그대로 표시(원문 JSON 노출, 안내 문구 없음). 모드별 분기나 `BROKER_ORDER_NOT_SUPPORTED` 참조는 없음. 타입은 `entities/order/model/types.ts:6`에 TOSS_LIVE 없음(UPBIT_LIVE 있음), `entities/account/model/types.ts:2`에 TOSS_LIVE·UPBIT_LIVE 모두 없음. 주문 API를 부르는 다른 곳(quant-worker, collector-api)은 없음. 프런트 코드는 고치지 않음 |
| 후속: 프런트 재제작 세션 | 1. 두 `TradingMode` 타입에 TOSS_LIVE(계좌 쪽은 UPBIT_LIVE도) 추가. 2. 주문 실패 경고창에서 `BROKER_ORDER_NOT_SUPPORTED`(501)를 «이 계좌는 아직 주문을 지원하지 않습니다» 같은 안내로 표시. 계획서 RISK-07, FLAG-05(2단계 화면 작업)와 같은 줄기. 사용자 결정 필요 |
| 후속(범위 밖, P-06): 자동 청산이 결정적 501 거부를 3회 재시도(`PositionExitTriggerOrchestrator.kt:170`, 새 트랜잭션·계좌 잠금 3번) 후 FAILED | 이번에 손대지 않음(리뷰 지적 내용 그대로 기록, 코드는 이번에 다시 읽지 않음). 계획서 §2 표는 토스 계좌에 1단계 내부 포지션이 없다고 봄. UPBIT_LIVE 계좌의 도달 가능성은 확인 안 함. 후속 단계에서 재시도 제외 대상에 501을 넣을지 결정 |
| 후속(범위 밖, P-07): 프런트 모드 타입 | 위 «프런트 재제작 세션»에 묶음 |
| `backend/trading-api/src/docs/`가 untracked로 남아 있음 | 이번 변경이 아님(작업 시작 전 상태). 건드리지 않음 |
