# Step 3 — 건드린 파일 에러 응답 정렬

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
| Step | 3 |
| Title | 건드린 파일 에러 응답 정렬 |
| Feature | multi-broker-kis-toss |
| Based on | development-plan §2 D5, D15, §5-2 후반, §6 에러 정렬 행, RISK-05, DEC-02 |
| Status | done |
| Owner |  |

---

**━━ Plan block (required before starting · from the planner / development-plan design) ━━**

## 1. Goal and scope  (plan)

> What this step (phase) **builds (what)** + **how far it goes (this step's boundary — name what is handed to the next step)**.

- Builds: 2단계에서 건드린 파일의 기존 check, require(400 BAD_REQUEST)를 범주 기반 예외로 옮겨 원인별 상태와 code로 응답한다
- Scope (where this step stops): OrderCommandService, ExecutionProcessor, Account, CommonApiExceptions, GlobalExceptionHandler만. KIS 502, 504 정렬(infrastructure/kis/**)은 2단계. trading-web은 읽기 전용 점검만, 수정이 필요하면 멈추고 ASK. 기존 동결 예외(ApiDomainException, 기존 CommonApiExceptions)는 옮기지 않음

---

## 2. Approach (how, in what order)  (plan)

> **By what method and in what order** this step (= phase) is solved — briefly (one line to a few). Based on the feature-spec / development-plan design.
> A rough level is enough (the detailed work log is §5). **Never leave it empty — the step's base is here.**

- 대상 파일의 사용자 입력·상태 검증 check, require를 전수 목록으로 만들고 경로마다 범주(A 400, F 409 등)를 정한다. 내부 불변식 requireNotNull은 대상 밖
- 경로마다 응답 상태와 code를 단언하는 실패 테스트를 먼저 쓴다(unit, integration)
- 새 범주 예외는 common에 범주·code·메시지만 두고 Spring을 import하지 않는다. 범주→HTTP 상태 매핑은 GlobalExceptionHandler가 맡는다(D15 수정). 2단계의 NotImplementedException도 이 모양으로 옮겨 ArchitectureRulesTest 실패 1건을 없앤다
- 범주 기반 예외로 옮겨 테스트를 통과시킨다. 없는 범주는 처음 쓰는 이 단계에서만 추가
- 바뀐 code를 AE20 목록으로 step 문서에 남기고 trading-web 주문·계좌 호출부를 읽기 전용으로 점검한다

---

## 3. Dependencies  (plan)

- Previous step: step 2 커밋 52db67b와 재리뷰
- Prerequisites: 없음

---

## 4. Completion criteria  (plan)

- [ ] 대상 파일에 400으로 뭉치는 check, require가 남지 않음
- [ ] 경로마다 상태와 code 단언 테스트 통과, 전체 테스트 실패 0, ArchitectureRulesTest 통과
- [ ] ktlintCheck, detekt, verifyLintBaselinesNotGrown 통과
- [ ] AE20 목록과 trading-web 점검 결과가 이 문서에 기록됨

---

**━━ Record block (filled in order while developing · never ahead) ━━**

## 5. Work log  (record)

> Fill it by spelling out the §2 approach as the actual work (concretely, what was done).

### 5-1. 대상 전수 목록과 경로별 범주·code 안 (2026-11-15, 시험 작성 전)

판단 근거: devkit backend-api AE1(범주표)·AE2(결정 순서)·AE6(한 예외 = 한 상황)·T12. 새 예외의 범주는 KDoc 첫 줄에 하위 상황 기호를 적는다. 메시지는 기존 문구를 그대로 둔다(프런트가 응답 본문을 그대로 화면에 보여주므로 문구가 바뀌면 사용자 화면이 바뀜).

레포 규칙 판정(멈춤 후보였던 항목): Account 도메인 엔티티가 common 예외를 쓰는 것은 PTB7의 «모든 계층 → 최상위 common»에 해당해 허용이다. Account.kt는 이미 BadRequestException, ConflictException, EntityMappingException을 import한다(기존 사용). 새 예외는 Spring 무의존이라 domain이 Spring을 우회 참조하는 새 경로도 생기지 않는다.

| # | 위치 | 상황 | 범주 | 새 예외(code) | 응답 |
|---|---|---|---|---|---|
| 1 | OrderCommandService:58 `check(isActive)` | 주문 대상 계좌가 이미 비활성 | F-2 | `AccountInactiveException`(`ACCOUNT_INACTIVE`) | 409 |
| 2 | OrderCommandService:79 `requireNotNull(limitPrice)` | LIMIT 주문에 가격 없음(사용자 입력) | A-2 | `LimitPriceRequiredException`(`LIMIT_PRICE_REQUIRED`) | 400 |
| 3 | OrderCommandService:82 `requireNotNull(expireAt)` | GTD 주문에 만료 시각 없음(사용자 입력) | A-2 | `ExpireAtRequiredException`(`EXPIRE_AT_REQUIRED`) | 400 |
| 4 | OrderCommandService:91 `check(orderableQuantity)` | 매도 가능 수량 부족(본인 잔량) | F-5 | `InsufficientOrderableQuantityException`(`INSUFFICIENT_ORDERABLE_QUANTITY`) | 409 |
| 5 | OrderCommandService:105 `check(stale)` | 시세가 60초 넘게 오래됨 | F-4 | `QuoteStaleException`(`QUOTE_STALE`) | 409 |
| 6 | OrderCommandService:259 `check(account.id)` | 다른 계좌의 주문 취소 시도 | E-2 | 기존 `OrderNotFoundException`(`ORDER_NOT_FOUND`) 재사용(OrderQueryService.ensureOrderOwnership과 같은 처리) | 404 |
| 7 | OrderCommandService:260 `check(status)` | 취소할 수 없는 상태(체결, 취소 완료 등) | F-2 | `OrderNotCancelableException`(`ORDER_NOT_CANCELABLE`) | 409 |
| 8 | ExecutionProcessor:156 `check(updatedRows == 1)` | 매도 반영 UPDATE가 조건(수량, 잠금 수량)을 못 맞춤. 쓰는 곳은 LocalMatchingEngine, KisExecutionNoticeService뿐이라 HTTP 응답 경로가 없고 잠금 수량 장부가 어긋났다는 우리 쪽 문제 | K-3 | `SellExecutionApplyFailedException`(`SELL_EXECUTION_APPLY_FAILED`) | 500 |
| 9 | Account:74 `addDeposit` amount<=0 | 입금 금액 0 이하 | A-1 | `InvalidAmountException`(`INVALID_AMOUNT`) | 400 |
| 10 | Account:102 `withdraw` amount<=0 | 출금 금액 0 이하 | A-1 | 위와 같음 | 400 |
| 11 | Account:103 `withdraw` 가용 예수금 부족 | 본인 잔액 부족 | F-5 | `InsufficientAvailableDepositException`(`INSUFFICIENT_AVAILABLE_DEPOSIT`) | 409 |
| 12 | Account:121 `lockDeposit` amount<=0 | 잠글 금액 0 이하. 부르는 곳은 OrderCommandService.recordBuyLockIfNeeded 하나이고 거기서 `lockedAmount > 0`일 때만 부르므로 지정가 가격 0 이하(DTO가 가격 양수를 검증하지 않음)도 이 줄에 닿지 않음 → 내부 계산값 약속이 깨진 경우만 남아 14와 같은 K-3(처음 A-1로 적었다가 호출 경로를 읽고 정정) | K-3 | `LedgerAmountInvalidException` | 500 |
| 13 | Account:122 `lockDeposit` 가용 예수금 부족 | 매수 주문의 본인 잔액 부족 | F-5 | 11과 같음(같은 상황이라 한 예외) | 409 |
| 14 | Account:140 `unlockDeposit` amount<=0 | 해제 금액 0 이하(내부 계산값) | K-3 | `LedgerAmountInvalidException`(`LEDGER_AMOUNT_INVALID`) | 500 |
| 15 | Account:141 `unlockDeposit` 잠금 예수금 부족 | 잠금 장부가 어긋남 | K-2 | `LockedDepositInsufficientException`(`LOCKED_DEPOSIT_INSUFFICIENT`) | 500 |
| 16 | Account:159 `confirmBuy` amount<=0 | 매수 확정 금액 0 이하(내부 계산값) | K-3 | 14와 같음 | 500 |
| 17 | Account:160 `confirmBuy` 잠금 예수금 부족 | 잠금 장부가 어긋남 | K-2 | 15와 같음 | 500 |
| 18 | Account:179 `receiveSellProceeds` amount<=0 | 매도 대금 0 이하(체결 수수료 계산 결과, 내부 계산값) | K-3 | 14와 같음 | 500 |
| 19 | Account:220 `rename` 공백 | 계좌명 비어 있음 | A-2 | `InvalidAccountNameException`(`INVALID_ACCOUNT_NAME`) | 400 |
| 20 | Account:229 `deactivate` 이미 비활성 | 이미 끝난 상태에 다시 비활성화 | F-2 | `AccountAlreadyInactiveException`(`ACCOUNT_ALREADY_INACTIVE`) | 409 |
| 21 | Account:234 `activate` 이미 활성 | 이미 활성 상태에 다시 활성화 | F-1 | `AccountAlreadyActiveException`(`ACCOUNT_ALREADY_ACTIVE`) | 409 |
| 22 | Account:253 `createRiskPolicy` 비율 범위 | maxPositionRatio가 0~1 밖 | A-1 | `InvalidRiskPolicyException`(`INVALID_RISK_POLICY`) | 400 |
| 23 | Account:256 `createRiskPolicy` maxDailyLoss<=0 | 값 범위 | A-1 | 22와 같음 | 400 |
| 24 | Account:259 `createRiskPolicy` maxOrderAmount<=0 | 값 범위 | A-1 | 22와 같음 | 400 |
| 25 | Account:343 `create` 계좌명 공백 | 19와 같은 상황 | A-2 | `InvalidAccountNameException` | 400 |
| 26 | Account:344 `create` 초기 예수금 음수 | 값 범위 | A-1 | `InvalidInitialDepositException`(`INVALID_INITIAL_DEPOSIT`) | 400 |
| 27 | Account:345 `create` 통화 코드 길이 | 형식 | A-1 | `InvalidBaseCurrencyException`(`INVALID_BASE_CURRENCY`) | 400 |
| 28 | Account:369 `validateTradingMode` | 계좌 타입과 거래 모드 조합이 올바르지 않음(잘못된 조합) | A-1 | `InvalidAccountModeCombinationException`(`INVALID_ACCOUNT_TYPE_TRADING_MODE`) | 400 |

대상 밖(사유 기록):
- requireNotNull 내부 불변식(OrderCommandService 59, 65, 101, 234, 235, 268, 286, 287, ExecutionProcessor 101~108, 139, 154, 217, 221, 225): 담당자 지시대로 대상 밖. 단 전역 핸들러가 IllegalArgumentException, IllegalStateException을 400 `BAD_REQUEST`로 뭉치는 매핑은 그대로 남음(핸들러의 전역 매핑 변경은 이 단계 범위 밖이라 §8에 후속 항목으로 올림).
- OrderCommandService:204 `require(triggerAuditInputs.isNotEmpty())`: 사용자 입력도 상태 검증도 아닌 호출 쪽 약속(PositionExitTriggerOrchestrator가 부름, HTTP 경로 없음). 지시서 대상 목록(58, 79, 82, 91, 105, 255~256)에도 없음.
- 지시서의 «255~256»은 실제 259~260(줄 이동)이며 위 6, 7번.

K 범주(500) 응답 모양: 새 `InternalErrorException`(K)은 핸들러가 내부 메시지를 응답에 싣지 않고 고정 문구 «서버 오류가 발생했습니다.»와 예외의 code를 보내며, 원문은 error 로그에 스택과 함께 남긴다(AE8, AE14).

새 예외 모양: 범주 I(`NotImplementedException`)와 K(`InternalErrorException`)는 common/exception에 파일 하나씩, Spring 무의존(`RuntimeException` 상속, `errorCode` 보유)이고 상태 매핑은 `GlobalExceptionHandler`가 한다. A, E, F 범주는 이미 있는 동결 기반 클래스(BadRequestException, NotFoundException, ConflictException)를 상속하는 새 클래스를 CommonApiExceptions.kt에 추가한다(기존 파일의 관행).

### 5-2. 한 일 (순서대로)

1. 호출 경로를 읽어 5-1 표를 확정(12번 lockDeposit은 호출 경로를 읽고 A-1에서 K-3으로 정정).
2. 예외 타입만 먼저 선언(`NotImplementedException`을 Spring 무의존 새 파일로 이동, `InternalErrorException` 신설, CommonApiExceptions.kt에 18개 추가). 동작은 아직 연결하지 않음.
3. 시험 작성: 신규 `GlobalExceptionHandlerTest`(범주 I, K 매핑), AccountTest(기존 IAE, ISE 단언을 예외 타입, code, 상태 단언으로 교체, 기준통화·maxOrderAmount 시험 추가), OrderCommandServiceTest(경로별 8건 추가, 501 단언에서 `ex.status` 줄은 핸들러 시험으로 이동), ExecutionProcessorTest(1건), OrderControllerIntegrationTest(경로별 7건 + 기존 2건에 code 단언), AccountControllerIntegrationTest(출금 초과 409로 정정 + 이중 비활성화 409 추가), RiskPolicyServiceTest(IAE를 새 예외로).
4. Red 확인(아래 §6).
5. 구현: `GlobalExceptionHandler`에 범주 I(501), K(500) 핸들러 추가(K는 고정 문구, 원문은 error 로그), Account 20곳, OrderCommandService 7곳, ExecutionProcessor 1곳을 새 예외로 교체. 복잡도, 던지기 개수 규칙 때문에 Account에 `validateRiskPolicy`, `validateCreationInputs`(위반 선택 뒤 한 곳에서 던짐), ExecutionProcessor에 `ensureSellApplied`(검사만 분리, 레포지토리 호출은 `processFill`에 그대로) 추출.
6. 린트 정리: ktlint `class-signature`는 상위 타입 호출을 한 줄로 쓰게 하고 detekt는 120자 제한이라, 한 줄에 들어가면 한 줄로, 넘으면 이 파일 앞부분에서 통과하는 `) : Base(...)` 형태로 작성. 줄 이동으로 어긋난 ktlint 기준선만 재생성(항목 수 762 그대로, (파일, 규칙)별 개수 변화 0).
7. 프런트 읽기 전용 점검, AE20 목록 작성(아래 5-3).

### 5-3. AE20 목록 (바뀐 응답, 호출자 점검)

기준 커밋 HEAD(a43538a) 대비. 모두 이전에는 전역 핸들러가 400 `BAD_REQUEST`로 뭉쳐 보냈음(`IllegalArgumentException`, `IllegalStateException`). 엔드포인트: `POST /api/v1/accounts/{id}/orders`(주문), `DELETE /api/v1/accounts/{id}/orders/{orderId}`(취소), `POST /api/v1/accounts`(계좌 생성), `POST .../deposit`, `POST .../withdraw`, `DELETE /api/v1/accounts/{id}`(비활성화), 리스크 정책 등록.

| 경로 | 이전 | 이후 |
|---|---|---|
| 비활성 계좌로 주문 | 400 BAD_REQUEST | 409 ACCOUNT_INACTIVE |
| 지정가 주문 가격 없음 | 400 BAD_REQUEST | 400 LIMIT_PRICE_REQUIRED |
| GTD 주문 만료 시각 없음 | 400 BAD_REQUEST | 400 EXPIRE_AT_REQUIRED |
| 매도 가능 수량 부족 | 400 BAD_REQUEST | 409 INSUFFICIENT_ORDERABLE_QUANTITY |
| 시장가 매수 시세가 60초 넘게 오래됨 | 400 BAD_REQUEST | 409 QUOTE_STALE |
| 다른 계좌 주문 취소 | 400 BAD_REQUEST | 404 ORDER_NOT_FOUND(메시지가 «해당 계좌의 주문이 아닙니다.»에서 «주문을 찾을 수 없습니다. orderId=N»으로 바뀜) |
| 취소 불가 상태 주문 취소 | 400 BAD_REQUEST | 409 ORDER_NOT_CANCELABLE |
| 입금, 출금 금액 0 이하 | 400 BAD_REQUEST | 400 INVALID_AMOUNT |
| 출금, 매수 주문 가용 예수금 부족 | 400 BAD_REQUEST | 409 INSUFFICIENT_AVAILABLE_DEPOSIT |
| 계좌 생성, 이름 변경 계좌명 공백 | 400 BAD_REQUEST | 400 INVALID_ACCOUNT_NAME |
| 계좌 생성 초기 예수금 음수 | 400 BAD_REQUEST | 400 INVALID_INITIAL_DEPOSIT |
| 계좌 생성 통화 코드 길이 | 400 BAD_REQUEST | 400 INVALID_BASE_CURRENCY |
| 계좌 타입과 거래 모드 조합 오류 | 400 BAD_REQUEST | 400 INVALID_ACCOUNT_TYPE_TRADING_MODE(메시지 끝에 타입, 모드 값이 덧붙음) |
| 이미 비활성화된 계좌 비활성화 | 400 BAD_REQUEST | 409 ACCOUNT_ALREADY_INACTIVE |
| 이미 활성화된 계좌 활성화 | 400 BAD_REQUEST | 409 ACCOUNT_ALREADY_ACTIVE |
| 리스크 정책 값 범위 오류 | 400 BAD_REQUEST | 400 INVALID_RISK_POLICY |
| 잠금 예수금 장부 어긋남, 내부 금액 0 이하, 매도 반영 실패(내부 결함. 단 잠금 예수금 부족은 LOCAL 시장가 매수 뒤 시세 상승으로 정상 사용에서도 닿음, §8) | 400 BAD_REQUEST(메시지 노출) | 500 LOCKED_DEPOSIT_INSUFFICIENT, LEDGER_AMOUNT_INVALID, SELL_EXECUTION_APPLY_FAILED(메시지는 «서버 오류가 발생했습니다.»로 고정, 원문은 error 로그) |
| 501 BROKER_ORDER_NOT_SUPPORTED | 501 | 501 그대로(매핑만 예외에서 핸들러로 이동) |

프런트 읽기 전용 점검(frontend/trading-web/src): `entities/order/api/orderApi.ts`, `entities/order/model/types.ts`, `entities/account/api/accountApi.ts`, `shared/api/index.ts`, `features/order-form/ui/OrderFormPanel.tsx`, `features/order-list/ui/OrderTablePanel.tsx`를 읽고 `status`, `code`, 메시지 문자열 비교를 검색함. 모든 호출이 실패 시 `` `${status} ${statusText}: ${본문}` `` 문자열로 `Error`를 던지고 화면은 그 `message`를 알림에 그대로 표시할 뿐, 상태 숫자나 code로 분기하는 곳이 없음. 유일한 문자열 분기 `PortfolioChartPage.tsx:46`(`message.includes("404")`)은 포트폴리오 벤치마크 호출이라 이번 변경 경로가 아님. 결과: 프런트 수정 불필요(DEC-02 멈춤 조건에 해당 없음). 사용자 눈에 보이는 변화는 알림 앞머리의 상태 숫자(400이 409, 404, 500으로)와 본문 JSON의 `code`, K 범주 메시지 고정 문구뿐. App-Impact 문구 후보: `App-Impact: checked — frontend/trading-web/src/entities/order/api/orderApi.ts, entities/account/api/accountApi.ts, shared/api/index.ts, features/order-form/ui/OrderFormPanel.tsx, features/order-list/ui/OrderTablePanel.tsx`.

---

## 6. Verification  (record)

시험 범위: 코디네이터 정정(2026-11-15)에 따라 전체 스위트는 돌리지 않음(커밋 게이트, CI 몫). 관련 묶음만 실행했고 판정은 `build/test-results/test/*.xml` 집계.

| What | How it was checked | Result |
|---|---|---|
| Red 1(타입만 선언, 핸들러 매핑 없음) | `./gradlew compileTestKotlin` | 컴파일 실패 4건, 전부 `GlobalExceptionHandlerTest`의 `handleNotImplemented`, `handleInternalError` 미해결(다른 시험은 컴파일됨) |
| Red 2(핸들러 매핑만 추가, Account, 주문 코드는 옛 상태) | `./gradlew test --tests AccountTest, OrderCommandServiceTest, ExecutionProcessorTest, RiskPolicyServiceTest, GlobalExceptionHandlerTest` | 130건 중 36건 단언 실패(AccountTest 26, OrderCommandServiceTest 8, ExecutionProcessorTest 1, RiskPolicyServiceTest 1), `GlobalExceptionHandlerTest` 4건은 통과 |
| Green, 관련 묶음 | 위 5개 + `ArchitectureRulesTest`, `OrderControllerIntegrationTest`, `AccountControllerIntegrationTest` 1회 | 154건, 실패 0, 오류 0, 건너뜀 0(RiskPolicyServiceTest 5, ExecutionProcessorTest 15, OrderCommandServiceTest 35, ArchitectureRulesTest 13, AccountTest 58, AccountControllerIntegrationTest 7, OrderControllerIntegrationTest 17, GlobalExceptionHandlerTest 4) |
| ArchitectureRulesTest | 같은 실행 | 13건 통과, `commonDoesNotDependOnJpaOrSpring` 포함(step 2 이후 실패하던 1건 해소). 동결 저장소(`src/test/resources/archunit_store`) 변경 0 |
| 501 응답 유지 | `OrderControllerIntegrationTest` TOSS_LIVE, UPBIT_LIVE 5건 + `GlobalExceptionHandlerTest` | 501 `BROKER_ORDER_NOT_SUPPORTED` 그대로 통과 |
| ktlint, detekt, 기준선 증가 | `./gradlew ktlintCheck detekt verifyLintBaselinesNotGrown -PlintBaselineRef=origin/main` | 통과. ktlint 기준선은 줄 이동 반영만(항목 762, HEAD 대비 (파일, 규칙)별 개수 변화 0), detekt 기준선 변경 0 |
| test-gate | `node .../tools/test-gate.js --select --run <바꾼 파일>` | «범위 없음 — 바뀐 파일에 대응 테스트가 없습니다»로 끝남(Kotlin 변경에 대응 시험을 못 고름). 그래서 위 관련 묶음을 gradle로 직접 실행 |
| 전체 스위트(S3S-03) | CI `full-test` 실행 43682358838, 커밋 `7111818`, 2026-11-15 | success. 작업 trading-api, trading-web, collector-api 모두 success. 직전 `a43538a` 실행(43637012955)은 `ArchitectureRulesTest > commonDoesNotDependOnJpaOrSpring` 1건 실패였고 이 커밋에서 해소 |

---

## 7. Result  (record)

- Done: 5-1 표의 28개 경로를 범주 기반 예외로 정렬. 커밋 `7111818`, 원격 푸시, CI 전체 스위트 success. Status done은 사람이 2026-11-15 이어받기에서 확인한 계획대로 처리
- 남은 후속: 시험 보강 S3Q-01~03(should-fix, deferred)은 별도 커밋으로 진행
- Changed files / PR: 커밋 `7111818`(PR은 1단계 끝에). 목록은 아래
  - main: `common/exception/NotImplementedException.kt`(신규, Spring 무의존), `common/exception/InternalErrorException.kt`(신규), `common/exception/CommonApiExceptions.kt`, `presentation/exception/GlobalExceptionHandler.kt`, `domain/entity/account/Account.kt`, `application/order/OrderCommandService.kt`, `application/order/ExecutionProcessor.kt`
  - test: `presentation/exception/GlobalExceptionHandlerTest.kt`(신규), `domain/entity/account/AccountTest.kt`, `application/order/OrderCommandServiceTest.kt`, `application/order/ExecutionProcessorTest.kt`, `application/account/RiskPolicyServiceTest.kt`, `presentation/controller/OrderControllerIntegrationTest.kt`, `presentation/controller/AccountControllerIntegrationTest.kt`
  - 설정: `backend/trading-api/config/ktlint/baseline.xml`(줄 이동만)

---

## 8. Problems found  (record)

> Found during the work — it can cause a new step, a change to an existing one, or a flag. Edit this step.md directly then (the reason goes in state.md §3).

| Problem | How it was handled |
|---|---|
| 전역 핸들러가 `IllegalArgumentException`, `IllegalStateException`을 400 `BAD_REQUEST`로 뭉치는 매핑이 그대로 남음. 대상 밖으로 둔 requireNotNull 내부 불변식(OrderCommandService 59, 65, 101, 234, 235, 268, 286, 287, ExecutionProcessor 101~108, 139, 154, 217, 221, 225)과 OrderCommandService:204 `require`, 이번에 안 만진 파일(Order, Position 등)의 check, require가 이 경로를 탐 | open-questions 후보: 핸들러의 전역 매핑 변경은 AE20 대상이고 접촉 파일이 크게 늘어 이 단계 범위 밖. 후속 단계 또는 별도 안건으로 사람 결정 필요 |
| (정정, 명세 리뷰 S3S-04) 처음엔 «지정가 매수 `limitPrice` 0 이하가 잠금 없이 접수됨»으로 적었으나 사실과 다름. `saveOrder`가 거치는 `Order.create`(`Order.kt:184` `require(limitPrice == null \|\| limitPrice > 0)`)가 잠금 호출보다 먼저 막아 400으로 거부됨 | 결함 아님. 12번 행(lockDeposit)을 K-3으로 둔 결론은 그대로(호출부가 `lockedAmount > 0`일 때만 부름) |
| (코드 리뷰 S3C-04) K-2 `LOCKED_DEPOSIT_INSUFFICIENT`는 정상 사용에서도 닿음. LOCAL 시장가 매수는 주문 시점 시세로 잠그고 다음 틱 시세로 체결하므로, 시세가 오르면 `confirmBuy`가 잠금 부족으로 던짐 | 잠금 부족 자체는 이 단계 전부터 있던 결함이라 고치지 않음(금융 동작 변경). 후속 안건으로 넘김 |
| 기존 시험 단언을 바꿈: OrderCommandServiceTest의 `ex.status == NOT_IMPLEMENTED` 줄 제거, AccountTest의 `IllegalArgumentException`, `IllegalStateException` 단언을 새 예외로 교체, AccountControllerIntegrationTest 출금 초과 400 BAD_REQUEST를 409로, RiskPolicyServiceTest의 IAE를 새 예외로, 모드별 허용 표 시험의 실패 메시지 문구 제거 | 근거: 계획 D5, D15와 DEC-02(사람 승인)가 응답 범주를 바꾸도록 정한 스펙 변경이라 `스펙, 시험, 코드` 순서를 따름. 단언은 더 구체적(타입, code, 상태)이고 약해진 것 없음. 501 상태 단언은 `GlobalExceptionHandlerTest`와 주문 통합 시험으로 이동. 모드 표 시험의 `"거부되어야 함: $type + $mode"` 메시지는 도우미 시그니처 때문에 사라졌고 단언 자체(예외 타입, code, 400)는 같은 조합 전부에 그대로 적용됨 |
| `test-gate.js --select --run`이 Kotlin 변경에 대응 시험을 못 골라 «범위 없음»으로 끝남 | 지시된 관련 묶음을 gradle로 직접 실행해 대체. devkit 쪽 Kotlin 시험 매핑은 별도 제안 후보 |
| 처음 레포지토리 호출을 새 private 메서드로 옮겼다가 ArchUnit `applicationDoesNotDependOnInfrastructureOrPresentation`이 동결 항목과 달라져 실패 | 호출은 `processFill`에 원래대로 두고 검사(`ensureSellApplied`)만 분리해 해소. 동결 저장소는 건드리지 않음 |
| `InvalidAccountModeCombinationException` 메시지에 `$accountType, $tradingMode`를 덧붙임(한 줄 120자 제한과 ktlint `class-signature`를 동시에 만족하려고 매개변수를 받는 형태로 씀) | 사용자 메시지가 기존 문구 + 값 두 개로 늘어난 것을 AE20 표에 기록. 문구 자체의 변경은 아님 |
| serena Kotlin 언어 서버가 시작되지 않음(`replace_content`, `find_symbol` 실패) | 글자 검색(범위 명시)과 내장 편집 도구로 진행. 코드 이름 검색 훅은 안내대로 같은 명령 재실행 |
