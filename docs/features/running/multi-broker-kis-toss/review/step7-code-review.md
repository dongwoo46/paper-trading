범위: `변경분` — 커밋 `356b248`(부모 `a30bb57`)의 backend 37파일. 문서 커밋과 main 에서 합쳐 온 변경(`d3b8056` 의 architecture 시험 5파일)은 범위 밖입니다.

# 코드 리뷰 결과 — multi-broker-kis-toss step 7 «시세 REST와 최종 확인» (1회차)

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 리뷰 대상 | worktree `.worktrees/multi-broker-kis-toss-phase1`, `git diff a30bb57 356b248 -- backend/` 37파일(main 신설 8 · main 수정 10 · 시험 신설 4 · 시험 수정 5 · fixture 9 · README 1) |
| 기준 | `rule-pointer --list` 를 파일마다 돌린 결과의 합집합: devkit `rules/dev-team/common.md`(AE18·AE21·CS1~CS25·C12·DD1~DD6) · `rules/dev-team/tests.md`(TT 조항) · 레포 `rules/backend-api.md`(PTB1~PTB9) · 레포 `rules/failure-handling.md`(PTF1~PTF6) · 레포 `rules/tests.md`(PTT1) · `always:dev-team`. 모든 행의 Diff checks 0행, linterCovered 0 |
| 프로필 | consumer (`profile:consumer` 행) |
| 리뷰어 | code-reviewer (구현자와 독립, 읽기 전용) |
| 날짜 | 2026-11-16 |

## 1. 요약

| 항목 | 값 |
|---|---|
| 리뷰한 렌즈 | 정확성 · 표준 · 유지보수성 · 보안 · 성능 · 회귀 시험 · 의도. 근본성: bugfix diff 가 아니고, 리뷰 후속 4건과 `+` 인코딩 수정은 각각 리미터·설정·매퍼·공용 전송 한 곳에 들어가 호출부 분기로 덮은 꼴이 아닙니다 |
| 의도 | 커밋 `의도:`·`기존:`·`변경:` 블록과 diff 가 맞습니다. `출처:` step-7.md·step 6 리뷰 S6-01·S6-02·S6-04·S6-05 는 열어 확인했고 사람 결정 2건(시각 원문, 줄 대기 상한 키 분리)은 지시문 그대로입니다. 블록에 없는 기존 동작 변경은 없습니다(KIS·collector-api·trading-web·기존 API 응답 변경 0, Redis 쓰기 0) |
| 수정 지킴 확인 | 확인 불가 — 탐침 샌드박스가 Gradle(`~/.gradle`·build 쓰기)과 Docker 소켓을 막아 `guards-fix` 를 돌리지 못했습니다. 통과가 아닙니다. 작성자 증거: Red 192건 실패 확인, 메인 재실행 22개 클래스 541건 실패 0, 린트·기준선 exit 0, 커밋 CI success |
| 뮤테이션 | 확인 불가(뮤테이션) — `mutation-run.js --base a30bb57` 가 스냅숏 단계의 `git add` 에서 EPERM 으로 멈춤 |
| 재사용 점검 | `reuse-inventory --match`(maskAllButLastFour · TossMarketQueryService · TossMarketDataClient · withPlusEncodedInQuery) 전부 «같은 이름·모양 없음». main 에 다른 마스킹 함수 0건(grep) |
| 발견 | 4개 (막음 0 · 후속 후보 4) |
| 머지 판정 | `review-verdict.js` 첫 줄: 머지 판정: 가능 — 막는 지적 0건 · 후속 후보 4건 |
| 한 줄 | 시세 세 API 는 명세 필드표대로 BigDecimal → JSON 문자열, 시각은 원문 그대로, 누락은 TOSS_RESPONSE_INVALID, 토스 4xx 는 400 TOSS_REQUEST_REJECTED 이고 입력 검증 문구에 입력값이 없습니다. `+` 수정은 다른 쿼리 값을 깨지 않습니다. 남은 것은 S6-02 수정이 만든 멈춤 끝 몰림(S7-01), 줄 대기 상한 하한 1 이 S6-04 부류를 다 막지 못함(S7-03), S6-01 의 `"null"` 글자 빈틈(S7-02)입니다 |

