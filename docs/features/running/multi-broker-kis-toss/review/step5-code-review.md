범위: `변경분` — 커밋 `908ac89`(부모 `e3b284e`)의 코드·시험·설정 변경. 문서 커밋 `e3b284e`·`197818d` 는 범위 밖입니다.

# 코드 리뷰 결과 — multi-broker-kis-toss step 5 «스키마 확정과 토스 전송 계층» (1회차)

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 리뷰 대상 | worktree `.worktrees/multi-broker-kis-toss`, `git diff e3b284e 908ac89` 의 docs 밖 57파일(main 신설 18 · main 수정 6 · 시험 신설·수정 10 · fixture 20 · 기준선 xml 2) |
| 기준 | `rule-pointer --list` 가 낸 목록 그대로: devkit `rules/dev-team/common.md`(AE18·AE21·CS1~CS25·C12·DD1~DD6) · `rules/dev-team/failure-handling.md`(FH1~FH6, Diff checks 4행) · `rules/dev-team/tests.md`(TT 조항) · 레포 `rules/failure-handling.md`(PTF1~PTF6) · 레포 `rules/backend-api.md`(PTB1~PTB9) · 레포 `rules/tests.md`(PTT1) · `always:dev-team` |
| 프로필 | consumer (`profile:consumer` 행) |
| 리뷰어 | code-reviewer (구현자와 독립, 읽기 전용) |
| 날짜 | 2026-11-15 |

## 1. 요약

| 항목 | 값 |
|---|---|
| 리뷰한 렌즈 | 정확성 · 표준 · 유지보수성 · 보안 · 성능 · 회귀 시험 · 의도. 근본성: bugfix diff 가 아닙니다 — R1-01·R1-02 는 설계 D10·D11 을 사람 재승인(FLAG-16)으로 고친 뒤 식과 상한 자체를 바꿔 반영해, 호출부 분기로 증상만 덮은 꼴이 아닙니다 |
| 의도 | 커밋 `의도:`·`기존:`·`변경:` 블록과 diff 가 맞습니다. `출처:` step-5.md·FLAG-16 사람 결정은 열어 확인했습니다. 블록에 없는 기존 동작 변경은 없습니다(`RestTemplateConfig` 는 값 불변 상수 승격, 기준선은 린트 항목 삭제, 새 설정 키는 기본값이 있어 기존 배포를 비활성으로 바꾸지 않음) |
| 수정 지킴 확인 | 확인 불가 — 탐침 샌드박스가 Gradle 캐시 쓰기와 Docker 소켓(시험이 Testcontainers Redis 사용)을 막아 `guards-fix` 를 돌리지 않았습니다. 통과가 아닙니다. 작성자 증거: CI `full-test` 43700036485 `success`(headSha `908ac89` 일치, `gh run view` 로 확인), step-5 §6 관련 시험 253건 실패 0 · 변이 실험 3건 |
| 뮤테이션 | `mutation-run.js --base e3b284e` exit 4, 파일 목록 없이 «뮤테이션 대상 없음» — 이 레포(Kotlin)에서 범위에 잡힌 파일이 없습니다 |
| 재사용 점검 | `reuse-inventory --target` (TossRateLimiter·TossTransportSupport·TossJson) 재생성 의심 0건. 마스킹·Retry-After 해석·Jackson 매퍼의 기존 도우미는 grep 으로 찾지 못했습니다 |
| 발견 | 8개 (막음 0 · 후속 후보 8) |
| 머지 판정 | `review-verdict.js` 첫 줄: 머지 판정: 가능 — 막는 지적 0건 · 후속 후보 8건 |
| 한 줄 | 비밀값 바닥(form 바이트 전송, 로그·응답에 토큰·secret·토스 원문 없음), 401 1회 한정, 429 곱셈 넘침 방지, 락 해제는 맞게 섰고 막는 지적은 없습니다. 남은 위험은 호출처가 붙는 step 6~7 에서 드러나는 것들입니다: 리미터 대기 상한 없음(C-01), base URL 형식 미검사로 400 오분류(C-02), 목록 원소 null·실수 절삭 통과(C-03), 요청 상한 검증 경계(C-04) |

