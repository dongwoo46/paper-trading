# Step 5 — 스키마 확정과 토스 전송 계층

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
| Step | 5 |
| Title | 스키마 확정과 토스 전송 계층 |
| Feature | multi-broker-kis-toss |
| Based on | development-plan §5 1단계 4번(스키마 확정, D18로 공식 명세가 원본)·5번(전송 계층), §2 D10(R1-01 TTL 식)·D11(R1-02 전체 상한)·D12(401 처리)·D13(RestTemplate, BigDecimal)·D14(결측)·D15(에러 범주)·D16(호출 한도)·D18(스키마 확정 방식), §4 설정 키, §6 401 처리·에러 구분·비밀값 로그 누출 행, open-questions FLAG-13·14·16·17, 조사 문서 «개별 API 스키마», 공식 명세 openapi.json 1.2.28 |
| Status | done |
| Owner |  |

---

**━━ Plan block (required before starting · from the planner / development-plan design) ━━**

## 1. Goal and scope  (plan)

> What this step (phase) **builds (what)** + **how far it goes (this step's boundary — name what is handed to the next step)**.

- Builds: 토스 키 없이 공식 명세만으로 토큰 실제 발급기와 공통 전송 계층을 시험으로 못박는다: TossTokenIssuer 실제 구현(form 요청, OAuth2 오류 형식 해석), TossTokenManager 빈 등록(FLAG-14), TossApiClient(Bearer·계좌 헤더, 401 1회 재시도, 429 대기, D15 에러 매핑), TossRateLimiter(명세 그룹별 최소 간격), R1-01 TTL 식과 R1-02 요청 전체 상한
- Scope (where this step stops): 포함: 위 클래스, 토큰·에러 봉투 DTO와 명세 기반 fixture(src/test/resources/fixtures/toss/, 가짜 값), 토큰·에러 필드표(이 step 문서 §8 또는 부록), 새 설정 token-request-deadline-millis와 rate-limit 키, D15 토스 예외 클래스 중 이 step이 처음 쓰는 것. 제외: holdings·accounts·buying-power·prices DTO와 어댑터(step 6~7), 내부 API 경로(step 6~7), 실제 토스 호출(사람이 키·허용 IP를 준비한 뒤 대조만, 이 step 완료 조건 아님), infrastructure/kis/** 변경

---

## 2. Approach (how, in what order)  (plan)

> **By what method and in what order** this step (= phase) is solved — briefly (one line to a few). Based on the feature-spec / development-plan design.
> A rough level is enough (the detailed work log is §5). **Never leave it empty — the step's base is here.**

- 실패하는 시험 먼저: TossTokenIssuerImplTest(MockRestServiceServer, form 본문 grant_type·client_id·client_secret, 200 최상위 access_token·token_type·expires_in, 400 invalid_request·401 invalid_client → TOSS_AUTH_FAILED, 403 access_denied → TOSS_IP_NOT_ALLOWED, 429·5xx·타임아웃·깨진 본문 → D15 code)
- TossTokenManagerTest 보강: R1-01 expires_in 61·119·120초에서 TTL이 식 max(expires_in − 여유, max(expires_in / 2, 1))와 같고 수명에 대해 단조, R1-02 대기 4.9초 뒤 락 획득 시 남은 시간이 발급 최악(연결 5초+읽기 10초)보다 짧으면 발급 0회·503·락 해제
- TossApiClientTest(MockRestServiceServer): Authorization Bearer와 X-Tossinvest-Account 헤더, 401 invalid-token·expired-token·token-revoked는 invalidate 후 1회 재시도, 연속 401이나 login-user-not-found는 TOSS_AUTH_FAILED(발급 최대 1회), 403 ip-not-allowed는 재시도 0회 TOSS_IP_NOT_ALLOWED, 403 edge-blocked·forbidden은 TOSS_AUTH_FAILED, 429 Retry-After 있음·없음(1초)·상한 초과, 500 maintenance → TOSS_MAINTENANCE, 그 밖 5xx·네트워크 → TOSS_UNAVAILABLE, 타임아웃 → TOSS_TIMEOUT, 그 밖 4xx → TOSS_REQUEST_REJECTED, 봉투 깨짐·필수 필드 누락 → TOSS_RESPONSE_INVALID. 우리 응답 message에 토스 원문 없음
- TossRateLimiterTest: 그룹별(AUTH, ACCOUNT, ASSET, ORDER_INFO, MARKET_DATA, MARKET_DATA_CHART) 최소 간격을 주입 시계·sleeper로 검증, 한도는 설정값(코드 상수 금지)
- 구현: 기존 RestTemplate 빈 재사용, 본문은 문자열로 받아 data class로 역직렬화(Map·JsonNode 수치 변환·Double 금지), code는 모르는 값 허용. TossTokenManager에 @Component(실제 발급기 주입), 토스 비활성이어도 컨텍스트 기동 유지
- 설정: application.yaml에 token-request-deadline-millis(기본 20000)와 rate-limit 키 추가, 값은 환경변수
- 비밀값 로그 누출 시험 보강: 발급기·401 재시도·에러 경로에서 토큰·client secret·암호화 키 0건, accountSeq는 끝 4자리만
- ktlintCheck detekt verifyLintBaselinesNotGrown -PlintBaselineRef=origin/main 통과(레포 전체 ktlintFormat 금지), 관련 시험 클래스 실행 후 xml 집계, 푸시 뒤 CI full-test

---

## 3. Dependencies  (plan)

- Previous step: step 4 done(토큰 기반, 리뷰 후속 9건 4988edb)
- Prerequisites: 설계 재승인(D10 R1-01 식, D11 R1-02 상한, §4 새 설정 키). Docker 실행 중(Testcontainers Redis). 토스 키·허용 IP는 불필요(명세 기반 fixture), 실호출 대조는 사람 준비 뒤 별도

---

## 4. Completion criteria  (plan)

- [ ] §6 401 처리·에러 구분 행의 기대 결과가 시험으로 통과, D15 code 전부가 하나 이상의 시험에 등장
- [ ] R1-01 TTL 단조 시험과 R1-02 전체 상한 시험 통과
- [ ] TossTokenManager가 빈으로 등록되고 토스 환경변수 없이 컨텍스트 기동 성공(EDGE-01)
- [ ] 토스 전송 코드에 Double·Float·toDouble·toFloat 0건, 토큰·비밀값 로그 누출 0건
- [ ] 관련 시험 실패 0, 린트·기준선 증가 검사 exit 0, CI full-test success
- [ ] infrastructure/kis/**·collector-api·trading-web 변경 0

---

**━━ Record block (filled in order while developing · never ahead) ━━**

## 5. Work log  (record)

> Fill it by spelling out the §2 approach as the actual work (concretely, what was done).

- 시험 먼저: `TossTokenIssuerImplTest`, `TossApiClientTest`, `TossApiClientTokenFlowTest`, `TossRateLimiterTest`, `TossFloatingPointScanTest`를 새 클래스 없이 작성하고 `TossTokenManagerTest`·`TossPropertiesTest`·`GlobalExceptionHandlerTest`·`TradingApiApplicationTests`에 추가 → `compileTestKotlin --offline` 이 미해결 참조로 실패(Red)
- D15 토스 예외 8개를 `common/exception/`에 파일 하나에 선언 하나로 추가: J 범주 `TossAuthFailed`·`TossIpNotAllowed`·`TossRateLimited`·`TossMaintenance`·`TossUnavailable`·`TossTimeout`·`TossResponseInvalid`, A 범주 `TossRequestRejected`(FLAG-12대로 기존 400 기반 상속). `GlobalExceptionHandler`는 code별 고정 문구만 응답, 토스 원문은 로그만
- `TossTokenIssuerImpl`: form 본문을 바이트로 직접 보내 RestTemplate DEBUG 로그에 client secret 이 찍히지 않게 함, 응답은 `TossTokenResponse`(token_type Bearer, expires_in > 0만 수용), 실패는 OAuth2 형식 `TossOAuth2Error`로 FLAG-18 매핑
- `TossTokenManager`: `@Component` 등록(FLAG-14), R1-01 TTL 식 `max(expires_in − 여유, max(expires_in / 2, 1))`, R1-02 요청 시작 기준 전체 상한 `token-request-deadline-millis`(락 획득 때 남은 시간이 발급 최악 15초보다 짧으면 발급 0회·락 해제·503)
- `TossApiClient`(+`TossApiRequest`, `TossFailureClassifier`, `TossTransportSupport`, `TossJson`, `TossErrorEnvelope`, `TossSuccessEnvelope`): Bearer·계좌 헤더, 401 세 code 는 invalidate 뒤 1회 재시도, 403·429·5xx·타임아웃·깨진 봉투를 D15 code 로. Jackson `FAIL_ON_NULL_FOR_PRIMITIVES` 켬
- `TossRateLimiter`(+`TossRateLimitGroup`): 명세 그룹 6개별 최소 간격, 한도는 `application.yaml` `trading.toss.rate-limits.*` 하나가 출처
- `TossProperties`: 새 키(`token-request-deadline-millis`, `rate-limit-max-retries`, `rate-limit-max-wait-millis`, `rate-limits`)와 범위 검사(위반 시 토스만 비활성). `RestTemplateConfig`의 연결 5초·읽기 10초를 상수로 올려 발급 최악 소요의 근거로 씀(값 불변)
- fixture 19개와 README를 `src/test/resources/fixtures/toss/`에 가짜 값으로 추가
- 린트: ktlint·detekt 기준선에서 이번에 고쳐진 항목만 삭제(`RestTemplateConfig`, `TradingApiApplicationTests`), 증가 0
- 변이 실험 3건(form 객체 전송 복귀, R1-02 검사 완화, R1-01 옛 식 복귀) 각각 시험 실패를 확인한 뒤 원복

---

## 6. Verification  (record)

| What | How it was checked | Result |
|---|---|---|
| Red | 구현 전 `./gradlew compileTestKotlin --offline` | 종료 코드 1, 미해결 참조 오류 327줄 |
| 토스 관련 시험 | `./gradlew test --offline --tests` 13개 클래스, `build/test-results/test/*.xml` 직접 집계(Docker 실행 중) | 253건, 실패 0, 오류 0, 건너뜀 0 (ApiClient 62, Properties 39, Issuer 36, Manager 30, Handler 22, RateLimiter 14, Cipher 14, Arch 13, Lock 10, Leak 6, TokenFlow 3, ContextLoads 2, FloatScan 2) |
| 연관 회귀 | `presentation.*`, `infrastructure.kis.*` 16개 클래스 | 125건, 실패 0, 오류 0 |
| 빈 등록·기동(EDGE-01) | `TradingApiApplicationTests` 토스 환경변수 없이 기동, TossTokenManager 빈 존재 | 통과 |
| R1-01 | TTL 표·단조·warn 시험, 옛 식 변이 | 통과, 변이 시 3건 실패 |
| R1-02 | 요청 상한 시험(남은 5.1초), 검사 완화 변이 | 통과, 변이 시 실패 |
| 로그 누출 | 발급기·401 재시도·에러 경로 로그 수집, form 객체 전송 변이 | 토큰·secret·키 0건, 변이 시 secret 7줄 노출로 실패 |
| 부동소수점 | `TossFloatingPointScanTest` | Double·Float·toDouble·toFloat·Map·JsonNode 0건 |
| 린트 | `./gradlew compileKotlin ktlintCheck detekt verifyLintBaselinesNotGrown -PlintBaselineRef=origin/main --offline` | exit 0, 기준선 증가 0 |
| 범위 | `git status` | `infrastructure/kis/**`·collector-api·trading-web 변경 0 |
| 전체 스위트 | 커밋 `908ac89` 푸시 뒤 CI `full-test` 실행 43700036485 | success (trading-api, collector-api, trading-web) |
| 코드 리뷰 | `review/step5-code-review.md` 1회차, `review-verdict.js` | 머지 가능, 막는 지적 0 · 후속 8 |

---

## 7. Result  (record)

- Done: §4 완료 기준을 시험·린트·CI 전체 스위트(43700036485)·코드 리뷰(막는 지적 0)로 충족, 커밋 `908ac89`. 사람이 2026-11-15 «done 처리, 후속은 step 6에»를 골라 done. 리뷰 후속 C-01~C-05는 step 6 범위로, C-06~C-08은 findings 원장
- Changed files / PR: 아래 경로는 `backend/trading-api/` 기준
  - main 신설: `common/exception/Toss{AuthFailed,IpNotAllowed,RateLimited,Maintenance,Unavailable,Timeout,ResponseInvalid,RequestRejected}Exception.kt`, `infrastructure/toss/{TossTokenIssuerImpl,TossApiClient,TossApiRequest,TossFailureClassifier,TossRateLimiter,TossRateLimitGroup,TossJson,TossTransportSupport}.kt`, `infrastructure/toss/dto/{TossTokenResponse,TossOAuth2Error,TossErrorEnvelope,TossSuccessEnvelope}.kt`
  - main 수정: `TossTokenManager`, `TossProperties`, `TossTokenIssuer`(주석), `GlobalExceptionHandler`, `RestTemplateConfig`(상수 승격), `application.yaml`
  - test 신설: `TossRateLimiterTest`, `TossTokenIssuerImplTest`, `TossApiClientTest`, `TossApiClientTokenFlowTest`, `TossFloatingPointScanTest`, `TossFixtures`, `src/test/resources/fixtures/toss/`(fixture 19개, README)
  - test 수정(추가만, 기존 단언 변경 없음): `TossTokenManagerTest`, `TossPropertiesTest`, `GlobalExceptionHandlerTest`, `TradingApiApplicationTests`(생성자 주입으로 바뀜)
  - 설정: `config/ktlint/baseline.xml`, `config/detekt/baseline.xml`(고쳐진 항목만 삭제)

---

## 8. Problems found  (record)

> Found during the work — it can cause a new step, a change to an existing one, or a flag. Edit this step.md directly then (the reason goes in state.md §3).

| Problem | How it was handled |
|---|---|
| 429 재시도 기본값이 계획서에 없음 | ASSUME: 최대 2회, 대기 합계 5000ms(재시도 전체 누적, 넘으면 잠들지 않고 `TOSS_RATE_LIMITED`). `Retry-After`는 정수 초만, 없거나 날짜·음수면 1초. 환경변수로 덮음. 리뷰에서 확인 |
| 토큰 발급 중 429 처리 | ASSUME: 대기·재시도 없이 `TOSS_RATE_LIMITED`(발급 최악 15초가 락 TTL·요청 상한의 근거라 대기를 넣지 않음) |
| 401 code 미상이나 본문 해석 불가 | ASSUME: 재시도 없이 `TOSS_AUTH_FAILED` |
| 404·409·422 등 그 밖 4xx | ASSUME: 전부 `TOSS_REQUEST_REJECTED`. step 6 에서 `account-not-found` 개별 매핑 필요할 수 있음 |
| 한도 수치의 출처 | `application.yaml` 하나, `TossProperties` 기본값은 빈 맵. 없는 그룹 사용은 `TOSS_INTEGRATION_DISABLED`, 0 이하 값은 기동 때 토스만 비활성 |
| `token-request-deadline-millis` 가 발급 최악 15초 이하일 때 | 범위 오류로 토스만 비활성 |
| 원시형 필드에 null·빈 문자열 | `FAIL_ON_NULL_FOR_PRIMITIVES` 켬. step 6~7 DTO 도 같은 전제 |
| `error-ip-not-allowed.json`, `error-edge-blocked.json` 은 명세에 예시가 없음 | 개요 문서 에러 코드 표로 만들고 message 는 임의(fixture README 명시). 실호출로 대조 필요 |
| maker 가 시험 파일(`TossApiClientTest`, 이번에 만든 파일) 한 곳을 Bash python 치환으로 고침 | 규칙 위반 자진 보고. 이후 Edit/Write 로만 다시 씀, 시험 통과 |
| ktlint·detekt 기준선 손수정 | 고쳐진 항목만 삭제, 증가 0. 리뷰 확인 요청 |
| ktlint 식 본문 한 줄 규칙과 120자 제한 충돌 | 이름을 줄이거나 문장 둘로 나눔 |
| 토큰·에러 필드표 부록 미작성 | 필드 원본은 공식 명세와 조사 문서 «개별 API 스키마» 절, fixture README. 부록은 리뷰 전 보강 |
