# Step 7 — 시세 REST와 최종 확인

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
| Step | 7 |
| Title | 시세 REST와 최종 확인 |
| Feature | multi-broker-kis-toss |
| Based on | development-plan §5 1단계 7번(시세 REST)·8번 중 전체 시험 재실행, §2 D9(내부 API 경로)·D13(BigDecimal)·D14(결측)·D15(400 TOSS_REQUEST_REJECTED)·D16(한도)·D18(공식 명세 원본)·D19(Redis에 안 씀), §6 시세·컨트롤러·부동소수점 행, §7 수치 무결성·결측 행, open-questions FLAG-08·10·18, 조사 문서 «개별 API 스키마», 공식 명세 openapi.json 1.2.28(PriceResponse·OrderbookResponse·OrderbookEntry·CandlePageResponse·Candle·SymbolQuery), review/step6-code-review.md S6-01·S6-02·S6-04·S6-05, 사람 결정 2026-11-15(시각은 토스 오프셋 그대로, S6-04 줄 대기 상한 키 분리) |
| Status | in progress |
| Owner |  |

---

**━━ Plan block (required before starting · from the planner / development-plan design) ━━**

## 1. Goal and scope  (plan)

> What this step (phase) **builds (what)** + **how far it goes (this step's boundary — name what is handed to the next step)**.

- Builds: 토스 시세(현재가·호가·캔들)를 trading-api가 REST로 직접 조회해 내부 API로 낸다(D9, D19 Redis에 안 씀): 포트 TossMarketDataPort와 값 타입, TossMarketDataClient(infrastructure), TossMarketQueryService(입력 검증), TossMarketController의 GET /api/toss/market/prices·/orderbook·/candles. 함께 step 6 리뷰 후속 S6-01·S6-02·S6-04·S6-05와 공용 클라이언트의 쿼리 `+` 인코딩 결함을 고치고, 1단계 최종 확인(CI 전체 스위트·린트·범위 점검)을 한다
- Scope (where this step stops): 포함: 위 클래스, 응답 DTO, 새 예외 InvalidMarketQuery(A 400), 시세 토스 DTO와 명세 기반 fixture(가짜 값), 필드표(이 문서 부록 A), 새 API 숫자는 JSON 문자열(FLAG-10), 시각은 토스 오프셋 그대로(사람 결정 2026-11-15), 새 설정 rate-limit-max-queue-wait-millis(S6-04 사람 결정). 제외: /trades·market-calendar·market-indicators·웹소켓·Redis quote 기록(3단계), 실제 토스 호출 대조(사람이 키·허용 IP 준비 뒤, §5 8번 수동 확인은 남김), infrastructure/kis/**·collector-api·trading-web, S6-03·S6-06·S6-07(findings 원장)

---

## 2. Approach (how, in what order)  (plan)

> **By what method and in what order** this step (= phase) is solved — briefly (one line to a few). Based on the feature-spec / development-plan design.
> A rough level is enough (the detailed work log is §5). **Never leave it empty — the step's base is here.**

- 필드 매핑(공식 명세 1.2.28, 부록 A): prices = symbol*, timestamp(null 허용), lastPrice*, currency*. orderbook = timestamp(null 허용), currency*, asks*[]{price*, volume*}(낮은 가격순), bids*[]{price*, volume*}(높은 가격순), 순서는 토스 그대로. candles = candles*[]{timestamp*, openPrice*, highPrice*, lowPrice*, closePrice*, volume*, currency*}(최신순 그대로), nextBefore(null 허용). 숫자는 BigDecimal(String), 누락·변환 실패는 TOSS_RESPONSE_INVALID(D14), 빈 목록은 정상. currency는 모르는 값 허용(문자열)
- 시각(사람 결정 2026-11-15 «토스 오프셋 그대로»): timestamp·nextBefore는 DTO에서 문자열로 받고 OffsetDateTime.parse로 형식만 검증(실패는 TOSS_RESPONSE_INVALID), 응답에는 토스 원문 문자열을 그대로 싣는다. Jackson OffsetDateTime 역직렬화는 UTC로 바꿔 미국 일봉 거래일이 흐려지므로 쓰지 않음. 요청 before도 오프셋 포함 ISO 문자열로 받아 검증 뒤 그대로 넘김
- 입력 검증(우리 쪽 먼저, 400 INVALID_MARKET_QUERY, 문구에 입력값을 싣지 않음): symbols는 콤마 구분 1~200개이고 원소마다 명세 패턴 ^[A-Za-z0-9.\-]+$(빈 원소 거부), symbol은 같은 패턴, interval은 1m|1d, count는 1~200(생략 시 토스 기본 100), before는 오프셋 포함 ISO date-time, adjusted는 boolean(생략 시 보내지 않음). 토스의 404 stock-not-found 등 4xx는 D15대로 400 TOSS_REQUEST_REJECTED
- 쿼리 인코딩 결함: TossApiClient가 UriComponentsBuilder.build().encode()를 써서 before의 `+`가 그대로 나가 토스가 공백으로 읽음(명세: `+`는 %2B). 값 인코딩을 고쳐 `+`가 %2B로 나가는지 MockRestServiceServer로 시험
- 한도 그룹: prices·orderbook = MARKET_DATA, candles = MARKET_DATA_CHART. 계좌 헤더 없음
- 리뷰 후속: S6-01 TossJson에서 빈·공백 문자열의 스칼라 강제 변환을 Fail로(null 허용 BigDecimal도 명시적 JSON null만 null). S6-02 TossRateLimiter에 그룹별 «멈춤 끝 시각»을 두고 acquire가 잠에서 깬 뒤 다시 확인해 남은 만큼 더 잠(이미 예약한 호출도 Retry-After를 나눔). S6-04 리미터 줄 대기 상한을 새 키 trading.toss.rate-limit-max-queue-wait-millis(TOSS_RATE_LIMIT_MAX_QUEUE_WAIT_MILLIS, 기본 5000, 1 이상)로 분리하고 rate-limit-max-wait-millis는 429 대기 예산만, 두 키 주석 갱신(사람 결정 2026-11-15). S6-05 끝 4자리 마스킹을 최상위 common 한 함수로 옮기고 TossApiRequest·BrokerAccountRef·TossAccount·TossAccountSummary의 toString이 그 함수만 씀(4자리 이하는 전부 가림). 각 항목 실패하는 시험 먼저
- 실패하는 시험 먼저: TossMarketDataClientTest(fixture KR·US prices, 빈 result, timestamp null, 필수 필드를 하나씩 뺀 응답, 정수부 15자리·소수 6자리 compareTo·toPlainString, orderbook KR·US·빈 호가, candles 1d·1m·마지막 페이지 nextBefore null, 미국 일봉 -04:00 원문 보존, before `+` 인코딩, 한도 그룹), TossMarketQueryServiceTest(입력 검증 경계: 0·1·200·201개, 빈 원소, 패턴 밖 문자, interval 밖 값, count 0·1·200·201, 오프셋 없는 before), 컨트롤러 통합 시험(KisAccountBalanceControllerIntegrationTest 패턴, 경로·상태·code, 숫자가 JSON 문자열, 시각 원문 그대로)
- 부동소수점 스캔 대상에 새 파일이 들어가는지 확인(application/toss/**, infrastructure/toss/**). 계획서 §4 설정 표·D16에 새 키 한 줄 반영(설계 재승인 필요)
- 최종 확인: ktlintCheck detekt verifyLintBaselinesNotGrown -PlintBaselineRef=origin/main 통과(레포 전체 ktlintFormat 금지), 관련 시험 실행 후 xml 집계, 푸시 뒤 CI full-test success, git diff로 infrastructure/kis/**·collector-api·trading-web 변경 0

---

## 3. Dependencies  (plan)

- Previous step: step 6 done(2692c2d 잔고·계좌 목록, 400d382 완료 기록)
- Prerequisites: Docker 실행 중(Testcontainers Redis). 토스 키·허용 IP 불필요(명세 기반 fixture). 새 설정 키는 계획서 변경이라 구현 전 설계 재승인

---

## 4. Completion criteria  (plan)

- [ ] §6 시세 행(prices·orderbook·candles 가격과 수량 BigDecimal, 누락은 TOSS_RESPONSE_INVALID)과 컨트롤러 행의 기대 결과가 시험으로 통과
- [ ] 입력 검증 경계 시험 통과, 문구에 입력값 없음
- [ ] 응답 시각이 토스 원문 문자열 그대로(미국 일봉 -04:00 보존), before의 `+`가 %2B로 전송
- [ ] S6-01·S6-02·S6-04·S6-05 각각 실패하던 시험이 통과
- [ ] 새 API 숫자는 JSON 문자열, 기존 API 응답 변경 0, Redis quote 쓰기 0
- [ ] 토스 코드에 Double·Float·toDouble·toFloat 0건, 토큰·비밀값 로그 누출 0건
- [ ] 관련 시험 실패 0, 린트·기준선 증가 검사 exit 0, CI full-test success
- [ ] infrastructure/kis/**·collector-api·trading-web 변경 0

---

**━━ Record block (filled in order while developing · never ahead) ━━**

## 5. Work log  (record)

> Fill it by spelling out the §2 approach as the actual work (concretely, what was done).

- 시험 먼저: 새 시험 4개 클래스(`TossMarketDataClientTest`, `TossMarketQueryServiceTest`, `TossMarketControllerIntegrationTest`, `MaskAllButLastFourTest`)와 기존 5개 클래스 보강, 스텁(`TODO()` 뼈대)으로 컴파일만 되게 한 뒤 Red 실행
- 포트 `TossMarketDataPort`와 값 타입, `TossMarketDataClient`(prices·orderbook MARKET_DATA, candles MARKET_DATA_CHART), 토스 DTO `TossMarketData`, 시각은 문자열로 받고 `OffsetDateTime.parse`로 형식만 검증
- `TossMarketQueryService` 입력 검증(400 `INVALID_MARKET_QUERY`, 새 예외 `InvalidMarketQueryException`), `TossMarketController` 3경로, 응답 DTO `TossMarketResponse`(숫자는 JSON 문자열)
- 쿼리 `+` 인코딩: `TossTransportSupport.withPlusEncodedInQuery` 한 곳에서 `+`를 `%2B`로, 쉼표·콜론은 그대로
- 리뷰 후속: S6-01 `withCoercionConfigDefaults`로 빈·공백 문자열 강제 변환 Fail. S6-02 그룹별 멈춤 끝 시각, 깬 뒤 재확인. S6-04 새 키 `rate-limit-max-queue-wait-millis`(application.yaml, TossProperties 범위 검사 1 이상). S6-05 `common/MaskAllButLastFour.kt` 한 함수로 옮기고 네 toString이 그것만 씀
- fixture 9개(prices 2, orderbook 3, candles 4)와 README 줄 추가
- 리뷰 후속(review/step7-code-review.md, 사람 결정 2026-11-16 «후속 4건도 고치자», 설계 재승인 ed07dc1):
  - S7-01 `TossRateLimiter`: 깬 뒤 멈춤이 예약 때보다 늘었고 아직 안 끝났으면 `reserveSlot` 다시 호출(멈춤 끝 뒤 간격대로 새 슬롯, 줄 대기 상한 재검사)
  - S7-02 `TossJson`: 문자열 토큰 `null`(앞뒤 공백 포함)을 실패로 돌리는 BigDecimal 역직렬화기, 나머지는 기본 역직렬화기에 위임
  - S7-03 `TossProperties`: 줄 대기 상한이 설정된 그룹 간격 최댓값보다 작으면 범위 오류로 비활성(계획서 D16·§4), 간격 식은 `TossRateLimiter.intervalMillisOf` 한 곳, 키 주석·`application.yaml` 주석 갱신
  - S7-04 시험: 컨트롤러 시험은 실제 `TossMarketQueryService` + 가짜 `TossMarketDataPort`, `TossMarketDataClientTest`는 실제 `TossRateLimiter` + 주입 시계

---

## 6. Verification  (record)

| What | How it was checked | Result |
|---|---|---|
| Red | 스텁으로 컴파일만 되게 한 뒤 새·바뀐 시험 실행(maker) | 실패 192건(MarketQueryService 91, MarketDataClient 60, TossJson 11, Controller 10, Mask 9, RateLimiter 6, Properties 3, ApiClient 2) |
| 메인 재실행 | `./gradlew test --rerun --offline --tests '*Toss*' '*MaskAllButLastFourTest' '*BrokerAccount*' '*ArchitectureRulesTest' '*KisAccountBalance*'`, 실행 뒤 생긴 xml만 집계 | 22개 클래스 541건, 실패 0, 오류 0, 건너뜀 0 |
| 린트 | `./gradlew compileKotlin compileTestKotlin ktlintCheck detekt verifyLintBaselinesNotGrown -PlintBaselineRef=origin/main --offline` 메인 재실행 | exit 0 |
| 부동소수점 | `TossFloatingPointScanTest`(대상에 `presentation/dto/toss`·`Toss*Controller` 추가) + 메인 grep | 0건(기존 주석 1줄 제외) |
| 범위 | `git status`로 `infrastructure/kis/**`·collector-api·frontend | 변경 0 |
| 전체 스위트(구현) | `356b248` 푸시 뒤 CI trading-api·collector-api·trading-web | success |
| 전체 스위트(main 합침) | `d3b8056` 메인 `./gradlew test --rerun --offline`, 실행 뒤 생긴 xml만 집계 | 82개 클래스 1049건, 실패 0 |
| 코드 리뷰 | `review/step7-code-review.md` 1회차 | 머지 가능, 막는 지적 0, 후속 4(S7-01~S7-04) |
| 후속 Red | 바꾼 시험 먼저 실행(maker) | S7-01 2건(송신 시각 `[2000,2000,2000]`, 상한 초과 미발생), S7-02 4건, S7-03 4건. S7-04는 시험만 바뀌어 Red 없음, 대신 `MAX_COUNT` 임시 변경으로 `count=201` 시험이 실패함을 확인 뒤 원복 |
| 후속 Green(바꾼 클래스) | maker 실행 | `TossJsonTest` 33, `TossMarketDataClientTest` 60, `TossPropertiesTest` 65, `TossRateLimiterTest` 27, `TossMarketControllerIntegrationTest` 19, 실패 0 |
| 후속 전체 스위트 | 메인 `./gradlew test --rerun --offline`, 실행 뒤 생긴 xml만 집계(실행 전 xml 0) | 82개 클래스 1073건, 실패 0, 오류 0, 건너뜀 0 |
| 후속 린트 | `./gradlew compileKotlin compileTestKotlin ktlintCheck detekt verifyLintBaselinesNotGrown -PlintBaselineRef=origin/main --offline` 메인 재실행 | exit 0 |
| 후속 범위·부동소수점 | `git diff --name-only origin/main` 의 `infrastructure/kis`·collector-api·frontend, toss 코드 grep | 변경 0, 부동소수점 0건(기존 주석 1줄 제외) |
| 후속 CI | 후속 커밋 푸시 뒤 PR CI | 대기 |

---

## 7. Result  (record)

- Done:
- Changed files / PR: 경로는 `backend/trading-api/src/` 기준
  - main 신설: `application/toss/{TossMarketQueryService,port/TossMarketDataPort}.kt`, `common/MaskAllButLastFour.kt`, `common/exception/InvalidMarketQueryException.kt`, `infrastructure/toss/{TossMarketDataClient,dto/TossMarketData}.kt`, `presentation/controller/TossMarketController.kt`, `presentation/dto/toss/TossMarketResponse.kt`
  - main 수정: `BrokerAccountBalancePort`, `TossAccountListPort`, `TossApiClient`, `TossApiRequest`, `TossJson`, `TossProperties`, `TossRateLimiter`, `TossTransportSupport`, `dto/TossAccount`, `application.yaml`
  - test 신설 4, 수정 5(`TossApiClientTest`, `TossJsonTest`, `TossRateLimiterTest`, `TossPropertiesTest`, `TossFloatingPointScanTest`), fixture 9
  - 리뷰 후속: main 수정 `TossJson`, `TossProperties`, `TossRateLimiter`, `application.yaml`, test 수정 `TossJsonTest`, `TossMarketDataClientTest`, `TossPropertiesTest`, `TossRateLimiterTest`, `TossMarketControllerIntegrationTest`
  - 커밋: 구현 `356b248`, main 합침 `d3b8056`, 리뷰 기록 `9e8a7fd`, 설계 재승인 `ed07dc1`, 후속 수정은 이 기록과 함께 커밋. PR은 `feature/multi-broker-kis-toss-market` → main

---

## 8. Problems found  (record)

> Found during the work — it can cause a new step, a change to an existing one, or a flag. Edit this step.md directly then (the reason goes in state.md §3).

| Problem | How it was handled |
|---|---|
| 컨트롤러 파라미터 형식 | ASSUME: 전부 `String?`로 받고 서비스 한 곳에서 검증, 숫자 형식 오류도 같은 `INVALID_MARKET_QUERY` |
| adjusted·count 표기 | ASSUME: adjusted는 소문자 `true`/`false`만, count는 숫자만(앞자리 0은 허용) |
| 중복 종목 | ASSUME: 형식만 검증, 중복은 막지 않음(토스에 그대로) |
| before 오프셋 표기 | ASSUME: `OffsetDateTime.parse` 통과 값만. `+09`처럼 콜론 없는 오프셋도 통과하고 토스가 거부하면 `TOSS_REQUEST_REJECTED` |
| 응답 시각 파싱 실패의 원인 예외 | ASSUME: cause를 잇지 않음(파싱 예외 문구에 응답 원문이 있음) |
| S6-01 적용 범위 | ASSUME: 스칼라뿐 아니라 객체·목록 칸의 빈·공백 문자열도 Fail. 문자열 칸의 빈 문자열은 그대로 읽음(시험 고정) |
| S6-02 깨어남 몰림 | 429로 멈춘 호출들이 같은 시각에 함께 깸. 리뷰 S7-01로 확인, 깬 뒤 슬롯 재예약으로 고침 |
| 줄 대기 상한과 AUTH 간격 | 리뷰 S7-03의 한 경우. 하한을 설정된 그룹 간격 최댓값으로 올려 고침(사람 결정 2026-11-16) |
| 그룹이 하나도 없을 때 상한 하한 | ASSUME: 1(기존 0 거부·1 통과 시험 유지). D16·§4는 «설정된 그룹 간격 최댓값 이상»만 정함 |
| S7-01 멈춤이 이미 끝난 경우 | ASSUME: 깬 시각에 늘어난 멈춤도 끝났으면 재예약하지 않음(슬롯 한 칸 추가 소모 방지) |
| S7-04 남은 목 | `TossMarketDataClientTest`의 `tokenManager`는 목 그대로(리뷰 지적은 리미터뿐). S6-07은 findings 원장 그대로 |
| 구현 중 규칙 어김 | maker가 `TossRateLimiterTest.kt` 추가분 52줄을 Edit 대신 셸 python으로 편집(자기 보고). `git diff`로 내용 확인, 이후 편집은 Edit/Write |
| S6-05 계좌 목록 요약 | `accountNoLast4`는 이미 끝 4자리라 그대로, `accountSeq`만 마스킹 |
| 기존 시험 단언 변경 | `TossRateLimiterTest.capped` 한 줄(상한 키 이름), 기대값 동일, `devkit:test-change` 표시 |
| 미국 일봉 fixture | 명세 예시가 없어 임의 가짜 값, README에 적음 |
| 실호출 대조(§5 8번) | 사람이 토스 키·허용 IP 준비 뒤, 1단계 PR 전 남은 일 |

---

## 부록 A. 필드표 (공식 명세 openapi.json 1.2.28, 2026-11-15 확인)

`*` = 명세 required. 숫자는 전부 decimal 문자열(maxLength 30) → `BigDecimal(String)`. required 누락·변환 실패는 `TOSS_RESPONSE_INVALID`(D14). 시각은 토스 원문 문자열 그대로(사람 결정 2026-11-15), `OffsetDateTime.parse`로 형식만 검증.

| API (한도 그룹) | 요청 | 토스 응답 필드 | 필수 | 우리 응답 |
|---|---|---|---|---|
| `GET /api/v1/prices` (MARKET_DATA) | `symbols*` 콤마 구분 1~200개, 패턴 `^[A-Za-z0-9.,\-]+$` | `result[].symbol` | * | `symbol` |
|  |  | `result[].timestamp` (date-time, null 허용) |  | `timestamp`(원문, null 그대로) |
|  |  | `result[].lastPrice` | * | `lastPrice`(JSON 문자열) |
|  |  | `result[].currency` (모르는 값 허용) | * | `currency`(문자열) |
| `GET /api/v1/orderbook` (MARKET_DATA) | `symbol*` 패턴 `^[A-Za-z0-9.\-]+$` | `result.timestamp` (null 허용) |  | `timestamp` |
|  |  | `result.currency` | * | `currency` |
|  |  | `result.asks[]{price*, volume*}` (낮은 가격순) | * | `asks`(순서 그대로) |
|  |  | `result.bids[]{price*, volume*}` (높은 가격순) | * | `bids`(순서 그대로) |
| `GET /api/v1/candles` (MARKET_DATA_CHART) | `symbol*`, `interval*` 1m\|1d, `count` 1~200(기본 100), `before` date-time(`+`는 %2B), `adjusted` boolean(기본 true) | `result.candles[]{timestamp*, openPrice*, highPrice*, lowPrice*, closePrice*, volume*, currency*}` (최신순) | * | `candles`(순서 그대로) |
|  |  | `result.nextBefore` (null 허용, 마지막 페이지면 null) |  | `nextBefore`(원문) |

- 1m 봉 timestamp는 봉 종료 시각(`[t-1분, t)` 집계), 1d 봉은 현지 자정 고정. 미국 일봉은 `-04:00`·`-05:00` 오프셋이라 KST로 바꾸지 않음
- 토스 오류: 400 invalid-request, 404 stock-not-found → D15대로 400 `TOSS_REQUEST_REJECTED`, 500 internal-error → `TOSS_UNAVAILABLE`, 429 → 기존 처리
- 우리 내부 API 파라미터 이름은 토스와 같게 둠(`symbols`, `symbol`, `interval`, `count`, `before`, `adjusted`)
