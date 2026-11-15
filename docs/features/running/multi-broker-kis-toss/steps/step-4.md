# Step 4 — 토스 토큰 기반

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
| Step | 4 |
| Title | 토스 토큰 기반 |
| Feature | multi-broker-kis-toss |
| Based on | development-plan §5 1단계 3번(토큰 기반, 스키마 없이 가능), §2 D10(토큰 저장)·D11(발급 직렬화)·D12(무효화 부분)·D15(J·K 범주 예외)·D17(설정과 비활성), §3 해당 행, §4 설정 키, §6 암호기·토큰 동시 발급과 저장·락·비활성·비밀값 로그 누출 행, feature-spec RULE-05·RULE-06 |
| Status | done |
| Owner |  |

---

**━━ Plan block (required before starting · from the planner / development-plan design) ━━**

## 1. Goal and scope  (plan)

> What this step (phase) **builds (what)** + **how far it goes (this step's boundary — name what is handed to the next step)**.

- Builds: 토스 키·스키마 없이 만들 수 있는 토큰 기반을 시험으로 못박는다: 설정과 비활성 판정(TossProperties), AES-256-GCM 암호기(AesGcmTokenCipher), Redis 분산 락(RedisDistributedLock), 토큰 캐시·락·무효화(TossTokenManager). 발급기는 가짜로 시험한다
- Scope (where this step stops): 포함: 위 4개 클래스, 발급기 추상화(실제 HTTP 구현은 step 5), 범주 J 기반 ServiceUnavailableException(503)과 핸들러 매핑, TOSS_TOKEN_UNAVAILABLE(J)·TOSS_INTEGRATION_DISABLED(K-1) 예외, application.yaml trading.toss.* 키(값은 환경변수, 기본값은 선택 키만). 제외: TossTokenIssuer 실제 요청 형식, TossApiClient·401 재시도(step 5), TossRateLimiter(step 5), 잔고·시세(step 6~7), KIS 토큰 이전(2단계), infrastructure/kis/** 변경

---

## 2. Approach (how, in what order)  (plan)

> **By what method and in what order** this step (= phase) is solved — briefly (one line to a few). Based on the feature-spec / development-plan design.
> A rough level is enough (the detailed work log is §5). **Never leave it empty — the step's base is here.**

- 실패하는 시험 먼저: AesGcmTokenCipherTest(왕복 일치, 같은 평문 두 번 암호화 시 암호문 다름, 변조·다른 키·다른 AAD 실패, 키 길이 오류 거부, 형식 v1.<IV>.<암호문+태그> base64url)
- AesGcmTokenCipher를 infrastructure/security/에 JDK javax.crypto만으로 구현(IV 12바이트 매번 무작위, AAD는 Redis 키 이름)
- TossProperties(@Component @ConfigurationProperties trading.toss): 필수 4개(base-url, client-id, client-secret, token-encryption-key) 하나라도 비었거나 암호화 키 형식이 틀리면 토스만 비활성, 기동 때 누락·오류 키 이름만 1회 로그. 비활성 시험은 컨텍스트 기동 성공과 호출 시 TOSS_INTEGRATION_DISABLED, 메시지에 키 이름만
- RedisDistributedLock: SET NX PX 소유자 UUID, Lua로 값이 내 UUID일 때만 DEL. Testcontainers Redis 시험으로 남의 락 해제 불가, TTL 만료 확인
- 범주 J ServiceUnavailableException을 common/exception/에 Spring 무의존으로 추가하고 GlobalExceptionHandler에서 503 매핑(D15 수정본, step 3의 I·K와 같은 모양), TossTokenUnavailableException·TossIntegrationDisabledException 파일 하나에 선언 하나
- TossTokenManager: 락 획득 → Redis 재확인 → 발급 → 암호화 저장(TTL = expires_in − 갱신 여유) → 해제. 락을 못 얻으면 대기 한도까지 toss:token만 다시 읽고 끝내 없으면 503 TOSS_TOKEN_UNAVAILABLE. 복호화 실패는 캐시 없음으로 보고 재발급. invalidate(token)은 Lua로 Redis 값이 그 토큰일 때만 삭제
- 통합 시험(Testcontainers Redis): 매니저 인스턴스 2개 × 스레드 8개 동시 getToken → 발급 정확히 1회·모두 같은 토큰, Redis 원값 v1. 시작·평문 미포함, TTL 확인, 대기 초과 시 발급 0회. 시계·대기는 주입해 실제로 오래 잠들지 않게
- 비밀값 로그 누출 시험: logback ListAppender로 발급·무효화·오류 경로 로그 수집 → 가짜 토큰, client secret, 암호화 키 문자열 0건
- ktlintCheck detekt verifyLintBaselinesNotGrown -PlintBaselineRef=origin/main 통과(레포 전체 ktlintFormat 금지), 관련 시험 클래스 실행 후 xml 집계

---

## 3. Dependencies  (plan)

- Previous step: step 3 done(I·K 범주 예외와 핸들러 매핑 모양)
- Prerequisites: Docker 실행 중(Testcontainers Redis). 토스 키·허용 IP는 불필요(가짜 발급기)

---

## 4. Completion criteria  (plan)

- [ ] §6 암호기·토큰 동시 발급과 저장·락·비활성·비밀값 로그 누출 행의 기대 결과가 시험으로 통과
- [ ] Redis에 평문 토큰이 저장되는 경로 0(RULE-05), 락 없이 발급하는 경로 0(RULE-06)
- [ ] ArchitectureRulesTest 통과(common은 Spring 무의존), 관련 시험 실패 0, 린트·기준선 증가 검사 exit 0
- [ ] infrastructure/kis/**·collector-api·trading-web 변경 0

---

**━━ Record block (filled in order while developing · never ahead) ━━**

## 5. Work log  (record)

> Fill it by spelling out the §2 approach as the actual work (concretely, what was done).

- 시험 먼저: AesGcmTokenCipherTest, RedisDistributedLockTest, TossPropertiesTest, TossTokenManagerTest, TossTokenSecretLeakTest, GlobalExceptionHandlerTest(J 추가)를 새 클래스 없이 작성 → `compileTestKotlin` 이 새 클래스 미해결 참조로 실패(Red)
- 범주 J `ServiceUnavailableException`(common, Spring 무의존)을 추가하고 `GlobalExceptionHandler.handleServiceUnavailable`(503, 메시지는 고정 문구만, 원인은 로그)로 매핑. `TossTokenUnavailableException`(J, `TOSS_TOKEN_UNAVAILABLE`)·`TossIntegrationDisabledException`(K-1, `TOSS_INTEGRATION_DISABLED`, 메시지에 환경변수 이름만)을 파일 하나에 선언 하나로 추가
- `AesGcmTokenCipher`(infrastructure/security, JDK 만): `v1.<IV>.<암호문+태그>` base64url, IV 12바이트 매번 무작위, AAD 인자. 복호화 실패는 `TokenCipherException`. base64 키 생성·검증(`fromBase64Key`, `isValidBase64Key`)은 틀린 입력의 값을 예외에 싣지 않음
- `TossProperties`(`trading.toss.*`): 필수 4개 누락·암호화 키 형식 오류를 `disabledKeyNames()` 로 판정, 기동 때 `@PostConstruct` 로 이름만 1회 로그, 호출 때 `requireEnabled()` 가 `TOSS_INTEGRATION_DISABLED`. 선택 키 3개(갱신 여유 60초, 락 TTL 20초, 락 대기 15초)만 기본값
- `RedisDistributedLock`: `SET NX PX` + 소유자 UUID, Lua 로 소유자일 때만 DEL
- `TossTokenManager`: 캐시 확인 → 락 → 재확인 → 발급 → 암호화 저장(TTL = expires_in − 여유) → 해제. 락 실패 시 `toss:token` 만 100ms 간격으로 대기 한도까지 재조회, 끝내 없으면 `TOSS_TOKEN_UNAVAILABLE`. 복호화 실패는 캐시 없음으로 재발급. `invalidate` 는 읽은 암호문을 Lua 로 «그 값일 때만 DEL»(암호문은 IV 가 매번 달라 평문 비교 불가). 발급기는 `TossTokenIssuer`(fun interface)+`TossIssuedToken`(toString 에 토큰 없음) 추상화, 시계·sleeper 주입
- `application.yaml` `trading.toss.*` 7개 키 추가(환경변수, 필수 4개는 빈 기본값)
- Testcontainers Redis: 레포에 `TradingApiApplicationTests` 의 `GenericContainer("redis:7-alpine")` 패턴만 있어 같은 이미지로 `support/SharedRedisContainer`(JVM 안 공유) 추가, 시험은 Lettuce 연결을 직접 만듦
- 린트: `ktlintCheck`·`detekt` 지적을 손으로 정리(새 파일 전부 0건). `GlobalExceptionHandler` 는 함수 11개로 `TooManyFunctions` 에 걸려 `@Suppress` 한 줄. 이 파일에 import·핸들러가 들어가 기존 ktlint 기준선 15건의 줄 번호가 밀려, 항목 수를 그대로 둔 채 줄 번호만 손으로 옮김
- 변이 실험(락 안 재확인 제거): `TossTokenManagerTest` 의 재확인 시험 1건이 실패하는 것을 확인한 뒤 되돌림

---

## 6. Verification  (record)

| What | How it was checked | Result |
|---|---|---|
| Red | 시험만 있는 상태에서 `./gradlew compileTestKotlin --offline` | 새 클래스(AesGcmTokenCipher, RedisDistributedLock, TossProperties, TossTokenIssuer 등) 미해결 참조로 컴파일 실패 |
| 관련 시험 | `./gradlew test --offline --tests` AesGcmTokenCipherTest·RedisDistributedLockTest·TossPropertiesTest·TossTokenManagerTest·TossTokenSecretLeakTest·GlobalExceptionHandlerTest·ArchitectureRulesTest·TradingApiApplicationTests, `build/test-results/test/*.xml` 집계 | 71건, 실패 0, 오류 0, 건너뜀 0 (Cipher 9, Lock 6, Properties 11, Manager 16, Leak 4, Handler 11, Arch 13, ContextLoads 1) |
| 컨텍스트 기동(EDGE-01) | `TradingApiApplicationTests.contextLoads` 가 토스 환경변수 없이 실제 `application.yaml` 로 기동 | 통과(토스만 비활성), `TossPropertiesTest` 가 필수 키 4개를 하나씩 비운 경우와 형식 오류를 기동 성공+키 이름만 확인 |
| 동시 발급 | Testcontainers Redis, 인스턴스 2개 × 스레드 16개 | 발급 1회, 토큰 1종, 락 해제 |
| 저장 형식(RULE-05) | Redis 원값 | `v1.` 시작, 평문 토큰 미포함, AAD 로 복호화 일치, TTL = expires_in − 여유 |
| 락 없는 발급 0(RULE-06) | 발급기 호출 시점의 락 키 존재, 락 보유 중 대기·한도 초과 | 발급 중 락 키 있음, 대기 초과 시 발급 0회·남의 락 유지 |
| 로그 누출(RULE-05) | logback ListAppender, 발급·캐시 적중·무효화·복호화 실패·대기 초과·수명 오류·발급기 실패 경로 | 가짜 토큰·client secret·암호화 키·암호문 0건 |
| 변이 실험 | 락 안 재확인 제거 | 재확인 시험 1건 실패 후 복구 |
| 린트 | `./gradlew compileKotlin ktlintCheck detekt verifyLintBaselinesNotGrown -PlintBaselineRef=origin/main --offline` | exit 0, 기준선 증가 0건 |
| 범위 | `git status` | `infrastructure/kis/**`·collector-api·trading-web 변경 0 |
| 전체 스위트 | 커밋 `da617e5` 푸시 뒤 CI `full-test` 실행 43688563304 | success (trading-api, collector-api, trading-web) |

---

## 7. Result  (record)

- Done: §4 완료 기준 4개를 시험·린트·CI 전체 스위트로 충족, 커밋 `da617e5`. 사람이 2026-11-15 «step 4 CI 확인과 done 처리»를 확인해 done. 리뷰 후속 9건은 open-questions FLAG-15로 step 5 전에 고침
- Changed files / PR: 아래 경로는 `backend/trading-api/` 기준
  - main 신설: `common/exception/{ServiceUnavailableException,TossTokenUnavailableException,TossIntegrationDisabledException}.kt`, `infrastructure/security/{AesGcmTokenCipher,TokenCipherException}.kt`, `infrastructure/redis/RedisDistributedLock.kt`, `infrastructure/toss/{TossProperties,TossTokenManager,TossTokenIssuer,TossIssuedToken}.kt`
  - main 수정: `presentation/exception/GlobalExceptionHandler.kt`(J 핸들러, import, `@Suppress`), `src/main/resources/application.yaml`(`trading.toss.*`)
  - test 신설: `infrastructure/security/AesGcmTokenCipherTest.kt`, `infrastructure/redis/RedisDistributedLockTest.kt`, `infrastructure/toss/{TossPropertiesTest,TossTokenManagerTest,TossTokenSecretLeakTest,FakeTossTokenIssuer}.kt`, `support/{SharedRedisContainer,LogCapture}.kt`
  - test 수정(추가만, 기존 단언 변경 없음): `presentation/exception/GlobalExceptionHandlerTest.kt`
  - 설정: `config/ktlint/baseline.xml`(GlobalExceptionHandler 15건 줄 번호만 이동, 항목 수 불변)

---

## 8. Problems found  (record)

> Found during the work — it can cause a new step, a change to an existing one, or a flag. Edit this step.md directly then (the reason goes in state.md §3).

| Problem | How it was handled |
|---|---|
| `TossTokenManager` 를 스프링 빈으로 두면 `TossTokenIssuer` 구현이 없어 컨텍스트 기동이 깨짐 | 이 step 에서는 빈 등록 안 함(일반 클래스, 시험이 직접 생성). 실제 발급기와 함께 `@Component` 를 붙이는 일은 step 5 로 넘김(change: step 5 범위에 «TossTokenManager 빈 등록» 한 줄 추가 필요) |
| `expires_in` 이 갱신 여유 이하이면 TTL 이 0 이하라 저장 불가 — 계획서에 처리 규정 없음 | ASSUME: 저장하지 않고 `TOSS_TOKEN_UNAVAILABLE`(J)로 실패(조용히 대체값을 쓰지 않음). 토스 토큰 수명을 모르므로 갱신 여유 기본값을 작게(60초) 둠. 스키마 확정 때 재확인(open-questions flag) |
| 선택 키 기본값(갱신 여유 60초, 락 TTL 20초, 락 대기 15초)은 계획서에 수치 없음 | ASSUME: 락 TTL 은 D11 규칙(연결 5초+읽기 10초보다 길게)을 만족하는 20초, 대기는 발급 최악 소요에 맞춘 15초. 토스 정책 수치가 아니라 운영 조정값(환경변수). 구현자 가정이라 step 5 리뷰에서 확인 |
| `GlobalExceptionHandler` 함수 11개로 detekt `TooManyFunctions` 위반, 이 파일에 줄이 추가돼 ktlint 기준선 15건의 줄 번호가 밀림 | `@Suppress("TooManyFunctions")` 한 줄과 기준선 줄 번호 15건 이동(항목 수 불변, `verifyLintBaselinesNotGrown` 통과). 기준선 파일 손수정이라 리뷰 확인 요청 |
| 락 대기자는 락이 풀렸어도 토큰이 없으면 재획득을 시도하지 않고 한도까지 읽기만 함(계획서 D11 그대로) | 발급자가 실패하면 대기자는 한도 뒤 503. 계획대로 두고, 재획득 시도가 필요한지는 step 5 에서 판단(ignore, 근거: D11 문구) |
| `docs/` 훅이 «토스페이먼츠 공식 문서 대조»를 요구하는 안내를 띄움 | 이 기능은 토스증권 Open API(결제 아님)이고 이 step 은 외부 계약(HTTP)을 구현하지 않아 해당 없음(ignore) |
