범위: `변경분` — 커밋 `2692c2d`(부모 `423ea35`)의 backend 44파일. 문서 커밋 `423ea35` 는 범위 밖입니다.

# 코드 리뷰 결과 — multi-broker-kis-toss step 6 «잔고 공통 포트와 토스 어댑터» (1회차)

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 리뷰 대상 | worktree `.worktrees/multi-broker-kis-toss`, `git diff 423ea35 2692c2d` 의 backend 44파일(main 신설 18 · main 수정 5 · 시험 신설 7 · 시험 수정 6 · fixture 7 · README 1) |
| 기준 | `rule-pointer --list` 가 낸 목록 그대로: devkit `rules/dev-team/common.md`(AE18·AE21·CS1~CS25·C12·DD1~DD6) · `rules/dev-team/tests.md`(TT 조항) · 레포 `rules/backend-api.md`(PTB1~PTB9) · 레포 `rules/failure-handling.md`(PTF1~PTF6) · 레포 `rules/tests.md`(PTT1) · `always:dev-team`. 모든 행의 Diff checks 0행 |
| 프로필 | consumer (`profile:consumer` 행) |
| 리뷰어 | code-reviewer (구현자와 독립, 읽기 전용) |
| 날짜 | 2026-11-15 |

## 1. 요약

| 항목 | 값 |
|---|---|
| 리뷰한 렌즈 | 정확성 · 표준 · 유지보수성 · 보안 · 성능 · 회귀 시험 · 의도. 근본성: bugfix diff 가 아닙니다 — C-01~C-05 는 리뷰 후속이고 각 수정이 리미터·설정·매퍼 층 자체에 들어가 호출부 분기로 덮은 꼴이 아닙니다 |
| 의도 | 커밋 `의도:`·`기존:`·`변경:` 블록과 diff 가 맞습니다. `출처:` step-6.md·step 5 리뷰 C-01~C-05 는 열어 확인했고 사람 결정 3건은 지시문 그대로입니다. 블록에 없는 기존 동작 변경은 없습니다(KIS 코드·기존 API 응답·`GlobalExceptionHandler` 변경 0) |
| 수정 지킴 확인 | 확인 불가 — 탐침 샌드박스가 Gradle(`~/.gradle`·build 쓰기)과 Docker 소켓(Testcontainers Redis)을 막아 `guards-fix` 를 돌리지 못했습니다. 통과가 아닙니다. 작성자 증거: step-6 §6 관련 묶음 476건·메인 재실행 297건 실패 0, 린트·기준선 exit 0 |
| 뮤테이션 | `mutation-run.js --base 423ea35` → «뮤테이션 대상 없음» |
| 재사용 점검 | `reuse-inventory --target`(BrokerAccountBalancePort·TossAccountListClient·BrokerAccountBalanceResponse) 재생성 의심 0건. 의미 중복 1건은 S6-05 |
| 발견 | 7개 (막음 0 · 후속 후보 7) |
| 머지 판정 | `review-verdict.js` 첫 줄: 머지 판정: 가능 — 막는 지적 0건 · 후속 후보 7건 |
| 한 줄 | 금액은 BigDecimal 로 끝까지 가고, KRW·USD 합계는 섞이지 않으며, 모드 `when` 은 else 없이 전수이고, 응답·예외·로그에 accountNo 전체·토큰이 없습니다. 막는 지적은 없습니다. 남은 것은 C-03·C-05 와 같은 부류의 빈틈(빈 문자열 금액이 null 로 통과 S6-01, 이미 예약한 스레드는 429 대기를 안 나눔 S6-02)과 잘못 연결된 계좌의 오분류(S6-03)입니다 |

기타: `skill-session.js open code-review` 는 샌드박스가 `~/.devkit` 쓰기를 막아 EPERM 으로 실패했습니다(리뷰 절차는 진행). S6-01·S6-02 는 Gradle 없이 커밋 빌드 산출 클래스(`build/classes/kotlin/main`, jackson 2.21.2)를 JDK 단일 소스 실행으로 직접 불러 확인했습니다.

