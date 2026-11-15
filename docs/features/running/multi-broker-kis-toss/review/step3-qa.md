# 3단계 QA — 범주 예외 이동

- 대상: worktree 미커밋 변경(기준 HEAD a43538a) `backend/trading-api` — 본 코드 7개 파일, 시험 7개 파일(신규 `GlobalExceptionHandlerTest` 포함)
- 역할: review-panel QA 자리(Tester). 코드는 고치지 않았고 결함은 보고만 합니다.
- 기준: `steps/step-3.md` §4 완료 기준, §5-1 경로표(28개 경로), §5-3 바뀐 응답 목록
- 경로 표기: `T/` = `backend/trading-api/src/test/kotlin/com/papertrading/api/`, `M/` = `backend/trading-api/src/main/kotlin/com/papertrading/api/`

## 1. 범위와 확인한 것

| 볼 것 | 결과 |
|---|---|
| 경로마다 상태·code 단언이 틀린 구현을 잡는가 | 돌연변이 7개 중 5개는 RED, 2개는 초록 그대로(취소 검사 순서, 500 핸들러 등록). 1개는 의도와 다른 이유로 RED(아래 2-2) |
| 거부 경로에서 금융 상태가 그대로인지 | 단위(AccountTest)는 잔액·잠금 불변을 단언함. 통합 시험은 거부 뒤 DB 상태(주문 행, 예수금, 잠금)를 하나도 단언하지 않음 |
| 500 응답이 내부 메시지를 숨기는지 | 핸들러 메서드를 직접 부르는 단위 시험만 있음(고정 문구, `accountId` 미포함 단언). HTTP 경로로 500을 거치는 시험은 없음 |
| 경계값(금액 0, 음수) | 입금·출금은 0과 음수, 잠금·해제·확정·매도 대금은 0만. 시세 60초 경계, 통화 코드 2자리는 없음 |
| 같은 요청의 400/409 우선순위 | 시험으로 못박혀 있지 않음(목 엄격성으로 우연히 걸림) |
| 다른 계좌 주문 취소 404 | 취소 가능한 PENDING 주문으로만 시험함. 취소 불가 상태인 남의 주문에서 404가 409보다 먼저인지는 안 봄 |
| 변경 밖 호출부 | 옛 `IllegalStateException`·`IllegalArgumentException`을 따로 잡는 본 코드 경로 없음(`TaxSummaryCommandService:105`의 IAE catch는 이번 경로와 무관), 변경 안 된 시험 중 옛 문구·타입을 단언하는 곳 없음(grep) |

## 2. 실행 기록

작성자 증거(8개 클래스 154건 실패 0, 2026-11-14T20:51Z xml)를 먼저 읽고, 전체 스위트는 돌리지 않았습니다. 원본 작업본은 건드리지 않고 `backend/trading-api`를 scratchpad 격리 사본으로 복사해 그 사본에서만 실행했습니다. 판정은 `build/test-results/test/*.xml` 집계입니다.

### 2-1. 기준 실행(돌연변이 없음)

- 명령: 사본에서 `./gradlew test --tests '*OrderCommandServiceTest' --tests '*GlobalExceptionHandlerTest' --tests '*.AccountTest' --tests '*ExecutionProcessorTest'`
- exit 0. OrderCommandServiceTest 35, AccountTest 58, ExecutionProcessorTest 15, GlobalExceptionHandlerTest 4, ArchitectureRulesTest 13(함께 실행됨) — 실패 0, 오류 0

### 2-2. 돌연변이 실행(사본, 2026-11-15T04:41~04:43Z)

