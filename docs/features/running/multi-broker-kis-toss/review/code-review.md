# 코드 리뷰 — multi-broker-kis-toss 2단계 (TOSS_LIVE 추가 · 브로커 미지원 주문 선행 거부)

범위: 변경분 (사람 선택 기록 없음 → 기본값). 대상 = 작업 트리 미커밋 `git diff -- backend/` + 새 파일 `BrokerApiExceptions.kt`, `RedisMarketQuoteAdapterTest.kt`, 기준 `e2904c8`. 호출부(LocalMatchingEngine, KisExecutionNoticeService, AutoExitOrderPlacementService, PositionExitTriggerOrchestrator, OrderController, GlobalExceptionHandler)와 trading-web 모드 타입은 영향 판단용으로만 읽음.
프로필: consumer (rule-pointer `--list` 반환). 기준 목록: `rules/dev-team/common.md`, `rules/dev-team/failure-handling.md`, `rules/dev-team/tests.md`, `always:dev-team`. 레포 자체 `rules/` 행 없음.

## 1. 규칙 준수

- common C12(주석은 명사형 표지): 새 주석 6곳이 종결어미 문장 → C-02.
- failure-handling FH1(돈·주문 POST 멱등): `placeOrder` 멱등 키 조회(L52)가 새 거부(L60)보다 앞 → 같은 키 재요청은 기존 주문 반환, 위반 없음. FH3·FH5·FH6 해당 없음(웹훅·소비자·건별 루프 변경 없음).
- common AE21(예외 삼키지 않음): 새 `throw` 는 `GlobalExceptionHandler.handleApiDomain` 이 501 로 응답, `LocalMatchingEngine` 은 기존 `runCatching` 경고 기록 → 새로 삼키는 곳 없음.
- tests(시험 먼저·관찰 가능한 동작): 거부 경로마다 단위·통합 시험 있음. 체결 경로 거부 시험의 빈틈은 C-01 에 합침.

## 2. 부작용

- `placeOrder` 호출부 3곳(OrderController, createAutoExitSellOrder, createGroupedAutoExitSellOrder) 모두 같은 거부를 탐. 그룹 자동 청산은 `AutoExitOrderPlacementService` 의 `REQUIRES_NEW` 라 바깥 트랜잭션 rollback-only 오염 없음.
- 자동 청산 재시도 루프가 결정적 501 을 3회 재시도 → C-03(범위 밖 후속).
- trading-web `TradingMode` 유니온에 `TOSS_LIVE` 없음 → C-04(범위 밖 후속). `AccountCard` 는 `?? account.tradingMode` 대체가 있어 깨지지는 않음.
- DB: `ddl-auto: create` 라 enum CHECK 제약이 기동마다 재생성, 컬럼 길이 20 ≥ `TOSS_LIVE`(9자) → 저장 실패 위험 없음.
- collector-api 구독: 거부가 `subscribe` 앞이라 `TOSS_LIVE` 구독 요청이 나가지 않음.

## 3. 버그·장애 위험

- 체결 경로 방어선 위치·범위 → C-01.
- 주문 경로: 거부 위치가 매도 수량 잠금(L62)·시세 조회(L63)·주문 저장(L64)·매수 잠금(L67)·구독(L68) 전부보다 앞 → 잠금·주문·외부 호출 잔여 없음. KIS·LOCAL 분기 동작은 이전과 같음(`LOCAL -> Unit` = 옛 `else -> {}`).
- 회귀: LocalMatchingEngine 은 LOCAL 주문만, KIS 통지는 KIS 모드 주문만 찾으므로 기존 모드 동작 변화 없음. 회귀 `예` 근거 없음.

## 지적