### step 5 후속 C-01~C-05 확인

| 항목 | 확인 결과 | 근거 |
|---|---|---|
| C-01 리미터 대기 상한 | 고쳐짐 | `TossRateLimiter.kt:36-46` 예약 전 `slot - now` 가 상한을 넘으면 슬롯을 잡지 않고 `TOSS_RATE_LIMITED`. 시험 `TossRateLimiterTest.kt:178-217`(초과·슬롯 미점유·경계·상한 0). 상한 키를 429 예산과 같이 쓰는 부작용은 S6-04 |
| C-02 base URL 형식 | 고쳐짐 | `TossProperties.kt:61-64, 78-85` scheme `https`·host 없으면 `TOSS_BASE_URL(형식 오류)`. 두 호출 지점(`TossApiClient.kt:55`, `TossTokenIssuerImpl` `issue`)이 `requireEnabled()` 를 먼저 불러 형식 오류 값으로 전송하지 않음. 값은 로그·예외에 안 실림(시험 `TossPropertiesTest` 9개 입력) |
| C-03 목록 null·실수 절삭 | 고쳐짐(같은 부류 빈틈 1) | `TossJson.kt:26, 28, 34` `NewStrictNullChecks`·`forContentNulls(FAIL)`·`ACCEPT_FLOAT_AS_INT` 끔. 오류 DTO 에 컬렉션 필드가 없어 전역 contentNulls 가 오류 분류를 깨지 않음. 남은 빈틈: 빈·공백 문자열 → null(S6-01) |
| C-04 하한 상수 | 고쳐짐, 설계 맞음 | 실행 검사 16초(`ISSUE_TIME_BUDGET_MILLIS`) = 발급 최악 15초 + AUTH 대기 1초. 발급은 분산 락으로 직렬이라 락 안 AUTH 대기는 최대 한 간격이고, 한도가 `Int`(≥1)라 간격은 1000ms 이하 → 1초 허용이 상한입니다. 범위 검사 17초 = 16초 + 락 획득 여유 1초라, 하한 값이어도 1초 안에 락을 얻은 첫 요청은 발급합니다(시험 `TossTokenManagerTest` 여유 경계 ±1ms). 한 값을 같이 쓰면 상한=하한일 때 Redis 왕복만으로 첫 요청이 매번 실패하므로 파생 상수 둘이 맞습니다. 같은 부류 형제 `MIN_LOCK_TTL_EXCLUSIVE_MILLIS`(`TossProperties.kt:142`, 15초 기준)는 그대로라 락 TTL 15001~15999 가 유효로 통과하는데, maker 가 §8 에 후속 후보로 이미 적어 여기 행을 따로 두지 않습니다(동의) |
| C-05 429 슬롯 공유 | 부분 | `TossRateLimiter.kt:52-56`·`TossApiClient.kt:118` 둘 다 같은 모니터의 `@Synchronized` 안에서만 맵을 만져 경합·갱신 유실은 없습니다. 다만 새로 들어온 호출만 미뤄지고 이미 예약해 잠든 호출은 대기 창 안에 보냅니다(S6-02). 포기하는 429 를 미루지 않는 것은 §8 ASSUME 이라 지적하지 않음 |

## 1-1. 프로필별 검토 절 (consumer)

