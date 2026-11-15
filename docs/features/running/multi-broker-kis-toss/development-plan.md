# Development Plan: KIS·토스증권 다중 브로커 연동 (1단계 토스 읽기 전용, 2단계 KIS 공통 계층)

> Tech Planner 산출물. 승인된 feature-spec을 받아 «어떻게 만들지, 지금 만들 수 있나»를 정한 결정 스냅샷입니다.
> 기반: [feature-spec.md](./feature-spec.md), [open-questions.md](./open-questions.md), [planning-approval.md](./planning-approval.md), [토스 Open API 조사](../../../research/2026-11-15-toss-open-api.md). 다음: state.md, steps/step-N.md
>
> 합의되면 고정합니다. 진행 상태는 여기 적지 않고 state.md에 적습니다.
> 상세 계획은 1단계(§0~§9)와 2단계(§10, 2026-11-16 추가)입니다. 3단계는 §5에 큰 줄기와 선행 조건만 둡니다(feature-spec §3, §10 그대로).
> 줄 번호는 `origin/main` e2904c8 기준입니다. 경로 앞의 `backend/trading-api/src/main/kotlin/com/papertrading/api/`는 줄였습니다.
> 틀: devkit v0.95.0 `templates/planner/development-plan.template.md`

---

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 기능 | `multi-broker-kis-toss` |
| 기반 feature-spec | feature-spec.md (spec-sig 91abe226ef1b389b, 2026-11-15 사용자 승인) |
| 작성자 | Claude (tech-planner) |
| 날짜 | 2026-11-15 |
| tier | 반자동 (`.claude/stack-profile.json`). 3단계 착수 전 재판정(FLAG-09) |
| 대상 repo / 브랜치 | paper-trading / `feature/multi-broker-kis-toss` (1단계는 trading-api만) |
| 상태 | draft |

---

## 1. 구현 가능성 판단 (착수 전 점검)

| 항목 | 값 |
|---|---|
| feature-spec 준비됨? | ready (FLAG-01, 02, 03은 사용자 결정, 나머지는 ASSUME 기록) |
| 막힌 ASK (open-questions) | none |
| 판정 | 🟡 일부 조건부 |
| 코멘트 | 🔴 없음. 토스 개별 API 응답 스키마가 없어 응답 매핑, 토큰 발급 형식, 에러 본문 해석은 «스키마 확정»(§5의 4번, 사람 작업) 뒤에만 만들 수 있습니다. 모드 추가, 주문 거부, 토큰 암호화, 분산 락, 공통 포트 모양은 지금 코드와 의존성으로 바로 됩니다. 사람 결정 DEC-01~03은 2026-11-15에 결정됨(§8). DEC-04는 스키마 확정 때 조건부로 묻는다 |

부분별 가능성:

| 부분 | 가능? | 필요한 선행 작업 | 근거(읽은 파일) |
|---|---|---|---|
| `TradingMode.TOSS_LIVE` 추가, 계좌 검증 | 🟢 | - | `domain/enums/TradingMode.kt:3-8`, `domain/entity/account/Account.kt:44-47, 361-369`, `build.gradle:30-52`(Flyway, Liquibase 없음), `application.yaml:19`(ddl-auto create) |
| 1단계 TOSS_LIVE 주문 거부 (FLAG-02, 자동 청산 포함) | 🟢 | DEC-01(UPBIT_LIVE 분기) | `application/order/OrderCommandService.kt:49-73, 171-236`, `application/position/AutoExitOrderPlacementService.kt:17-25`, `application/position/PositionExitTriggerOrchestrator.kt:139-182` |
| 토큰 Redis 암호화 저장 (FLAG-01) | 🟢 | 암호화 키 생성과 주입(사람) | `build.gradle:13-17`(Java 21, JDK AES-GCM 사용 가능), `build.gradle:50`(spring-security는 test 범위뿐) |
| 분산 락으로 발급 지점 하나 (FLAG-01) | 🟢 | - | `build.gradle:33`(spring-boot-starter-data-redis), `infrastructure/kis/KisTokenManager.kt:25, 35`(JVM `synchronized`만 있음) |
| 토스 HTTP 호출 | 🟢 | - | `infrastructure/config/RestTemplateConfig.kt:11-15`, `infrastructure/kis/KisOrderRestClient.kt` |
| 잔고 공통 포트 모양 (FLAG-03) | 🟢 | - | `application/account/port/KisAccountBalancePort.kt:6-27` |
| 토큰 발급 요청·응답 형식, 에러 본문의 code 필드 | 🟡 | 스키마 확정 | 조사 문서 «출처와 범위»(개별 API 문서 미확보) |
| 토스 잔고, 보유, 계좌 목록 매핑 | 🟡 | 스키마 확정(현금 위치, 수익률 단위, 통화 필드) | 조사 문서 |
| 토스 시세 REST (prices, orderbook, candles) | 🟡 | 스키마 확정, REST 호스트 확인(조사 문서에 없음) | 조사 문서 |
| 401, 403, 429, 5xx 구분 | 🟡 | 상태 코드는 조사 문서로 확정, 본문 code 필드 이름만 스키마 확정 | 조사 문서 «에러 코드» |
| 그룹별 호출 한도 | 🟢 (1단계 호출 프로세스 1개) | 엔드포인트별 한도 그룹 대응표 확정 | `docker-compose.yml:59-80`(trading-api 컨테이너 1개) |
| 테스트 기반 | 🟢 | Docker 실행 | `src/test/.../presentation/controller/OrderControllerIntegrationTest.kt:34-53`(Testcontainers Postgres, Redis) |
| 실제 계좌 잔고 1회 조회 (수동) | 🟡 | 허용 IP 등록과 키 발급(사람), DEC-03 | 조사 문서 «인증과 접속 조건» |

---

## 2. 아키텍처·통합 결정