| 돌연변이 | 바꾼 곳 | 결과 | 잡은 시험 |
|---|---|---|---|
| M1 취소 검사 순서 뒤집기(상태 409 검사를 소유 404 검사 앞으로) | `M/application/order/OrderCommandService.kt:266-269` | 못 잡음 — 단위 35건, 통합 17건 모두 초록 | 없음 |
| M2 시세 오래됨을 옛 `check`(IllegalStateException)로 되돌림 | `OrderCommandService.kt:111-113` | 잡음 | `시장가 매수 — 시세가 60초를 넘게 오래되면 409 QUOTE_STALE` |
| M3 입력 검증(400)을 비활성 검사(409) 뒤로 이동 | `OrderCommandService.kt:61-64` | 단위는 RED지만 원인이 `MockKException`(계좌 조회 스텁 없음), 우선순위 단언 아님. 통합은 초록 | 우연히 걸림 |
| M4 `@ExceptionHandler(InternalErrorException::class)` 제거 | `M/presentation/exception/GlobalExceptionHandler.kt:34` | 못 잡음 — GlobalExceptionHandlerTest 4건 초록 | 없음 |
| M5 `AccountInactiveException` 범주 409→400 | `M/common/exception/CommonApiExceptions.kt` | 잡음 | 단위 `비활성 계좌 주문 — 409 ACCOUNT_INACTIVE`, 통합 `비활성화된 계좌의 주문은 409 ACCOUNT_INACTIVE` |
| M6 `LockedDepositInsufficientException` 범주 K→409 | 같은 파일 | 잡음(시험 컴파일 실패 — `GlobalExceptionHandlerTest.kt:49` 타입 불일치) | 컴파일 |
| M7 출금 잔액 부족을 옛 `require`(IllegalArgumentException)로 되돌림 | `M/domain/entity/account/Account.kt:114` | 잡음 | AccountTest 2건(출금, recordWithdrawal) |

- M6이 컴파일을 막아 첫 실행은 판정 불가였고, 사본에서 M6만 되돌린 뒤 M1~M5, M7로 다시 돌렸습니다(exit 1, OrderCommandServiceTest 실패 4, AccountTest 실패 2, GlobalExceptionHandlerTest 실패 0).
- 통합: 같은 사본에서 `./gradlew test --tests '*OrderControllerIntegrationTest'`(Docker 29.5.2) — exit 1, 17건 중 실패 1(M5의 비활성 계좌 시험뿐). M1, M3은 통합에서도 초록.
- 각 돌연변이는 서로 다른 경로라 시험별 결과가 섞이지 않습니다.
- 원본 보존 관측: 실험 뒤 `git diff --numstat HEAD -- backend/`가 실험 전과 같은 12개 파일·줄 수, `revert-probe.js` exit 0.

## 3. 지적

