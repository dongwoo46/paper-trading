# Step 6 — 잔고 공통 포트와 토스 어댑터

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
| Step | 6 |
| Title | 잔고 공통 포트와 토스 어댑터 |
| Feature | multi-broker-kis-toss |
| Based on | development-plan §5 1단계 6번(잔고), §2 D3(accountSeq 연결)·D7(잔고 공통 포트, cashBuyingPower 칸)·D8(모드 전수 when)·D9(내부 API 경로)·D13(BigDecimal)·D14(결측)·D15(에러 범주)·D16(한도), §3 바꿀 파일, §6 잔고 어댑터·모드 누락 표·컨트롤러·부동소수점·로그 누출 행, §7 수치 무결성·단위·결측 행, open-questions FLAG-04·07·10·18, 조사 문서 «개별 API 스키마», 공식 명세 openapi.json 1.2.28(Account·HoldingsOverview·BuyingPowerResponse), review/step5-code-review.md C-01~C-05, 사람 결정 2026-11-15(수익률 퍼센트 통일, accountNo 끝 4자리) |
| Status | in progress |
| Owner |  |

---

**━━ Plan block (required before starting · from the planner / development-plan design) ━━**

## 1. Goal and scope  (plan)

> What this step (phase) **builds (what)** + **how far it goes (this step's boundary — name what is handed to the next step)**.

- Builds: 토스 계좌의 잔고·보유·매수 가능 금액을 공통 포트로 읽어 내부 API로 내고, 운영자가 accountSeq를 확인할 계좌 목록 API를 만든다: BrokerAccountBalancePort와 값 타입(D7), TossAccountBalanceAdapter(holdings 1회 + buying-power KRW·USD 2회), TossAccountListClient, BrokerAccountQueryService(D8), TossAccountListQueryService, GET /api/v1/accounts/{accountId}/broker-balance, GET /api/toss/accounts. 함께 step 5 리뷰 후속 C-01~C-05를 고친다
- Scope (where this step stops): 포함: 위 클래스와 응답 DTO, 새 예외 BrokerBalanceNotSupported(I 501)·NotBrokerAccount(404)·TossAccountNotLinked(F-4 409), holdings·accounts·buying-power 토스 DTO와 명세 기반 fixture(가짜 값), 필드표(이 문서 §8 부록), 새 API 응답의 금액·수량·가격·비율은 JSON 문자열(FLAG-10, 새 API에만), C-01~C-05 수정. 제외: prices·orderbook·candles(step 7), 실제 토스 호출(사람 준비 뒤 대조만), infrastructure/kis/**·KisAccountQueryService·/api/kis/account/balance, 기존 API 응답 형태, USD 원화 환산·합산, C-06~C-08(findings 원장)

---

## 2. Approach (how, in what order)  (plan)

> **By what method and in what order** this step (= phase) is solved — briefly (one line to a few). Based on the feature-spec / development-plan design.
> A rough level is enough (the detailed work log is §5). **Never leave it empty — the step's base is here.**

- 필드 매핑(공식 명세 1.2.28): 통화별 합계 BrokerCurrencyBalance(KRW·USD 두 행 고정) = cashBalance null(DEC-04), cashBuyingPower = buying-power?currency=<c>의 cashBuyingPower, marketValue = holdings.marketValue.amount.<c>, unrealizedPnl = holdings.profitLoss.amount.<c>, returnRate = profitLoss.amount.<c> / totalPurchaseAmount.<c>를 §7 KIS 방식(scale 6 HALF_UP, ×100, scale 2)으로 계산하고 원금 0이면 null. usd가 null이면 그 통화 합계 칸은 null(0 대체 금지). 토스 합계 rate는 원화 환산 전체 기준이라 쓰지 않음(FLAG-07)
- 종목 BrokerBalancePosition = ticker symbol, currency, quantity, avgPrice averagePurchasePrice, currentPrice lastPrice, marketValue marketValue.amount, unrealizedPnl profitLoss.amount, returnRate profitLoss.rate × 100(movePointRight(2), 반올림 없음, 사람 결정 «퍼센트로 통일»). marketCountry·currency·accountType은 모르는 값을 허용(문자열로 받음), 지원하지 않는 통화 종목은 TOSS_RESPONSE_INVALID 대신 그대로 싣고 통화별 합계에는 넣지 않음. asOf는 조회 시각(KST)
- 실패하는 시험 먼저: TossAccountBalanceAdapterTest(fixture 정상 KRW+USD, 보유 0, usd null, 필수 필드를 하나씩 뺀 응답, 긴 자릿수 정수부 15자리·소수 6자리 compareTo·toPlainString 일치, 목록 원소 null, X-Tossinvest-Account 헤더, 한도 그룹 ASSET·ORDER_INFO), TossAccountListClientTest, BrokerAccountQueryServiceTest(TradingMode.entries 전수 표: TOSS_LIVE 어댑터, KIS_PAPER·KIS_LIVE·UPBIT_LIVE 501 BROKER_BALANCE_NOT_SUPPORTED, LOCAL 404 NOT_BROKER_ACCOUNT, externalAccountId 비었거나 숫자가 아닌 TOSS 409 TOSS_ACCOUNT_NOT_LINKED, 없는 계좌 404, 표 키 집합 = entries)
- 컨트롤러 통합 시험(KisAccountBalanceControllerIntegrationTest 패턴, 서비스 @MockitoBean): 경로, 상태, code, 금액이 JSON 문자열이고 값이 정확한지, accountNo가 끝 4자리만 나오는지(사람 결정), accountSeq는 전체 값
- 리뷰 후속: C-01 리미터 예약 전 대기가 rate-limit-max-wait-millis를 넘으면 슬롯을 잡지 않고 TOSS_RATE_LIMITED. C-02 TOSS_BASE_URL이 절대 https URL이 아니면 기동 판정에서 형식 오류로 토스만 비활성. C-03 TossJson 매퍼에 KotlinFeature.NewStrictNullChecks 켜고 ACCEPT_FLOAT_AS_INT 끔. C-04 요청 상한 하한을 발급 최악 + AUTH 간격 + 락 여유를 담은 한 상수로 두고 범위 검사와 실행 검사가 같이 씀. C-05 429 때 그 그룹의 다음 슬롯을 now + Retry-After 뒤로 미는 리미터 메서드. 각 항목 시험 먼저
- 부동소수점 스캔 대상에 application/account/port/Broker*, application/account/result/Broker*, application/toss/** 추가. 로그 누출 시험에 accountNo·accountSeq 끝 4자리만 확인 추가
- ktlintCheck detekt verifyLintBaselinesNotGrown -PlintBaselineRef=origin/main 통과(레포 전체 ktlintFormat 금지), 관련 시험 클래스 실행 후 xml 집계, 푸시 뒤 CI full-test

---

## 3. Dependencies  (plan)

- Previous step: step 5 done(908ac89 전송 계층, 2a5d8b0 완료 기록)
- Prerequisites: Docker 실행 중(Testcontainers Redis). 토스 키·허용 IP 불필요(명세 기반 fixture). 사람 결정 2026-11-15 두 건(수익률 퍼센트 통일, accountNo 끝 4자리)은 계획서 D7 범위 안의 세부라 설계 재승인 여부는 리뷰 때 확인

---

## 4. Completion criteria  (plan)

- [ ] §6 잔고 어댑터·모드 누락 표(잔고)·컨트롤러 행의 기대 결과가 시험으로 통과
- [ ] C-01~C-05 각각 실패하던 시험이 통과
- [ ] 새 API 응답의 금액·수량·가격·비율이 JSON 문자열, 기존 API 응답 변경 0
- [ ] 토스 잔고 코드와 Broker* 포트·결과에 Double·Float·toDouble·toFloat 0건, 토큰·비밀값 로그 누출 0건, accountNo·accountSeq 로그는 끝 4자리만
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

`*` = 명세 required. 숫자는 전부 decimal 문자열 → `BigDecimal(String)`. required 누락·변환 실패는 `TOSS_RESPONSE_INVALID`(D14).

| API (한도 그룹) | 토스 필드 | 필수 | 우리 칸 |
|---|---|---|---|
| `GET /api/v1/accounts` (ACCOUNT) | `result[].accountNo` | * | 응답 `accountNoLast4`(끝 4자리, 사람 결정 2026-11-15) |
|  | `result[].accountSeq` (int64) | * | 응답 `accountSeq`(전체 값, 내부 계좌 `externalAccountId`에 넣는 값, D3) |
|  | `result[].accountType` (enum, 모르는 값 허용) | * | 응답 `accountType`(문자열 그대로) |
| `GET /api/v1/holdings` (ASSET), 헤더 `X-Tossinvest-Account` | `totalPurchaseAmount.{krw*, usd}` | * | 통화별 returnRate 계산의 분모 |
|  | `marketValue.amount.{krw*, usd}` | * | `BrokerCurrencyBalance.marketValue` |
|  | `profitLoss.amount.{krw*, usd}` | * | `BrokerCurrencyBalance.unrealizedPnl`, returnRate 분자 |
|  | `profitLoss.rate`, `dailyProfitLoss.*`, `*.amountAfterCost` | * | 안 씀(rate는 원화 환산 전체 기준, FLAG-07) |
|  | `items[].symbol` | * | `BrokerBalancePosition.ticker` |
|  | `items[].currency` (KRW\|USD, 모르는 값 허용) | * | `currency` |
|  | `items[].quantity` | * | `quantity` |
|  | `items[].averagePurchasePrice` | * | `avgPrice` |
|  | `items[].lastPrice` | * | `currentPrice` |
|  | `items[].marketValue.amount` | * | `marketValue` |
|  | `items[].profitLoss.amount` | * | `unrealizedPnl` |
|  | `items[].profitLoss.rate` (소수 비율) | * | `returnRate` = ×100, `movePointRight(2)`(반올림 없음) |
|  | `items[].name`, `marketCountry`, `cost`, `dailyProfitLoss` | * | 안 씀(필수 검사만) |
| `GET /api/v1/buying-power?currency=` (ORDER_INFO), 헤더 `X-Tossinvest-Account` | `result.currency` | * | 요청 통화와 같은지 확인 |
|  | `result.cashBuyingPower` | * | `BrokerCurrencyBalance.cashBuyingPower`(DEC-04) |

- 통화별 합계 `returnRate` = `unrealizedPnl / totalPurchaseAmount`(scale 6 HALF_UP) × 100, scale 2 HALF_UP(§7 KIS 방식). 원금이 0이거나 null이면 null
- `usd`가 null이면 USD 행의 `marketValue`·`unrealizedPnl`·`returnRate`는 null, `cashBuyingPower`는 buying-power USD 값
- `cashBalance`는 늘 null(예수금 API 없음, DEC-04)