| 결정 | 선택 | 근거 | 기각한 대안 |
|---|---|---|---|
| D1 브로커 지정 | 계좌의 `TradingMode`가 곧 브로커 지정. `TOSS_LIVE` 추가, 별도 broker 컬럼 없음. 이력: `docs/ADR.md` ADR-005 «계좌별로 모드 결정»을 그대로 따름 | `Account.kt:44-47`이 이미 계좌별 모드를 가짐, RULE-01, RULE-02 | Account에 `broker` 컬럼 신설: `tradingMode`와 어긋날 수 있는 중복 상태 |
| D2 스키마 | 마이그레이션 없음 | enum 컬럼 셋 모두 `EnumType.STRING`, length 20(`Account.kt:44-47`, `domain/entity/order/Execution.kt:84-86`, `domain/entity/feepolicy/FeePolicy.kt:31-33`). ORDINAL 사용 없음. trading-api에 Flyway 의존성 없음. 기동마다 스키마 재생성(`application.yaml:19` create, test는 create-drop) | V13 SQL로 CHECK 재정의: `db/migration/*`에 trading_mode CHECK가 없어 고칠 대상이 없고, 앱이 그 SQL을 실행하지도 않음 (예외 상황은 RISK-04) |
| D3 내부 계좌와 토스 `accountSeq` 연결 (FLAG-04) | 기존 `Account.externalAccountId`(`Account.kt:65-67`)에 저장하고 뜻은 모드별로 해석(KIS는 `CANO-상품코드`, TOSS는 `accountSeq`). 운영자가 `GET /api/toss/accounts`로 확인한 값을 계좌 생성·수정 요청의 `externalAccountId`로 넣음. 생성 때 외부 호출 없음. 미연결 계좌는 조회 때 409 `TOSS_ACCOUNT_NOT_LINKED` | FLAG-04 가정 «KIS 계좌 연결과 같은 방식», 스키마 변경 0, 수정 경로가 이미 있음(`Account.kt:224-226`, `presentation/dto/account/AccountRequest.kt:22`) | 새 컬럼 `broker_account_seq`: 같은 개념이 컬럼 둘로 나뉨. 별도 매핑 테이블: 계좌와 브로커가 1:1이라 계좌당 행이 늘 1개뿐. 계좌 생성 때 토스 `GET /accounts` 자동 호출: 내부 계좌 생성이 토스 장애와 ACCOUNT 한도(초당 1회)에 묶임 |
| D4 TOSS_LIVE 주문 거부 위치 (FLAG-02) | `placeOrder`에서 계좌 로드 직후(L58 다음)에 전수 `when`으로 먼저 거부. `submitExternalOrderIfNeeded`(L171-177)의 `else -> {}`도 없애고 모든 모드 명시(TOSS_LIVE는 같은 예외, 방어용). 이력: open-questions.md §4 FLAG-02 (B, C 기각, 사용자 2026-11-15). 이번 결정은 채택안 A 안에서 거부 위치만 정함 | L70 분기에서만 막으면 그 앞에서 L60 매도 수량 잠금, L62 주문 저장, L65 매수 잠금, L66 collector 구독 HTTP 호출(롤백 안 됨)이 먼저 일어남. IOC, FOK는 L68에서 L70 전에 반환해 CANCELLED 주문이 남음. 시장가 매수는 L101에서 409 `QUOTE_UNAVAILABLE`로 엉뚱하게 실패. 따라서 EDGE-08 «주문과 자금 잠금 미생성»을 못 지킴. 자동 청산 두 경로는 `placeOrder`를 거침(L194, L221). 주문 생성 지점은 `saveOrder`(L112-133) 하나뿐 | L70에서만 거부: 위 이유. 주문 API 앞단 검사: 자동 청산 경로가 빠짐(FLAG-02 C와 같음) |
| D5 주문 거부 에러 | `BrokerOrderNotSupportedException`(브로커 주문이 아직 없는 모드의 주문 거부). 범주 I(501, 계획됐으나 미구현이며 3단계 실주문에서 만듦), code `BROKER_ORDER_NOT_SUPPORTED` | devkit `rules/dev-team/backend-api.md` AE1, T7 | `check()`의 IllegalStateException: `presentation/exception/GlobalExceptionHandler.kt:31-35`가 400 `BAD_REQUEST`로 뭉쳐 원인을 구분 못 함. 409: 사용자가 할 선행 단계가 없음(T2) |
| D6 다른 모드 분기 | `application/order/ExecutionProcessor.kt:161-172`의 `else` 제거. LOCAL은 지금처럼 매도대금 즉시 입금, KIS는 동기화만, TOSS_LIVE와 UPBIT_LIVE는 `BrokerOrderNotSupportedException`(주문 선행 거부로 도달 불가, DEC-01). `infrastructure/redis/RedisMarketQuoteAdapter.kt:21-26`은 전수 `when`이라 컴파일이 강제하므로 키 `quote:toss:live:{ticker}` 추가(1단계엔 쓰는 쪽이 없어 조회 결과 null) | RULE-03 «누락 시 조용히 넘어가지 않고 실패» | ExecutionProcessor의 `else` 유지: TOSS_LIVE가 LOCAL처럼 즉시 입금되는 길로 빠짐. 시세 키 대신 예외: `application/position/PositionQueryService.kt:50` 같은 조회 경로가 깨짐 |
| D7 잔고 공통 포트 (FLAG-03) | `application/account/port/BrokerAccountBalancePort.kt`(브로커 공통 잔고 조회 포트)의 `fetchBalance(account: BrokerAccountRef): BrokerBalanceSnapshot`. `BrokerAccountRef`(포트에 넘기는 계좌 식별값)는 `accountId`, `tradingMode`, `externalAccountId`. `BrokerBalanceSnapshot`은 `asOf`, `currencyBalances: List<BrokerCurrencyBalance>`, `positions: List<BrokerBalancePosition>`. `BrokerCurrencyBalance`(통화별 합계)는 `currency`, `cashBalance`, `cashBuyingPower`, `marketValue`, `unrealizedPnl`, `returnRate`. 토스는 예수금 API가 없어 `cashBalance`는 null, `cashBuyingPower`(매수 가능 금액, 토스 `GET /api/v1/buying-power`의 `cashBuyingPower`)는 별도 칸으로 채움(DEC-04, 사람 결정 2026-11-15). `BrokerBalancePosition`(종목별 보유)은 `ticker`, `currency`, `quantity`, `avgPrice`, `currentPrice`, `marketValue`, `unrealizedPnl`, `returnRate`. 금액과 수량은 전부 `BigDecimal`. 브로커가 주지 않는 값은 0이 아니라 null이며, 필드별 nullable은 스키마 확정 때 고정. 이력: open-questions.md §4 FLAG-03 (B, C 기각) | `KisBalanceSnapshot` 필드(`KisAccountBalancePort.kt:10-27`)를 그대로 쓰고 통화 축만 추가(FLAG-07: USD를 원화와 합산 금지). KIS 전용 `trId` 인자는 뺌(2단계에 KIS 어댑터가 `tradingMode`로 정함) | `KisBalanceSnapshot`을 그대로 공통화: 단일 통화 합계라 USD가 섞임. `KisAccountBalancePort` 이름을 바꿔 재사용: 1단계에 KIS 코드를 건드림(FLAG-03 A는 KIS를 2단계로 미룸) |
| D8 포트 선택 | `BrokerAccountQueryService`(계좌 모드로 어댑터를 골라 잔고를 조회하는 서비스)에서 `account.tradingMode` 전수 `when`. TOSS_LIVE는 토스 어댑터. KIS_PAPER, KIS_LIVE, UPBIT_LIVE는 501 `BROKER_BALANCE_NOT_SUPPORTED`(KIS는 2단계 이전까지 `/api/kis/account/balance` 사용). LOCAL은 404 `NOT_BROKER_ACCOUNT`(E-1, URL 대상의 종류가 틀림, T8). 이력: `docs/ADR.md` ADR-005 «Strategy 패턴, 구현체만 추가». 지금 주문 코드도 포트 없이 `when` 분기(`OrderCommandService.kt:171-177`)라 ADR과 이미 어긋나 있고, 1단계는 RULE-03을 우선함 | 새 모드가 생기면 컴파일 에러로 드러남(RULE-03). 1단계 어댑터는 1개 | 어댑터 목록 주입과 `supports()` 레지스트리: 1단계엔 고를 어댑터가 1개뿐이고 누락이 런타임에만 드러남. 2단계 공통 계층 설계 때 다시 봄 |
| D9 내부 API 경로 | 공통 잔고는 `GET /api/v1/accounts/{accountId}/broker-balance`. 토스 전용은 `GET /api/toss/accounts`(accountSeq 목록), `GET /api/toss/market/prices`, `/api/toss/market/orderbook`, `/api/toss/market/candles`. 기존 `/api/kis/account/balance`는 1단계에 그대로 둠 | 계좌 하위 경로 관례(`presentation/controller/AccountController.kt:37`, `OrderController.kt:27`), 브로커 전용 경로는 `/api/kis/account` 패턴(`KisAccountBalanceController.kt:12`), RULE-10 | `/api/toss/account/balance`: 2단계에 KIS가 공통 포트로 오면 경로를 또 바꿔야 함. `/api/kis/account/balance`에 토스 분기: `mode`가 `KisAccountMode`라 KIS 전용 계약을 깸 |
| D10 토큰 저장 (FLAG-01) | Redis `toss:token`에 AES-256-GCM 암호문 `v2.<키 식별자>.<IV>.<암호문+태그>`(base64url, 옛 `v1.` 값은 복호화 실패로 보고 재발급, IV 12바이트를 매번 무작위 생성, AAD는 키 이름) 저장, TTL은 `max(expires_in − 갱신 여유, max(expires_in / 2, 1))`초(갱신 여유는 설정값). 이 식은 수명에 대해 단조라 수명이 갱신 여유 바로 위인 토큰도 절반 이상 보관한다(R1-01, FLAG-16). 뒤 항이 쓰이면 warn 1회. 받은 토큰은 버리지 않음(버리면 재발급마다 직전 토큰이 폐기돼 503이 반복됨, step 4 리뷰 C-01). 키 교체(C-08): 암호문에 키 식별자(키 SHA-256 앞 8바이트, base64url)를 넣고, 선택 설정 `TOSS_TOKEN_PREVIOUS_ENCRYPTION_KEY`가 있으면 그 식별자의 암호문은 이전 키로 복호화한다(저장은 늘 현재 키, 이전 키 형식이 틀리면 토스만 비활성). 옛 키·새 키 인스턴스가 함께 도는 동안 서로의 토큰을 못 읽어 재발급·폐기가 반복되는 것을 막는다. JDK `javax.crypto`만 쓰고 키는 `TOSS_TOKEN_ENCRYPTION_KEY`(base64, 32바이트). 복호화 실패(키 교체 등)는 캐시 없음으로 보고 락을 잡아 재발급. 암호기 `AesGcmTokenCipher`(토큰 암복호화)는 `infrastructure/security/`에 둬 2단계 KIS 토큰(`KisTokenManager.kt:85` 평문 저장)에도 씀. 이력: open-questions.md §4 FLAG-01 (B 메모리 보관, C 평문 저장 기각) | main 범위 암호화 라이브러리 없음(`build.gradle:50`은 test 범위), Java 21 툴체인(`build.gradle:13-17`)에 AES-GCM 기본 포함 | spring-security-crypto 추가: 새 의존성인데 JDK로 같은 일을 함. Jasypt: 새 의존성, 유지보수 정체 |
| D11 발급 직렬화 (FLAG-01, RULE-06) | `RedisDistributedLock`(Redis SETNX 기반 분산 락): `SET toss:token:lock <소유자 UUID> NX PX <락 TTL>`, 해제는 Lua로 «값이 내 UUID일 때만 DEL». 순서는 락 획득, Redis 재확인, 발급, 암호화 저장, 해제. 저장은 Lua 한 번으로 «락 값이 내 UUID일 때만 SET»(락 만료 뒤 늦은 저장이 다른 인스턴스의 새 토큰을 덮어쓰지 않게, C-03). 락을 잃어 저장을 못 하면 캐시를 다시 읽어 있으면 그 토큰, 없으면 503. 락을 못 얻으면 대기 한도까지 `toss:token`을 다시 읽고, 그 사이 토큰도 락도 없으면(보유자 발급 실패) 락 획득을 다시 시도해 직접 발급한다(대기자가 빈 락 앞에서 한도까지 스레드를 잡지 않게, C-02). 끝내 없으면 503 `TOSS_TOKEN_UNAVAILABLE`. 요청 하나의 전체 상한(대기와 발급을 합친 시간)은 설정값 `token-request-deadline-millis`(기본 20초)로 두고, 대기자가 락을 얻었을 때 남은 시간이 발급 최악 소요(연결 5초+읽기 10초)보다 짧으면 발급하지 않고 락을 풀고 503 `TOSS_TOKEN_UNAVAILABLE`(R1-02, FLAG-16, 사람 결정 2026-11-15). 락 없이는 발급하지 않음. 락 TTL은 연결 5초와 읽기 10초(`RestTemplateConfig.kt:13-14`)를 더한 값보다 길게 | `setIfAbsent`와 `DefaultRedisScript`가 data-redis에 있음. 기존 `synchronized`(`KisTokenManager.kt:25, 35`)는 한 프로세스 안만 막음 | Redisson, ShedLock, spring-integration-redis: 락 키 1개를 위해 새 의존성 추가. JVM `synchronized`: 프로세스 사이 중복 발급으로 `token-revoked` 유발 |
| D12 401 처리 (EDGE-03, EDGE-04) | `expired-token`, `token-revoked`면 Lua로 «Redis 값이 실패한 그 토큰일 때만 삭제»한 뒤 `getToken`을 다시 부름(다른 곳이 새로 발급했으면 그 토큰을 읽고, 아니면 락을 잡고 1회 발급). 원 요청은 1회만 재시도하고 또 401이면 503 `TOSS_AUTH_FAILED` | 무한 재발급 금지, 토큰 1개 제약 | 401마다 무조건 재발급: 프로세스끼리 서로 토큰을 폐기하는 핑퐁 |
| D13 HTTP 클라이언트와 숫자 변환 (RULE-09) | 기존 `RestTemplate` 빈 재사용. 본문은 문자열로 받아 Kotlin data class(금액, 수량, 가격 필드는 `BigDecimal`)로 역직렬화. `Map::class.java`, `JsonNode` 수치 변환, `Double`, `Float` 금지 | KIS와 같은 클라이언트(`KisOrderRestClient.kt`, `KisTokenManager.kt`). Jackson은 `BigDecimal` 필드를 숫자 토큰에서 바로 읽고 문자열 숫자도 변환함. Map 파싱은 JSON 소수를 `Double`로 만듦(`KisAccountBalanceAdapter.kt:64, 127-128` 방식) | RestClient, WebClient: HTTP 방식이 둘이 됨(WebClient는 webflux 추가). KIS처럼 Map 파싱: 부동소수점을 거침 |
| D14 결측 필드 (FLAG-06) | 필수 필드 누락이나 숫자 변환 실패는 503 `TOSS_RESPONSE_INVALID`(J-2). 0으로 채우지 않음. 필수와 선택의 구분은 스키마 확정 때 필드표로 고정 | core 6(silent default 금지) | KIS처럼 누락 시 `BigDecimal.ZERO`(`KisAccountBalanceAdapter.kt:127-128`): 잔고가 0으로 잘못 보임 |
| D15 토스 실패 응답 범주 | 토스 쪽 실패는 범주 J(503)로 두고 code로 원인을 가름: `TOSS_AUTH_FAILED`, `TOSS_IP_NOT_ALLOWED`(허용 IP 등록 안내 고정 문구, 재시도 안 함), `TOSS_RATE_LIMITED`, `TOSS_MAINTENANCE`, `TOSS_UNAVAILABLE`(그 밖의 5xx와 네트워크), `TOSS_TIMEOUT`, `TOSS_RESPONSE_INVALID`, `TOSS_TOKEN_UNAVAILABLE`. 토스가 우리 입력(종목 등)을 거부한 4xx는 A(400) `TOSS_REQUEST_REJECTED`. 키 미설정은 K-1(500) `TOSS_INTEGRATION_DISABLED`(누락된 환경변수 이름만). 미연결 계좌는 F-4(409) `TOSS_ACCOUNT_NOT_LINKED`. 토스 원문은 응답에 싣지 않고 로그에만 남김(AE8). 없는 범주 기반 `ServiceUnavailableException`(J, 503), `NotImplementedException`(I, 501), `InternalErrorException`(K, 500)을 `common/exception/`에 추가(AE6). 이 새 범주 예외는 범주·code·메시지만 들고 Spring `HttpStatus`를 import하지 않으며, 범주→HTTP 상태 매핑은 `presentation/exception/GlobalExceptionHandler.kt`가 맡음(레포 규칙 `rules/backend-api.md` 계층 지도 조항 «common은 Spring을 import하지 않는다», 사람 결정 2026-11-15 «새 예외만 핸들러 매핑»). 2단계에서 만든 `NotImplementedException`도 step 3에서 같은 모양으로 옮김. ArchUnit 동결 목록에 든 기존 예외(`ApiDomainException`, 기존 `CommonApiExceptions`)는 그대로 둠 | backend-api.md AE1, AE4(외부 의존 실패는 503), T4, T14 | KIS처럼 401, 403, 502, 504로 전달(`CommonApiExceptions.kt:93-103`): 우리 API 호출자의 인증·권한 문제가 아닌데 401, 403을 줘서 원인을 오해시킴. KIS와 범주가 달라지는 점은 2단계 KIS 정렬 때 AE20 목록으로 맞춤. 새 범주 예외가 `HttpStatus`를 직접 들기: 레포 규칙(common은 Spring 금지)과 ArchUnit에 걸림. 기존 동결 예외까지 전부 핸들러로 옮기기: 건드리는 파일과 응답 점검 범위가 커짐(사람 결정 2026-11-15) |
| D16 호출 한도 (RULE-08) | `TossRateLimiter`(프로세스 안에서 그룹별 최소 간격을 지키는 리미터). 그룹별 초당 한도는 설정값이고 기본값은 조사 문서 수치. 429면 `Retry-After`(없으면 1초)만큼 기다린 뒤 재시도. 최대 재시도 횟수나 최대 대기 상한을 넘으면 503 `TOSS_RATE_LIMITED`. 리미터 줄 대기 상한은 429 대기 예산과 다른 키(`rate-limit-max-queue-wait-millis`)로 둠(S6-04, 사람 결정 2026-11-15: 같은 키면 429 무대기 0에서 연속 호출이 실패). 줄 대기 상한은 가장 긴 그룹 간격 이상이어야 함(S7-03, 사람 결정 2026-11-16: 더 작으면 그 그룹의 연속 호출이 늘 실패). 대기는 주입한 sleeper로 해서 테스트가 실제로 잠들지 않음 | 1단계 토스 호출 프로세스는 trading-api 1개(`docker-compose.yml:59-80`, FLAG-08). 한도는 «사전 공지 없이 바뀔 수 있음»이라 코드 상수 금지 | Redis 공유 리미터: 호출 프로세스가 둘이 되는 3단계로 미룸(선행 조건). 429만 처리: RULE-08의 «한도를 지키고»를 못 지킴 |
| D17 설정과 비활성 (EDGE-01, RULE-05) | `TossProperties`(`@Component @ConfigurationProperties(prefix = "trading.toss")`, `KisOrderProperties.kt:6-8` 패턴). 필수 4개 중 하나라도 비었거나 암호화 키 형식이 틀리면 토스만 비활성. 앱은 정상 기동하고, 기동 때 누락·오류 키의 이름만 1회 로그, 토스 API 호출 때 `TOSS_INTEGRATION_DISABLED`. REST 호스트 기본값은 두지 않음(조사 문서에 없음) | KIS 설정 패턴(`application.yaml:38-50`), 값 날조 금지 | `@ConditionalOnProperty`로 빈 미생성: 서비스와 컨트롤러 주입이 깨지거나 Optional 분기가 퍼짐. 형식이 틀린 키에 기동 실패: EDGE-01(앱 정상 기동)과 충돌 |
| D18 스키마 확정 방식 (FLAG-06) | 공식 명세 `openapi.tossinvest.com/openapi-docs/latest/openapi.json`(1.2.28)을 필드의 원본으로 삼아 필드표를 step 문서에 기록하고, 명세의 스키마·예시로 만든 fixture를 `src/test/resources/fixtures/toss/`에 커밋(가짜 계좌·금액·종목, 숫자는 명세대로 decimal 문자열). 사람이 키와 허용 IP를 준비하면 실제 API를 1회씩 호출해 숫자 표기와 `expires_in` 크기만 대조하고, 어긋나면 멈추고 ASK(사람 결정 2026-11-15) | 2026-11-15 공식 명세 확인(조사 문서 «개별 API 스키마»). 이전 판은 «개별 API 문서의 공개 주소가 없음»을 근거로 실제 호출만으로 확정 | 커뮤니티 MCP 서버 문서로 필드 추정: 공식이 아니라 날조 위험. WTS 문서만 보기: 실제 숫자 표기를 확인 못 함 |
| D19 1단계 시세 위치 (FLAG-08) | trading-api가 토스 REST를 직접 조회해 그대로 응답. Redis 시세 해시에는 쓰지 않음 | 조회 전용 목적, 주문 경로와 분리 | Redis `quote:*`에 기록: 60초 신선도 판정(`RedisMarketQuoteAdapter.kt:18, 38`)과 섞여 주문 경로에 영향, 3단계 범위 |

