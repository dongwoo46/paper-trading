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
| Status | todo |
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

-

---

## 6. Verification  (record)

| What | How it was checked | Result |
|---|---|---|
|  | test / screenshot / API contract / manual |  |

---

## 7. Result  (record)

- Done:
- Changed files / PR:

---

## 8. Problems found  (record)

> Found during the work — it can cause a new step, a change to an existing one, or a flag. Edit this step.md directly then (the reason goes in state.md §3).

| Problem | How it was handled |
|---|---|
|  | new step / change an existing step / open-questions flag / ignore |

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