- 1. 규칙 준수 — 대조한 조항 출처: `rule-pointer --list` 목록만(모델이 따로 고른 규칙 없음).
  - common.md: AE18·AE21·CS14·CS21·CS22 — 새 catch 는 `TossProperties.isAbsoluteHttpsUrl` 의 `URISyntaxException` 하나이고 `false` → 형식 오류로 비활성이라 호출자·운영자에게 보입니다. CS16·CS20·CS23 해당 없음. DD1·DD3 — 새 API 계약 두 개는 D9 승인 경로이고 필드(끝 4자리, 숫자 문자열, 퍼센트)는 사람 결정입니다. DD2·CS2·CS3·CS19 — `accountSeq`·`externalAccountId` 가 D3 용어 그대로. CS1·CS4·CS5 → S6-05. CS7 — 포트 두 개는 HTTP 경계라 맞습니다. CS8 — 새 404·409 예외의 동결 기반 상속은 FLAG-12 결정. CS18 해당 없음(조회 전용). CS24 — 통화 비교는 상수(`TossCurrencyAmounts.KRW/USD`), 상태 문자열 비교 없음. CS25 — 새 함수 모두 40줄·중첩 2단 이하. C12·CS13·CS15 — 새 주석은 명사형 표지, TODO/FIXME 0. CS17 해당 없음.
  - tests.md: TT1 — 새 소스마다 시험. TT7 → S6-07(TT44 형태 포함). TT9 — 컨트롤러 시험이 HTTP 본문(문자열 타입·끝 0)을 단언. TT16 — `Clock.fixed`·`StillClock` 주입. TT20·TT21 — 이름이 상황·동작·기대를 담고 레포 기존 백틱 문장형을 따름. TT25·TT26 — 기존 시험 경계 이동 2곳에 `devkit:test-change C-04` 와 근거. TT33·TT40 — Red 실행이 구현 뒤 스크래치 복사본에서 재구성됐다고 maker 가 §8 에 자진 보고했고, 시험 선작성 여부는 리뷰로 확인할 수 없습니다. TT49 → 뮤테이션 행.
  - 레포 rules/backend-api.md: PTB1·PTB2·PTB3·PTB5·PTB6 — 도메인 모델·엔티티 변경 없어 해당 없음. PTB4 — 비교는 `signum()`, 나눗셈은 `divide(principal, 6, HALF_UP)`, `/`·`Double` 생성자 없음. `BigDecimal` 을 담은 data class 는 포트·응답 DTO 이고 토스 표기(끝 0)를 보존하는 것이 설계라 값 객체 scale 정규화 대상이 아닙니다. PTB7 — application → domain·common·port, infrastructure → `application.*.port`·common, presentation → application·`domain.enums` 뿐. PTB8 — 포트와 시그니처 타입이 `application/account/port`·`application/toss/port` 에 있고 유스케이스는 리포지토리·클라이언트를 직접 주입받지 않음. PTB9 — `@Service` 만, 외부 호출뿐이라 `@Transactional` 없음.
  - 레포 rules/failure-handling.md: PTF1·PTF2·PTF4·PTF5·PTF6 — 원장·파생 잔고·요율·시각 구간·주문 가능 금액 차감이 없어 해당 없음(토스 잔고는 조회 결과를 그대로 냄). PTF3 — GET 조회만이고 타임아웃은 `TOSS_TIMEOUT`, 남기는 상태 없음.
  - 레포 rules/tests.md PTT1 — 새 Redis·PostgreSQL 어댑터 없음. 바뀐 `TossTokenManager` 는 Testcontainers Redis 시험. 충족.
  - always:dev-team — 재사용 S6-05, 함수 길이·주석 위와 같음, 시간은 `LocalDateTime.now(Clock.systemDefaultZone())` 로 시간대를 붙이지 않음(Clock 빈 없음 확인).