`TradingMode` 영향 지점 전수(main 코드를 글자 검색. Serena Kotlin 언어 서버가 기동에 실패해 심볼 검색 대신 grep을 씀):

| 위치 | 현재 | 1단계 조치 |
|---|---|---|
| `domain/enums/TradingMode.kt:3-8` | LOCAL, KIS_PAPER, KIS_LIVE, UPBIT_LIVE | `TOSS_LIVE`를 끝에 추가 |
| `domain/entity/account/Account.kt:361-369` | STOCK은 LOCAL, KIS_PAPER, KIS_LIVE (불리언 OR라 컴파일이 누락을 못 잡음) | STOCK에 TOSS_LIVE 허용, CRYPTO는 거부 유지 |
| `application/order/OrderCommandService.kt:58, 171-177` | `else -> {}` | D4: 선행 거부와 전수 명시 |
| `OrderCommandService.kt:180-209, 212-236` | 자동 청산이 `placeOrder`(L194, L221)를 부름 | 변경 없음, 같은 거부를 타는지 테스트 |
| `application/order/ExecutionProcessor.kt:161-172` | `else -> receiveSellProceeds` | D6: `else` 제거 |
| `infrastructure/redis/RedisMarketQuoteAdapter.kt:21-26` | 전수 `when`(else 없음) | 컴파일이 강제, `quote:toss:live:` 키 추가 |
| `infrastructure/redis/QuoteEventListener.kt:69-78` | 문자열을 모드로 바꾸고 모르는 값은 null로 무시 | 변경 없음(토스 시세 발행 없음, 3단계) |
| `infrastructure/kis/KisExecutionNoticeParser.kt:79-87` | 문자열 기준, KIS만 | 변경 없음 |
| `infrastructure/redis/RedisLivePositionCacheAdapter.kt:22, 51, 64` | `valueOf` | 변경 없음 |
| `application/portfolio/TaxSummaryCommandService.kt:60`, `TaxSummaryQueryService.kt:46-47` | LOCAL만 허용 | 변경 없음(토스도 거부 유지) |
| `application/account/AccountQueryService.kt:25-27`, `infrastructure/persistence/AccountRepository.kt:21` | 모드 필터 | 변경 없음 |
| `infrastructure/persistence/SettlementTaxReadRepositoryImpl.kt:30, 55`, `OrderRepositoryImpl.kt:52, 73, 91`, `PositionRepositoryImpl.kt:23` | LOCAL, KIS 조건 | 변경 없음 |
| `infrastructure/persistence/FeePolicyRepository.kt:16-22`, `domain/entity/feepolicy/FeePolicy.kt:31-33` | 체결 때 수수료 정책 조회 | 변경 없음(1단계 토스 체결 없음, 3단계) |
| `domain/entity/order/Execution.kt:84-86` | `execution_mode` STRING | 변경 없음 |
| `application/position/PositionQueryService.kt:50`, `application/portfolio/PortfolioSnapshotCommandService.kt:30` | `getQuote(account.tradingMode, …)` | 변경 없음(토스 계좌는 1단계에 내부 포지션이 없고, 키 조회 결과는 null) |
| `presentation/dto/account/AccountRequest.kt:14`, `AccountResponse.kt:17`, `presentation/controller/AccountController.kt:61` | enum 직렬화, 필터 파라미터 | 변경 없음(자동 수용) |
| `infrastructure/scheduler/KisPaperPollingScheduler.kt:1-10` | 모드 분기 없음, `@Component`가 아니라 빈으로 등록 안 됨 | 영향 없음 |
| `domain/enums/AccountSource.kt:3-6` | KIS, LOCAL (응답 DTO 전용, 저장 안 됨) | `TOSS` 추가, 공통 잔고 결과의 `source`로 씀 |
| `presentation/dto/account/AccountResponse.kt:18, 34` | `source = LOCAL` 하드코딩(KIS 계좌도 LOCAL) | 1단계엔 안 건드림(기존 응답 변경은 AE20 대상, 2단계) |
| collector-api `presentation/internal/InternalSubscriptionController.kt:20-21` | 모드 문자열로 구독 | 영향 없음(D4 선행 거부로 TOSS_LIVE 구독 요청이 가지 않음) |
| trading-web `src/entities/account/model/types.ts:2`, `src/entities/order/model/types.ts:6` | 유니온 타입에 TOSS_LIVE 없음 | 1단계엔 화면 없음(FLAG-05), 2단계 (RISK-07) |
| 테스트 | `TradingMode.entries`, `values()` 열거 없음 | 영향 없음 |

---

## 3. 바꿀 파일·모듈

경로는 `backend/trading-api/` 아래입니다. «main/»은 `src/main/kotlin/com/papertrading/api/`, «test/»는 `src/test/kotlin/com/papertrading/api/`입니다.