기타: `skill-session.js open code-review` 는 샌드박스가 `~/.devkit` 쓰기를 막아 EPERM 으로 실패했습니다(리뷰 절차는 진행). maker 가 보고한 ASSUME 8건은 설계 판단으로 보고 결함으로 지적한 것이 없습니다(C-05 는 429 기본값이 아니라 대기가 리미터와 공유되지 않는 구조 문제입니다).

## 1-1. 프로필별 검토 절 (consumer)

- 1. 규칙 준수 — 대조한 조항 출처: `rule-pointer --list` 목록만(모델이 따로 고른 규칙 없음).
  - common.md: AE18·AE21·CS14·CS21·CS22 — `TossJson.readOrNull` 은 좁은 `JsonProcessingException` 만 잡고 null 을 주지만 오류 본문 분류 보조라 호출 쪽이 상태 코드로 실패를 냅니다(성공처럼 보이는 대체값 아님). `parseResult`·`parse` 는 같은 예외를 `TOSS_RESPONSE_INVALID` 로 다시 던지고, `send`·`post` 는 `RestClientResponseException`·`RestClientException` 만 잡습니다. CS24 → C-06. CS25 — 새 함수는 모두 40줄·중첩 3단 안입니다. CS1·CS4·CS5 — 위 재사용 점검. CS7 — `TossTokenIssuer` 는 외부 HTTP 경계라 인터페이스가 맞습니다. C12·CS15 — 새 주석은 명사형 표지이고 TODO/FIXME 가 없습니다. DD1·DD3 — 새 에러 code 8개(API 계약)는 D15 사람 승인 설계 그대로입니다.
  - failure-handling.md: Diff checks 4행 — 돈·주문·발송 POST, 웹훅, 메시지 소비자, 건별 루프가 이 diff 에 없어 해당 없음. FH1~FH6 해당 없음(토큰 발급 POST 는 돈·주문이 아니고 조회는 GET).
  - tests.md: TT1 — 새 소스마다 시험이 있습니다. TT16 — 시계·sleeper 는 주입하지만 환경변수 의존 단언 1건 → C-07. TT7·TT48 — 프로세스 밖 HTTP 만 `MockRestServiceServer` 로 대체. TT49 → 위 뮤테이션 행.
  - 레포 rules/failure-handling.md: PTF1·PTF2·PTF4·PTF5·PTF6 — 원장·체결·예수금·시각 구간이 없어 해당 없음. PTF3 — 토큰 발급 타임아웃은 상태를 남기지 않고(캐시가 빌 때만 발급) 조회 GET 은 멱등이라 해당 없음.
  - 레포 rules/backend-api.md: PTB1~PTB6·PTB4 — 도메인 모델·금액 계산이 없어 해당 없음(금액 필드는 `BigDecimal`, `USE_BIG_DECIMAL_FOR_FLOATS`, Double·Float 0건). PTB7 — 새 import 는 `infrastructure.toss → infrastructure.config·common`, `presentation → common` 뿐이고, common 의 새 예외 8개는 Spring 을 import 하지 않습니다(`TossRequestRejectedException` 은 FLAG-12 결정대로 동결 `BadRequestException` 상속). PTB8·PTB9 — 포트·유스케이스는 step 6~7.
  - 레포 rules/tests.md PTT1 — `TossTokenManagerTest`·`TossApiClientTokenFlowTest` 가 `SharedRedisContainer` 실제 Redis 를 씁니다. 충족.
  - always:dev-team — 시험 먼저(step-5 §6 Red 기록), 함수 길이·주석 위와 같음, 시간대 붙임 없음(epoch 밀리초만).