- 2. 사이드 이펙트 — `TossRateLimiter.acquire` 호출처는 `TossApiClient.execute` 와 `TossTokenIssuerImpl.issue`(락 안 AUTH) 둘이고, 발급 쪽은 한 프로세스에서 대기가 1초 이하라 상한을 1초 미만으로 둘 때만 새 `TOSS_RATE_LIMITED` 가 납니다(S6-04 와 같은 원인). 전역 매퍼 변경은 오류 본문 해석(`readErrorDetail`·`readOAuth2Error`)에도 걸리는데 두 DTO 에 컬렉션 필드가 없어 영향이 없습니다(`retryAfterSeconds` 가 실수로 오면 오류 상세 전체가 null 이 되어 상태 코드로만 분류 — 명세 밖 값이라 지적 안 함). C-02·C-04 로 `http://` 주소나 요청 상한 15001~16999 를 쓰던 배포는 토스만 비활성이 됩니다(커밋 블록에 적힘). 새 컨트롤러는 `/api/v1/accounts` 접두를 `AccountController` 와 나누지만 경로가 겹치지 않고 컨텍스트 기동 시험이 통과합니다. KIS·기존 응답 변경 0(diff 확인).
- 3. 버그·장애 위험 — 금액: 토스 DTO → 포트 → 응답까지 `BigDecimal`, 응답은 `toPlainString`. 통화별 합계는 KRW 행이 `.krw`·buying-power KRW 만, USD 행이 `.usd`·buying-power USD 만 읽고 응답 통화가 요청 통화와 다르면 실패해 섞이지 않습니다. null 과 0: `cashBalance` 는 늘 null, `usd` null 은 그 칸만 null, 원금 0 은 수익률 null. 수익률 단위는 사람 결정대로 종목 `movePointRight(2)`, 합계 KIS 방식(scale 6 HALF_UP ×100 scale 2, `KisAccountBalanceAdapter.kt:162-168` 과 같은 식). 빈틈: S6-01. 개인정보: accountNo 는 `TossAccountListClient.kt:24` 에서 끝 4자리로 바뀌고 `TossAccount.toString` 에 없음, 예외 문구(409·404·501)에 값 없음, 로그는 마스킹된 `TossApiRequest` 만. 토큰 경로 변경 없음. accountSeq 표현 불일치 S6-05. 모드 `when`(`BrokerAccountQueryService.kt:26-34`)은 else 없는 전수이고 시험 표 키 집합 = `TradingMode.entries`. 미연결 409 는 형식 판정(전각 숫자·부호·Long 초과 거부)까지 맞고, 숫자지만 틀린 accountSeq 는 S6-03. 동시성: S6-02. 부하: S6-04. OSIV 는 `open-in-view: false` 라 토스 호출 동안 DB 연결을 잡지 않습니다.
- 조항을 어기지 않는 코드 품질 지적은 `should-fix` 이하로 두었습니다.

## 2. 발견 (findings)