기타: `rule-pointer --list` 에 변경 파일 37개를 한 번에 넘기면 `tests.md` 하나만 돌아오고(본 코드 18개만 넘겨도 `profile` 행만), 파일 하나씩 넘기면 위 목록이 나옵니다. 도구 쪽 문제로 보여 파일별 결과의 합집합을 기준으로 썼습니다. 탐침은 커밋 빌드 산출 클래스(`build/classes/kotlin/main`, 356b248 뒤 01:03 빌드, 그 뒤 main 소스 변경 0)를 탐침 폴더로 복사해 JDK 단일 소스 실행으로 불렀습니다(jackson 2.21.2, spring-web 6.2.17).

### step 6 후속 S6-01·S6-02·S6-04·S6-05 확인

| 항목 | 확인 결과 | 근거 |
|---|---|---|
| S6-01 빈·공백 문자열 → null | 고쳐짐(같은 부류 빈틈 1) | `TossJson.kt:40-44` `withCoercionConfigDefaults` 로 EmptyString → Fail, 공백도 같은 처리. 탐침: `usd:""` → InvalidFormatException, `asks:""` → MismatchedInputException. 시험 `TossJsonTest` 금액·정수·불린 × null 허용·불가. 남은 빈틈: `"null"` 글자 → S7-02 |
| S6-02 예약된 호출의 429 창 | 고쳐짐(새 부작용 1) | `TossRateLimiter.kt:26-36` 깬 뒤 멈춤 끝 재확인. 탐침: t=50 의 1초 멈춤 뒤 t=50~1050 사이 송신 0건. 다만 창 안 예약이 모두 멈춤 끝 한 순간으로 몰림 → S7-01 |
| S6-04 줄 대기 상한 키 | 고쳐짐(하한 빈틈 1) | 새 키 `rate-limit-max-queue-wait-millis`(`TossProperties.kt:45, 99`, `application.yaml`), `reserveSlot` 이 새 키만 봄, 429 예산 키 주석 갱신, 계획서 D16·§4 반영. 시험 `TossRateLimiterTest`(429 예산 0 이어도 줄 대기 통과, 새 키 초과 실패). 하한 1 은 0 만 막음 → S7-03 |
| S6-05 끝 4자리 마스킹 | 고쳐짐 | `common/MaskAllButLastFour.kt` 한 함수, `TossApiRequest`·`BrokerAccountRef`·`TossAccount`·`TossAccountSummary` 네 toString 이 그 함수만 씀, 4자리 이하는 `****`, `maskTail` 삭제. 같은 부류 잠복: `CreateAccountCommand`·`UpdateAccountCommand`(`application/account/command/AccountCommands.kt:13, 30`)와 `AccountRequest` 의 기본 data class 문자열이 토스 모드에서 accountSeq 전체를 싣지만, 이 diff 밖 기존 코드이고 찍는 로그가 없어 행을 두지 않음 |

### 요청 확인 항목

