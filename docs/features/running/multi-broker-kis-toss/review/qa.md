# QA — multi-broker-kis-toss step-2 (TOSS_LIVE 추가 · TOSS_LIVE/UPBIT_LIVE 주문 501 거부)

- 대상: worktree `.worktrees/multi-broker-kis-toss`, 기준 `e2904c8` + 미커밋 변경(`git diff -- backend/` 11개 파일, 새 파일 `BrokerApiExceptions.kt`, `RedisMarketQuoteAdapterTest.kt`)
- 방법: devkit `test-design`. 저장소 코드는 바꾸지 않음. 탐침 테스트와 돌연변이는 전부 격리 사본(`$TMPDIR/.../scratchpad/qa/trading-api`, worktree 를 `rsync` 로 복사, `build`·`.gradle` 제외)에서만 실행
- 사본 정합: 실험 뒤 `diff -rq src/main <worktree>/src/main` 결과 차이 0. worktree 무결성: `git diff --numstat -- backend/` 11줄, `git status --porcelain` 16줄(시작과 같음), `revert-probe.js` exit 0
- 실행 시각: 2026-11-15 (KST)

## 1. 결함 표

| ID | file:line | 무엇이 틀렸나 | 등급 | 출처 | 회귀 | 확신도 | 근거(실행한 명령과 결과) |
|---|---|---|---|---|---|---|---|
| Q1 | `application/order/ExecutionProcessor.kt:171-172` (부작용 위치 `:121` `applyExecution` · `:123` `executionRepository.save` · `:154` `applySellExecutionAtomically`) | TOSS_LIVE/UPBIT_LIVE 매도 체결 거부가 체결 저장·주문 상태 FILLED 전환·포지션 원자 감소 **뒤에서** 던진다. «입금 없이 실패»는 호출자 트랜잭션 롤백에만 기대고, `processFill`(`:89`)은 public 이면서 `@Transactional` 이 없어 트랜잭션 밖 호출자가 생기면 부분 반영이 남는다. 거부는 `processFill` 맨 앞(체결 중복 확인 직후)이 맞다 | should-fix | CLAUDE.md Financial Safety «Never mutate financial state implicitly» · development-plan D6 «입금 없음» | 아니오(새 코드). 지금 호출자(`fillLocal`·`fillExternal`, 둘 다 `@Transactional`)로는 도달 불가 | 동작 높음 · 영향 중간 | 탐침 P7 `./gradlew test --offline --tests '*QaProbe*'` exit 0: 예외 전에 `executionRepository.save` 2회(모드 2개), `applySellExecutionAtomically(99, 5)` 2회, 메모리상 주문 상태 FILLED 확인 |
| Q2 | `application/order/ExecutionProcessor.kt:140-149` (BUY 분기), 주석 `:170` | 거부가 매도 분기에만 있다. TOSS_LIVE/UPBIT_LIVE **매수** 체결은 예외 없이 포지션 생성·`recordBuyExecution`(예수금 차감)까지 반영된다. 주석 «도달하면 입금 없이 실패시킨다»와 달리 매수 쪽은 조용히 통과 | nit | development-plan D6 · RULE-03 «누락 시 조용히 넘어가지 않고 실패» | 아니오. 도달 불가: 매칭 엔진은 LOCAL 주문만 조회(`infrastructure/persistence/OrderRepositoryImpl.kt:52`), KIS 체결 통지는 KIS 모드로 거름(`KisExecutionNoticeService.kt:29-33`) | 높음 | 탐침 P6 exit 0: 두 모드 모두 `fillExternal` 정상 종료, 주문 FILLED, `positionRepository.save` 2회 |
| Q3 | `application/position/PositionExitTriggerOrchestrator.kt:170-181` | 자동 청산 주문이 501 로 거부되면 결정적 실패인데도 `catch (ex: Exception)` 이 `maxOrderCreateRetries`(3) 번 다시 시도한 뒤 트리거 FAILED + Slack 1건. 무한 재시도는 아님(트리거가 ARMED 에서 빠짐), 같은 거부를 3번 반복하는 낭비뿐 | nit | 코드 읽기 | 아니오(기존 재시도 정책에 새 예외가 들어감) | 중간(실행 안 함, 코드 읽기만) | `AutoExitOrderPlacementService.kt:16` `REQUIRES_NEW` 라 바깥 트랜잭션은 rollback-only 가 되지 않음. 이 경로의 실행 증거는 작성자 테스트 «그룹 자동 청산 매도 — placeOrder 와 같은 거부를 탄다»(서비스 단위)까지 |
| Q4 | `frontend/trading-web/src/entities/order/model/types.ts:6` · `entities/account/model/types.ts:2` · `shared/api/index.ts:8-11` · `features/order-form/ui/OrderFormPanel.tsx:97-98` | AE20 점검: UPBIT_LIVE 주문 응답이 201 → 501 로 바뀜. 클라이언트에 상태별 분기는 없고 범용 경로가 `"501 Not Implemented: {원문 JSON}"` 을 그대로 화면에 띄운다(동작은 함, 문구가 날것). 프론트 타입에 TOSS_LIVE 가 없음(계좌 타입은 UPBIT_LIVE 도 없음 — 기존) | nit | backend-api AE20 · development-plan DEC-02(프론트 수정 필요 시 멈추고 ASK) | 아니오(기존 범용 오류 처리) | 높음 | 읽기 전용 확인: `grep -rn "TradingMode" frontend/trading-web/src` · `fetchJson` 본문 · `OrderFormPanel.tsx:97-98` |
| Q5 | `domain/entity/account/Account.kt:362-367` · `AccountTest` 표 | 계좌 조합 표는 `AccountType.entries` 만 전수 강제한다. 새 TradingMode 를 넣어도 `AccountTest` 는 통과(새 모드는 두 계좌 타입 모두에서 거부 — 닫힌 쪽 기본값이라 안전). 주문·체결·시세 표와 달리 «새 모드를 어느 계좌 타입에 허용할지»를 결정하라고 강제하지 않음 | nit | development-plan RULE-03 | 아니오 | 높음 | 돌연변이 M2: `AccountTest` 51건 실패 0 |

