# Step 11 — 주문 공통 포트

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
| Step | 11 |
| Title | 주문 공통 포트 |
| Feature | multi-broker-kis-toss |
| Based on | development-plan.md §5 2단계 4번, §10.2 D25·D26·D28, §10.3 주문 행, §10.4 AE20 395행(주문 접수 실패 500·400 → D28 code), DEC-05 A(시그니처에 묶인 시험은 같은 커밋에서 새 포트로 옮기고 목록 기록), DEC-08 A(취소는 포트 밖), review/step9-code-review.md S9-01·S9-02(사람 결정 2026-11-21: 함께 고침) |
| Status | done |
| Owner |  |

---

**━━ Plan block (required before starting · from the planner / development-plan design) ━━**

## 1. Goal and scope  (plan)

> What this step (phase) **builds (what)** + **how far it goes (this step's boundary — name what is handed to the next step)**.

- Builds: BrokerOrderPort(supportedModes, submit(order): BrokerOrderReceipt)와 KIS 구현 KisOrderAdapter 를 만들고 KisPaperOrderExecutor·KisLiveOrderExecutor 를 BrokerOrderSubmitter 하나로 합친다. 주문 경로 KIS 실패를 D28 code 로 정렬하고, KIS 실패 분류를 잔고·주문 어댑터가 함께 쓰게 해 S9-01(연결 거부가 KIS_TIMEOUT)·S9-02(rt_cd 1 한도가 인증으로 가려짐)를 고친다
- Scope (where this step stops): main: application/order/port/BrokerOrderPort(+BrokerOrderReceipt), infrastructure/kis/KisOrderAdapter(KisOrderRestClient.placeOrder 감쌈, 모드 전수 when 으로 paper·live, 그 밖 모드는 범주 K 내부 오류이고 KIS 호출 0, 호출 전 externalAccountId 없음·XXXXXXXX-XX 아님 → 409 KIS_ACCOUNT_NOT_LINKED), application/order/BrokerOrderSubmitter(List<BrokerOrderPort> 주입, D25 방식 표·기동 검사, externalOrderId 기록, 로그·Slack 문구는 모드 이름으로 지금 글자 그대로), OrderCommandService(생성자, submitExternalOrderIfNeeded 의 KIS 두 분기 → submitter, TOSS_LIVE·UPBIT_LIVE 선행 거부·방어 분기 그대로), 새 예외 KisOrderRejectedException(A 400 KIS_ORDER_REJECTED, 파일 하나 선언 하나, KDoc 첫 줄 범주 기호), GlobalExceptionHandler 고정 문구 1줄, KIS 실패 분류 공용화(잔고 어댑터 ensureSuccess·전송 실패도 같은 분류 사용), 원인 체인 타임아웃 판정을 토스·KIS 공용 보조로(토스 동작 불변), KisPaperOrderExecutor·KisLiveOrderExecutor 삭제(cancel·pollFills 호출 0건이라 함께 사라짐). 범위 밖: KisOrderRestClient·KisTokenManager 파일(안 고침), 취소 연동(DEC-08), 토스 주문 어댑터(GL4), mapKisErrorCode 문구 순서 자체(step 9 범위 밖 기록), ORNO/ODNO 필드명(TODO), AccountResponse.source(step 12), 체결(step 13), characterization/kis 시험

---

## 2. Approach (how, in what order)  (plan)

> **By what method and in what order** this step (= phase) is solved — briefly (one line to a few). Based on the feature-spec / development-plan design.
> A rough level is enough (the detailed work log is §5). **Never leave it empty — the step's base is here.**

- 시험 먼저: BrokerOrderSubmitter 선택 검사(구현 누락, 한 모드 구현 둘, 정책 밖 모드 지원 → 생성 때 예외·메시지에 모드 이름), 모드 표(KIS_PAPER·KIS_LIVE → KIS 어댑터, 로그·Slack 문구 지금 글자), KisOrderAdapter 모드(paper·live 전달, 그 밖 내부 오류·호출 0), 계좌 미연결 409(호출 0), 실패 매핑 표(KisErrorCode ORDER_REJECTED·INVALID_ACCOUNT·DUPLICATE → 400 KIS_ORDER_REJECTED, AUTH → 503 KIS_AUTH_FAILED, RATE_LIMITED → 503 KIS_RATE_LIMITED, TEMPORARY_UNAVAILABLE → 503 KIS_UNAVAILABLE, 원인 체인 타임아웃 → KIS_TIMEOUT, 연결 거부 ConnectException·UnknownHost → KIS_UNAVAILABLE, 그 밖 RestClientException → KIS_UNAVAILABLE, ORNO 없는 성공 응답 → KIS_UNAVAILABLE(가정)), 잔고 어댑터 S9-01(연결 거부 원인 → KIS_UNAVAILABLE)·S9-02(rt_cd 1 + msg_cd EGW00201 또는 «초당 거래건수» → KIS_RATE_LIMITED) → 실패 확인
- 한도 판정은 msg_cd EGW00201 또는 «초당 거래건수» 문구로 하고 rt_cd 1 인증 판정보다 앞에 둠. 문구 «초과» 단독으로 한도 판정 금지(주문가능금액 초과 같은 거부가 503 이 됨)
- 원인 체인 타임아웃 판정(causeChain·isTimeout)을 infrastructure 공용 보조로 빼고 TossTransportSupport 가 그것을 부름(토스 시험 그대로 통과로 불변 확인)
- BrokerOrderPort·KisOrderAdapter·BrokerOrderSubmitter 구현, OrderCommandService 연결, 실패 시 예외는 그대로 던져 주문 트랜잭션 롤백 동작 유지(DEC-08). 응답 message 는 code별 고정 문구, KIS 원문은 로그만(AE8)
- 실행기 둘 삭제, OrderCommandServiceTest 의 실행기 mock 을 submitter 기준으로 옮기고 옮긴 시험 목록을 §5 에 기록, 쓰는 곳 없어진 옛 KIS 예외 있으면 삭제(KisResponseParseException 은 KisOrderRestClient 가 계속 써서 남김), ArchUnit 해결 줄 있으면 제거
- trading-web src 에 주문 API 호출부 재검색(읽기 전용) 결과 기록

---

## 3. Dependencies  (plan)

- Previous step:
- Prerequisites: Previous step: step 10 커밋 f9c8589, Prerequisites: Docker(Testcontainers)

---

## 4. Completion criteria  (plan)

- [x] characterization/kis 10건 고치지 않고 통과(--rerun, 실행 뒤 생긴 xml)
- [x] 선택 검사·모드 표·KIS 주문 어댑터 모드·계좌 미연결·실패 매핑 표·S9-01·S9-02 시험 통과
- [x] 전체 시험 새 실패 0(직전 1116건 기준)
- [x] compileKotlin·compileTestKotlin·ktlintCheck·detekt·verifyLintBaselinesNotGrown exit 0
- [x] KisPaperOrderExecutor·KisLiveOrderExecutor 사용처 0, application 에서 KisOrderRestClient import 0, 옮긴 시험 목록 기록
- [x] trading-web 주문 호출부 재점검 결과 기록

---

**━━ Record block (filled in order while developing · never ahead) ━━**

## 5. Work log  (record)

> Fill it by spelling out the §2 approach as the actual work (concretely, what was done).

1. 시험 먼저(Red 1): `KisAccountBalanceAdapterTest` 에 S9-01(연결 거부 `ConnectException`·DNS `UnknownHostException` 원인 → KIS_UNAVAILABLE)·S9-02(rt_cd 1 + msg_cd EGW00201, rt_cd 1 + «초당 거래건수» → KIS_RATE_LIMITED)·«초과» 단독은 한도 아님 시험을 넣고 옛 코드로 돌려 5건 실패 확인(`15 tests completed, 5 failed`).
2. 시험 먼저(Red 2): `BrokerOrderSubmitterTest`·`KisOrderAdapterTest` 신설, `OrderCommandServiceTest` 를 submitter 기준으로 옮기고 `GlobalExceptionHandlerTest` 에 KIS_ORDER_REJECTED 2건 추가 → `compileTestKotlin` 이 새 타입 미해결로 실패(오류 105줄) 확인.
3. 공용 타임아웃 판정: `infrastructure/client/TransportTimeouts`(원인 체인에 `SocketTimeoutException`·`HttpTimeoutException` 있을 때만 타임아웃). `TossTransportSupport.transportFailure` 가 이것을 부르고 자기 `causeChain`·`isTimeout` 은 지움(토스 동작 불변, 토스 시험으로 확인).
4. KIS 실패 분류 공용화: `infrastructure/kis/KisFailureClassifier`(internal object).
   - `isRateLimited(msgCd, msg1)` = msg_cd `EGW00201` 또는 msg1 에 «초당 거래건수». «초과» 단독은 안 봄.
   - `transportFailure`: `ResourceAccessException` + 원인 체인 타임아웃만 `KisTimeoutException`, 나머지 `RestClientException` 은 `KisUnavailableException`(S9-01).
   - `balanceFailure(rtCd, msgCd, msg1)`: 한도 → 인증(rt_cd 1·«인증»·token) → 권한(rt_cd 7·«권한») → 그 밖 KIS_UNAVAILABLE 순서(S9-02, 한도가 rt_cd 1 판정보다 앞).
   - `orderFailure`: `KisApiException` 은 `KisErrorCode` 로 가름(rt_cd 를 보지 않음) — 엄격 한도 판정 먼저, ORDER_REJECTED·INVALID_ACCOUNT·DUPLICATE_OR_ALREADY_PROCESSED → 400 `KIS_ORDER_REJECTED`, AUTH_FAILED → 503 KIS_AUTH_FAILED, RATE_LIMITED(엄격 판정 못 넘음) → 400 KIS_ORDER_REJECTED(§8 가정), TEMPORARY_UNAVAILABLE → KIS_UNAVAILABLE. `RestClientException` → `transportFailure`. `IllegalArgumentException`(placeOrder 의 ORNO `requireNotNull`) → KIS_UNAVAILABLE(가정). 그 밖 예외는 그대로.
   - 엄격 판정에 msg1 이 필요해 `KisApiException` 의 생성자 인자 `kisMessage` 를 `val` 로 노출(메시지 문자열·`mapKisErrorCode` 불변).
5. 잔고 어댑터: `ensureSuccess` 가 msg_cd 를 읽어 `balanceFailure` 로, 전송 실패 `catch` 둘을 `transportFailure` 하나로. 계좌번호 분해는 새 `kisAccountNumberParts`(infrastructure/kis 내부 함수, 잔고·주문 어댑터 공용)로 옮김 — ArchUnit 이 infrastructure → application 비포트 의존을 막아 application 의 `linkedKisAccountNumber` 는 못 씀.
6. 새 예외 `common/exception/KisOrderRejectedException`(KDoc 첫 줄 «A-1», `BadRequestException` 상속 — `TossRequestRejectedException` 과 같은 동결 400 기반, 메시지 고정 «KIS가 주문을 받지 않았습니다.», 원문은 cause). `GlobalExceptionHandler.handleApiDomain` 이 `fixedDomainMessage(code)` 를 먼저 보고 KIS_ORDER_REJECTED 는 고정 문구 «KIS가 주문을 받지 않았습니다. 주문 내용과 계좌 상태를 확인해 주세요.» 로 응답(예외 메시지 무관).
7. 포트·어댑터: `application/order/port/BrokerOrderPort`(supportedModes, `submit(order): BrokerOrderReceipt(externalOrderId)`). `infrastructure/kis/KisOrderAdapter`: 계좌 모드 전수 `when`(else 없음) KIS_PAPER → "paper", KIS_LIVE → "live", LOCAL·UPBIT_LIVE·TOSS_LIVE → `InternalErrorException(KIS_ORDER_MODE_UNSUPPORTED)`(토큰·KIS 호출 0) → 계좌번호 검사(없음·형식 틀림 → 409 KIS_ACCOUNT_NOT_LINKED, 호출 0) → `KisOrderRestClient.placeOrder`(파일 안 고침) → 실패는 `orderFailure`.
8. 접수기: `application/order/BrokerOrderPortTable`(잔고 표와 같은 모양, 한 모드 구현 둘이면 생성 실패)·`BrokerOrderSubmitter`(`@Service`, `List<BrokerOrderPort>` 주입, 전수 `routeOf`: KIS 둘 → 포트, TOSS_LIVE·UPBIT_LIVE → 501, LOCAL → 내부 오류. `init` 에서 «포트로 보내는 모드 = 표 키» 검사, 어긋나면 모드 이름과 함께 기동 실패). `submit` 은 옛 실행기와 같은 `@Transactional`(호출 트랜잭션 참여), 같은 조기 반환(`order.id`·`account.id` 없음), 같은 `runCatching` 묶음. 로그·Slack 문구는 옛 글자에서 «KIS_PAPER»/«KIS_LIVE» 만 `mode.name` 으로 치환. 실패 로그는 같은 글자에 예외를 붙여(`log.error(it)`) 고정 문구 예외 뒤의 KIS 원문이 로그에 남게 하고 예외는 그대로 다시 던짐.
9. `OrderCommandService`: 생성자의 두 실행기 → `BrokerOrderSubmitter`, `submitExternalOrderIfNeeded` 의 KIS 두 분기 → `brokerOrderSubmitter.submit(order)` 한 줄. TOSS_LIVE·UPBIT_LIVE 선행 거부(`rejectIfBrokerOrderNotSupported`)와 방어 분기 그대로, `placeOrder` 의 `@Transactional` 그대로라 접수 실패 예외가 밖으로 나가 주문·원장·잠금이 롤백됨(지금과 같음). 생성자 매개변수 8개라 detekt LongParameterList 를 생성자에만 `@Suppress`(이유 주석), 옛 baseline ID 1줄 제거.
10. 삭제: `KisPaperOrderExecutor.kt`·`KisLiveOrderExecutor.kt`(`cancel()`·`pollFills()` 호출 0건이라 함께 사라짐), `infrastructure/scheduler/KisPaperPollingScheduler.kt`(빈 아님, `pollFills()` 만 부르던 죽은 클래스, 실행기 삭제로 컴파일 불가 — development-plan §10.3 삭제 행).
11. ArchUnit 동결 저장소: `bb05cc60…` 에서 실행기 소유 10줄 제거, `OrderCommandService` 생성자 4줄은 매개변수 목록 글자만 새 서명으로 교체(같은 저장소 4개 의존, 새 의존 아님). `5b47cd17…` 에서 `KisPaperPollingScheduler` 3줄 제거.
12. 린트: 바꾼 파일만 `ktlintFormat -PinternalKtlintGitFilter=<바꾼 .kt>` 로 고침(다른 파일 변경 0 확인). ktlint `function-signature` 가 detekt 120자와 부딪히는 `GlobalExceptionHandler`·`KisAccountBalanceAdapter` 의 옛 식 본문 함수는 원래 모양으로 되돌리고 trailing comma 만 고친 뒤 `ktlintGenerateBaseline` 로 줄 번호를 맞춤(4파일 항목만 줄고 늘어난 항목 0). detekt baseline 에서 쓰는 곳이 사라진 2줄(`UnusedPrivateProperty:KisLiveOrderExecutor`, 옛 `OrderCommandService` 생성자 LongParameterList) 제거.
13. trading-web 재검색(읽기 전용): `grep -rn "orders" frontend/trading-web/src` 에서 시험 밖 0건. 시험 `src/shared/api/http.test.ts:80` 의 `requestJson("/api/orders", …)` 1건은 HTTP 보조 함수 시험의 임의 경로라 주문 API 호출부 아님. `KIS_ORDER_REJECTED`·`KIS_ACCOUNT_NOT_LINKED`·`INTERNAL_SERVER_ERROR`·`KIS_RESPONSE_PARSE_ERROR` 분기 0건.

옮긴 시험 목록(DEC-05 A, 새 포트 기준):

| 시험 | 바뀐 것 | 건수(메서드) |
|---|---|---|
| `application/order/OrderCommandServiceTest` | 목 `KisPaperOrderExecutor`·`KisLiveOrderExecutor` → 목 `BrokerOrderSubmitter`. «KIS_PAPER 주문은 paper executor를 호출한다»·«KIS_LIVE 주문은 live executor를 호출한다» → «…브로커 주문 접수기를 저장된 주문으로 한 번 부른다»(저장된 주문 인스턴스로 단언). 모드 누락 표 `KIS_PAPER_EXECUTOR`·`KIS_LIVE_EXECUTOR` → `BROKER_SUBMITTER`(계좌 모드로 단언), `verifyNoOrderSideEffects` 의 실행기 0회 → 접수기 0회. «KIS 접수 실패 예외는 바꾸지 않고 그대로 올린다» 신설 | 24 → 25 |
| `infrastructure/kis/KisAccountBalanceAdapterTest` | 목록 밖(S9-01·S9-02): 원인 없는 `ResourceAccessException("Read timed out")` 타임아웃 시험 → `SocketTimeoutException` 원인 포함, 실재 확인 안 된 rt_cd 9 + «rate limit exceeded» 한도 시험 → rt_cd 1 + EGW00201. 연결 거부·DNS·msg_cd 없는 «초당 거래건수»·«초과» 단독 신설 | 11 → 15 |
| `presentation/exception/GlobalExceptionHandlerTest` | 목록 밖: KIS_ORDER_REJECTED 400 고정 문구 2건 추가 | +2 |
| 신설 `application/order/BrokerOrderSubmitterTest` | 모드 표(키 = TradingMode 전체, 5모드), 성공 로그·Slack 글자(KIS 둘), 포트 실패 그대로·주문번호·Slack 없음, 선택 검사(누락·한 모드 둘·TOSS_LIVE/UPBIT_LIVE/LOCAL 지원) | 신설 |
| 신설 `infrastructure/kis/KisOrderAdapterTest` | 실제 `KisOrderRestClient` + 가짜 RestTemplate: 지원 모드, paper·live 주소·키·tr_id, 그 밖 모드 내부 오류·호출 0, 계좌 미연결 6형태 409·호출 0, 응답 실패 매핑 6행, rt_cd 없음, ORNO 없음, 전송 실패 5행 | 신설 |

시험 파일의 근거 주석: 옮긴 파일마다 `devkit:test-change` 줄(D26·S9-01·S9-02).

### 고침 라운드 1 (review/step11-code-review.md, 사람 결정 2026-11-21)

14. 시험 먼저: `KisOrderAdapterTest`(ORNO 없음 → 결과 미확인, 읽기 타임아웃·`HttpTimeoutException`·응답 변환 실패 → 결과 미확인, `HttpConnectTimeoutException`·연결 거부·DNS → KIS_UNAVAILABLE, HTTP 오류 6행), `KisAccountBalanceAdapterTest`(HTTP 500 + EGW00201 → 한도, HTTP 500 + 토큰 만료 → 인증, HTTP 500 빈 본문 → KIS_UNAVAILABLE), `GlobalExceptionHandlerTest`(KIS_ORDER_RESULT_UNKNOWN 고정 문구·J 범주), 시험 보조 `KisHttpErrorFixtures.kt`(`httpError`·`kisBody`). 새 예외 클래스만 먼저 만들고 돌려 13건 실패 확인.
15. C-01: 새 예외 `common/exception/KisOrderResultUnknownException`(KDoc 첫 줄 «J», `ServiceUnavailableException` 상속, code `KIS_ORDER_RESULT_UNKNOWN`), 핸들러 KIS 고정 문구 표에 «KIS 접수 결과를 확인하지 못했습니다. 다시 주문하기 전에 KIS 주문 내역을 확인해 주세요.» 1줄. `KisFailureClassifier.orderFailure` 를 주문 전용 분류로 나눔:
    - `ResourceAccessException`: 원인 체인에 `ConnectException`·`UnknownHostException`·`HttpConnectTimeoutException` 이 있으면 요청을 못 보낸 것 → KIS_UNAVAILABLE, 그 밖(읽기 타임아웃·`SocketTimeoutException` 전부·연결 끊김 등) → KIS_ORDER_RESULT_UNKNOWN.
    - `HttpStatusCodeException`: KIS 본문이면 확정 실패 분류(아래 C-02), 본문 없음·못 읽음이면 5xx → 결과 미확인, 4xx → KIS_UNAVAILABLE.
    - 그 밖 `RestClientException`(응답 변환 실패 등) → 결과 미확인.
    - `IllegalArgumentException`(rt_cd 0 인데 ORNO 없음) → 결과 미확인.
    - 잔고 경로 `transportFailure` 의 타임아웃 → KIS_TIMEOUT 은 그대로. 롤백 동작 불변(DEC-08).
16. C-02: `HttpStatusCodeException.responseBodyAsString` 을 Jackson 으로 읽어 `rt_cd` 가 있고 0 이 아니면 KIS 본문으로 봄(`KisOrderRestClient` 안 고침). 잔고는 `balanceFailure(rt_cd, msg_cd, msg1)`, 주문은 `mapKisErrorCode` + msg_cd 를 넣은 한도 판정. 본문이 없거나 못 읽으면 잔고 KIS_UNAVAILABLE, 주문은 15번 규칙.
17. `TransportTimeouts.causeChain` 을 공개(주문 분류가 «보내지 않음» 원인을 같은 체인으로 찾음).
18. C-08: `docs/TODO.md:123` 근거 위치를 `BrokerOrderSubmitter.kt:54-66`·`KisOrderAdapter.kt:24-25` 로(옛 파일 삭제 표기).
19. 계획서: development-plan §10.2 D28 행에 결과 미확인 code 문장·HTTP 오류 본문 분류 문장, §10.4 주문 행(395행)에 503 `KIS_ORDER_RESULT_UNKNOWN`, 각각 «(2026-11-21 step 11 리뷰 C-01·C-02, 사람 결정)».
20. C-03~C-07·C-09·C-10 은 지시대로 손대지 않음(원장 몫).
21. 사람 결정(2026-11-21): 주문 경로에서 HTTP 200 인데 본문에 rt_cd 가 없거나 본문이 null 이면 문구와 무관하게 KIS_ORDER_RESULT_UNKNOWN. `KisFailureClassifier.orderResponseFailure` 가 `kisRtCode == null` 을 한도·code 판정보다 먼저 봄(placeOrder 는 rt_cd 없음을 `KisApiException(rtCd=null)` 로 던짐). 잔고 경로는 그대로. 시험 먼저: `KisOrderAdapterTest` 의 «rt_cd 가 없는 응답은 KIS_UNAVAILABLE» 을 «rt_cd 가 없는 200 응답은 문구와 무관하게 결과 미확인»(빈 본문·«계좌» 문구 본문)으로 바꾸고 «본문이 없는 200 응답» 신설 → `31 tests completed, 2 failed` 확인 → 구현 → 통과. detekt ReturnCount 때문에 code 별 분기를 `orderFailureByCode` 로 뗌.

### 고침 라운드 3 (review/step11-rereview-1.md, 사람 결정 2026-11-21)

22. R1-02 시험 먼저: `KisOrderAdapterTest` HTTP 오류 표에 500 + EGW00002 «서버 에러가 발생 했습니다.» → 결과 미확인, 500 + rt_cd 1 주문 거부 문구 2종(«모의투자 장시작전», «주문가능금액을 초과») → 결과 미확인, 503 + «인증» 문구 → 인증 행 추가(기존 500 + EGW00201 → 한도, 500 + token 문구 → 인증, 400 + 주문 거부 본문 → KIS_ORDER_REJECTED 행 유지). `35 tests completed, 3 failed` 확인.
23. R1-02 구현: `KisFailureClassifier.orderHttpFailure` 가 5xx 를 `orderServerErrorFailure` 로 보냄 — 본문 없음 → 결과 미확인, `isRateLimited`(msg_cd EGW00201·«초당 거래건수») → KIS_RATE_LIMITED, msg1 «인증»·«token»(잔고 분류와 같은 문구 판정을 파일 수준 `isAuthMessage` 로 공용화, 새 msg_cd 값은 넣지 않음) → KIS_AUTH_FAILED, 그 밖 → KIS_ORDER_RESULT_UNKNOWN. 4xx·200 본문 분류와 잔고 경로는 그대로. detekt TooManyFunctions(object 11 한도) 때문에 `isAuthMessage`·`orderFailureByCode` 를 파일 수준 private 함수로 옮김.
24. R1-01(주석만): `IllegalArgumentException` → 결과 미확인 줄의 주석을 «ORNO 없음 외에 주문 전 토큰 발급 실패(requireNotNull)·빈 base URL 도 섞임, 보수 분류» 로 고침. 동작 변경 없음.
25. R1-03: `docs/TODO.md` P1 `kis-order-cancel-timeout` 항목에 자동 청산 재시도(`PositionExitTriggerOrchestrator.kt:170` `catch (Exception)`)가 `KIS_ORDER_RESULT_UNKNOWN` 도 같은 orderGroupId 로 최대 3회 재접수 → 실매도 중복 위험 한 줄. 코드는 안 고침.
26. 계획서: development-plan §10.2 D28 의 HTTP 오류 본문 분류 문장에 «주문 경로 5xx 는 한도·인증만 본문으로 확정, 나머지는 결과 미확인» 과 «(2026-11-21 step 11 재리뷰 R1-02, 사람 결정)».
27. 린트 중 사고: 분류기 한 파일에 `ktlintFormat -PinternalKtlintGitFilter` 를 돌렸더니 `GlobalExceptionHandler.kt`·`KisAccountBalanceAdapter.kt` 의 baseline 항목(식 본문 function-signature)까지 고쳐 detekt 120자에 걸림 → 두 파일을 HEAD + 이번 step 의 의미 변경(같은 스크립트)으로 다시 만들어 되돌림. 린트 exit 0 재확인.

---

## 6. Verification  (record)

| What | How it was checked | Result |
|---|---|---|
| Red 1 | `./gradlew test --offline --tests 'com.papertrading.api.infrastructure.kis.KisAccountBalanceAdapterTest'`(구현 전) | exit 1, `15 tests completed, 5 failed`(연결 거부·DNS·EGW00201·초당 거래건수·초과 단독) |
| Red 2 | `./gradlew compileTestKotlin --offline`(구현 전) | exit 1, 오류 105줄(`BrokerOrderSubmitter`·`BrokerOrderPort`·`BrokerOrderReceipt`·`KisOrderAdapter`·`KisOrderRejectedException` 미해결) |
| 집중 시험 | `./gradlew test --offline --tests 'com.papertrading.api.application.order.*' --tests 'com.papertrading.api.infrastructure.kis.*' --tests 'com.papertrading.api.infrastructure.toss.*' --tests 'com.papertrading.api.presentation.exception.*' --tests 'com.papertrading.api.architecture.*' --tests 'com.papertrading.api.application.account.*'` | 1차 exit 1, `635 tests completed, 1 failed`(시험 쪽 결함: 빈 문자열 `""` 에 `doesNotContain("")` — 단언 조건을 빈 값 제외로 고침) → 2차 exit 0 BUILD SUCCESSFUL |
| 특성화 시험 | `./gradlew test --rerun --offline --tests 'com.papertrading.api.characterization.kis.*'`, 시작 전 타임스탬프 파일보다 새 xml 만 집계 | exit 0, BUILD SUCCESSFUL in 2m 21s. xml 3개, 10건(3+4+3) 실패 0 오류 0 건너뜀 0 |
| 특성화 시험 무변경 | `git diff --stat -- backend/trading-api/src/test/kotlin/com/papertrading/api/characterization` | 변경 0 |
| 전체 시험 | `./gradlew test --rerun --offline`(backend/trading-api), 시작 전 타임스탬프 파일보다 새 xml 만 집계 | exit 0, BUILD SUCCESSFUL in 9m 50s. 새 xml 87개(디렉터리 전체 87개, 오래된 xml 섞임 0), 1160건 성공 / 실패 0 / 오류 0 / 건너뜀 0. 직전 1116건(xml 85개) 대비 +44건·xml +2(신설 2클래스), 새 실패 0. 파일별: BrokerOrderSubmitterTest 14, KisOrderAdapterTest 23, OrderCommandServiceTest 38, KisAccountBalanceAdapterTest 15, GlobalExceptionHandlerTest 31, OrderControllerIntegrationTest 17, ArchitectureRulesTest 15, ApplicationLayerRulesTest 6, TradingApiApplicationTests 3(앱 컨텍스트 기동 = 주문 포트 선택 검사가 기동을 깨지 않음) |
| 린트 | `./gradlew compileKotlin compileTestKotlin ktlintCheck detekt verifyLintBaselinesNotGrown -PlintBaselineRef=origin/main --offline` | exit 0, BUILD SUCCESSFUL. ktlint·detekt baseline origin/main 대비 늘어난 항목 0건. 경고는 기존 `fromHttpUrl` deprecated 1건 |
| 옛 실행기 사용처 | `grep -rn "KisPaperOrderExecutor\|KisLiveOrderExecutor\|KisPaperPollingScheduler" backend/trading-api/src`(동결 저장소 제외) | 코드 0건(`BrokerOrderSubmitter` KDoc 의 이력 표기 1건만) |
| application → KisOrderRestClient | `grep -rln "import com.papertrading.api.infrastructure.kis.KisOrderRestClient" …/application` | 0건 |
| 규칙 지목 | `rule-pointer.js --list`(새 main 파일 6개) | architecture·backend-api(팀·레포)·data-access·failure-handling 지목, 지목 조항 확인(§8 PTF3 행) |
| trading-web 호출부 | §5 13번 | 주문 API 호출 0건 → AE20 `App-Impact: none` 근거 |
| 고침 라운드 Red | 새 예외 클래스만 둔 뒤 `./gradlew test --offline --tests 'com.papertrading.api.infrastructure.kis.*' --tests '…GlobalExceptionHandlerTest'` | exit 1, `90 tests completed, 13 failed`(ORNO 없음, 전송 4행, HTTP 오류 5행, 잔고 HTTP 500 본문 2건, 핸들러 문구 1건) |
| 고침 라운드 집중 시험 | `--tests` infrastructure.kis·infrastructure.toss·presentation.exception·application.order·architecture | exit 0 BUILD SUCCESSFUL |
| 고침 라운드 린트 | 같은 린트 명령 | exit 0, ktlint·detekt baseline 늘어난 항목 0건 |
| 고침 라운드 전체 시험(특성화 포함) | `./gradlew test --rerun --offline`, 시작 전 타임스탬프 파일보다 새 xml 만 집계 | exit 1, 9m 18s. 새 xml 87개(전체 87개), 1171건 / 실패 1 / 오류 0. 실패 1건은 SnapshotJobServiceAtomicRollbackIntegrationTest 의 ApplicationContext 기동 실패(원인 PSQLException «The connection attempt failed», EOFException — Testcontainers Postgres 연결, 맥 부하), 이번 변경과 무관한 portfolio 시험. 그 클래스만 --rerun 재실행 exit 0(1건 통과). 특성화 kis 별도 --rerun exit 0, 새 xml 3개 10건(3+4+3) 실패 0, characterization 폴더 diff 0. 1차 1160건 대비 +11건(고침 라운드 시험), 새 실패 0 |
| 사람 결정 반영 뒤 전체 시험(특성화 포함) | `./gradlew test --rerun --offline`, 시작 전 타임스탬프 파일보다 새 xml 만 집계 | 1회차 exit 1, 9m 29s, 새 xml 87개, 1172건 / 실패 1 — SnapshotJobServiceAtomicRollbackIntegrationTest 컨텍스트 기동 실패(원인 RedisConnectionException «Unable to connect to localhost:50728», RedisProtocolException «Invalid first byte: 72 (H)» — 테스트 컨테이너 Redis 포트, 환경). 지시대로 그 클래스만이 아니라 전체를 다시 돌림(docker info: 29.5.2, 실행 컨테이너 0, 부하 4.88). 2회차 exit 0, BUILD SUCCESSFUL in 9m 42s, 새 xml 87개(디렉터리 전체 87개), 1172건 성공 / 실패 0 / 오류 0 / 건너뜀 0. 특성화 kis 3+4+3=10건 실패 0, characterization diff 0. 직전 1116건 대비 +56건, 새 실패 0 |
| 사람 결정 반영 뒤 린트 | 같은 린트 명령 | exit 0, ktlint·detekt baseline 늘어난 항목 0건 |
| 고침 라운드 3 Red | `./gradlew test --offline --tests '…KisOrderAdapterTest'`(구현 전) | exit 1, `35 tests completed, 3 failed`(500 + EGW00002, 500 + 주문 거부 문구 2종) |
| 고침 라운드 3 린트 | 같은 린트 명령 | exit 0, ktlint·detekt baseline 늘어난 항목 0건 |
| 고침 라운드 3 전체 시험(특성화 포함) | `./gradlew test --rerun --offline`, 시작 전 타임스탬프 파일보다 새 xml 만 집계 | exit 0, BUILD SUCCESSFUL in 10m 35s. 새 xml 87개(디렉터리 전체 87개), 1176건 성공 / 실패 0 / 오류 0 / 건너뜀 0(직전 1172건 대비 +4, R1-02 표 행). 특성화 kis 3+4+3=10건 실패 0, characterization diff 0. 환경 실패 없어 재실행 안 함 |

---

## 7. Result  (record)

- Done: KIS 주문 접수가 공통 주문 포트(`BrokerOrderPort` → `KisOrderAdapter`)와 접수기 하나(`BrokerOrderSubmitter`)로 감. 모드 → 구현 표와 기동 검사(D25)가 주문에도 적용. application 의 `KisOrderRestClient` import 0. 주문 경로 KIS 실패가 500 `INTERNAL_SERVER_ERROR`·400 `BAD_REQUEST`·400 `KIS_RESPONSE_PARSE_ERROR` 에서 D28 code(400 KIS_ORDER_REJECTED, 409 KIS_ACCOUNT_NOT_LINKED, 503 KIS_AUTH_FAILED·KIS_RATE_LIMITED·KIS_UNAVAILABLE·KIS_TIMEOUT)로 바뀜, 롤백 동작 불변. 잔고·주문 어댑터가 같은 분류(`KisFailureClassifier`)를 쓰고 S9-01·S9-02 해소, 토스·KIS 가 같은 타임아웃 판정 사용. 특성화 시험 10건 고치지 않고 통과.
- Changed files / PR: (커밋 전, backend/trading-api 기준)
  - 신설 main: application/order/BrokerOrderSubmitter.kt, application/order/BrokerOrderPortTable.kt, application/order/port/BrokerOrderPort.kt, infrastructure/kis/KisOrderAdapter.kt, infrastructure/kis/KisFailureClassifier.kt, infrastructure/kis/KisAccountNumberParts.kt, infrastructure/client/TransportTimeouts.kt, common/exception/KisOrderRejectedException.kt
  - 수정 main: application/order/OrderCommandService.kt, infrastructure/kis/KisAccountBalanceAdapter.kt, infrastructure/kis/KisApiException.kt(`kisMessage` val), infrastructure/toss/TossTransportSupport.kt, presentation/exception/GlobalExceptionHandler.kt
  - 삭제 main: application/order/KisPaperOrderExecutor.kt, application/order/KisLiveOrderExecutor.kt, infrastructure/scheduler/KisPaperPollingScheduler.kt
  - 신설 test: application/order/BrokerOrderSubmitterTest.kt, infrastructure/kis/KisOrderAdapterTest.kt
  - 수정 test: application/order/OrderCommandServiceTest.kt, infrastructure/kis/KisAccountBalanceAdapterTest.kt, presentation/exception/GlobalExceptionHandlerTest.kt
  - 수정 설정: src/test/resources/archunit_store/bb05cc60-ae2b-4a57-8f2e-cec4890e2499(10줄 제거·4줄 서명 교체), src/test/resources/archunit_store/5b47cd17-6b3b-427e-b590-4897afb99e7d(3줄 제거), config/ktlint/baseline.xml(4파일 항목 축소·줄 번호), config/detekt/baseline.xml(2줄 제거)
  - 문서: 이 파일(§4~8)
  - 고침 라운드 1: 신설 main common/exception/KisOrderResultUnknownException.kt, 신설 test infrastructure/kis/KisHttpErrorFixtures.kt, 수정 infrastructure/kis/KisFailureClassifier.kt·infrastructure/client/TransportTimeouts.kt·presentation/exception/GlobalExceptionHandler.kt·KisOrderAdapterTest.kt·KisAccountBalanceAdapterTest.kt·GlobalExceptionHandlerTest.kt, 문서 docs/TODO.md(123행)·development-plan.md(D28 행, 395행)

---

## 8. Problems found  (record)

> Found during the work — it can cause a new step, a change to an existing one, or a flag. Edit this step.md directly then (the reason goes in state.md §3).

| Problem | How it was handled |
|---|---|
| 지시 둘의 충돌: «RATE_LIMITED → KIS_RATE_LIMITED» 와 ««초과» 단독으로 한도 판정 금지». `mapKisErrorCode`(고치지 말 것)는 «초과»·rate·limit 문구만으로 RATE_LIMITED 를 붙여 «주문가능금액을 초과» 도 RATE_LIMITED 가 됨 | 가정(되돌리기 쉬움): 주문 경로는 엄격 판정(«초당 거래건수», 주문 응답엔 msg_cd 가 안 실려 문구만)을 넘을 때만 503 KIS_RATE_LIMITED, 못 넘는 RATE_LIMITED code 는 400 KIS_ORDER_REJECTED. 시험 `KisOrderAdapterTest` «주문가능금액을 초과 했습니다» 행이 근거. 사람 확인 요청 |
| 가정: ORNO 없는 성공 응답 → KIS_UNAVAILABLE. 주문 경로의 `IllegalArgumentException` 전부를 이것으로 봄(계좌번호 `requireNotNull` 은 사전 검사로 도달 불가) | 지시대로. 다른 IAE 원천이 생기면 같은 503 으로 감 |
| 잔고 경로 동작 변화: 옛 코드는 «rate»·«limit»·«초과» 문구(`mapKisErrorCode`)도 한도로 봤으나 이제 EGW00201·«초당 거래건수» 만 | S9-02 지시. 옛 시험(rt_cd 9 «rate limit exceeded») 1건을 `devkit:test-change` 로 교체 |
| 목록 밖 시험 2개를 고침: `KisAccountBalanceAdapterTest`(S9-01·S9-02 기준 교체 2건+신설), `GlobalExceptionHandlerTest`(+2) | 기대값을 낮춘 것이 아니라 KIS 실제 응답 모양으로 올림 |
| 계획 밖 삭제 1개: `KisPaperPollingScheduler.kt` | step-11 §1 에는 없으나 development-plan §10.3 삭제 행에 있고 실행기 삭제로 컴파일 불가. 동결 저장소 3줄도 함께 제거 |
| 동결 저장소 서명 교체 4줄: `OrderCommandService` 생성자 매개변수 목록이 바뀌어 같은 위반(application → persistence 저장소 4개)의 글자가 달라짐 | 클래스·의존 대상이 같고 줄 수 불변임을 확인하고 교체. `git diff` 로 검토 가능 |
| AE19 남은 것: `OrderCommandService` 의 `requireNotNull`(`IllegalArgumentException` → 핸들러 400 `BAD_REQUEST`)이 실제로는 범주 K 불변식. 이 파일을 건드려 AE19 상 정렬 대상 | 이번 범위(생성자·KIS 분기) 밖이고 응답 변경(AE20)이라 고치지 않음. 사람 결정 필요(AI 가 건드린 파일이라 `API-Errors-Deferred` 예외 대상 아님) — 후속 후보 |
| AE9 레벨: 주문 거부(사용자 입력 성격)도 접수기가 error 로 남김 | 지시 «로그 문구 글자 그대로» 를 따라 레벨도 유지. 후속 후보 |
| PTF3(타임아웃은 UNKNOWN 으로 두고 조회로 확정): KIS_TIMEOUT 이어도 지금처럼 주문 전체 롤백 — KIS 가 접수했을 수 있는 주문이 우리 쪽에 남지 않음 | 기존 동작(DEC-08 «롤백 동작 유지»), 이번에 바꾸지 않음. 위험으로 기록 |
| `BrokerOrderPortTable` 이 `BrokerBalancePortTable` 과 같은 모양(포트 타입만 다름) | 잔고 파일을 안 건드리려고 따로 둠. 제네릭 표 하나로 합칠지는 후속 후보 |
| 고침 라운드: 연결 타임아웃과 읽기 타임아웃 구분이 구현체마다 다름. 이 레포는 `RestTemplateBuilder` 가 classpath 로 구현체를 고르고(`RestTemplateConfig.kt`), JDK 구현은 연결 타임아웃을 `HttpConnectTimeoutException` 으로 주지만 Simple·Apache 는 연결 타임아웃도 `SocketTimeoutException`(«Connect timed out») 이라 예외 종류로 못 가름 | 보수적으로 `SocketTimeoutException` 은 전부 결과 미확인. 요청을 못 보낸 게 예외 종류로 확실한 `ConnectException`·`UnknownHostException`·JDK `HttpConnectTimeoutException` 만 KIS_UNAVAILABLE. 메시지 글자 판정은 쓰지 않음 |
| 고침 라운드: 그 밖 보수 판정 — 연결 끊김·TLS 오류 같은 나머지 `ResourceAccessException`, 응답 변환 실패 같은 그 밖 `RestClientException`, 본문 없는 5xx(502·HTML 포함)는 전부 결과 미확인 | 보내졌을 수 있어 재시도 안내를 하지 않음(PTF3). TLS 핸드셰이크 실패는 실제로는 미전송이지만 예외 종류를 늘리지 않고 보수 쪽에 둠 |
| 고침 라운드: 남은 불확정 — 200 응답인데 `rt_cd` 가 없으면(`KisErrorCode.KIS_TEMPORARY_UNAVAILABLE`) KIS_UNAVAILABLE 이었음 | 닫음: 사람 결정(2026-11-21)으로 주문 경로의 rt_cd 없음·본문 null 은 KIS_ORDER_RESULT_UNKNOWN(§5 21번). 잔고 경로는 그대로 KIS_UNAVAILABLE |
| 고침 라운드: HTTP 4xx 빈 본문 → KIS_UNAVAILABLE 은 «처리 전 거절» 가정 | 지시대로. KIS 가 4xx 로 접수 뒤 오류를 주는 사례는 확인 못 함(네트워크 차단) |
| 고침 라운드 3: 인증 판정은 msg1 문구(«인증»·«token»)만. KIS 의 인증 오류 msg_cd 값은 확인한 근거가 없어 넣지 않음 | 인증 msg_cd 목록은 실호출 대조 몫(1단계 §5 8번 수동 실호출과 함께) |
| 고침 라운드 3: 5xx + 주문 거부 문구(rt_cd≠0)도 결과 미확인 — KIS 가 실제로는 거부했을 수 있어 사용자가 내역 확인을 한 번 더 함 | 사람 결정(R1-02): 5xx 는 처리 안 한 게 확실한 한도·인증만 확정 |
| 후속(R1-01): 주문 경로 `IllegalArgumentException` 전부가 결과 미확인 — ORNO 없음 외에 주문 전 토큰 발급 실패·빈 base URL 같은 설정 오류도 «KIS 주문 내역을 확인» 안내로 나감 | 동작은 보수 쪽이라 그대로, 주석만 사실대로. 원인별로 가를지는 후속 |
| 후속(R1-03): 자동 청산 재시도가 결과 미확인도 같은 orderGroupId 로 최대 3회 재접수 | `docs/TODO.md` P1 `kis-order-cancel-timeout` 에 기록, 코드 안 고침 |
| 멈춤 조건에 걸린 것 | 없음(특성화 시험 수정 0, trading-web 주문 호출부 0, Docker 정상) |