| 항목 | 판단 | 근거 |
|---|---|---|
| `+` 인코딩이 다른 값을 깨는지 | 깨지 않음 | `encode()` 뒤 쿼리에 남는 `+` 는 값·이름에서 온 것뿐(공백은 `%20`)이라 전부 `%2B` 로 바꿔도 됨. 탐침 결과: 쉼표 `005930,AAPL,BRK.B,ABC-1` 그대로 · `+09:00` → `%2B09:00` · `-04:00`·`Z`·소수초 그대로 · 확장 연도 `+12026-…` → `%2B12026-…` · `%` → `%25` · 이미 인코딩된 `a%2Bb` → `a%252Bb`(글자 그대로 보존, 기존 동작) · 공백 `%20` · `&`·`=` → `%26`·`%3D` · `?` 그대로·`#` → `%23` · 한글 UTF-8 · 이름의 `+` 도 `%2B` · 쿼리 없으면 원본 URI 그대로 · IPv6·포트 유지. 모든 결과의 `getQuery()` 가 원래 값으로 되돌아옴. 시세 입력은 `%` 가 검증에서 막혀(패턴·`OffsetDateTime.parse`) 이중 인코딩 경로가 없음 |
| 시각·숫자·누락(D14) | 맞음 | DTO 시각은 String, `OffsetDateTime.parse` 로 형식만 검증하고 원문 반환(`TossMarketDataClient.kt:90-98`), 응답 DTO 도 원문. 시각이 숫자로 와도 문자열로 읽힌 뒤 형식 검증에서 TOSS_RESPONSE_INVALID. 가격·수량은 BigDecimal → `toPlainString` → JSON 문자열. 필수 칸 누락·`null` 은 실패(작성자 시험), 숫자 칸의 `"null"` 글자·빈 문자열·목록 칸의 빈 문자열도 실패(탐침). 문자열 칸의 빈 값(`currency:""`)은 그대로 통과하는데 §8 ASSUME(S6-01 적용 범위)이라 지적 안 함. 빈 목록 정상. null 허용 숫자 칸이 시세 DTO 에는 없어 S7-02 는 시세에 닿지 않음 |
| 입력 검증 경계·문구 | 맞음 | symbols 1~200(201 거부), 빈 원소·패턴 밖 거부, count 1~200 숫자만, interval `1m`·`1d`, adjusted 소문자 true·false, before 오프셋 포함 ISO. 빈 값(`count=`)도 400. 문구는 고정 필드명·규칙만(`InvalidMarketQueryException.kt:7-11`). 토스 4xx(401·403·429 제외)는 `TossFailureClassifier.kt:32` 의 `clientErrors` → 400 TOSS_REQUEST_REJECTED, 문구에 토스 상태 숫자만 |
| §8 «줄 대기 상한과 AUTH 간격» | 후속 필요 → S7-03 | AUTH 는 일반형의 한 경우. 상한이 어떤 그룹 간격보다 작으면 그 그룹의 연속 호출이 늘 실패하고, ORDER_INFO(간격 167)는 잔고 API 한 번에 두 번 부르므로 상한 1~166 이면 잔고가 매번 실패. 기본 5000 에선 안 남 |
| §8 «S6-02 깨어남 몰림» | 막아야 할 결함 → S7-01 | 탐침으로 재현. 리뷰 제안(깬 뒤 남은 만큼 더 잠)이 간격 보존을 빠뜨린 것이라 제안 쪽 결함이기도 함. 깬 뒤 슬롯을 다시 예약하면 간격과 상한 검사가 함께 돌아옴 |

## 1-1. 프로필별 검토 절 (consumer)