- 2. 사이드 이펙트 — `TossApiClient`·`TossTokenManager`·`TossRateLimiter` 의 main 호출처는 0건(grep)이라 새 빈만 생깁니다. `TossTokenManager` 빈 등록으로 기동 때 `TossTokenIssuerImpl` 이 주입되며 컨텍스트 기동 시험·CI 가 통과했습니다. `RestTemplateConfig` 는 값이 그대로(연결 5초·읽기 10초)라 같은 빈을 쓰는 KIS·Slack 동작이 바뀌지 않습니다. `GlobalExceptionHandler` 는 새 code 문구만 더했고 기존 `TOSS_TOKEN_UNAVAILABLE`·기본 문구는 그대로입니다. TTL 식(R1-01)은 수명이 갱신 여유의 두 배 이상이면 옛 식과 같은 값입니다. 기준선 xml 은 항목 삭제뿐(detekt 4줄, ktlint 2파일 6줄)이고, 지운 항목은 `RestTemplateConfig` 상수 승격·끝 줄바꿈과 `TradingApiApplicationTests` 끝 줄바꿈으로 실제로 고쳐졌습니다.
- 3. 버그·장애 위험 — 보안: form 본문을 바이트로 보내 RestTemplate DEBUG 로그에는 `[B@…` 만 찍히고, 발급·오류 로그는 상태와 `safeCode` 만, `TossTokenResponse`·`TossIssuedToken` 문자열 표현에 토큰이 없고, accountSeq 는 끝 4자리입니다. 응답은 code 별 고정 문구이고 `TOSS_REQUEST_REJECTED` 는 우리 문구와 상태 숫자만 싣습니다. 401: `authRetried` 로 1회 한정, `invalidate` 는 읽은 암호문 그대로 비교해 다른 쪽이 새로 저장한 토큰을 지우지 않고, 루프는 매 회 반환·예외·재시도 계수 증가 중 하나라 끝납니다. 429: 곱셈 전에 86,400초로 잘라 남은 상한과 비교해 넘침이 없습니다. 락: `issueHolding` 의 finally 가 R1-02 실패 경로에서도 해제합니다. 리미터 슬롯 예약은 `@Synchronized` 안이라 여러 스레드에서도 겹치지 않습니다. 남은 위험: C-01(부하), C-02(설정 형식), C-03(결측 통과), C-04(시간 경계), C-05(429 공유 안 됨), C-08(벽시계).
- 조항을 어기지 않는 코드 품질 지적은 `should-fix` 이하로 두었습니다.

## 2. 발견 (findings)