| ID | file:line | 렌즈 | 무엇이 틀렸나 | 등급 | 출처 | 회귀 | 확신도 | 근거 조항 |
|---|---|---|---|---|---|---|---|---|
| C-01 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:170 | 정확성 | 미지원 모드 방어선이 매도 분기 안, 그것도 `order.applyExecution`(L121)·`executionRepository.save`(L123)·`applySellExecutionAtomically`(L154, DB 즉시 갱신) 뒤에 있음. 매수 분기(L140-149)는 모드 검사가 없어 TOSS_LIVE·UPBIT_LIVE 매수 체결이 들어오면 포지션 생성·`recordBuyExecution`·잠금 소진이 그대로 진행되고, 주석 «도달하면 입금 없이 실패시킨다»와 RULE-03 «조용히 넘어가지 않고 실패»가 매수에선 성립하지 않음. 매도도 `fillLocal`/`fillExternal` 의 `@Transactional` 롤백에만 기댐 — `processFill` 은 public·비트랜잭션이라 직접 호출되면 원자적 수량 차감이 커밋된 뒤 예외. 단위 시험(ExecutionProcessorTest REJECTED)은 입금·원장만 보고 `executionRepository.save`·`applySellExecutionAtomically` 호출 여부는 안 봄(목이라 롤백을 못 봄), 누락 표도 매도만 다룸. 오늘은 체결 진입점 두 곳(LOCAL 매칭·KIS 통지)이 이 모드를 못 넘겨 도달 불가 → should-fix. 고침: `processFill` 맨 앞(L105 직후, 변경 전)에서 전수 `when` 으로 한 번 거부하고, 매수·매도 양쪽에 «저장·차감 호출 0회» 시험 추가. 이때 예외는 501 사용자 거부가 아니라 내부 불변식 위반(500 계열)이 맞는지 계획 D6 와 함께 사람 판단 필요 | should-fix | 이 변경 | 아니오 | 중간 | label:precondition-check-missing |
| C-02 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:173 | 표준 | 새 주석이 종결어미 문장: OrderCommandService.kt:173 «막는다 … 남는다», :181 «깨진다», :184 «체결한다», ExecutionProcessor.kt:170 «않는다 … 실패시킨다», BrokerApiExceptions.kt:5 «받는다 … 생긴다». C12 는 명사형 표지(`막음`·`→`)만 허용. 예: «잠금·저장·구독 전 거부 → 뒤에서 막으면 잠금·collector 호출 잔존» | nit | 이 변경 | 아니오 | 높음 | C12 |
| C-03 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:170 | 효율 | 새 `BrokerOrderNotSupportedException` 은 결정적 거부인데 `catch (ex: Exception)` 재시도 루프(최대 3회)에 들어가 매번 `REQUIRES_NEW` 트랜잭션과 계좌 행 잠금을 잡은 뒤 실패 알림·`markFailed`. TOSS_LIVE·UPBIT_LIVE 포지션에 ARMED 트리거가 있을 때만 발생(현재 체결로는 그런 포지션이 안 생김). 비재시도 예외는 첫 시도에서 실패 처리 | nit | 범위 밖 | 아니오 | 낮음 | label:redundant-work-per-item |
| C-04 | frontend/trading-web/src/entities/order/model/types.ts:6 | 부작용 | 백엔드가 이제 `TOSS_LIVE` 계좌 생성을 받고 응답에 실을 수 있는데 프론트 `TradingMode` 유니온·라벨 표에 없음 → 계좌 카드가 원문 `TOSS_LIVE` 로 표시(대체값 있어 깨지지는 않음). trading-web 수정은 계획상 범위 밖이라 후속 | nit | 범위 밖 | 아니오 | 중간 | label:hardcoded-value-not-ssot |

## 렌즈 실행 기록

- 회귀 시험 렌즈(`test-suite-audit guards-fix`): 확인 불가 — 탐침 샌드박스에서 Gradle 홈 캐시(`~/.gradle`) 쓰기와 네트워크가 막혀 실행 불가, `OrderControllerIntegrationTest` 는 DB 를 씀(외부 상태). 작성자 증거(step-2.md §6, 413개·실패 0)를 근거로만 둠.
- 뮤테이션 렌즈(TT49 대상: OrderCommandService·ExecutionProcessor): `mutation-run --base e2904c8` exit 4·목록 없음 «뮤테이션 대상 없음». 변경이 미커밋이라 base..HEAD 범위가 비어 대상 0 으로 본 것으로 보임 → 사실상 확인 불가(뮤테이션). 커밋 뒤 재실행 필요.
- 재사용 점검(`reuse-inventory`): 미실행(Kotlin 대상). 새 `NotImplementedException` 은 기존 `CommonApiExceptions` 범주 기반 패턴을 따름, 중복 없음.

## 요약

- 병합 판정: `review-verdict` → «머지 판정: 가능 — 막는 지적 0건 · 후속 후보 4건» (exit 0).
- 의도 메모: 전달된 변경 설명에 없는 동작 2가지 — 계좌 생성 API 가 STOCK+TOSS_LIVE 를 받음(Account.kt:366, 임의 `initialDeposit` 허용, 통합 시험이 1,000만 원으로 생성), `AccountSource.TOSS` 추가(생산하는 곳 없음, `AccountResponse` 는 LOCAL 고정). 계획 step 2 에는 있으므로 커밋 `의도:` 블록에 적어 두면 됨.
- 기존 메모: 이 변경 전에 만들어진 UPBIT_LIVE PENDING 주문은 매칭 경로가 없어 잠금이 계속 남음(이번 변경과 무관, 정리 경로 없음).