| 경로 / 모듈 | 변경 종류 | 목적 | 위험 |
|---|---|---|---|
| main/domain/enums/TradingMode.kt | 수정 | `TOSS_LIVE` | 중간 (모든 분기에 영향, §2 표로 관리) |
| main/domain/enums/AccountSource.kt | 수정 | `TOSS` | 낮음 |
| main/domain/entity/account/Account.kt | 수정 | `validateTradingMode`의 STOCK에 TOSS_LIVE | 낮음 |
| main/application/order/OrderCommandService.kt | 수정 | D4 선행 거부, `submitExternalOrderIfNeeded` 전수 명시 | 높음 (주문 핵심 경로) |
| main/application/order/ExecutionProcessor.kt | 수정 | D6 `else` 제거 | 중간 (체결 정산 경로) |
| main/infrastructure/redis/RedisMarketQuoteAdapter.kt | 수정 | `quote:toss:live:` 키 | 낮음 |
| main/common/exception/CommonApiExceptions.kt | 수정 | 범주 기반 J, I, K 추가(D15), 새 범주 예외는 Spring 무의존 | 낮음 (AE19는 DEC-02) |
| main/common/exception/BrokerOrderNotSupportedException.kt 외 브로커·토스 예외 파일 | 신설 | 브로커·토스 예외(D5, D8, D15). 예외마다 KDoc 첫 줄에 범주 기호(AE6). 파일 하나에 선언 하나(detekt MatchingDeclarationName) | 낮음 |
| main/presentation/exception/GlobalExceptionHandler.kt | 수정 | 새 범주 예외의 범주→HTTP 상태 매핑(D15) | 중간 (모든 에러 응답 경로) |
| main/application/account/port/BrokerAccountBalancePort.kt | 신설 | D7 포트와 값 타입 | 중간 (2단계 KIS가 따라올 모양) |
| main/application/account/BrokerAccountQueryService.kt | 신설 | D8 모드별 어댑터 선택, 미연결 검사 | 중간 |
| main/application/account/result/BrokerAccountBalanceResult.kt | 신설 | 공통 잔고 응답(`accountId`, `source`, `tradingMode`, `asOf`, `currencyBalances`, `positions`) | 낮음 |
| main/presentation/controller/BrokerAccountBalanceController.kt | 신설 | `GET /api/v1/accounts/{accountId}/broker-balance` | 낮음 |
| main/infrastructure/security/AesGcmTokenCipher.kt | 신설 | D10 | 높음 (보안) |
| main/infrastructure/redis/RedisDistributedLock.kt | 신설 | D11 SETNX와 Lua 해제 | 중간 |
| main/infrastructure/toss/TossProperties.kt | 신설 | D17 설정과 활성 판정 | 낮음 |
| main/infrastructure/toss/TossTokenIssuer.kt | 신설 | `POST /oauth2/token` 호출만 맡음(요청 형식은 스키마 확정 후) | 중간 |
| main/infrastructure/toss/TossTokenManager.kt | 신설 | D10, D11, D12의 캐시, 락, 무효화 | 높음 (토큰 1개 제약) |
| main/infrastructure/toss/TossRateLimiter.kt | 신설 | D16 | 낮음 |
| main/infrastructure/toss/TossApiClient.kt | 신설 | 공통 전송: 헤더(`Authorization`, `X-Tossinvest-Account`), 한도, 401 재시도, 429 대기, 에러 매핑(D15) | 높음 |
| main/infrastructure/toss/TossAccountBalanceAdapter.kt | 신설 | `BrokerAccountBalancePort`의 토스 구현(`GET /api/v1/holdings`와 현금 출처) | 중간 |
| main/infrastructure/toss/TossAccountListClient.kt | 신설 | `GET /api/v1/accounts` | 낮음 |
| main/infrastructure/toss/TossMarketDataClient.kt | 신설 | prices, orderbook, candles | 낮음 |
| main/infrastructure/toss/dto/*.kt | 신설 | 토스 응답 DTO(`BigDecimal` 필드), 스키마 확정 후 | 중간 |
| main/application/toss/TossAccountListQueryService.kt, TossMarketQueryService.kt (+ result) | 신설 | 토스 전용 조회. application 계층을 거침(`KisLiveOrderExecutor.kt` 패턴) | 낮음 |
| main/presentation/controller/TossAccountController.kt, TossMarketController.kt | 신설 | `/api/toss/accounts`, `/api/toss/market/*` | 낮음 |
| src/main/resources/application.yaml | 수정 | `trading.toss.*` (§4) | 낮음 |
| test/application/order/OrderCommandServiceTest.kt | 수정 | 모드 전수 표 테스트, TOSS_LIVE 거부(자동 청산 포함) | - |
| test/presentation/controller/OrderControllerIntegrationTest.kt | 수정 | TOSS_LIVE 주문 거부 통합(행 수와 잠금 불변) | - |
| test/application/order/ExecutionProcessorTest.kt, test/domain/entity/account/AccountTest.kt | 수정 | D6, 계좌 검증 | - |
| test/infrastructure/security/AesGcmTokenCipherTest.kt | 신설 | 암복호화 | - |
| test/infrastructure/toss/*Test.kt | 신설 | 토큰(Testcontainers Redis), 전송, 어댑터, 시세, 비활성, 로그 누출, 부동소수점 스캔 | - |
| test/application/account/BrokerAccountQueryServiceTest.kt, test/presentation/controller/BrokerAccountBalanceControllerIntegrationTest.kt, Toss*ControllerIntegrationTest.kt | 신설 | 모드 전수 표, 상태와 code 매핑 | - |
| src/test/resources/fixtures/toss/*.json | 신설 | 정제한 실제 응답(D18) | 중간 (개인정보 정제) |

건드리지 않음: `infrastructure/kis/**`, `KisAccountQueryService.kt`, `KisAccountBalanceController.kt`, collector-api, trading-web, quant-worker, `db/migration/*`.

---

## 4. 데이터·API 작업

| 항목 | 필요한 변경 | 소유 | 막는가(blocking)? |
|---|---|---|---|
| `TradingMode.TOSS_LIVE` | enum 값 추가, DB 변경 없음(D2) | backend | 예 (모든 작업의 시작) |
| Redis 키 | `toss:token`(암호문, TTL), `toss:token:lock`(소유자 UUID, PX) | backend | 아니오 |
| 설정 키와 환경변수 | 필수: `trading.toss.base-url`=`TOSS_BASE_URL`(기본값 없음), `client-id`=`TOSS_CLIENT_ID`, `client-secret`=`TOSS_CLIENT_SECRET`, `token-encryption-key`=`TOSS_TOKEN_ENCRYPTION_KEY`. 선택(기본값 있음): `token-refresh-skew-seconds`=`TOSS_TOKEN_REFRESH_SKEW_SECONDS`, `token-lock-ttl-millis`=`TOSS_TOKEN_LOCK_TTL_MILLIS`, `token-lock-wait-millis`=`TOSS_TOKEN_LOCK_WAIT_MILLIS`, `token-request-deadline-millis`=`TOSS_TOKEN_REQUEST_DEADLINE_MILLIS`, `rate-limit-max-retries`=`TOSS_RATE_LIMIT_MAX_RETRIES`, `rate-limit-max-wait-millis`=`TOSS_RATE_LIMIT_MAX_WAIT_MILLIS`(429 대기 예산), `rate-limit-max-queue-wait-millis`=`TOSS_RATE_LIMIT_MAX_QUEUE_WAIT_MILLIS`(리미터 줄 대기 상한, 기본 5000, 설정된 그룹 간격 중 최댓값 이상이어야 하고 아니면 범위 오류로 토스만 비활성. S6-04 사람 결정 2026-11-15, 하한은 S7-03 사람 결정 2026-11-16으로 «1 이상»에서 바꿈), `rate-limits.<그룹>`(초당 한도, 그룹 이름은 공식 명세의 `Rate Limits Group` 값 `AUTH`·`ACCOUNT`·`ASSET`·`ORDER_INFO`·`MARKET_DATA`·`MARKET_DATA_CHART`, 기본값은 조사 문서 수치). 값은 `.env`나 컨테이너 환경에만 두고 저장소에 넣지 않음 | backend, 사람(키 발급, 암호화 키 생성) | 예 (스키마 확정과 수동 확인부터) |
| 토스 허용 IP 등록 | 수집과 수동 확인을 돌릴 환경의 IP 등록 (DEC-03) | external(사람) | 예 (§5의 4번, 8번) |
| 토스 스키마 확정 (FLAG-06) | 실제 응답 수집, 정제 fixture, 필드표. 확인할 것: REST 호스트, 토큰 요청 형식(form인지 JSON인지)과 응답 필드(`access_token`, `expires_in` 여부), 에러 본문의 code 필드 이름과 값(`expired-token`, `token-revoked`, `ip-not-allowed`, `maintenance`), holdings 필드와 현금 위치, 수익률 단위(비율인지 퍼센트인지), 통화 필드, 종목 기호 형식(내부 `ticker`와의 변환), 엔드포인트별 한도 그룹, 숫자 표기 | external(사람), backend | 예 (§5의 5~7번) |
| `GET /api/v1/accounts/{accountId}/broker-balance` | 신설 | backend | 아니오 |
| `GET /api/toss/accounts`, `GET /api/toss/market/prices`, `/orderbook`, `/candles` | 신설. 파라미터 이름은 토스와 같게(`symbols`, `symbol`, `interval`, `count`, `before`, `adjusted`), 시세 시각은 토스 오프셋 원문 그대로(사람 결정 2026-11-15, 필드표는 steps/step-7.md 부록 A) | backend | 아니오 |
| 주문 API 에러 응답 | TOSS_LIVE, UPBIT_LIVE 계좌 주문은 501 `BROKER_ORDER_NOT_SUPPORTED`(DEC-01). DEC-02로 건드린 파일의 기존 400 응답 code도 범주 기반으로 바뀜. 둘 다 AE20 대상(`OrderController.kt:32` POST와 trading-web 주문·계좌 호출부 읽기 전용 점검) | backend | 아니오 |
| 계약 비교 | OpenAPI 문서와 springdoc이 없음(`build.gradle`)이라 `openapi-diff` 대상 없음. 신설 경로와 enum 값 추가뿐 | backend | 아니오 |

---

## 5. 구현 순서 개요

굵은 순서만입니다. Maker가 5~7개 단계로 나눠 state.md와 steps/step-N.md를 만듭니다. 단계마다 테스트를 먼저 씁니다(CLAUDE.md TDD).

1단계 (토스 읽기 전용):

1. 기준선: 변경 전 `./gradlew test` 결과(실패 목록 포함)를 기록. RULE-10 비교 기준
2. 모드와 주문 안전장치: `TOSS_LIVE`, `AccountSource.TOSS`, 계좌 검증, 범주 기반과 브로커 예외, `placeOrder` 선행 거부(TOSS_LIVE, UPBIT_LIVE, DEC-01)와 전수 `when`(D4, D6), 누락 표 테스트와 통합 테스트. 이어서 건드린 파일의 기존 `check`, `require`를 범주 기반 예외로 정렬하고 바뀐 code를 AE20 목록으로 남겨 trading-web 호출부를 읽기 전용 점검(DEC-02)
3. 토큰 기반: `TossProperties`(비활성 처리), `AesGcmTokenCipher`, `RedisDistributedLock`, `TossTokenManager`(발급기는 가짜로 테스트), 로그 누출 테스트. 스키마 없이 가능
4. 스키마 확정(사람): 허용 IP 등록, 실제 응답 수집, 정제 fixture 커밋, 필드표 기록(§4 확인 목록)
5. 토스 전송 계층: `TossTokenIssuer` 실제 형식, `TossApiClient`(401 재시도, 429 대기, 에러 매핑), `TossRateLimiter`
6. 잔고: 공통 포트, 토스 어댑터, 계좌 목록, `BrokerAccountQueryService`, 내부 API
7. 시세 REST: prices, orderbook, candles, 내부 API
8. 수동 확인: 실제 토스 계좌 잔고 1회, 시세 1회 조회, 전체 테스트 재실행

2단계 (KIS를 공통 브로커 계층으로 리팩터링, 결정·파일·시험·위험은 §10):

1. 기준선과 특성화 시험: 착수 시점 main에서 `./gradlew test` 결과와 KIS 시험 목록을 기록하고, 옛 코드에 경계 특성화 시험(D21)을 넣어 통과시킴. RULE-10 비교 기준
2. KIS 에러 정렬(DEC-05가 A일 때만): 범주 기반 KIS 예외, 잔고 경로 매핑, 핸들러 고정 문구, AE20 목록과 trading-web 호출부 재점검(D28). 3번이 같은 파일을 건드리기 전에 끝냄(AE19). DEC-05가 B면 이 묶음은 없음
3. 브로커 선택과 KIS 잔고: 포트별 지원 모드와 기동 때 검사(D25), KIS 잔고 어댑터를 `BrokerAccountBalancePort`로(D22), `broker-balance`의 KIS 지원(D24), `/api/kis/account/balance`는 계약 그대로 공통 포트 경유(D23)
4. 주문 공통 포트: `BrokerOrderPort`와 `KisOrderAdapter`, 실행기 둘을 `BrokerOrderSubmitter` 하나로, 주문 경로 KIS 에러 정렬(D26, D28). TOSS_LIVE·UPBIT_LIVE 501 그대로
5. 계좌 응답 출처와 ADR: `AccountResponse.source`를 모드에서 산출(D29), ADR-005 갱신(D25, D26)
6. 체결 입력 공통화: PR #53 머지 뒤에만. `KisExecutionNotice`를 application의 `BrokerExecutionNotice`로(D27)
7. 최종 확인: 빌드·전체 시험·린트, 기준선과 특성화 시험 비교, (선택) KIS 모의 계좌로 두 잔고 경로 값 대조 1회(사람)
- 선행 조건: 1단계 기준선 대비 KIS 테스트 전부 통과(RULE-10). 1단계 §5 8번 수동 실호출 대조는 병행하며 선행 조건이 아님(D20, 사람 결정 2026-11-16)
- 2단계에서 뺀 것: trading-web 타입과 화면(FLAG-05, RISK-07), 토스 실주문, KIS 토큰 이전(🔴, DEC-07)

3단계 (토스 시세 웹소켓, 실주문과 체결 이벤트), 큰 줄기:
- collector-api가 토스 웹소켓을 받아(FLAG-08) Redis `quote:toss:live:*`에 기록. 토스 실주문 어댑터(`clientOrderId`와 내부 `idempotencyKey` 연결). 체결 이벤트를 `ExecutionProcessor` TOSS_LIVE 분기로(1단계의 예외를 대체)
- 선행 조건: FLAG-09 tier 재판정(실거래, 샌드박스 없음). 주문, 조건주문, 웹소켓 스키마 확정. collector-api가 같은 키와 같은 락 규약으로 `toss:token`을 공유(RULE-06). 호출 프로세스가 둘이 되므로 Redis 공유 한도. 웹소켓 제약(연결 2개, 구독 100건, 구독 전체 교체 방식, LOSSY, 재연결 뒤 `GET /orders`로 재대사) 설계. 고액 주문 확인(1억원 이상)과 반대 방향 대기 주문(409) 정책. 실주문은 따로 사용자 승인

---

## 6. 테스트 계획

명령은 모두 `cd /Users/dongwoo/Desktop/paper-trading/.worktrees/multi-broker-kis-toss/backend/trading-api` 뒤에 실행합니다. Testcontainers 테스트는 Docker가 떠 있어야 합니다.

| 테스트 | 종류 | 명령 / 방법 | 기대 결과 |
|---|---|---|---|
| 기준선 | integration | `./gradlew test` (변경 전) | 결과와 실패 목록을 step 문서에 기록 |
| 모드 누락 표: 주문 | unit | `OrderCommandServiceTest`: `TradingMode.entries` 전부에 기대 동작 표(LOCAL은 내부 매칭, KIS는 실행기, TOSS와 UPBIT는 거부). 표의 키 집합이 `entries`와 같은지 단언 | 새 모드를 표에 안 넣으면 실패 |
| TOSS_LIVE 주문 거부: 단위 | unit | `placeOrder` 지정가 매수, 시장가 매수, 매도, IOC, FOK, `createAutoExitSellOrder`, `createGroupedAutoExitSellOrder` | 모두 `BrokerOrderNotSupportedException`. `orderRepository.save`, `accountLedgerRepository.save`, 포지션 잠금, `collectorSubscriptionPort.subscribe`, `marketQuotePort.getQuote` 호출 0회 |
| TOSS_LIVE 주문 거부: 통합 | integration | `OrderControllerIntegrationTest` 패턴. TOSS_LIVE 계좌로 `POST /api/v1/accounts/{id}/orders` (매수, 포지션을 넣은 뒤 매도, IOC) | 501, code `BROKER_ORDER_NOT_SUPPORTED`. orders와 ledgers 행 수 불변, `lockedDeposit` 0, `availableDeposit` 불변, 포지션 잠금 수량 불변, 구독 mock 미호출 |
| 체결 분기 | unit | `ExecutionProcessorTest`: TOSS_LIVE, UPBIT_LIVE 매도 체결, LOCAL, KIS 기존 경우 | TOSS_LIVE와 UPBIT_LIVE는 예외와 입금 없음(DEC-01), LOCAL과 KIS는 기존과 같음 |
| 에러 정렬 | unit, integration | 건드린 파일의 기존 `check`, `require` 경로마다 응답 상태와 code 단언. AE20 목록과 trading-web 호출부 점검 결과를 step 문서에 기록 | 범주 기반 code로 응답, 프론트 수정 필요 시 멈춤(DEC-02) |
| 계좌 검증 | unit | `AccountTest` | STOCK+TOSS_LIVE 허용, CRYPTO+TOSS_LIVE 거부 |
| 암호기 | unit | `AesGcmTokenCipherTest` | 왕복 일치, 같은 평문을 두 번 암호화하면 암호문이 다름, 변조나 다른 키는 실패, 키 길이가 틀리면 거부 |
| 토큰 동시 발급과 저장 | integration (Testcontainers Redis) | `TossTokenManager` 인스턴스 2개와 스레드 8개가 빈 캐시에서 동시에 `getToken`. 발급기는 호출 수를 세는 가짜 | 발급 정확히 1회, 모두 같은 토큰. Redis 원값은 `v1.`로 시작하고 평문 토큰을 담지 않음. TTL은 `expires_in`에서 여유를 뺀 값 |
| 락 | integration (Testcontainers Redis) | 소유자가 아닌 쪽의 해제, 대기 한도 초과 | 남의 락은 안 풀림. 대기 초과 시 `TOSS_TOKEN_UNAVAILABLE`, 발급 0회 |
| 401 처리 | unit (`MockRestServiceServer`) | `token-revoked`, `expired-token` 응답 | 1회 재발급이나 재조회 뒤 성공. 다른 프로세스가 이미 새 토큰을 저장했으면 발급 0회. 401이 연속이면 `TOSS_AUTH_FAILED`, 발급 최대 1회 |
| 에러 구분 | unit (`MockRestServiceServer`) | 403 `ip-not-allowed`, 429(`Retry-After` 있음, 없음, 상한 초과, 반복), 500 `maintenance`, 502와 503, 타임아웃, 깨진 본문, 400 | 각각 D15의 code. 403은 재시도 0회. 429 대기 시간은 가짜 sleeper로 검증. 우리 응답 message에 토스 원문 없음 |
| 비활성 | unit | 필수 키 4개를 하나씩 비움, 암호화 키 형식 오류 | 컨텍스트 기동 성공, 호출 때 `TOSS_INTEGRATION_DISABLED`, 메시지에 키 이름만 |
| 잔고 어댑터 | unit (fixture) | 정상(KRW+USD), 보유 0, 필수 필드를 하나씩 뺀 응답, 숫자를 문자열과 숫자로 둘 다, 긴 자릿수(예: 정수부 15자리와 소수 6자리) | `BigDecimal` 값 정확 보존(`compareTo == 0`, `toPlainString` 일치), USD가 KRW 합계에 섞이지 않음, 누락은 `TOSS_RESPONSE_INVALID`, `X-Tossinvest-Account` 헤더가 accountSeq |
| 모드 누락 표: 잔고 | unit | `BrokerAccountQueryServiceTest`: `entries` 전부를 표로 | TOSS_LIVE는 어댑터, KIS와 UPBIT는 501, LOCAL은 404 `NOT_BROKER_ACCOUNT`, 미연결 TOSS는 409, 없는 계좌는 404 `ACCOUNT_NOT_FOUND` |
| 시세 | unit (fixture) | prices, orderbook, candles | 가격과 수량이 `BigDecimal`로 매핑, 누락은 `TOSS_RESPONSE_INVALID` |
| 컨트롤러 | integration | `KisAccountBalanceControllerIntegrationTest` 패턴(서비스 `@MockitoBean`) | 경로, 상태, code 매핑 |
| 부동소수점 스캔 | unit (결정적 checker) | 소스 파일 스캔 테스트: `main/infrastructure/toss/**`, `main/application/account/port/Broker*`, `main/application/account/result/Broker*`, `main/application/toss/**` | `Double`, `Float`, `toDouble`, `toFloat` 0건 |
| 비밀값 로그 누출 | unit | logback `ListAppender`로 발급, 401 재시도, 에러 경로의 로그 수집 | 가짜 토큰, client secret, 암호화 키 문자열 0건. accountSeq는 끝 4자리만 |
| 빌드와 전체 | integration | `./gradlew compileKotlin && ./gradlew test` | 전부 통과, 기준선 대비 새 실패 0 (RULE-10) |
| 실제 계좌 | manual | 허용 IP 환경에서 `broker-balance` 1회, `prices` 1회 | 금액과 수량이 토스 앱 화면과 일치. 기록은 마스킹 |

---

## 7. 도메인 제약 반영 (어떻게 지킬 것인가)

규칙 출처는 이 저장소 `CLAUDE.md`의 «Financial Safety»입니다(`rules/<project>` 폴더 없음). 한도 수치는 설정값으로만 둡니다.

| 차원 | 적용 | 구현·검증 방법 | review routing |
|---|---|---|---|
| 수치 무결성 | y | 토스 DTO의 금액, 수량, 가격은 `BigDecimal` 필드로 직접 역직렬화(D13), Map이나 Double을 거치지 않음. 결정적 checker는 부동소수점 스캔 테스트. golden은 긴 자릿수 fixture 정확 보존, 문자열과 숫자 표기 둘 다. 수익률을 직접 계산할 때는 기존 KIS 방식(`KisAccountBalanceAdapter.kt:162-168`: scale 6 HALF_UP, ×100, scale 2)과 같게 | 수치 매핑 변경은 사람 확인 |
| 단위·기준치 | y | 통화별 `BrokerCurrencyBalance`로 나누고 환산·합산 코드 경로를 두지 않음(FLAG-07 가정, 환율 API 미호출). 수익률 단위와 통화 필드는 스키마 확정 확인 목록에 있음. golden은 KRW+USD fixture에서 통화별 합계가 서로 섞이지 않음 | 원화 합산이 필요해지면 ASK (FLAG-07) |
| 결측·LOD/LOQ | y | 결측·이상치로 적용. 필수 필드 누락과 변환 실패는 `TOSS_RESPONSE_INVALID`, 0 대체 금지(D14). 필수와 선택은 스키마 확정 필드표가 기준. golden은 필수 필드를 하나씩 뺀 fixture | 필드표 확정은 사람 확인 |
| 규제·표현 | n | - | - |
| PHI | n | 참고: accountSeq와 보유 내역은 금융 개인정보라 fixture는 가짜 값으로 정제, 로그는 끝 4자리 마스킹, 토큰과 키는 로그·응답·커밋 금지(RULE-05). 로그 누출 테스트로 검증 | - |

---

## 8. 리스크·막힘

| ID | 리스크 / 막힘 | impact | 완화 / 필요한 결정 |
|---|---|---|---|
| RISK-01 | 토스 개별 API 스키마 미확보(FLAG-06). 발급기, 매핑, 에러 본문 code를 만들 수 없음 | 높음 | §5의 4번(사람)을 5~7번 앞에 둠. 2번, 3번은 스키마 없이 진행 |
| RISK-02 | 허용 IP 필수. 로컬과 서버 IP를 각각 등록해야 하고 회선이 바뀌면 403 | 중간 | `TOSS_IP_NOT_ALLOWED` 고정 안내, 재시도 안 함 |
| RISK-03 | 토큰은 클라이언트당 1개. 같은 client_id를 다른 Redis를 쓰는 환경(로컬, 서버)이나 외부 도구가 함께 쓰면 서로 폐기함. 분산 락은 같은 Redis 안만 보호 | 높음 | DEC-03 |
| RISK-04 | `ddl-auto: create`(`application.yaml:19`)라 재기동마다 스키마가 다시 생겨 토스 계좌 연결도 재기동 때 사라질 수 있음(기존 KIS와 같고 이번 범위 밖). 반대로 `.env`에서 ddl-auto를 덮어써 기존 DB를 유지하는 환경이면, Hibernate가 enum 컬럼에 만들었을 수 있는 CHECK 제약이 `TOSS_LIVE` 저장을 막을 수 있음. `.env`는 저장소 밖이라 확인하지 못함 | 중간 | §5의 2번 착수 때 운영자에게 실제 ddl-auto 값 확인. 덮어쓴 환경이면 제약 재생성 SQL 전에 멈추고 ASK |
| RISK-05 | 건드린 파일 전체를 에러 규칙에 맞추는 의무(backend-api.md AE19). `OrderCommandService`, `ExecutionProcessor`, `Account`, `CommonApiExceptions`의 기존 `check`, `require`(400으로 응답)와 KIS 502, 504를 지금 맞추면 기존 에러 응답이 대거 바뀜(AE20) | 중간 | DEC-02 |
| RISK-06 | UPBIT_LIVE 주문은 지금도 외부 제출 없이 자금만 잠김(`else -> {}`, 내부 매칭은 LOCAL만 대상 `OrderRepositoryImpl.kt:52`). FLAG-02와 같은 결함 | 중간 | DEC-01 |
| RISK-07 | trading-web 타입(`src/entities/account/model/types.ts:2`)에 TOSS_LIVE가 없어 계좌 목록에 낯선 값으로 보일 수 있음 | 낮음 | 1단계엔 화면 없음(FLAG-05), 2단계 화면 작업 때 타입 추가 |
| RISK-08 | 429 대기가 요청 스레드를 막음 | 낮음 | 최대 대기 상한을 설정하고 넘으면 즉시 503 |
| RISK-09 | 수익률 단위(비율과 퍼센트) 혼동으로 100배 오차 | 중간 | 스키마 확정 확인 목록과 fixture 테스트 |
| RISK-10 | Testcontainers 테스트는 Docker가 필요. 기준선과 전체 테스트를 못 돌리면 RULE-10 증거가 없음 | 중간 | Docker 기동을 확인한 뒤 실행. 못 돌리면 완료로 보고하지 않음 |
| DEC-01 | 결정됨(사용자, 2026-11-15): UPBIT_LIVE도 501 `BROKER_ORDER_NOT_SUPPORTED`로 거부. 주문이 선행 거부되므로 `ExecutionProcessor`의 UPBIT_LIVE 매도 분기도 도달 불가가 되어 TOSS_LIVE와 같이 예외, 입금 없음(RULE-03). 주문 API 응답이 바뀌므로 AE20 대상(trading-web 호출부 점검) | 중간 | 기각: 지금 동작 유지와 TODO 결함 등록(알려진 결함을 코드에 남김) |
| DEC-02 | 결정됨(사용자, 2026-11-15): AE19를 1단계에서 지금 정렬. 건드린 파일(`OrderCommandService`, `ExecutionProcessor`, `Account`, `CommonApiExceptions`)의 기존 `check`, `require`를 범주 기반 예외(D15)로 옮기고, 바뀌는 에러 code는 AE20 목록으로 남겨 trading-web 호출부를 읽기 전용으로 점검. 프론트 수정이 필요하면 멈추고 ASK(trading-web 변경은 범위 밖). KIS 502, 504 정렬은 `infrastructure/kis/**`라 2단계 그대로 | 중간 | 기각: `API-Errors-Deferred`로 미루기(건드린 파일에 옛 400 처리가 남음) |
| DEC-03 | 결정됨(사용자, 2026-11-15): 1단계 동안 토스 키는 로컬 한 곳에만 둠. 허용 IP는 지금 맥의 공인 IP만 등록. 서버 배포 때 키 위치를 다시 정하고 재발급 | 높음 | 기각: 서버 한 곳만(서버 배포가 아직 없음), 환경별 키 분리(토스가 키를 여러 개 발급하는지 미확인) |
| DEC-04 | 조건부 결정: 스키마 확정 때 holdings에 현금이 없으면 무엇을 «현금»으로 보나 | 중간 | 결정됨(사람 2026-11-15): 공식 명세에 예수금 API가 없어 `cashBalance`는 null, 매수 가능 금액은 `cashBuyingPower` 별도 칸(D7). 예수금으로 대체하지 않음 |

---

## 9. 구현자에게 넘기기 (Handoff)

구현자는 아래 경계 안에서만 작업합니다. 실행은 steps/step-1.md부터입니다. 착수 전에 Maker는 §5를 5~7단계로 나눠 `step-scaffold`로 state.md 로드맵과 step-1을 만들고, 로드맵과 계획이 확정되면 사람이 `node tools/feature-approval.js set multi-broker-kis-toss tech-plan --by <승인자>`로 설계 승인 흔적을 남깁니다.

이것만 만들어라 (승인된 범위):
- `TradingMode.TOSS_LIVE`, `AccountSource.TOSS`, 계좌 검증(STOCK만)
- `placeOrder` 선행 거부, `submitExternalOrderIfNeeded`와 `ExecutionProcessor`의 전수 `when`, `RedisMarketQuoteAdapter` 토스 키, 모드 누락 표 테스트
- 범주 기반 예외(J, I, K)와 §2 D15의 브로커·토스 예외
- 토스 설정과 비활성, AES-GCM 암호기, Redis 분산 락, 토큰 관리(401 재시도 1회), 그룹별 한도와 429 처리
- `BrokerAccountBalancePort`와 토스 어댑터, `GET /api/v1/accounts/{accountId}/broker-balance`
- `GET /api/toss/accounts`, `GET /api/toss/market/prices`, `/orderbook`, `/candles`
- 정제 fixture와 §6 테스트 전부

이건 하지 마라 (범위 밖):
- `infrastructure/kis/**`, `KisAccountQueryService`, `/api/kis/account/balance` 변경 (2단계)
- collector-api, trading-web, quant-worker 변경
- 토스 주문, 조건주문, 웹소켓, 체결 이벤트, 랭킹, 섹터, 수급 API (3단계 또는 non-goal)
- USD의 원화 환산과 합산, 환율 API 호출
- 주문·체결 공통 포트, KIS 토큰 암호화 이전, `AccountResponse.source` 수정 (2단계)
- 새 라이브러리 추가, 한도나 IP 같은 토스 정책 수치의 코드 상수화
- 실제 토큰, 키, accountSeq, 실제 금액을 커밋, 로그, 응답, 보고에 싣기

이때는 멈추고 물어봐라:
- 에러 정렬 결과 trading-web 호출부 수정이 필요할 때(DEC-02, 프론트 변경은 범위 밖)
- 토스 키를 로컬 외 환경에 두려 할 때(DEC-03)
- 스키마가 §2 D7 포트 모양과 맞지 않을 때, holdings에 현금이 없을 때(DEC-04), 수익률 단위나 통화 필드가 불분명할 때
- 토큰 발급이 client credentials 방식이 아니거나, `accountSeq`가 계좌별 식별자가 아니거나, 한도·IP·토큰 정책이 조사 문서와 다를 때
- 운영 DB가 ddl-auto를 덮어써 enum 제약 변경 SQL이 필요할 때(RISK-04)
- 기준선 대비 KIS 테스트가 새로 실패할 때, Docker가 없어 통합 테스트를 못 돌릴 때
- 새 의존성이 필요해 보일 때

---

## 10. 2단계 상세 (KIS를 공통 브로커 계층으로 리팩터링)

> 2026-11-16 추가(Claude tech-planner). 1단계 절(§0~§9)의 결정은 그대로 두고, 2단계 결정은 D20부터, 사람 결정은 DEC-05부터 씁니다. 굵은 순서는 §5 «2단계»입니다.
> 줄 번호는 main `263f047` 기준이고 경로 줄임은 §3과 같습니다(«main/», «test/»). 범위는 trading-api만입니다(사람 결정 2026-11-16).

### 10.0 메타데이터

| 항목 | 값 |
|---|---|
| 기반 | feature-spec §3 2단계, §4 6행, RULE-01, RULE-03, RULE-10. 사람 결정 2026-11-16(범위) |
| 날짜 | 2026-11-16 |
| 대상 브랜치 | `feature/multi-broker-kis-toss-phase2` (main `263f047`, 1단계 step 1~7 머지 뒤) |
| 상태 | draft (설계 재승인 전) |

### 10.1 구현 가능성

| 항목 | 값 |
|---|---|
| feature-spec 준비됨? | ready (2단계 의도는 §3 «공통 브로커 계층으로 KIS 전면 리팩터링», §4 6행) |
| 막힌 ASK | 없음. DEC-05~DEC-08은 2026-11-16 사용자가 모두 A로 결정(§10.8) |
| 판정 | 🟡 일부 조건부, 🔴 1건 |
| 코멘트 | 🔴는 KIS 토큰 이전 하나이고 §5 2단계 순서에 넣지 않았습니다(DEC-07). KIS 에러 정렬과 시그니처에 묶인 KIS 시험 처리는 RULE-10을 어떻게 판정할지(DEC-05)에 달렸습니다. 체결 묶음은 PR #53 머지 뒤에만 됩니다 |

부분별 가능성:

| 부분 | 가능? | 필요한 선행 작업 | 근거(읽은 파일) |
|---|---|---|---|
| 기준선과 경계 특성화 시험 | 🟢 | Docker | 1단계 Testcontainers·`MockRestServiceServer` 시험(§6), `test/presentation/controller/KisAccountBalanceControllerIntegrationTest.kt:30-46` |
| KIS 잔고를 `BrokerAccountBalancePort`로 | 🟢 | - | `main/infrastructure/kis/KisAccountBalanceAdapter.kt:28-88`(모드를 `trId` 문자열로 판정 L41, 국내주식 API L44), `main/application/account/port/BrokerAccountBalancePort.kt:12-54`, `main/application/account/port/KisAccountBalancePort.kt:6-27` |
| `/api/kis/account/balance` 계약 유지 | 🟢 | - | `main/presentation/controller/KisAccountBalanceController.kt:12-20`, `main/application/account/KisAccountQueryService.kt:19-63`, `main/application/account/result/KisAccountBalanceResult.kt:8-19` |
| `broker-balance`의 KIS 지원 | 🟢 | - | `main/application/account/BrokerAccountQueryService.kt:26-34` |
| 브로커 선택 재설계 | 🟢 | - | `BrokerAccountQueryService.kt:21-22`(매개변수 이름=빈 이름에 기댐), `src/test/resources/archunit_store/stored.rules`(application의 Spring 의존 동결 규칙 `5ffdb0db`) |
| 주문 공통 포트 (KIS만, TOSS_LIVE·UPBIT_LIVE 501 유지) | 🟢 | - | `main/application/order/OrderCommandService.kt:42-53, 179-199`, `KisPaperOrderExecutor.kt:15-40`, `KisLiveOrderExecutor.kt:16-43`, `main/infrastructure/kis/KisOrderRestClient.kt:39-65`, 레포 규칙 `rules/backend-api.md:37, 40` |
| 체결 입력 공통화 | 🟡 | PR #53 머지 | `main/application/order/KisExecutionNoticeService.kt:3, 19-53`, `main/infrastructure/kis/KisExecutionWebSocketManager.kt:4, 134, 227-229`, PR #53 변경 파일 목록(`gh pr view 53`), `git merge-tree` 시험 결과(RISK-11) |
| KIS 에러 정렬 | 🟡 | DEC-05 | `main/common/exception/CommonApiExceptions.kt:67-104`, `KisAccountBalanceAdapter.kt:58-69, 90-102`, `KisAccountBalanceControllerIntegrationTest.kt:86-111`(401·403·502·504와 예외 원문 message 단언), `main/presentation/exception/GlobalExceptionHandler.kt:36-43, 96-114` |
| `AccountResponse.source`를 모드에서 산출 | 🟢 | - | `main/presentation/dto/account/AccountResponse.kt:18, 34`, `main/domain/enums/AccountSource.kt:3-7`, trading-web `src`에 `source` 읽는 곳 0건 |
| KIS 토큰을 D10·D11 방식으로 | 🔴 (이번 범위) | 범위를 collector-api까지 넓히거나 미룸(DEC-07) | `main/infrastructure/kis/KisTokenManager.kt:14-16, 28, 32-40, 85`, collector-api `infra/kis/source/rest/KisTokenRedisStore.kt:12-21`, `KisAccessTokenClient.kt:33-41, 63-64`(같은 키 `kis:token:{mode}`를 평문으로 읽고 씀, DB 저장소도 따로 둠) |

🔴 사유: trading-api만 암호화하면 collector-api가 암호문을 KIS 토큰으로 보내 KIS 인증이 실패하거나, trading-api가 collector가 넣은 평문을 못 풀어 KIS 토큰을 따로 또 발급합니다(KIS 토큰은 발급 횟수가 제한돼 두 서비스가 공유 중, `KisTokenManager.kt:16`). 되게 하려면 collector-api의 저장·발급을 같은 형식·키·락 규약으로 함께 바꿔야 하는데 2단계 범위 밖입니다.

### 10.2 아키텍처·통합 결정 (D20~)

| 결정 | 선택 | 근거 | 기각한 대안 |
|---|---|---|---|
| D20 2단계 범위와 선행 조건 | trading-api만. trading-web 타입·화면(FLAG-05, RISK-07)과 토스 실주문은 뺌. TOSS_LIVE·UPBIT_LIVE 주문은 지금처럼 501 `BROKER_ORDER_NOT_SUPPORTED`(D4, D5, DEC-01 그대로). 선행 조건은 «1단계 기준선 대비 KIS 테스트 전부 통과(RULE-10)»로 좁히고, 1단계 §5 8번 수동 실호출 대조는 병행 | 사람 결정 2026-11-16 | 1단계 완료(수동 확인 포함)를 선행 조건으로: 허용 IP·키 준비 일정에 2단계가 묶임. trading-web 포함: 사람 결정으로 뺌 |
| D21 RULE-10 증거 | §5 2단계 1번에서 옛 코드에 경계 특성화 시험(입력을 고정하고 밖으로 보이는 결과를 통째로 단언하는 시험)을 먼저 넣어 통과시키고, 이후 모든 묶음에서 고치지 않고 통과시킴. 경계 셋: (a) `/api/kis/account/balance`를 서비스 mock 없이 끝까지(KIS 응답은 `MockRestServiceServer` fixture, 토큰 관리자만 mock) 부른 응답 JSON과 KIS 호출 URL·`tr_id`·헤더 (b) KIS_PAPER·KIS_LIVE 주문 접수의 KIS 요청(경로, `tr_id`, 본문), `externalOrderId` 저장, 원장·잠금 행, Slack 문구 (c) KIS 체결 통지 1건이 만드는 체결·포지션·원장 행. 바뀌는 시그니처에 묶인 기존 KIS 시험의 처리는 DEC-05 | 기존 시험이 옛 시그니처에 묶여 글자 그대로는 전후 같은 시험을 못 돌림: `fetchBalance(1L, "VTTC8434R")`(`test/application/account/KisAccountQueryServiceTest.kt:41-56`, `test/infrastructure/kis/KisAccountBalanceAdapterTest.kt:73, 102`), 실행기 mock(`test/application/order/OrderCommandServiceTest.kt:62-68, 209-240`). 컨트롤러 시험은 서비스를 mock해(`KisAccountBalanceControllerIntegrationTest.kt:38`) 리팩터링 대상 코드를 지나지 않음 | 단위 시험만으로 판정: 경계 동작 변화를 못 잡음. 옛 시그니처를 남기는 덧댐 어댑터: 공통 계층으로 못 가고 죽은 포트가 남음 |
| D22 KIS 잔고 이전 | `KisAccountBalanceAdapter`가 `BrokerAccountBalancePort`를 구현(지원 모드 KIS_PAPER, KIS_LIVE). `trId`와 REST 주소 모드는 `ref.tradingMode` 전수 `when`으로(KIS_PAPER → `VTTC8434R`·paper, KIS_LIVE → `TTTC8434R`·live, 그 밖은 범주 K 내부 오류이고 KIS 호출 없음). 계좌 로드는 어댑터에서 빼고 호출 서비스가 `BrokerAccountRef`로 넘김. 응답 해석(Map 파싱, 누락 시 0)은 그대로(가정 3). 통화는 KRW 한 줄: `cashBalance` = `dnca_tot_amt`, `cashBuyingPower` = null(이 API에 매수 가능 금액이 없고 새 KIS 호출은 만들지 않음), 나머지 합계는 지금 값. 종목 `currency`는 KRW. `asOf`는 KST `LocalDateTime`. `KisAccountBalancePort`, `KisBalanceSnapshot`, `KisBalancePosition` 삭제. 이력: D7 «KIS 전용 `trId` 인자는 뺌(2단계에 KIS 어댑터가 `tradingMode`로 정함)» | 국내주식 잔고 API(`KisAccountBalanceAdapter.kt:44`)라 KRW만 옴. 지금 모드 판정이 `trId` 문자열 비교(L41)라 모드가 문자열로 새고 있음. RULE-03 | 새 어댑터를 따로 두고 옛 어댑터 유지: 같은 KIS 호출 코드가 둘. 포트에 `trId` 인자 추가: D7에서 기각한 KIS 전용 인자 |
| D23 `/api/kis/account/balance` 존폐 | 유지, 성공 응답 계약 불변(`KisAccountBalanceResult` 그대로, 금액은 JSON 숫자 그대로. FLAG-10 문자열은 새 API만). `KisAccountQueryService`가 계좌를 읽고 `mode` 파라미터를 모드로 바꿔(LIVE → KIS_LIVE, PAPER → KIS_PAPER) `BrokerAccountRef`를 만들어 KIS 어댑터를 부름. 지금처럼 `trId`는 계좌 모드가 아니라 `mode` 파라미터가 정함(가정 2). 대사(reconciliation) 계산은 이 서비스에 그대로 | trading-web `src`에 호출부 0건(읽기 전용 검색). 진행 중인 trading-web 재제작 계획이 이 경로를 쓸 예정(`docs/features/running/trading-web-shadcn-rebuild/development-plan.md:50, 167`), 프록시 표에 접두사가 있음(`frontend/trading-web/proxy.config.ts:25`). RULE-10 | 삭제: 다른 기능의 계획이 깨지고 AE20 큰 변경. `broker-balance`로 넘기기: 금액 표기(문자열)와 대사 유무가 달라 같은 계약이 아님 |
| D24 `broker-balance`의 KIS 지원 | `BrokerAccountQueryService`에서 KIS_PAPER, KIS_LIVE를 501에서 KIS 어댑터 조회로. `source` = KIS. `externalAccountId`가 `XXXXXXXX-XX`(지금 KIS 규칙 `KisAccountBalanceAdapter.kt:113-119`)가 아니면 409 `KIS_ACCOUNT_NOT_LINKED`(F-4, `TOSS_ACCOUNT_NOT_LINKED`와 같은 범주). 금액은 문자열(FLAG-10). 대사는 넣지 않음(가정 4). UPBIT_LIVE 501, LOCAL 404는 그대로. 501 → 200은 기존 응답 변경이라 §10.4 AE20 목록에 올림 | feature-spec §4 6행 «어떤 계좌로든 … 공통 포트가 해당 어댑터를 고름». D9가 계좌 하위 경로를 고른 이유(2단계에 KIS가 와도 경로 유지) | KIS는 옛 경로만 쓰게 501 유지: 2단계 목적과 어긋남 |
| D25 브로커 선택 방식 (D8 재검토) | 공통 포트마다 `supportedModes: Set<TradingMode>`를 두고, 서비스는 `List<포트>`를 주입받아 생성 때 모드 → 구현 표를 만듦. 어느 모드를 어댑터·501·404로 보낼지는 서비스의 전수 `when`(else 없음)이 정하고, 생성 때 «`when`이 어댑터로 보내는 모드 집합 = 구현 표의 키 집합, 모드마다 구현 1개»를 검사해 어긋나면 기동 실패. 잔고 포트와 주문 포트에 같은 방식. ADR-005를 이 방식으로 갱신. 이력: D8(1단계는 어댑터 1개라 레지스트리 기각, «2단계 공통 계층 설계 때 다시 봄») | 지금 선택은 매개변수 이름과 빈 이름이 같다는 약속에 기댐(`BrokerAccountQueryService.kt:21-22`). KIS 어댑터가 같은 포트를 구현하면 그 약속 하나에 선택이 걸림. application은 Service·Async·트랜잭션·context.event 밖의 Spring 의존이 ArchUnit으로 막혀 `@Qualifier`를 못 씀(`stored.rules` `5ffdb0db`). 모드 누락이 컴파일(`when`)과 기동(표 검사) 두 곳에서 드러남(RULE-03) | `supports()` 레지스트리만: 누락이 호출 때만 드러남. 매개변수 이름 주입 유지: 암묵적이고 어댑터가 늘수록 깨지기 쉬움. 모드마다 포트 인터페이스: 포트 수가 모드 수만큼 늘어남 |
| D26 주문 공통 포트 | `main/application/order/port/BrokerOrderPort`(브로커에 주문을 접수하는 포트): `supportedModes`, `submit(order: Order): BrokerOrderReceipt`(`externalOrderId`). KIS 구현 `main/infrastructure/kis/KisOrderAdapter`가 `KisOrderRestClient.placeOrder`를 감싸고 모드로 paper·live를 정함(`KisOrderRestClient`는 안 고침). 두 실행기를 `BrokerOrderSubmitter`(포트를 골라 접수하고 외부 주문번호 기록, 로그, Slack 알림을 하는 서비스) 하나로 합치고 로그·Slack 문구는 모드 이름으로 지금과 같은 글자. `submitExternalOrderIfNeeded`의 KIS 두 분기는 이 서비스 호출, TOSS_LIVE·UPBIT_LIVE는 D4 선행 거부와 방어 분기 그대로. 취소는 포트에 넣지 않음(DEC-08). 이력: ADR-005 «OrderExecutionPort» | 레포 규칙 «application → infrastructure 금지, 출력 포트는 `application/<컨텍스트>/port`»(`rules/backend-api.md:37, 40`)인데 실행기가 `KisOrderRestClient`를 직접 import(`KisPaperOrderExecutor.kt:5`, `KisLiveOrderExecutor.kt:5`). 두 실행기는 모드 문자열과 문구만 다름. 지금 주문 취소는 KIS 취소를 부르지 않음(`OrderCommandService.kt:260-303`, 실행기 `cancel()` 호출 0건) | 실행기 둘을 두고 포트만 주입: 모드마다 KIS 이름의 클래스가 남음. 토스 주문 어댑터 자리까지 지금 만들기: 실주문 범위 밖, 쓰지 않는 코드. 포트에 취소를 넣고 연동: KIS 동작이 바뀜(RULE-10) |
| D27 체결 입력 공통화 | 입력 경계만 공통화: application이 import하는 infrastructure 타입 `KisExecutionNotice`(`KisExecutionNoticeService.kt:3`)를 application 명령 `BrokerExecutionNotice`(브로커 체결 통지, 모드 포함)로 바꾸고 `KisExecutionNoticeService`를 `BrokerExecutionNoticeService`로 이름을 바꿈. KIS 주문 찾기 쿼리와 `ExecutionProcessor`는 그대로. PR #53 머지 뒤에만 착수하고, 그 차례에 PR #53이 열려 있으면 멈추고 ASK | 토스 체결은 3단계이고 실주문이 빠져 체결 쪽 구현은 KIS 하나뿐(최소 코드). PR #53이 같은 두 파일과 `ExecutionProcessor`를 고침(RISK-11) | 체결 처리 포트 신설: 구현 1개, 쓰는 곳 없음. PR #53 전에 착수: 충돌, D6 되돌림 위험 |
| D28 KIS 에러 정렬 (적용은 DEC-05 뒤) | KIS 쪽 실패는 범주 J(503)로 두고 code로 원인을 가름: 인증 `KIS_AUTH_FAILED`, 권한 `KIS_PERMISSION_DENIED`, 그 밖 원격 실패 `KIS_UNAVAILABLE`, 타임아웃 `KIS_TIMEOUT`, 호출 한도 `KIS_RATE_LIMITED`. 계좌번호 없음·형식 틀림은 F-4(409) `KIS_ACCOUNT_NOT_LINKED`. KIS가 주문을 받지 않은 경우(`KisErrorCode`의 ORDER_REJECTED, INVALID_ACCOUNT, DUPLICATE_OR_ALREADY_PROCESSED)는 A(400) `KIS_ORDER_REJECTED`(D15의 `TOSS_REQUEST_REJECTED`와 같은 범주). 잔고 경로는 `KisAccountBalanceAdapter`에서, 주문 경로는 새 `KisOrderAdapter`에서 바꿈. 새 예외는 1단계 범주 기반(Spring 무의존, D15)을 상속하고 응답 message는 code별 고정 문구(KIS 원문은 로그만, AE8). 옛 code(`KIS_UNAUTHORIZED`, `KIS_FORBIDDEN`, `KIS_BAD_GATEWAY`, `KIS_GATEWAY_TIMEOUT`, `KIS_RESPONSE_PARSE_ERROR`)는 재사용하지 않고 은퇴, 쓰는 곳이 없어진 옛 예외 클래스만 삭제(`KisResponseParseException`은 `KisOrderRestClient`가 계속 써서 남음). `KisOrderRestClient`·`KisTokenManager` 안에서 나는 예외는 파일을 안 건드려 그대로 | backend-api AE4(외부 의존 실패는 503), D15와 같은 근거(우리 호출자의 인증·권한 문제가 아닌데 401·403을 줌). AE19(건드린 파일 전체 정렬): §5 2단계 3번·4번이 KIS 어댑터 파일을 건드림. 주문 경로 KIS 실패는 지금 `KisApiException`이 기본 처리기로 가 500 `INTERNAL_SERVER_ERROR`(`KisOrderRestClient.kt:161-166`, `GlobalExceptionHandler.kt:96-102`) | 옛 code 이름 유지하고 상태만 503: 이름이 502·504를 뜻해 오해. 401·403·502·504 유지: AE4 위반이 남음(미루기는 사람만 고름, DEC-05 B) |
| D29 `AccountResponse.source` | `AccountSource`에 모드 → 출처 함수 하나(전수 `when`): LOCAL → LOCAL, KIS_PAPER·KIS_LIVE → KIS, TOSS_LIVE → TOSS, UPBIT_LIVE → UPBIT(`AccountSource`에 UPBIT 추가, 가정 1). `AccountResponse.from`과 `BrokerAccountQueryService`가 이 함수를 씀. KIS·TOSS·UPBIT 계좌의 `source`가 LOCAL에서 바뀌므로 AE20 목록에 올림 | 지금 모든 계좌가 LOCAL로 나감(`AccountResponse.kt:34`, §2 영향 지점 표). trading-web `src`에 `source`를 읽는 곳 0건 | `source` 필드 삭제: 응답 필드 삭제는 큰 AE20 변경. UPBIT_LIVE를 LOCAL로 둠: 틀린 값을 유지 |

### 10.3 바꿀 파일·모듈

| 경로 / 모듈 | 변경 종류 | 목적 | 위험 |
|---|---|---|---|
| test/ 아래 KIS 특성화 시험 3개(이름은 step 문서에서) | 신설 | D21 (a)(b)(c) | - |
| main/application/account/port/BrokerAccountBalancePort.kt | 수정 | `supportedModes`(D25) | 낮음 |
| main/application/account/port/KisAccountBalancePort.kt | 삭제 | D22 | 낮음 |
| main/infrastructure/kis/KisAccountBalanceAdapter.kt | 수정 | 공통 포트 구현, 모드 전수 `when`, 계좌 로드 제거(D22), 에러 정렬(D28) | 높음 (KIS 잔고 경로, RULE-10) |
| main/infrastructure/toss/TossAccountBalanceAdapter.kt | 수정 | `supportedModes` = TOSS_LIVE | 낮음 |
| main/application/account/BrokerAccountQueryService.kt | 수정 | D24, D25 | 중간 |
| main/application/account/KisAccountQueryService.kt | 수정 | D23(L35의 `!!`도 없앰, backend `CLAUDE.md`) | 중간 |
| main/application/order/port/BrokerOrderPort.kt | 신설 | D26 | 중간 |
| main/infrastructure/kis/KisOrderAdapter.kt | 신설 | D26, 주문 경로 에러 정렬(D28) | 높음 (실주문 경로) |
| main/application/order/BrokerOrderSubmitter.kt | 신설 | D26 | 높음 (실주문 경로) |
| main/application/order/OrderCommandService.kt | 수정 | 생성자, `submitExternalOrderIfNeeded`의 KIS 분기(D26) | 높음 (주문 핵심 경로) |
| main/application/order/KisPaperOrderExecutor.kt, KisLiveOrderExecutor.kt | 삭제 | D26(호출 0건인 `cancel()`, `pollFills()`도 함께 사라짐) | 중간 |
| main/infrastructure/scheduler/KisPaperPollingScheduler.kt | 삭제 | 빈이 아니고(§2 영향 지점 표) `pollFills()`만 부르며, 실행기 삭제로 컴파일되지 않음 | 낮음 |
| main/common/exception/Kis*Exception.kt | 신설 | D28 새 예외(파일 하나에 선언 하나, KDoc 첫 줄 범주 기호) | 낮음 |
| main/common/exception/CommonApiExceptions.kt | 수정 | 쓰는 곳이 없어진 옛 KIS 예외와 기반 클래스 삭제(D28) | 중간 (ArchUnit 동결 대상) |
| main/presentation/exception/GlobalExceptionHandler.kt | 수정 | KIS code 고정 문구(D28) | 중간 |
| main/domain/enums/AccountSource.kt | 수정 | UPBIT, 모드 → 출처 함수(D29) | 낮음 |
| main/presentation/dto/account/AccountResponse.kt | 수정 | D29 | 낮음 |
| main/application/order/KisExecutionNoticeService.kt → BrokerExecutionNoticeService.kt, main/application/order/command/BrokerExecutionNotice.kt | 이름 바꿈, 신설 | D27(PR #53 뒤) | 중간 (체결 경로) |
| main/infrastructure/kis/KisExecutionNotice.kt, KisExecutionNoticeParser.kt, KisExecutionWebSocketManager.kt | 수정 | D27 입력 타입 교체 | 중간 |
| src/test/resources/archunit_store/* | 수정 | 해결된 동결 위반 줄 제거(실행기 2파일, 체결 서비스) | 낮음 (PR #53과 같은 파일, RISK-11) |
| docs/ADR.md | 수정 | ADR-005 갱신(D25, D26) | 낮음 |
| test/application/account/KisAccountQueryServiceTest.kt, test/infrastructure/kis/KisAccountBalanceAdapterTest.kt, test/application/order/OrderCommandServiceTest.kt, test/application/account/BrokerAccountQueryServiceTest.kt | 수정 | 새 포트 기준(DEC-05), 모드 표 | - |
| test/presentation/controller/KisAccountBalanceControllerIntegrationTest.kt | 수정 | 오류 매핑 시험만 D28 기준(DEC-05 A일 때만) | - |
| test/ 아래 `BrokerOrderSubmitterTest`, `KisOrderAdapterTest`, 선택 검사 시험, `AccountSource`·`AccountResponse` 모드 표 시험 | 신설 | §10.6 | - |

건드리지 않음: collector-api, trading-web, quant-worker, `main/infrastructure/kis/KisTokenManager.kt`(DEC-07), `main/infrastructure/kis/KisOrderRestClient.kt`, `main/application/order/ExecutionProcessor.kt`, `main/domain/entity/**`, `db/migration/*`, 토스 주문·체결 코드, PR #53 브랜치.

### 10.4 데이터·API 작업 (AE20 목록)

| 항목 | 지금 → 2단계 뒤 | 소유 | 막는가(blocking)? |
|---|---|---|---|
| `GET /api/v1/accounts/{accountId}/broker-balance`, KIS 계좌 | 501 `BROKER_BALANCE_NOT_SUPPORTED` → 200, 미연결 409 `KIS_ACCOUNT_NOT_LINKED`, KIS 실패 503 `KIS_*`. 호출부: trading-web `src` 0건 | backend | 아니오 |
| `GET /api/kis/account/balance` 성공 응답 | 불변(D23) | backend | 아니오 |
| 같은 경로 오류 (DEC-05 A일 때) | 401 `KIS_UNAUTHORIZED` → 503 `KIS_AUTH_FAILED`, 403 `KIS_FORBIDDEN` → 503 `KIS_PERMISSION_DENIED`, 502 `KIS_BAD_GATEWAY` → 503 `KIS_UNAVAILABLE`, 504 `KIS_GATEWAY_TIMEOUT` → 503 `KIS_TIMEOUT`, 400 `BAD_REQUEST`(계좌번호 없음)·400 `KIS_RESPONSE_PARSE_ERROR`(형식) → 409 `KIS_ACCOUNT_NOT_LINKED`. message는 예외 원문 → code별 고정 문구. 호출부: trading-web `src` 0건, 재제작 계획은 성공 응답만 언급 | backend | 예 (DEC-05) |
| `POST /api/v1/accounts/{accountId}/orders`, KIS 계좌 접수 실패 (DEC-05 A일 때) | 500 `INTERNAL_SERVER_ERROR` → D28 code(503·409·400), 400 `BAD_REQUEST`(계좌번호 없음)·400 `KIS_RESPONSE_PARSE_ERROR` → 409 `KIS_ACCOUNT_NOT_LINKED`. 주문 롤백 동작은 그대로. 호출부: trading-web `src`에 주문 호출 0건 | backend | 예 (DEC-05) |
| 계좌 응답 `source`(`AccountResponse`를 쓰는 계좌 API) | KIS·TOSS·UPBIT 계좌 LOCAL → KIS·TOSS·UPBIT(D29). 호출부 0건 | backend | 아니오 |
| Redis 키, 설정 키, DB 스키마 | 변경 없음(KIS 토큰 이전 안 함) | - | 아니오 |
| 새 HTTP 경로 | 없음. 만들게 되면 기존 프록시 접두사 아래에만(`proxy.config.ts:22-31`을 컨트롤러 소스와 대조하는 `src/test/proxy-table.test.ts:134-142`) | backend | 아니오 |
| 계약 비교 | OpenAPI 문서가 없어 `openapi-diff` 대상 없음(§4와 같음) | - | 아니오 |

호출부 점검은 `263f047` 기준 읽기 전용 검색 결과입니다. 묶음마다 착수 때 trading-web을 다시 검색하고, 호출부가 생겼으면 멈추고 ASK(프런트 변경은 범위 밖).

### 10.5 구현 순서

§5 «2단계» 1~7번입니다. 묶음마다 테스트를 먼저 씁니다(CLAUDE.md TDD).

### 10.6 테스트 계획

명령은 `cd /Users/dongwoo/Desktop/paper-trading/.worktrees/multi-broker-kis-toss-phase2/backend/trading-api` 뒤에 실행합니다. Testcontainers 시험은 Docker가 필요합니다.

| 테스트 | 종류 | 명령 / 방법 | 기대 결과 |
|---|---|---|---|
| 기준선 | integration | 착수 전 `./gradlew test --rerun --offline`, 실행 뒤 생긴 xml만 집계 | 결과와 실패 목록, KIS 시험 목록(`Kis`·`kis`를 담은 시험 파일, `263f047`에서 14개)을 step 문서에 고정 |
| 특성화: KIS 잔고 | integration (Testcontainers, `MockRestServiceServer`) | 옛 코드에서 먼저 작성·통과. PAPER, LIVE, 보유 0, 필드 누락 응답 | 응답 JSON 전체와 KIS 호출 URL·`tr_id`·헤더가 2단계 2번·3번 뒤에도 같음 |
| 특성화: KIS 주문 | integration | KIS_PAPER·KIS_LIVE 계좌로 매수·매도 접수, KIS order-cash 응답 고정 | `tr_id`(VTTC0802U·VTTC0801U, TTTC0802U·TTTC0801U), 본문, `externalOrderId`, 원장·잠금 행, Slack 문구가 4번 전후 같음 |
| 특성화: KIS 체결 | integration | 같은 KIS 체결 통지(부분, 전량) | 체결·포지션·원장 행이 6번 전후 같음 |
| 잔고 모드 표 | unit | `BrokerAccountQueryServiceTest`: `TradingMode.entries` 전부 | TOSS_LIVE → 토스, KIS_PAPER·KIS_LIVE → KIS 어댑터(ref 모드 일치), UPBIT_LIVE 501, LOCAL 404, KIS 미연결 409 `KIS_ACCOUNT_NOT_LINKED` |
| 선택 검사 | unit | 구현 누락, 한 모드에 구현 둘, 정책이 어댑터로 안 보내는 모드를 구현이 지원 | 서비스 생성 때 예외(기동 실패), 메시지에 모드 이름 |
| KIS 어댑터 모드 | unit | `ref.tradingMode` KIS_PAPER, KIS_LIVE, 그 밖 | paper 주소와 `VTTC8434R`, live 주소와 `TTTC8434R`, 그 밖은 내부 오류이고 KIS 호출 0회 |
| 옛 경로 `mode` | unit | KIS_PAPER 계좌를 `mode=LIVE`로 | 지금처럼 live 호출(가정 2) |
| 주문 모드 표 | unit | `OrderCommandServiceTest`: `entries` 전부 | LOCAL 내부 매칭, KIS 둘은 `BrokerOrderSubmitter`, TOSS_LIVE·UPBIT_LIVE 501과 잠금·저장·구독 0회(1단계 단언 유지) |
| 접수 서비스 | unit | `BrokerOrderSubmitterTest`: KIS_PAPER, KIS_LIVE, 접수 실패 | 포트 1회, `externalOrderId` 기록, Slack 문구가 지금과 같은 글자, 실패는 그대로 던짐 |
| 에러 정렬 (DEC-05 A) | unit, integration | KIS 실패 종류마다 잔고·주문 경로 | §10.4의 상태와 code, 고정 문구, 응답에 KIS 원문·계좌번호 없음 |
| 출처 | unit | `AccountSource` 함수와 `AccountResponse`: `entries` 전부 | 모드별 출처, 표 키 집합이 `entries`와 같음 |
| 아키텍처 | unit | `ArchitectureRulesTest`, `ApplicationLayerRulesTest` | 새 위반 0, 해결된 동결 위반 줄이 저장소에서 빠짐 |
| 부동소수점 스캔 | unit (결정적 checker) | 1단계 스캔 대상에 `main/application/order/port/**`, `main/application/order/BrokerOrderSubmitter.kt`, `main/infrastructure/kis/KisOrderAdapter.kt` 추가 | 0건 |
| 비밀값 로그 누출 | unit | 1단계 로그 누출 시험을 KIS 새 파일 경로로 넓힘 | 토큰, app secret 0건, 계좌번호는 끝 4자리만 |
| 프록시 표 | unit (프런트, 실행만) | `cd /Users/dongwoo/Desktop/paper-trading/.worktrees/multi-broker-kis-toss-phase2/frontend/trading-web && npx vitest run src/test/proxy-table.test.ts` | 통과. trading-web 파일은 안 고침 |
| 빌드·전체·린트 | integration | `./gradlew compileKotlin compileTestKotlin ktlintCheck detekt verifyLintBaselinesNotGrown -PlintBaselineRef=origin/main --offline`, `./gradlew test --rerun --offline` | exit 0, 기준선 대비 새 실패 0, 특성화 시험은 고치지 않고 통과 |
| KIS 실호출 (선택) | manual | KIS 키가 있는 환경에서 모의 계좌 하나를 옛 경로와 `broker-balance`로 1회씩 | 같은 값(표기만 숫자와 문자열), 기록은 마스킹 |

### 10.7 도메인 제약 반영

| 차원 | 적용 | 구현·검증 방법 | review routing |
|---|---|---|---|
| 수치 무결성 | y | KIS 숫자 경로(Map 값 → 문자열 → `BigDecimal`, `KisAccountBalanceAdapter.kt:127-128`)와 수익률 계산(L162-168)을 바꾸지 않음. golden은 특성화 시험 (a)의 응답 JSON 전체 일치. 새 파일은 부동소수점 스캔 | 수치 경로 변경은 사람 확인 |
| 단위·기준치 | y | KIS는 KRW 한 줄(D22). 통화 합산 코드 없음(FLAG-07) | - |
| 결측·이상치 | y | KIS 누락 값 0 채움은 그대로(가정 3, 기존 결함으로 기록). 새 API의 KIS `cashBuyingPower`는 0이 아니라 null | 0 채움을 고치려면 별도 결정 |
| 규제·표현 | n | - | - |
| PHI | n | 참고: KIS 계좌번호는 `BrokerAccountRef.toString`이 끝 4자리만 보임(`BrokerAccountBalancePort.kt:22-24`). 새 KIS 예외 응답에는 계좌번호를 싣지 않음(지금 `KisOrderRestClient.kt:155`는 메시지에 원값을 넣음) | - |

### 10.8 리스크·막힘

| ID | 리스크 / 막힘 | impact | 완화 / 필요한 결정 |
|---|---|---|---|
| RISK-11 | 열린 PR #53(`fix/trading-api-money-state`)과 겹침. 그 브랜치는 1단계 전 main(`e510634`)에서 갈라져 지금 main과 이미 충돌(`git merge-tree`: `ExecutionProcessor.kt`, `ExecutionProcessorTest.kt`, ktlint 기준선). 그 PR의 `ExecutionProcessor`에는 `else -> account.receiveSellProceeds(netProceeds)`가 남아 있어, 충돌을 PR 쪽으로 풀면 D6이 되돌아가 TOSS_LIVE·UPBIT_LIVE 매도가 LOCAL처럼 입금됨. 같은 ArchUnit 동결 파일(`archunit_store/bb05cc60…`), `KisExecutionNoticeService`, `KisExecutionWebSocketManager`도 고침. 마스킹 함수도 따로 추가(`LogMasking.kt`의 `maskKisAccountNumber`, main에는 `MaskAllButLastFour.kt`) | 높음 | PR #53을 main에 맞춰 먼저 머지(추천, 머지는 사람). 2단계 1~5번은 PR #53 본문 파일을 안 건드리고 6번만 머지 뒤(D27). 충돌 해소 원칙: main의 전수 `when`과 진입부 거부(`ExecutionProcessor.kt:162-173, 239-245`), 1단계 모드 표 시험을 지킴. 해소 뒤 `ExecutionProcessorTest`·`OrderCommandServiceTest` 모드 표 통과와 `ExecutionProcessor.kt`에 `else ->` 0건 확인. 2단계 도중 머지되면 다음 묶음 시작 때 main을 합치고 기준선을 다시 기록. 동결 파일은 양쪽의 줄 삭제를 합침. 그 PR은 고치지 않음 |
| RISK-12 | RULE-10 문구(«기존 KIS 테스트가 전후로 모두 통과»)와 시그니처에 묶인 시험(D21 근거)이 맞지 않음 | 높음 | DEC-05 |
| RISK-13 | KIS 토큰 평문 저장이 계속 남음(CLAUDE.md «Never store tokens in plaintext») | 높음 | DEC-07 |
| RISK-14 | FLAG-11(`GlobalExceptionHandler.kt:61-71`의 `IllegalArgumentException`·`IllegalStateException` → 400 묶음)이 «2단계 KIS 정렬 때 다룸»으로 미뤄져 있음. 바꾸면 main 31파일 162곳의 `require`·`check` 계열 응답이 함께 바뀜 | 중간 | DEC-06 |
| RISK-15 | KIS 주문 경로의 기존 결함 둘. 주문 취소가 KIS 취소를 부르지 않아, KIS에 살아 있는 주문의 잠금이 풀리고 나중 체결 통지는 종료된 주문이라 버려짐(`OrderCommandService.kt:260-303`, `ExecutionProcessor.kt:100`). KIS 접수 타임아웃 뒤 우리 주문은 롤백되는데 KIS에는 접수됐을 수 있음(`KisLiveOrderExecutor.kt:30-42`가 예외를 다시 던짐) | 높음 | DEC-08. 2단계는 동작을 바꾸지 않음 |
| RISK-16 | KIS 응답은 fixture로만 검증하고 KIS 실호출이 없음 | 중간 | §10.6 선택 수동 확인(사람). 특성화 fixture는 기존 KIS 시험의 응답 모양을 씀 |
| RISK-17 | 기동 때 선택 검사(D25)가 잘못 짜이면 앱이 안 뜸 | 중간 | 선택 검사 단위 시험과 전체 컨텍스트를 띄우는 기존 통합 시험이 같은 묶음에서 통과해야 완료 |
| DEC-05 | 결정됨(사용자, 2026-11-16): A. 결정 전 선택지: RULE-10을 무엇으로 판정하나. A ⭐ 돈·주문·체결 동작과 성공 응답 불변을 RULE-10으로 보고 D21 특성화 시험을 증거로 삼음. 시그니처에 묶인 KIS 단위 시험은 포트를 바꾸는 같은 커밋에서 새 포트 기준으로 옮기고 목록을 step 문서에 남김. KIS 에러 응답 변경(D28)은 AE20 목록으로 허용. B: 시험 옮김만 허용하고 KIS 에러 응답은 그대로(D28 제외, 건드린 KIS 파일 커밋에 사람이 `API-Errors-Deferred` 기록). C: 글자 그대로(기존 시험 수정 0), 이 경우 D22와 D26을 못 함 | 높음 | 막는 묶음: §5 2단계 2~4번 |
| DEC-06 | 결정됨(사용자, 2026-11-16): A. 결정 전 선택지: FLAG-11. A ⭐ 이번에도 미루고 건드린 KIS 파일 안의 `require`·`check`만 범주 예외로 바꿈. B: 핸들러 매핑을 지금 바꿈(31파일 응답 변경, AE20 목록이 커짐) | 중간 | 막는 묶음: 없음(A면 그대로 진행) |
| DEC-07 | 결정됨(사용자, 2026-11-16): A. 결정 전 선택지: KIS 토큰 이전(🔴). A ⭐ 2단계에서 빼고 collector-api를 함께 바꿀 수 있는 작업으로 미룸. B: 범위를 collector-api까지 넓혀 두 서비스가 같은 암호 형식·키·락을 씀(collector의 DB 토큰 저장소도 확인 필요). C: trading-api만 다른 키에 암호 저장하고 공유를 끊음 | 높음 | 막는 묶음: 없음(순서에 없음) |
| DEC-08 | 결정됨(사용자, 2026-11-16): A. 결정 전 선택지: RISK-15의 결함 둘. A ⭐ 2단계는 동작 불변, 결함 둘을 `docs/TODO.md`에 별도 기능으로 등록. B: 4번 묶음에서 취소 연동과 타임아웃 뒤 처리를 함께 고침(RULE-10 예외, KIS_LIVE 실주문 취소 시험 필요) | 높음 | 막는 묶음: 없음(A면 그대로) |
| 가정 1 | `AccountSource`에 UPBIT 추가(D29) | 낮음 | 응답 enum 값 추가라 되돌리기 쉬움 |
| 가정 2 | 옛 경로는 `mode` 파라미터가 `trId`를 정하는 지금 동작 유지(계좌 모드와 달라도 거부하지 않음, D23). 불일치 거부와 경로 폐지 시점은 trading-web 재제작이 쓸 경로를 정한 뒤 | 낮음 | 읽기 전용 조회 |
| 가정 3 | KIS 응답 누락 값 0 채움 유지(D22, RULE-10). core 6(silent default 금지)과 어긋나는 기존 결함으로 기록 | 중간 | 고치려면 KIS 동작 변경이라 별도 결정 |
| 가정 4 | `broker-balance`에 대사를 넣지 않음(D24). 대사는 옛 경로에만 | 낮음 | 화면이 필요로 할 때 Product 결정 |

### 10.9 구현자에게 넘기기 (Handoff)

착수 전에 Maker가 §5 «2단계»로 state.md 로드맵과 step 문서를 만들고, 사람이 설계 재승인 흔적을 남깁니다(§9와 같은 절차).

이것만 만들어라 (승인된 범위):
- §5 «2단계» 1~7번, D20~D29. D28은 DEC-05가 A일 때만, 6번은 PR #53 머지 뒤

이건 하지 마라 (범위 밖):
- trading-web, collector-api, quant-worker 변경. PR #53 브랜치 수정
- 토스 주문·체결, TOSS_LIVE·UPBIT_LIVE 주문 허용
- KIS 토큰 저장·발급 방식 변경(DEC-07), `KisOrderRestClient` 수정
- KIS 주문 취소 연동, 접수 타임아웃 처리 변경(DEC-08), KIS 0 채움 수정(가정 3)
- `GlobalExceptionHandler`의 `IllegalArgumentException`·`IllegalStateException` 매핑 변경(DEC-06이 B가 아니면)
- `/api/kis/account/balance` 삭제나 성공 응답 변경, 새 대사 기능
- `KisExecutionWebSocketManager`를 다른 계층으로 옮기기, 새 의존성 추가

이때는 멈추고 물어봐라:
- 특성화 시험이 옛 코드에서 통과하지 않을 때(지금 동작이 계획의 이해와 다름)
- 기준선 대비 KIS 시험이 새로 실패하거나, 특성화 시험을 고쳐야 할 것 같을 때
- DEC-05가 정해지지 않았는데 2~4번 차례일 때, 6번 차례에 PR #53이 열려 있을 때
- trading-web에 §10.4 경로의 호출부가 새로 생겼을 때, 기존 프록시 접두사 밖 경로가 필요할 때
- 동결 목록 밖 ArchUnit 새 위반이 생길 때
- Docker가 없어 통합 시험을 못 돌릴 때