| ID | file:line | 렌즈 | 근거 조항 | 무엇이 틀렸나 | 등급 | 출처 | 회귀 | confidence | 구조 고침 | 재현 |
|---|---|---|---|---|---|---|---|---|---|---|
| C-01 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossRateLimiter.kt:19-23 | 성능 | label:precondition-check-missing | 리미터가 예약한 대기(`slot - now`)를 상한 없이 요청 스레드에서 잡니다. 슬롯이 줄 선 순서대로 간격만큼 밀려, ACCOUNT(초당 1) 그룹에 동시 요청 N개가 오면 N번째는 약 N−1초를 잡니다. 들어오는 속도가 한도보다 빠르면 대기열이 끝없이 길어져 Tomcat 스레드를 묶고 토스와 무관한 API 까지 멈출 수 있습니다. `TossApiClient` 의 대기 합계 상한(`rateLimitMaxWaitMillis`, RISK-08)은 429 `Retry-After` 대기만 세고 이 대기는 세지 않습니다. 호출처가 붙는 step 6~7 부터 드러납니다 | should-fix | 이 변경 | 아니오 | 높음 | 예약 전에 대기가 상한을 넘으면 슬롯을 잡지 않고 `TOSS_RATE_LIMITED` 로 바로 실패 | — |
| C-02 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossTokenIssuerImpl.kt:53 | 정확성 | label:missing-input-validation | `TOSS_BASE_URL` 은 비었는지만 봅니다(`TossProperties.kt:59`). 이 변경이 그 값으로 처음 호출을 만드는데(여기와 `TossApiClient.kt:67`), scheme 이 빠진 값(`api.example.com`)이면 이 레포 RestTemplate 의 JDK 클라이언트가 `IllegalArgumentException: URI with undefined scheme` 을 던집니다(탐침으로 JDK 문구 확인). 이 예외는 `RestClientException` 이 아니라 두 catch 를 지나 `GlobalExceptionHandler.handleIllegalArgument` 의 400 `BAD_REQUEST`(JDK 문구 그대로)로 나가, 서버 설정 오류가 호출자 입력 오류로 보이고 D17 의 `TOSS_INTEGRATION_DISABLED` 로 가지 않습니다. `http://` 도 받아 client secret 과 토큰이 평문으로 나갈 수 있습니다 | should-fix | 이 변경 | 아니오 | 중간 | 기동 판정(`missingOrMalformedKeyNames`)에서 절대 `https` URL 인지 검사해 아니면 `TOSS_BASE_URL(형식 오류)` 로 토스만 비활성 | — |
| C-03 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossJson.kt:21-27 | 정확성 | label:failure-or-absence-reported-as-value | D14(결측·숫자 변환 실패는 대체값 없이 `TOSS_RESPONSE_INVALID`)가 두 경로에서 새어 나갑니다. 1. 목록 원소 null: `{"result":[null]}` 를 `List<PriceSample>` 로 읽으면 실패 없이 `[null]` 이 됩니다. 비 null 원소 타입에 null 이 들어가 step 6~7 어댑터에서 NPE → 500 이 되거나 `?.` 로 조용히 빠집니다. 2. 실수→정수 절삭: `ACCEPT_FLOAT_AS_INT` 기본값이 켜져 있어 `expires_in: 3600.9` → 3600, `0.4` → 0 으로 읽습니다(지금은 0 이 `expires_in > 0` 검사에 걸리지만 Long·Int 필드 전반에 같은 절삭). 탐침: 커밋 빌드 클래스와 jackson 2.21.2 로 `TossJson.readSuccess`·`readValue` 를 직접 불러 네 경우 모두 예외 없이 위 값이 나옴 | should-fix | 이 변경 | 아니오 | 높음 | 매퍼에 `KotlinFeature.NewStrictNullChecks` 를 켜고 `ACCEPT_FLOAT_AS_INT` 를 끔, 경우마다 시험 한 줄 | — |
| C-04 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossProperties.kt:77 | 정확성 | label:missing-input-validation | 범위 검사는 `deadline <= 15000` 만 막고, 실행 검사(`TossTokenManager.kt:79-80`)는 `deadline − 경과 < 15000` 이면 발급하지 않습니다. 경과에는 Redis 왕복 세 번(캐시 확인·락 획득·재확인)이 늘 들어가 `TOSS_TOKEN_REQUEST_DEADLINE_MILLIS=15001` 같은 «유효» 값이면 경과가 2ms 만 돼도 첫 요청부터 매번 `TOSS_TOKEN_UNAVAILABLE` 인데 기동 로그는 «토스 연동 활성»입니다. 같은 자리의 발급 최악 15초(`ISSUE_WORST_CASE_MILLIS`)는 락 안에서 도는 AUTH 한도 대기(`TossTokenIssuerImpl.kt:38`)를 빼고 셉니다 | should-fix | 이 변경 | 아니오 | 높음 | 하한을 «발급 최악 + AUTH 간격 + 락 획득 여유»를 담은 한 상수로 두고 범위 검사와 실행 검사가 같은 값을 씀 | — |
| C-05 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossApiClient.kt:107-119 | 정확성 | label:required-state-not-designed | 429 를 받은 스레드만 `Retry-After` 만큼 자고 공용 리미터의 다음 슬롯은 그대로입니다. 토스가 한도를 공지 없이 낮추면(D16 이 한도를 설정값으로 둔 이유) 다른 스레드는 대기 창 안에서도 설정 속도로 계속 보내 각자 429 를 받고 재시도 2회를 다 써 `TOSS_RATE_LIMITED` 로 몰려 실패합니다. 429 가 한도 초과를 알려도 그 그룹의 송신 속도가 줄지 않습니다 | should-fix | 이 변경 | 아니오 | 중간 | 429 때 그 그룹의 다음 슬롯을 `now + Retry-After` 뒤로 미는 리미터 메서드 하나로 모든 스레드가 같은 대기를 공유 | — |
| C-06 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/exception/GlobalExceptionHandler.kt:106-112 | 표준 | CS24 | 응답 문구를 에러 code 문자열 리터럴 7개(`"TOSS_AUTH_FAILED"` 등)로 고릅니다. 같은 code 가 각 예외 파일(`TossAuthFailedException.kt:9` 등)과 시험에 따로 리터럴로 있어, 지금은 시험이 맞춰 잡지만 세 곳 중 한 곳만 바꾸면 조용히 기본 문구로 떨어집니다. step 4 재리뷰 R1-03(후속, 같은 함수의 리터럴 1개)과 같은 부류가 1개에서 8개로 늘었습니다 | should-fix | 이 변경 | 아니오 | 높음 | code 와 공개 문구를 예외 쪽 상수 한 자리에 두고 처리기는 그 값만 씀(R1-03 제안과 같음) | — |
| C-07 | backend/trading-api/src/test/kotlin/com/papertrading/api/TradingApiApplicationTests.kt:50 | 회귀 시험 | TT16 | `integrationEnabled()` 가 false 라고 단언하는데 `application-test.yaml` 은 `trading.toss.*` 를 덮지 않아 값이 실행 셸의 `TOSS_BASE_URL`·`TOSS_CLIENT_ID`·`TOSS_CLIENT_SECRET`·`TOSS_TOKEN_ENCRYPTION_KEY` 에서 옵니다(`application.yaml:53-56`). D18 대로 사람이 키를 준비해 실호출 대조를 하는 셸이나 나중에 CI 에 토스 비밀값을 넣으면 코드 변화 없이 이 시험이 빨개집니다 | should-fix | 이 변경 | 아니오 | 중간 | 이 시험에서 토스 필수 4개를 빈 값으로 고정(`@SpringBootTest(properties = [...])` 또는 test 프로필) | — |
| C-08 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossRateLimiter.kt:14 | 정확성 | label:env-or-local-state-assumed | 간격 계산에 벽시계(`System::currentTimeMillis`)를 씁니다. NTP 보정·VM 재개로 시계가 뒤로 X초 뛰면 이미 예약된 `nextFreeSlotMillis` 가 그만큼 미래가 되어 다음 호출이 X초를 잡습니다(C-01 과 겹치면 스레드가 줄줄이 묶임). 같은 기본값이 R1-02 요청 상한 계산(`TossTokenManager.kt:25`, `:54-55`)에도 쓰여 뒤로 뛰면 상한이 늘어납니다 | should-fix | 이 변경 | 아니오 | 낮음 | 경과 시간은 `System.nanoTime()` 기반 단조 시계로(주입 모양은 그대로) | — |