- 1. 규칙 준수 — 대조한 조항 출처: 파일별 `rule-pointer --list` 합집합만(모델이 따로 고른 규칙 없음).
  - common.md: AE18·AE21·CS14·CS21·CS22 — 새 catch 는 `DateTimeParseException` 두 곳(`TossMarketQueryService.kt:77-83`, `TossMarketDataClient.kt:90-96`)이고 둘 다 범주 예외로 다시 던짐, try 본문은 parse 한 줄. 원인을 잇지 않는 것은 §8 ASSUME(파싱 예외 문구에 원문)이라 지적 안 함. CS16·CS20·CS23 해당 없음. DD1·DD3 — 새 API 계약 세 경로는 D9 승인 경로 그대로(`/api/toss/market/*`), 필드는 부록 A·사람 결정. DD2·CS2·CS3·CS19 — 파라미터 이름이 토스와 같고(부록 A) 포트 타입 이름이 명세 이름을 따름. CS1·CS4·CS5 — S6-05 통합 확인, 재사용 도구 신규. CS7 — `TossMarketDataPort` 는 HTTP 경계라 맞음. CS8 — `InvalidMarketQueryException` 은 기존 400 기반 상속(FLAG-12 결정과 같은 꼴). CS18 해당 없음(조회 전용). CS24 — 봉 단위는 enum `code`, 상태 문자열 비교 없음. CS25 — 새 함수 모두 40줄·중첩 2단 이하. C12·CS13·CS15 — 새 주석은 명사형 표지, TODO/FIXME 0. CS12·CS17 해당 없음.
  - tests.md: TT1 — 새 소스마다 시험. TT7·TT44 → S7-04. TT9 — 컨트롤러 시험이 HTTP 본문(문자열 타입·원문 시각·`null`)을 단언. TT16 — 리미터 시험은 주입 시계. TT20·TT21 — 이름이 상황·동작·기대를 담고 레포 백틱 문장형. TT25·TT26 — 기존 시험 단언 변경 1곳(`TossRateLimiterTest.capped`)에 `devkit:test-change S6-04` 와 근거. TT33·TT40 — maker Red 192건(작성자 보고, 리뷰로 재확인 불가). TT49 → 뮤테이션 확인 불가. 멈춤 끝 몰림(S7-01)을 잡는 시험은 없음.
  - 레포 rules/backend-api.md: PTB1·PTB2·PTB3·PTB5·PTB6 — 도메인 모델·엔티티 변경 없어 해당 없음. PTB4 — 가격·수량 BigDecimal, 계산 없이 `toPlainString` 만. PTB7 — application → port·common, infrastructure → `application.toss.port`·common, presentation → application(`TossMarketQueryService`, port 타입)뿐, `common/MaskAllButLastFour.kt` 는 import 0. PTB8 — 포트와 시그니처 타입이 `application/toss/port`, 유스케이스는 포트만 주입. PTB9 — `@Service` 만, 외부 호출뿐이라 `@Transactional` 없음.
  - 레포 rules/failure-handling.md: PTF1·PTF2·PTF4·PTF5·PTF6 — 원장·잔고·요율·시각 구간·주문 가능 금액 없음. PTF3 — GET 조회만이고 남기는 상태 없음.
  - 레포 rules/tests.md PTT1 — 새 Redis·PostgreSQL 어댑터 없음. 충족.
  - always:dev-team — 재사용 위와 같음, 함수 길이·주석 위와 같음, 시각은 우리 코드가 시간대를 붙이지 않고 토스 원문을 그대로 냄(사람 결정 2026-11-15), 시험 먼저는 TT33 행.
- 2. 사이드 이펙트 — `TossJson` 전역 강제 변환 변경은 step 6 잔고 DTO 와 오류 본문 해석(`readErrorDetail`)에도 걸립니다. 오류 본문 `data:""` 처럼 명세 밖 값이 오면 이제 오류 상세 전체가 null 이 되어 401 의 재시도 code 를 못 읽고 상태 코드로만 분류합니다(명세 밖이라 지적 안 함). `rate-limit-max-wait-millis` 를 줄 대기 조정용으로 쓰던 배포는 이제 그 값이 줄 대기에 안 걸리고 새 키 기본 5000 을 씀(커밋 블록에 적힘). `TossRateLimiter.acquire` 호출처 둘(`TossApiClient.execute`, `TossTokenIssuerImpl.issue` 의 AUTH) 중 멈춤을 거는 쪽은 `TossApiClient` 뿐이라 AUTH 는 멈추지 않음. `+` 수정은 공용 `uriOf` 한 곳이라 잔고·계좌 목록 쿼리(`currency=USD` 등)에도 걸리지만 `+` 가 없는 값은 원본 URI 를 그대로 돌려줌. 새 컨트롤러 경로는 기존 `/api/toss`(`TossAccountController`)와 겹치지 않음.
- 3. 버그·장애 위험 — 동시성·부하: S7-01(멈춤 끝 몰림과 늘어난 잠의 상한 미검사), S7-03(설정 하한). 수치 무결성: 시세는 부동소수점 0, 결측은 실패. 빈틈 S7-02 는 잔고 USD 칸. 개인정보: 시세는 계좌 헤더 없음, 요청 로그는 마스킹된 `TossApiRequest` 만이고 쿼리 값은 안 실림. 외부 호출 실패는 기존 D15 분류 그대로.
- 조항을 어기지 않는 코드 품질 지적은 `should-fix` 이하로 두었습니다.