blocker 0 · should-fix 1 · nit 4.

## 2. 확인됨 — 결함 아님

| ID | 물음 | 결과 | 근거 |
|---|---|---|---|
| C1 | 변경 전에 남은 UPBIT_LIVE 대기 주문·잠긴 자금이 취소 경로에서 같은 거부를 타서 영영 잠기나 | 아니오. `cancelOrder`(`OrderCommandService.kt:250-290`)에 모드 확인이 없어 매수 잠금 해제·매도 수량 잠금 해제가 그대로 된다. GTD 만료도 같은 `cancelOrder` 를 탄다(`infrastructure/scheduler/LocalMatchingScheduler.kt:25-33`). 이 주문들이 체결되지 않는 것은 변경 전과 같음(매칭 엔진이 LOCAL 만 조회) | 탐침 P1(매수, 두 모드: CANCELLED · `lockedDeposit` 0 · `availableDeposit` 1,000,000 복귀 · 해제 원장 1건), P2(매도: `orderableQuantity` 5 복귀) — exit 0 |
| C2 | 같은 `idempotencyKey` 재요청이 거부보다 먼저 기존 주문을 돌려주는 순서가 맞나 | 맞음. 거부된 요청은 아무것도 저장하지 않으므로 같은 키 재요청은 다시 501. 기존 주문이 돌아오는 경우는 변경 전에 접수된 UPBIT_LIVE 주문뿐이고, 그건 멱등 계약대로 «이미 접수된 주문»이다 | 탐침 P3: 기존 주문과 같은 객체 반환, `accountRepository.findByIdWithLock` 0회 |
| C3 | 비활성 계좌 + 거부 모드, 지정가 limitPrice 누락 + 거부 모드 | 둘 다 501 보다 먼저 400(`IllegalStateException` · `IllegalArgumentException`, `GlobalExceptionHandler.kt:25-35`). 우선순위로 문제없음. 400 범주 정렬은 step-3(DEC-02) 범위 | 탐침 P4, P5 exit 0 |
| C4 | `ExecutionProcessor` 예외 시 호출자에서 무한 재시도·롤백 문제 | 없음. `LocalMatchingEngine.kt:28-30` 주문별 `runCatching`, `QuoteEventListener` 단계별 `runCatching`, Redis Pub/Sub 은 재전달 없음. `fillLocal`·`fillExternal` 은 `@Transactional` 이라 예외 시 롤백(`ApiDomainException` 은 `RuntimeException`). 게다가 두 호출자 모두 거부 모드 주문에 도달하지 않음(Q2 근거) | 코드 읽기 + 탐침 P7 |
| C5 | 계좌 모드 null | `Account.tradingMode` 는 `lateinit var` 비널 타입(`Account.kt:46`)이라 `requireNotNull` 은 사실상 죽은 코드. 변경과 무관(기존) | 코드 읽기 |
| C6 | 자동 청산 경로(`createAutoExitSellOrder` · `createGroupedAutoExitSellOrder`) | 둘 다 `placeOrder` 를 거쳐 같은 거부를 탐. 수량 잠금 전 거부 | 작성자 테스트 2종 통과 + 돌연변이 M5 에서 둘 다 RED |