## 3. 범위 밖 (안 본 것)

- 문서 커밋 `e3b284e`·`197818d` 와 step-5.md §8 «토큰·에러 필드표 부록 미작성» — 명세 정합성은 spec-review 몫이라 보지 않았습니다.
- 실제 토스 호출(키·허용 IP 필요), fixture 값과 실응답 대조 — 이 step 완료 조건이 아닙니다.
- 시험 실행: 전체 스위트는 지시대로 돌리지 않았고, 개별 시험도 샌드박스가 Gradle·Docker 를 막아 돌리지 못했습니다. 근거는 CI `full-test` success 와 작성자 집계(253건)입니다. C-02·C-03 은 Gradle 없이 JDK·빌드 산출 클래스로 탐침했습니다.
- 발급 최악 15초가 실제 상한인지(DNS 해석 시간, 읽기 타임아웃이 전체 시간이 아닌 점) — D11 설계 전제라 지적하지 않았습니다.

<!-- devkit:standard-list hex:7b22636861696e223a22646f63732f66656174757265732f72756e6e696e672f6d756c74692d62726f6b65722d6b69732d746f73732f7265766965772f73746570352d636f64652d7265766965772e6d64222c22726f756e6473223a7b2231223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2231393738313864646134623864313432333734386632323535663430383339303239393662643334227d7d7d -->

<!-- devkit:reviewed-head 197818dda4b8d1423748f2255f4083902996bd34 -->