## 2. 발견 (findings)

| ID | file:line | 근거 조항 | 무엇이 틀렸나 | 등급 | 출처 | 회귀 | confidence | 구조 고침 | 재현 |
|---|---|---|---|---|---|---|---|---|---|
| S7-01 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossRateLimiter.kt:31-35 | label:required-state-not-designed | S6-02 수정 뒤 429 멈춤 창 안에 슬롯을 예약해 둔 호출이 모두 «멈춤 끝 시각» 한 순간으로 모여 함께 나갑니다. 깬 뒤 `pauseEnd - now` 만큼만 더 자고 바로 보내므로 예약 때 받은 간격이 사라지고, 멈춤 끝에 새로 잡힌 첫 슬롯과도 겹칩니다. 클래스 KDoc(`:9` «스레드가 여럿이어도 슬롯이 겹치지 않고»)과 어긋납니다. 탐침(실제 시계): MARKET_DATA 초당 10(간격 100ms), t≈0~10 에 6개 예약, t=50 에 `deferNextSlot(1000)`, 새 호출 3개 → 예약 5개와 새 호출 1개가 t=1052~1058 의 6ms 안에 송신, 그 뒤 새 호출만 100ms 간격. 몰리는 수는 «초당 한도 × Retry-After 초 + 1» 쯤이라 429 예산 안의 Retry-After 5초면 MARKET_DATA(15) 기준 약 75개가 한 번에 나가 다시 429 를 부르고, 그 429 가 다시 멈춤을 늘려 다음 줄도 같은 시각으로 모입니다. 늘어난 잠은 줄 대기 상한 검사를 다시 거치지 않아, 예약 때 상한 바로 밑이던 호출은 상한 + Retry-After 만큼 요청 스레드를 잡습니다(RISK-08 의 상한 밖) | should-fix | 이 변경 | 아니오 | 높음 | 깬 뒤 멈춤이 늘었으면 그 자리에서 `reserveSlot` 을 다시 불러 멈춤 끝 뒤 간격대로 새 슬롯을 받음(줄 대기 상한 검사도 다시 거침). 시험은 주입 시계로 «예약 셋 + 잠든 사이 멈춤 → 송신 시각이 멈춤 끝부터 간격만큼 벌어짐» | — |
| S7-03 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossProperties.kt:44, 99 | label:missing-input-validation | 새 키 하한 1 은 0 만 막고 S6-04 의 실패 부류는 남깁니다. 줄 대기 상한이 한 그룹의 간격(올림 1000 ÷ 초당 한도)보다 작으면 그 그룹의 연속 두 번째 호출은 다른 요청이 없어도 늘 `TOSS_RATE_LIMITED` 입니다. 탐침(주입 시계): 상한 1·100·166 → ORDER_INFO(초당 6, 간격 167) 연속 2회 실패, 그래서 잔고 API 의 buying-power KRW·USD 둘째 호출이 매번 실패. 167~999 → ACCOUNT(초당 1) 연속 2회 실패. 1000 부터 둘 다 통과. 이 값들은 범위 검사를 통과해 토스가 «활성»으로 뜹니다. 키 주석(`:44` «0 이하면 … 실패함»)은 1 이상이면 안전한 것처럼 읽힙니다. §8 «줄 대기 상한과 AUTH 간격»(AUTH 기본 초당 5 → 간격 200, 발급이 락 안에서 연달아 일어날 때만)은 이 일반형의 한 경우입니다. 기본 5000 에서는 나지 않아 설정 실수 때만 남습니다 | should-fix | 이 변경 | 아니오 | 높음 | 범위 검사가 줄 대기 상한을 «설정된 그룹 간격 중 최댓값» 이상으로 요구하고 어기면 범위 오류로 비활성. 하한 1 은 사람 결정(계획서 §4)이라 바꾸려면 사람 승인, 승인 전에는 키 주석·yaml 주석에 실제 조건을 적음 | — |
| S7-02 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossJson.kt:38-44 | label:falsy-or-empty-value-mishandled | S6-01 과 같은 부류가 문자열 `"null"` 로 남습니다. Jackson BigDecimal 역직렬화는 강제 변환 설정과 따로, 앞뒤 공백을 자른 `null` 글자를 null 로 읽습니다. 그래서 null 허용 금액 `TossCurrencyAmounts.usd`(`TossHoldingsOverview.kt:18`)에 `"usd":"null"`·`"usd":" null "` 가 오면 실패 없이 null 이 되고 USD 행 평가액·손익·수익률이 «해외 보유 없음»과 같은 null 로 나갑니다. 새 주석(`:38` «칸이 비었다는 뜻은 명시적 JSON null 만»)과 어긋납니다. 탐침(커밋 빌드 클래스): `usd:"null"` → `usd=null` 성공, `usd:""` → InvalidFormatException, `krw:"null"`·시세 `lastPrice:"null"` → KotlinInvalidNullException(null 불가 칸은 D14 대로 실패). 시세 DTO 에는 null 허용 숫자 칸이 없어 이번 API 에는 닿지 않습니다. 토스가 `"null"` 글자를 보내는지는 명세를 열 수 없어 확인하지 못했습니다 | should-fix | 이 변경 | 아니오 | 중간 | `TossJson` 모듈에 BigDecimal 역직렬화기 하나를 두어 문자열 토큰의 `null` 글자를 실패로 돌리고 나머지는 기본 역직렬화기에 맡김, `usd:"null"` 시험 한 줄 | — |
| S7-04 | backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/TossMarketControllerIntegrationTest.kt:41 | TT7 | 프로세스 안 협력자를 목으로 바꿉니다. 컨트롤러 시험이 우리 유스케이스 `TossMarketQueryService` 를 `@MockitoBean` 으로 바꿔 «입력 검증 실패 → 400 INVALID_MARKET_QUERY» 는 목이 던진 예외가 처리기를 지나는지만 보고, 실제 서비스 검증과 HTTP 를 잇는 시험이 없습니다. `TossMarketDataClientTest.kt:42, 327` 도 우리 리미터를 relaxed 목으로 두고 그룹별 호출 횟수를 단언합니다(TT44). S6-07 과 같은 부류라 원장 S6-07 과 함께 처리하길 권합니다 | should-fix | 이 변경 | 아니오 | 낮음 | 컨트롤러 시험은 실제 서비스 + 가짜 `TossMarketDataPort`, 한도 그룹은 실제 `TossRateLimiter` + 주입 시계로 그룹 간격을 관찰 | — |

