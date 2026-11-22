# Step 9 — KIS 에러 정렬

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
| Step | 9 |
| Title | KIS 에러 정렬 |
| Feature | multi-broker-kis-toss |
| Based on | development-plan.md §5 2단계 2번, §10.2 D28, §10.4 AE20 목록 2~3행, DEC-05 A, DEC-06 A. 1단계 D15(범주 기반 예외는 common 에서 Spring 무의존, HTTP 매핑은 GlobalExceptionHandler) |
| Status | done |
| Owner |  |

---

**━━ Plan block (required before starting · from the planner / development-plan design) ━━**

## 1. Goal and scope  (plan)

> What this step (phase) **builds (what)** + **how far it goes (this step's boundary — name what is handed to the next step)**.

- Builds: KIS 잔고 경로의 실패 응답을 범주 기반(J 503, F-4 409)과 code별 고정 문구로 맞추고, 쓰는 곳이 없어진 옛 KIS 예외(401·403·502·504)와 그 기반 클래스를 지운다. 성공 응답과 돈 경로는 그대로(RULE-10, 특성화 시험 고치지 않음)
- Scope (where this step stops): 잔고 경로만: main/infrastructure/kis/KisAccountBalanceAdapter.kt 의 예외 던지는 자리, main/common/exception/Kis*Exception.kt 신설(KIS_AUTH_FAILED·KIS_PERMISSION_DENIED·KIS_UNAVAILABLE·KIS_TIMEOUT·KIS_RATE_LIMITED 는 J, KIS_ACCOUNT_NOT_LINKED 는 F-4), CommonApiExceptions.kt 의 KisAuthorizationException·KisForbiddenException·KisRemoteCallException·KisTimeoutException 과 Unauthorized·Forbidden·BadGateway·GatewayTimeout 기반 클래스 삭제(KisResponseParseException 은 KisOrderRestClient 가 써서 남김), GlobalExceptionHandler.kt 의 KIS code 고정 문구, archunit_store 의 해결된 동결 줄 제거, 관련 시험 2파일(KisAccountBalanceAdapterTest 오류 단언, KisAccountBalanceControllerIntegrationTest 오류 매핑). 범위 밖: KIS_ORDER_REJECTED 와 주문 경로(step 11, KisOrderAdapter), 포트·시그니처 변경(step 10), KisOrderRestClient·KisTokenManager, GlobalExceptionHandler 의 IllegalArgument·IllegalState 묶음 매핑(DEC-06 A), characterization/kis 시험

---

## 2. Approach (how, in what order)  (plan)

> **By what method and in what order** this step (= phase) is solved — briefly (one line to a few). Based on the feature-spec / development-plan design.
> A rough level is enough (the detailed work log is §5). **Never leave it empty — the step's base is here.**

- 시험 먼저: 어댑터 단위 시험에 실패 종류별(rt_cd 1·인증 문구, rt_cd 7·권한 문구, 그 밖, 한도 문구, ResourceAccessException, RestClientException, externalAccountId 없음, 형식 틀림) 새 예외와 code 단언, 컨트롤러 통합 시험에 §10.4 상태·code·고정 문구 단언과 응답 본문에 KIS 원문·계좌번호 없음 단언 → 실패 확인
- 새 예외 6개를 파일 하나에 선언 하나로(KDoc 첫 줄 범주 기호, ServiceUnavailableException·ConflictException 상속, Spring import 없음)
- 어댑터 매핑: 인증 → KIS_AUTH_FAILED, 권한 → KIS_PERMISSION_DENIED, 그 밖 rt_cd 실패는 mapKisErrorCode 가 KIS_RATE_LIMITED 면 KIS_RATE_LIMITED 아니면 KIS_UNAVAILABLE, ResourceAccessException → KIS_TIMEOUT, RestClientException → KIS_UNAVAILABLE, externalAccountId 없음(지금 requireNotNull 400)·형식 틀림(지금 KIS_RESPONSE_PARSE_ERROR 400) → KIS_ACCOUNT_NOT_LINKED. KIS 원문은 cause 와 로그에만(AE8, 계좌번호 마스킹)
- 핸들러에 KIS code 고정 문구 추가(토스 문구와 같은 자리·같은 말투)
- 옛 예외·기반 클래스 삭제, archunit_store 해결 줄 제거, ArchitectureRulesTest 통과 확인
- trading-web src 에 /api/kis/account/balance 호출부 재검색(읽기 전용) 결과와 AE20 목록을 이 문서 §5 에 기록

---

## 3. Dependencies  (plan)

- Previous step: step 8 특성화 시험 통과(338a5ad)
- Prerequisites: Docker(Testcontainers), DEC-05 A

---

## 4. Completion criteria  (plan)

- [ ] §10.4 2~3행의 상태·code·고정 문구가 컨트롤러 통합 시험으로 단언되고 통과
- [ ] characterization/kis 10건 고치지 않고 통과(--rerun, 실행 뒤 생긴 xml)
- [ ] trading-api 전체 시험 기준선(1073건) 대비 새 실패 0
- [ ] compileKotlin·compileTestKotlin·ktlintCheck·detekt·verifyLintBaselinesNotGrown exit 0
- [ ] 옛 KIS 401·403·502·504 예외와 기반 클래스 사용처 0, archunit_store 해결 줄 제거
- [ ] trading-web 호출부 재점검 결과 기록

---

**━━ Record block (filled in order while developing · never ahead) ━━**

## 5. Work log  (record)

> Fill it by spelling out the §2 approach as the actual work (concretely, what was done).

1. 시험 먼저(Red): KisAccountBalanceAdapterTest 오류 단언을 새 예외 기준으로 바꾸고 실패 종류를 8건으로 늘림(인증, 권한, 한도, 그 밖 rt_cd, ResourceAccessException, RestClientException, externalAccountId 없음, 형식 틀림). KisAccountBalanceControllerIntegrationTest 는 §10.4 상태·code·고정 문구와 «응답 본문에 KIS 원문·계좌번호 없음» 단언으로 교체. GlobalExceptionHandlerTest 에 KIS code 별 고정 문구, 예외 클래스 code·범주, KIS_ACCOUNT_NOT_LINKED 409 추가. `./gradlew compileTestKotlin` 이 새 예외 미해결 참조로 실패함을 확인(Red).
2. 새 예외 6개(common/exception, 파일 하나에 선언 하나, KDoc 첫 줄 범주 기호, Spring import 없음): KisAuthFailedException·KisPermissionDeniedException·KisUnavailableException·KisTimeoutException·KisRateLimitedException(J, ServiceUnavailableException 상속, 생성자는 cause 만 받고 메시지는 KIS 원문·계좌번호 없는 고정 문구), KisAccountNotLinkedException(F-4, ConflictException 상속, 값 없는 안내 문구).
3. 어댑터(KisAccountBalanceAdapter): requireNotNull 400 → KisAccountNotLinkedException, 형식 틀림 KIS_RESPONSE_PARSE_ERROR 400 → KisAccountNotLinkedException, ResourceAccessException → KisTimeoutException, RestClientException → KisUnavailableException, rt_cd 실패는 인증/권한 판정은 그대로 두고 그 밖은 mapKisErrorCode 결과가 KIS_RATE_LIMITED 면 KisRateLimitedException 아니면 KisUnavailableException. KIS 원문은 KisApiException(cause)에만 있고 응답에는 안 실림(핸들러 로그에는 cause 로 남음). 계좌번호는 어떤 예외 메시지에도 안 넣음.
4. 핸들러(GlobalExceptionHandler): serviceUnavailableMessage 의 else 를 kisServiceUnavailableMessage 로 위임해 KIS 5개 code 고정 문구 추가(한 함수에 모으면 detekt CyclomaticComplexMethod 한도 15 도달).
5. 삭제: CommonApiExceptions.kt 의 KisAuthorizationException·KisForbiddenException·KisRemoteCallException·KisTimeoutException(옛)·UnauthorizedException·ForbiddenException·BadGatewayException·GatewayTimeoutException. KisResponseParseException 은 KisOrderRestClient.kt 가 써서 남김. 삭제 뒤 옛 이름·옛 code 문자열 잔존 검색(src, config): 코드 0건, detekt baseline 의 낡은 ID 3줄만 남아 제거.
6. archunit_store 3baebd4b…: 삭제된 클래스의 동결 줄 4줄(BadGateway, Forbidden, GatewayTimeout, Unauthorized 생성자) 제거. 남은 줄은 손대지 않음.
7. 린트 baseline: 코드 줄이 밀려 ktlint baseline 의 줄 번호가 어긋난 4파일(CommonApiExceptions, KisAccountBalanceAdapter, 시험 2파일)의 줄 번호만 맞춤(항목 수는 CommonApiExceptions 에서 옛 클래스 4줄 줄고 나머지 불변, 새 위반을 baseline 에 흡수하지 않음). 새 예외 5개 파일은 처음엔 ktlint class-signature 위반이라 Toss 예외와 같은 줄바꿈으로 고침. detekt baseline 낡은 ID 3줄 제거.
8. 프런트 재검색(읽기 전용): `grep -rn "api/kis/account/balance" frontend/trading-web/src` → 결과 0건(exit 1). `kis/account` 로 넓혀도 0건. AE20 목록(§10.4 2~3행) 대로 호출부 없음 → 진행.

---

## 6. Verification  (record)

| What | How it was checked | Result |
|---|---|---|
| Red 확인 | `./gradlew compileTestKotlin --offline`(구현 전) | 새 예외 6개와 `errorCode` 미해결 참조로 컴파일 실패 |
| 집중 시험 | `./gradlew test --rerun --offline --tests '*KisAccountBalanceAdapterTest' --tests '*KisAccountBalanceControllerIntegrationTest' --tests 'com.papertrading.api.characterization.kis.*' --tests '*Architecture*' --tests '*ApplicationLayerRules*' --tests '*GlobalExceptionHandlerTest'`(backend/trading-api), 실행 뒤 생긴 xml 만 집계 | exit 0, BUILD SUCCESSFUL. xml 8개(오래된 xml 무시 0), 72건 성공 / 실패 0 / 오류 0 / 건너뜀 0. 파일별: ArchitectureRulesTest 15, ApplicationLayerRulesTest 6, characterization/kis 3+3+4=10(고치지 않음, git diff 0), KisAccountBalanceAdapterTest 10, KisAccountBalanceControllerIntegrationTest 2, GlobalExceptionHandlerTest 29 |
| 전체 시험 | `./gradlew test --rerun --offline`(backend/trading-api), 실행 시작 시각(`date +%s`) 뒤에 생긴 xml 만 집계 | exit 0, BUILD SUCCESSFUL in 9m 56s. xml 85개(오래된 xml 무시 0), 1095건 성공 / 실패 0 / 오류 0 / 건너뜀 0. 기준선 1073건(xml 82개)은 특성화 시험 추가 전 수치라 85-82=3개 클래스(특성화 10건 + 이 step 신규 12건)가 늘어난 값과 맞음 → 기준선 대비 새 실패 0 |
| 린트 | `./gradlew compileKotlin compileTestKotlin ktlintCheck detekt verifyLintBaselinesNotGrown -PlintBaselineRef=origin/main --offline` | exit 0. ktlint baseline origin/main 대비 늘어난 항목 0건, detekt baseline 늘어난 항목 0건. detekt baseline 낡은 ID 3줄 제거 뒤에도 같은 명령을 다시 돌려 exit 0(detekt 재실행됨) |
| 옛 예외 사용처 0 | 옛 클래스·옛 code 문자열 잔존 검색(`src`, `config`) | 코드 0건. detekt baseline 낡은 ID 3줄만 남아 제거함 |
| archunit_store | 동결 파일 `git diff --stat` | 4줄 삭제(옛 BadGateway·Forbidden·GatewayTimeout·Unauthorized 생성자), 그 밖 줄 불변. ArchitectureRulesTest 15건 통과 |
| trading-web 호출부 | `grep -rn "api/kis/account/balance" frontend/trading-web/src` | 0건(exit 1), `kis/account` 로 넓혀도 0건 → AE20 목록의 «호출부 0건»과 같음 |

---

## 7. Result  (record)

- Done: 잔고 경로 KIS 실패가 범주 J 503(KIS_AUTH_FAILED·KIS_PERMISSION_DENIED·KIS_UNAVAILABLE·KIS_TIMEOUT·KIS_RATE_LIMITED)과 F-4 409(KIS_ACCOUNT_NOT_LINKED)로 바뀌고 code 별 고정 문구로 응답. 옛 KIS 401·403·502·504 예외와 기반 클래스 4개 삭제. 성공 응답·특성화 시험 10건 불변.
- Changed files / PR: (커밋 전, backend/trading-api 기준)
  - 신설 main: common/exception/ KisAuthFailedException.kt, KisPermissionDeniedException.kt, KisUnavailableException.kt, KisTimeoutException.kt, KisRateLimitedException.kt, KisAccountNotLinkedException.kt
  - 수정 main: common/exception/CommonApiExceptions.kt(옛 8개 클래스 삭제), infrastructure/kis/KisAccountBalanceAdapter.kt, presentation/exception/GlobalExceptionHandler.kt
  - 수정 test: infrastructure/kis/KisAccountBalanceAdapterTest.kt, presentation/controller/KisAccountBalanceControllerIntegrationTest.kt, presentation/exception/GlobalExceptionHandlerTest.kt
  - 수정 설정: src/test/resources/archunit_store/3baebd4b-ce02-4e9d-9ab2-4419048a522d(4줄 삭제), config/ktlint/baseline.xml(줄 번호 이동과 옛 클래스 4줄 삭제), config/detekt/baseline.xml(낡은 ID 3줄 삭제)
  - 문서: 이 파일(§5~8)

---

## 8. Problems found  (record)

> Found during the work — it can cause a new step, a change to an existing one, or a flag. Edit this step.md directly then (the reason goes in state.md §3).

| Problem | How it was handled |
|---|---|
| 가정(되돌리기 쉬움): 잔고 경로 rt_cd 실패 중 인증·권한 판정에 안 걸린 것은 `mapKisErrorCode` 결과가 KIS_RATE_LIMITED 면 KIS_RATE_LIMITED, 나머지는 KIS_UNAVAILABLE | 어댑터 한 줄 분기(`else` 안), 시험 2건으로 고정. 틀리면 분기 한 곳만 고침 |
| `mapKisErrorCode` 는 문구 휴리스틱이고 `account`·`계좌` 문구를 한도 문구보다 먼저 봄 → 한도 문구에 `account` 가 들면 KIS_INVALID_ACCOUNT 로 분류돼 KIS_UNAVAILABLE 로 감 | 이 step 에서 안 고침(KisApiException.kt 는 범위 밖, 주문 경로도 같이 씀). step 11 에서 주문 경로 정렬 때 함께 점검 권고 |
| 컨트롤러 통합 시험은 `KisAccountQueryService` 를 목으로 갈음하는 기존 구조라 «어댑터 → 핸들러 → HTTP 응답» 전 구간 단일 시험은 없음. 어댑터 단위 시험(예외 종류·code)과 컨트롤러 시험(상태·code·고정 문구·본문에 원문·계좌번호 없음)이 이어 붙어 증명 | 구조는 유지(범위 밖). 전 구간 시험이 필요하면 후속 판단 |
| ktlint baseline 이 줄 번호 키라 import 추가·클래스 삭제로 줄이 밀린 기존 위반 항목이 «새 위반»으로 잡힘 | 새 위반 흡수가 아니라 기존 항목의 줄 번호만 맞춤(`verifyLintBaselinesNotGrown` 0건 증가로 확인). 새 예외 5개 파일은 baseline 에 넣지 않고 Toss 와 같은 모양으로 고쳐 위반 0 |
| 멈춤 조건에 걸린 것 | 없음(특성화 시험 수정 0, 동결 목록 밖 ArchUnit 새 위반 0, trading-web 호출부 0, Docker 정상) |