| ID | file:line | 근거 조항 | 무엇이 틀렸나 | 등급 | 출처 | 회귀 | confidence | 구조 고침 | 재현 |
|---|---|---|---|---|---|---|---|---|---|
| S6-01 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossJson.kt:24-35 | label:falsy-or-empty-value-mishandled | D14(숫자 변환 실패는 대체값 없이 `TOSS_RESPONSE_INVALID`)가 빈·공백 문자열에서 샙니다. Jackson 기본 강제 변환이 빈 문자열을 null 로 바꿔, null 허용 금액인 `TossCurrencyAmounts.usd`(`TossHoldingsOverview.kt:18`)에 `"usd":""`·`"usd":" "` 가 오면 실패 없이 null 이 되고, USD 행의 평가액·손익·수익률이 «해외 보유 없음»과 같은 null 로 나갑니다. null 불가 칸(`krw`)은 같은 값에서 실패해 둘의 처리가 갈립니다. 탐침: 커밋 빌드 클래스로 `TossJson.readSuccess` 를 직접 불러 `usd` 빈 문자열·공백 → `usd=null` 성공, `krw` 빈 문자열 → `KotlinInvalidNullException` | should-fix | 이 변경 | 아니오 | 중간 | 매퍼에서 빈·공백 문자열의 스칼라 강제 변환을 `CoercionAction.Fail` 로 꺼 null 허용 칸도 명시적 JSON null 만 null 로 받음, 경우마다 시험 한 줄 | — |
| S6-02 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossRateLimiter.kt:48-56 | label:required-state-not-designed | C-05 수정이 이미 슬롯을 예약하고 잠든 호출에는 닿지 않습니다. `acquire`(`:21-25`)는 예약한 대기만큼 락 밖에서 자고 깨면 바로 보내므로, `deferNextSlot` 이 맵의 다음 슬롯만 밀어도 그 앞에 잡힌 슬롯은 Retry-After 창 안에 나갑니다. 탐침: ASSET 초당 5, t=0 에 세 호출 예약(대기 0·200·400ms) 뒤 `deferNextSlot(ASSET, 1000)` → 새 호출은 1000ms 를 기다리지만 200·400ms 호출은 창 안에 송신. 기본값(상한 5초)이면 그룹마다 최대 «상한 ÷ 간격»개(ASSET 약 25개, step 7 의 MARKET_DATA 약 75개)가 창 안에 나가 각자 429 와 재시도 예산을 씁니다. KDoc(`:49`) «모든 스레드가 같은 Retry-After 를 나눔»과 어긋남 | should-fix | 이 변경 | 아니오 | 중간 | 그룹별 «멈춤 끝 시각»을 따로 두고 `acquire` 가 잠에서 깬 뒤 그 시각을 다시 확인해 남은 만큼 더 잠 | — |
| S6-03 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/BrokerAccountQueryService.kt:52-55 | label:missing-input-validation | 연결 판정이 형식(숫자·Long 범위)만 봐서, 오타·해지 등으로 숫자지만 틀린 `externalAccountId` 는 연결로 통과합니다. 토스가 그 계좌 헤더를 거부하면 `TossFailureClassifier` 가 403 은 `TOSS_AUTH_FAILED` 503 «토스 키 설정을 확인해 주세요», 그 밖 4xx 는 `TOSS_REQUEST_REJECTED` 400(호출자 입력 오류)으로 냅니다. 호출자 입력은 accountId 뿐이라 400 은 원인을 오해시키고, 해결책인 재연결(409 `TOSS_ACCOUNT_NOT_LINKED` 의 안내)로 가지 않습니다. 토스가 모르는 accountSeq 에 어떤 상태·code 를 주는지는 명세를 열 수 없어 확인하지 못했습니다 | should-fix | 이 변경 | 아니오 | 낮음 | D18 실호출 대조 때 계좌 헤더 거부의 상태·code 를 확인해 그 code 를 409 `TOSS_ACCOUNT_NOT_LINKED` 로 분류 | — |
| S6-04 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossRateLimiter.kt:41 | label:required-state-not-designed | C-01 이 429 대기 예산 키 `rate-limit-max-wait-millis` 를 리미터 줄 대기 상한으로 같이 씁니다. 범위 검사(`TossProperties.kt:94`)가 0 을 유효로 받는데(«429 는 기다리지 않음» 의도), 0 이면 대기가 1ms 라도 필요한 호출은 모두 `TOSS_RATE_LIMITED` 입니다. 잔고 어댑터는 ORDER_INFO 를 연달아 두 번 부르므로(KRW·USD) 첫 buying-power 응답이 간격(초당 6 → 167ms)보다 빨리 오면 다른 요청이 없어도 자기 둘째 호출에서 실패합니다(시험 `TossRateLimiterTest.kt:212-216` 이 상한 0 실패를 못박음). 키 주석(`TossProperties.kt:39`) «429 재시도 횟수와 그 대기 시간 합계의 상한»은 새 뜻을 담지 않음 | should-fix | 이 변경 | 아니오 | 중간 | 리미터 줄 대기 상한을 별도 키로 두거나, 같은 키를 쓰려면 하한을 «한 요청 안 연속 호출 간격» 이상으로 두고 주석 갱신 | — |
| S6-05 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/port/BrokerAccountBalancePort.kt:21 | CS1 | 끝 4자리 마스킹을 기존 `TossTransportSupport.maskTail` 대신 새로 짜 동작이 갈립니다. 4자리 이하 값을 `maskTail` 은 `****` 로 숨기지만 `BrokerAccountRef.toString` 은 `****12` 처럼 전체를 보입니다. 같은 값의 다른 표현도 어긋납니다: `TossAccount.toString`(`TossAccount.kt:8`)은 로그 안전용으로 덮어썼는데 accountSeq 전체를 싣고, `TossAccountSummary`(`TossAccountListPort.kt:9`)는 기본 data class 문자열에 전체를 싣습니다. 지금 이 둘을 찍는 로그는 없어 잠복이지만, 계획 §7·step-6 §4 «accountSeq 로그는 끝 4자리»와 어긋나고 다음 로그 한 줄이 그대로 샙니다. `maskTail` 이 infrastructure 에 있어 application 이 import 하면 PTB7 에 걸리는 것이 재작성 이유로 보입니다 | should-fix | 이 변경 | 아니오 | 중간 | 마스킹 함수를 모든 계층이 import 할 수 있는 최상위 `common` 한 곳에 두고 `TossApiRequest`·`BrokerAccountRef`·`TossAccount`·`TossAccountSummary` 문자열 표현이 그 함수만 씀 | — |
| S6-06 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/BrokerAccountBalanceController.kt:16 | label:hardcoded-value-not-ssot | `GET /api/v1/accounts/abc/broker-balance` 처럼 accountId 가 숫자가 아니면 `GlobalExceptionHandler.kt:55-59` 가 모든 타입 불일치를 400 `INVALID_BUSINESS_DATE`(날짜 오류 code)로 내고 문구에는 Spring 원문(파라미터 이름·타입·입력값)을 그대로 싣습니다. 처리기는 이 변경이 건드리지 않은 기존 줄이고 새 경로가 그 결함을 물려받습니다 | should-fix | 기존 | 아니오 | 높음 | 타입 불일치 처리기가 고정 code(`BAD_REQUEST` 류)와 고정 문구를 내고 날짜 전용 code 는 날짜 파라미터에서만 | — |
| S6-07 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/account/BrokerAccountQueryServiceTest.kt:36 | TT7 | 프로세스 안 협력자를 목으로 바꿉니다. `AccountQueryService`(우리 유스케이스)를 `mockk` 로 대신해 «없는 계좌 → `ACCOUNT_NOT_FOUND`»(`:188-195`)는 목이 던진 예외가 지나가는지만 보고, 컨트롤러 시험도 서비스를 `@MockitoBean` 으로 바꿔 실제 조회 계약을 어디서도 잇지 않습니다. `TossAccountBalanceAdapterTest.kt:45, 152-157` 도 우리 리미터를 relaxed 목으로 두고 호출 순서·횟수를 단언합니다(TT44) | should-fix | 이 변경 | 아니오 | 낮음 | 계좌 조회를 포트로 두고 가짜 구현을 쓰거나 실제 서비스 + 가짜 리포지토리, 리미터는 실제 `TossRateLimiter` + 주입 시계로 그룹 간격을 관찰 | — |