## 3. 범위 밖 (안 본 것)

- 문서 커밋(step-7.md 부록 A·계획서 D16·§4)의 명세 정합성 — spec-review 몫입니다. 필드표와 DTO·어댑터·응답 매핑이 서로 맞는지만 대조했고 맞습니다.
- 공식 명세 openapi.json 원문과 실제 토스 호출 — 샌드박스가 네트워크를 막아 열지 못했습니다. 토스가 쿼리 `+` 를 정말 공백으로 읽는지, `"null"` 글자를 보내는지는 §5 8번 실호출 대조 때 확인할 항목입니다.
- 시험 실행: 전체 스위트는 지시대로 돌리지 않았고, 개별 시험도 샌드박스가 Gradle·Docker 를 막아 돌리지 못했습니다. 근거는 작성자 집계(541건)입니다.
- main 에서 합쳐 온 `d3b8056` 의 architecture 시험 변경(`ApplicationLayerRules` 등 5파일).
- 사람 결정(시각 원문, 새 키와 하한 1, 새 API 숫자는 JSON 문자열)은 결함으로 보지 않았고, S7-03 은 하한의 효과만 적었습니다.

<!-- devkit:standard-list hex:7b22636861696e223a22646f63732f66656174757265732f72756e6e696e672f6d756c74692d62726f6b65722d6b69732d746f73732f7265766965772f73746570372d636f64652d7265766965772e6d64222c22726f756e6473223a7b2231223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2264336238303536623739303865616338353135373031383635316533653333333564303461613530227d7d7d -->