## 3. 돌연변이(격리 사본) — 모드 누락 표와 거부 테스트가 실제로 잡는가

변형은 `scratchpad/mutate.py`(원본 worktree 는 읽기만, 사본에 씀)로 적용, 매 변형 전에 4개 파일을 원본으로 되돌림.

| ID | 변형 | 명령 | 결과 |
|---|---|---|---|
| M1 | `TradingMode` 에 `NEW_LIVE` 만 추가 | `./gradlew compileKotlin --offline` | exit 1. 전수 `when` 4곳이 컴파일 실패: `ExecutionProcessor.kt:162` · `OrderCommandService.kt:175` · `:183` · `RedisMarketQuoteAdapter.kt:21` |
| M2 | `NEW_LIVE` 추가 + 메인 코드 4곳에 분기 추가(테스트 표는 그대로) | `./gradlew test --offline --tests '*OrderCommandServiceTest' --tests '*ExecutionProcessorTest' --tests '*RedisMarketQuoteAdapterTest' --tests '*AccountTest' --tests '*QaProbe*'` | exit 1. 모드 누락 표 3종만 RED(`entries` 집합 불일치) — 주문·체결·시세. `AccountTest` 51/0 통과(Q5) |
| M3 | `placeOrder` 의 `rejectIfBrokerOrderNotSupported(tradingMode)` 호출 삭제 | `--tests '*OrderCommandServiceTest' --tests '*ExecutionProcessorTest'` | exit 1. `OrderCommandServiceTest` 7건 RED(거부 6종 + 주문 표) |
| M4 | 체결 분기의 거부를 옛 동작 `account.receiveSellProceeds(netProceeds)` 로 되돌림 | 같음 | exit 1. `ExecutionProcessorTest` 3건 RED(TOSS·UPBIT 매도 + 체결 표) |
| M5 | 거부 호출을 `lockSellQuantityIfNeeded` 뒤로 옮김 | 같음 | exit 1. 매도·자동 청산 2종, 3건 RED |

결론: 새 모드 추가는 컴파일(M1)과 표 테스트(M2) 두 겹으로 막힌다. 거부 제거·옛 입금 복귀·거부 위치 후퇴(M3–M5)도 모두 RED.

## 4. 실행 기록

| 명령 | 위치 | exit | 결과 |
|---|---|---|---|
| 작성자 증거 집계(`build/test-results/test/*.xml` 58개 파싱) | worktree | — | 413건 · 실패 0 · 오류 0 · 건너뜀 0. 변경 소스 중 결과 XML 보다 새 파일 0(`find src -newer …`) — 신선함 |
| `./gradlew test --offline --tests '*OrderCommandServiceTest' --tests '*ExecutionProcessorTest' --tests '*AccountTest' --tests '*RedisMarketQuoteAdapterTest' --tests '*OrderControllerIntegrationTest'` | 사본(변경 없음 상태) | 1 | 단위 74건(14 + 7 + 51 + 2) 통과. `OrderControllerIntegrationTest` 는 `ContainerFetchException`(postgres:16-alpine) — Docker 데몬 없음(`docker.sock` 없음) |
| 통합 테스트 | — | 재실행 불가 | 작성자 XML 만 증거: 10건 실패 0, `2026-11-14T17:41:41Z`(소스 수정 02:39 KST 이후). TOSS_LIVE 4종 + UPBIT_LIVE 지정가 1종 포함. 이번 QA 에서 직접 확인한 것 아님 |
| `./gradlew test --offline --tests '*QaProbe*'` | 사본 + 탐침 2파일 | 0 | P1–P7 7건 통과(위 C1–C4, Q1, Q2 근거) |
| 돌연변이 M1–M5 | 사본 | 1 | 위 §3 |

탐침 파일(저장소에 넣지 않음): `scratchpad/qa/trading-api/src/test/kotlin/com/papertrading/api/application/order/QaProbeOrderTest.kt` · `QaProbeFillTest.kt`.

## 5. 넘길 것

- Q1: 거부를 `processFill` 앞쪽(체결 중복 확인 직후, `applyExecution` 전)으로 옮기고 매수·매도 공통으로 막으면 Q2 도 같이 닫힘. 회귀 테스트는 P6·P7 형태(예외 전 `executionRepository.save` 0회)로. 고칠 사람: Maker(`bugfix`).
- Q3–Q5: 선택. Q4 는 프론트 범위라 DEC-02 대로 사람 확인 필요.
- 이번에 하지 않은 것: `test-evidence.md`(요청 범위가 이 파일 하나), 통합 테스트 재실행(Docker 없음), 전체 스위트 재실행(요청대로 안 함).