## 3. 범위 밖 (안 본 것)

- 문서 커밋 `423ea35`(step-6.md 필드표 부록 A)의 명세 정합성 — spec-review 몫입니다. 필드표와 DTO·어댑터 매핑이 서로 맞는지만 대조했고 맞습니다.
- 공식 명세 openapi.json 원문 — 샌드박스가 네트워크를 막아 열지 못했습니다. 계좌에 해외 주식 서비스가 없을 때 `buying-power?currency=USD` 가 4xx 를 주면 잔고 전체가 실패하는지는 D18 실호출 대조 때 확인할 항목입니다.
- 시험 실행: 전체 스위트는 지시대로 돌리지 않았고, 개별 시험도 샌드박스가 Gradle·Docker 를 막아 돌리지 못했습니다. 근거는 작성자 집계(476건·297건)입니다.
- 사람 결정(수익률 퍼센트 통일, accountNo 끝 4자리, 새 API 숫자는 JSON 문자열)은 결함으로 보지 않았습니다.

<!-- devkit:standard-list hex:7b22636861696e223a22646f63732f66656174757265732f72756e6e696e672f6d756c74692d62726f6b65722d6b69732d746f73732f7265766965772f73746570362d636f64652d7265766965772e6d64222c22726f756e6473223a7b2231223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2232363932633264623563643661343739393539643036363161316335386266653435306638616362227d7d7d -->

<!-- devkit:reviewed-head 2692c2db5cd6a479959d0661a1c58bfe450f8acb -->