| ID | file:line | 근거 조항 | 무엇이 틀렸나 | 등급 | 출처 | 회귀 |
|---|---|---|---|---|---|---|
| S3Q-01 | `T/application/order/OrderCommandServiceTest.kt:340`, `T/presentation/controller/OrderControllerIntegrationTest.kt:350` (대상 `M/application/order/OrderCommandService.kt:266-269`) | TT32 | 다른 계좌 주문 취소 시험이 둘 다 취소 가능한 PENDING 주문을 씀. 소유 검사(404)와 상태 검사(409) 순서가 뒤집혀도 초록이라, 남의 체결·취소 완료 주문에 409 `ORDER_NOT_CANCELABLE`과 `status=FILLED` 같은 값이 나가 주문 존재와 상태가 새는 회귀를 못 잡음. 남의 계좌의 취소 불가 주문(예: 이미 취소된 주문)을 다른 계좌로 취소해 404를 단언하는 시험이 필요함 | should-fix | 이번 변경(404 숨김 신설) | M1 못 잡음(단위·통합) |
| S3Q-02 | `T/presentation/exception/GlobalExceptionHandlerTest.kt:36` (대상 `M/presentation/exception/GlobalExceptionHandler.kt:34`) | TT9 | 500 숨김 시험이 핸들러 메서드를 직접 불러 `@ExceptionHandler` 등록 자체를 검증하지 않음. 범주 K는 HTTP로 닿는 경로가 있음(주문 취소 → `recordBuyUnlock` → `unlockDeposit`에서 잠금 장부가 어긋나면 `LOCKED_DEPOSIT_INSUFFICIENT`). 등록이 빠지면 Spring 기본 오류 본문이 나가 `code`가 사라져도 초록. MockMvc로 잠금 예수금을 어긋나게 만든 뒤 취소해 500, `code`, 고정 문구, 원문 미노출을 단언하는 통합 시험 1건이 필요함 | should-fix | 이번 변경(K 핸들러 신설) | M4 못 잡음(단위). 통합에는 K 경로 시험이 없음(읽기 근거) |
| S3Q-03 | `T/presentation/controller/OrderControllerIntegrationTest.kt:305, 320, 350, 362`, `T/application/order/OrderCommandServiceTest.kt:327` | TT9 | 거부 경로 통합 시험이 상태·code만 보고 DB 금융 상태를 단언하지 않음. 특히 매수 예수금 부족은 주문을 먼저 저장하고(`OrderCommandService.kt:150` 잠금이 `orderId`를 씀) 잠금에서 던지므로 롤백에만 기대는데, 주문 행 0건과 가용 예수금 불변을 보지 않음. 이중 취소(362)는 두 번째 취소 뒤 잠금·가용 예수금이 두 번 풀리지 않았는지, 남의 계좌 취소(350)는 원래 주문이 PENDING이고 잠금이 그대로인지, 매도 수량 부족(320)은 포지션 잠금 수량 불변을 보지 않음. 단위 예수금 부족 시험(327)도 잔액 단언이 없음(AccountTest가 `lockDeposit` 쪽은 단언함). 시험 이름 관례 `재화부족_강화시도_실패하고차감없음`(TT21)의 «차감없음» 부분이 빠짐 | should-fix | 이번 변경(새 시험) | 실험 안 함(롤백 설정 돌연변이는 통합 환경이 필요해 읽기 근거) |
| S3Q-04 | `T/application/order/OrderCommandServiceTest.kt:262` (대상 `OrderCommandService.kt:61-64`) | TT32 | 같은 요청에 400(가격 없음)과 409(비활성 계좌)가 겹칠 때 무엇이 이기는지 시험이 없음. 검증을 비활성 검사 뒤로 옮기면 단위 시험은 목 스텁 부재로 우연히 RED, 통합은 초록. 현재 순서(400 먼저)가 의도라면 비활성 계좌 + 가격 없음 요청으로 400을 단언하고, 의도가 정해지지 않았다면 open-questions로 올림 | nit | 이번 변경(기존 순서 유지) | M3 단위는 다른 이유로 RED, 통합 못 잡음 |
| S3Q-05 | `T/application/order/OrderCommandServiceTest.kt:306` (대상 `OrderCommandService.kt:111`) | TT48 | 시세 오래됨 시험이 120초만 봄. 60초(허용)와 61초(거부) 경계가 없어 `>= 60`이나 `> 30` 같은 문턱 회귀를 못 잡음. 서비스가 `Instant.now()`를 직접 불러 경계 시험을 결정적으로 쓸 수 없음(시계 주입 필요 — 본 코드 변경이라 사람 결정) | nit | 기존 동작 + 이번 새 시험 | 실험 안 함(읽기 근거) |
| S3Q-06 | `T/domain/entity/account/AccountTest.kt:515`, `:479`, `:308` | TT32 | 경계 축 누락: 잠금·해제·확정·매도 대금은 0만 보고 음수가 없음, 기준 통화는 4자리만 보고 2자리·빈 문자열이 없음, `maxPositionRatio`는 1.1만 보고 음수가 없음(범위 하한 회귀 미검출) | nit | 이번 변경(통화) + 기존(비율·음수) | 실험 안 함 |
| S3Q-07 | `T/presentation/exception/GlobalExceptionHandlerTest.kt:36` | `label:원문-로그` | 500 응답에서 원문을 숨기는 대신 error 로그에 스택과 함께 남긴다는 약속(step-3 §5-1, backend-api AE14 «Log the original»)을 시험이 보지 않음. 로그 줄이 빠져도 초록 | nit | 이번 변경 | 실험 안 함 |

## 4. 요약

- 막는 지적(blocker) 0건, should-fix 3건, nit 4건.
- 범주를 바꾸거나 옛 `IllegalStateException`·`IllegalArgumentException`으로 되돌리는 회귀는 단위 시험이 잡습니다(M2, M5, M6, M7).
- 못 잡는 것은 두 곳입니다. 다른 계좌 취소의 404/409 순서(M1)와 500 핸들러 등록(M4). 거부 뒤 DB 금융 상태는 통합 시험에서 단언하지 않습니다.
- 넘길 곳: S3Q-01~03은 Maker(시험 보강), S3Q-04·05는 의도 확인이 먼저(open-questions 후보), S3Q-06·07은 손대는 김에.
