# 코드 리뷰 결과 — multi-broker-kis-toss step 4 «토스 토큰 기반» (재리뷰 1 = 2회차)

통과 뒤 회차: 사람 결정 FLAG-15(2026-11-15 팝업 답, 후속 9건 전부 수정)

범위: `변경분` (HEAD 7ce7a70 위 미커밋 `backend/trading-api/` 12파일, `git diff -- backend/`). 비교점 = 1회차가 본 da617e5 코드. 재리뷰 모드: 지난 막는 지적 판정 + 이번 수정이 깬 것만.

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 리뷰 대상 | 미커밋 변경 main 6파일(`RedisDistributedLock.kt` · `AesGcmTokenCipher.kt` · `TossProperties.kt` · `TossTokenManager.kt` · `GlobalExceptionHandler.kt` · `application.yaml`), 시험 6파일(`RedisDistributedLockTest` · `AesGcmTokenCipherTest` · `TossPropertiesTest` · `TossTokenManagerTest` · `TossTokenSecretLeakTest` · `GlobalExceptionHandlerTest`) |
| 기준 | `rule-pointer --list` 가 낸 목록 그대로: `rules/dev-team/common.md`(AE18 · AE21 · CS1~CS25 · C12 · DD1~DD6) · `rules/dev-team/failure-handling.md`(FH1~FH6 + Diff checks 4행) · `rules/dev-team/tests.md`(TT 조항) · 레포 `rules/tests.md`(PTT1) · `always:dev-team`. 설계 기준: `development-plan.md` D10 · D11(재승인 대기 문구) · D15 |
| 프로필 | consumer (`profile:consumer` 행) |
| 리뷰어 | code-reviewer (구현자와 독립, 읽기 전용) |
| 날짜 | 2026-11-15 |

## 1. 요약

| 항목 | 값 |
|---|---|
| 리뷰한 렌즈 | 정확성 · 표준 · 유지보수성 · 보안 · 성능 · 회귀 시험 (근본성: 출시 전 기능의 리뷰 후속 반영이고 `root-cause.md` 없음 — bugfix diff 로 보지 않음 · 의도: 확인 불가(미커밋) — 커밋 의도 블록 없음) |
| 수정 지킴 확인 | 확인 불가(미커밋) — 변경이 미커밋이라 `guards-fix` 의 merge-base diff 에 안 잡힘. 탐침 사본(`probe-copy.js`)에서 단위 시험만 돌리려 했으나 Gradle 래퍼 잠금 파일(`~/.gradle/wrapper/dists/gradle-8.14.4-bin/…/gradle-8.14.4-bin.zip.lck`) 쓰기가 EPERM 으로 막힘, Redis 시험은 Docker 소켓도 막힘. 통과 아님. 읽어서 본 것: C-01~C-05 · C-07~C-09 새 시험은 수정 전 코드에서 빨개질 단언을 가짐(읽기 판단, 측정 아님) |
| 작성자 시험 증거 | `build/test-results/test/*.xml` 8클래스 104건(1+13+10+14+25+22+6+13) 실패 0 · 오류 0, 실행 16:51:42 KST — 마지막 소스 수정(`RedisDistributedLock.kt` 16:51:11) 뒤 |
| 뮤테이션 | 확인 불가(뮤테이션) — `mutation-run.js --base main` exit 4 «뮤테이션 대상 없음», 목록 없음. 미커밋이라 범위에 안 잡힌 것으로 보여 통과로 보지 않음 |
| 재사용 점검 | `reuse-inventory --target` 4파일(RedisDistributedLock · TossTokenManager · GlobalExceptionHandler · AesGcmTokenCipher) 재생성 의심 0건 |
| 발견 | 8개 (막음 0 · 후속 후보 8) |
| 머지 판정 | `review-verdict.js <이 파일> --previous step4-code-review.md` 첫 줄: 머지 판정: 가능 — 막는 지적 0건 · 지난 지적 미해결 0건 · 후속 후보 8건 (재리뷰 · 2회차) |
| 한 줄 | 후속 9건 모두 고쳐졌고 이번 수정이 막는 결함을 새로 만들지 않음. 남은 것은 같은 부류의 남은 꼴 2건(R1-01 TTL 경계 바로 위 구간, R1-02 대기+발급 전체 상한 없음), CS24 1건(R1-03), 시험 공백 1건(R1-04)과 nit 4건 |

## 1-1. 프로필별 검토 절 (consumer)

- 1. 규칙 준수 — 대조 출처: `rule-pointer --list` 목록만.
  - common.md: AE21 · CS14 · CS21 — 새 catch 없음. `decodeBase64` 의 catch 는 다른 예외로 다시 던짐(원인 버린 사유 주석 그대로). 잡이 처리기는 격리 지점에서 기록하고 가둠(C-07) → 충족. 락 상실 경로는 warn + 캐시 재조회 또는 503 — 성공처럼 보이는 대체값 아님. CS22 — `decryptParts` try 몸통 변화 없음(키 인자만). CS24 위반 1건(R1-03, 기존 `defaultMessage` 의 같은 꼴은 안 건드린 줄이라 `기존`). CS1 · CS4 · CS5 — 재생성 의심 0건, Lua 중복 제거(C-06). CS25 — 새·바뀐 함수 40줄 · 중첩 3단계 이하(`issueUnderLock` 12줄, `computeDisabledKeyNames` 17줄). C12 — 새 주석 전부 명사형 한 줄, 종결어미 · `**` 없음. CS15 — TODO · FIXME 없음. DD1 · DD3 — 503 응답 message 문구만 바뀜(code · 상태 그대로), 호출처 없음(빈 미등록, FLAG-14).
  - failure-handling.md: Diff checks 4행 — 돈 · 주문 · 발송 POST, 웹훅, 메시지 소비자, 건별 루프 모두 이 diff 에 없음 → 해당 없음. FH1~FH6 해당 없음.
  - tests.md: TT1 — 바뀐 소스 5개 모두 시험 동반. TT25 · TT26 — 단언이 바뀐 시험 파일 4개(AesGcmTokenCipherTest · TossTokenManagerTest · TossTokenSecretLeakTest · GlobalExceptionHandlerTest)에 `devkit:test-change` 표지, 근거 = D10 · D15 명세 변경(사람 결정 FLAG-15). RedisDistributedLockTest · TossPropertiesTest 는 추가만. TT44 — `callCount` 단언은 발급 1회(RULE-06) 경계 계약이라 허용(1회차와 같음). TT16 1건(R1-08). 시험 공백 1건(R1-04).
  - 레포 rules/tests.md PTT1 — `storeIfHeld` · `deleteIfEquals` · 락 상실 · 이전 키 경로 모두 Testcontainers Redis 실제 엔진으로 시험. 충족.
  - always:dev-team — 시간은 epoch 밀리초만(시간대 없음). 시험 먼저: 작성자 Red 기록은 측정 불가(위 «수정 지킴 확인»).
- 2. 사이드 이펙트
  - `RedisDistributedLock`: 호출처는 `TossTokenManager` 하나(grep). `release` 동작 같음(같은 Lua).
  - `AesGcmTokenCipher`: 생성자 · `fromBase64Key` 에 기본값 인자 추가라 기존 호출 그대로, 호출처는 `TossProperties` · `TossTokenManager` 뿐. 형식 v1 → v2: da617e5 의 v1 값은 배포된 적 없음(관리자 빈 미등록) → 실데이터 영향 없음, 남은 v1 값은 복호화 실패 → 재발급(D10, 시험 있음).
  - `GlobalExceptionHandler`: `ServiceUnavailableException` 하위는 `TossTokenUnavailableException` 하나(grep) → 503 문구 변경은 토스 경로만이고 그마저 호출처 없음. 잡이 처리기 로그 추가는 모든 엔드포인트에 걸림(§3).
  - `application.yaml` 새 키 1개(`TOSS_TOKEN_PREVIOUS_ENCRYPTION_KEY`), 빈 기본값.
  - `TossProperties` `by lazy`: 바인딩 후처리기가 `@PostConstruct` 보다 먼저 돌아 바인딩 뒤 첫 계산 — 컨텍스트 시험 25건 통과로 확인.
- 3. 버그 · 장애 위험
  - 보안: Redis 값은 `v2.<키 식별자>.<IV>.<본문>` 만. 키 식별자는 32바이트 무작위 키의 SHA-256 앞 8바이트라 역산 불가, base64url 이라 `.` 없음(분리 안전). 로그 · 예외 문구에 식별자 · 암호문 · 키 없음(`decryptOrNull` 은 고정 문구 message 만). 이전 키 형식 오류는 이름만 싣고 비활성(시험 있음). 503 응답에 예외 message 안 실음(C-09).
  - 동시성: `storeIfHeld` 는 GET 비교와 SET PX 가 한 스크립트라 원자적. 두 키 스크립트는 Redis Cluster 면 CROSSSLOT 이지만 `application.yaml:31-33` 이 단일 host/port → 해당 없음. `deleteIfEquals` 는 읽은 암호문 그대로 비교. 대기 중 재획득 뒤 `issueHolding` 이 캐시를 다시 읽어 이중 발급 없음. 남은 위험 R1-02.
  - 실패 처리: C-03 락 상실 → warn + 캐시 재조회, 없으면 503(D11 그대로). 이때 방금 받은 토큰은 버려지지만 D11 문구가 정한 것이고 발급이 락 TTL 20초를 넘길 때만이라 지적 안 함. C-07 로그 1회.
  - 설정 범위: C-04 경계값 시험 있음(15000 비활성 · 15001 활성 등). 하한 숫자 출처 R1-05.
  - TTL 계산: R1-01 · R1-06.
- 조항을 어기지 않는 코드 품질 지적은 `should-fix` 이하로 둠.

## 2. 발견 (findings)

| ID | file:line | 렌즈 | 근거 조항 | 무엇이 틀렸나 | 등급 | 출처 | 회귀 | confidence | 구조 고침 | 재현 |
|---|---|---|---|---|---|---|---|---|---|---|
| R1-01 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossTokenManager.kt:93-97 | 정확성 | label:redundant-work-per-item | C-01 과 같은 부류의 남은 꼴. 줄인 TTL 은 `expires_in − 갱신 여유 ≤ 0` 일 때만 적용돼 저장 TTL 이 수명에 대해 단조가 아님: 갱신 여유 60초에서 `expires_in=60` → 30초, `expires_in=61` → 1초, 61~119초 구간은 전부 절반보다 짧은 1~59초. 이 구간의 토큰은 TTL 이 끝날 때마다(61초면 매초) 재발급되고 재발급이 직전 토큰을 폐기해 쓰던 요청이 401 → C-01 이 막으려던 «재발급 반복 · 직전 토큰 폐기»가 경계 바로 위에 그대로 남음. 시험(TossTokenManagerTest.kt:292)은 60 · 30 · 1 · 0 만 봐 이 구간이 빔 | should-fix | 이번 수정 | 아니오 | 높음 | 한 식으로 연속되게 `ttl = max(expires_in − 갱신 여유, max(expires_in / 2, 1))`, D10 문구도 같은 식으로(재승인 대기 중이라 함께 올림) | TossTokenManagerTest 에 `FakeTossTokenIssuer(expiresInSeconds = 61)` + `properties(skewSeconds = 60)` → `getExpire(TOKEN_KEY, SECONDS)` 가 1 (60 이면 30) |
| R1-02 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossTokenManager.kt:51-54 | 정확성 | label:required-state-not-designed | 대기 한도가 «락 획득 대기»만 묶고 대기자가 락을 얻은 뒤의 발급 시간은 묶지 않음. `tryAcquire`(:52)가 한도 확인(:54)보다 앞이라 한도 직전 · 직후에 락을 얻어도 그대로 발급. 토스 장애 때 보유자 발급이 연결 5초 + 읽기 10초로 실패해 락이 풀리면, 대기하던 요청이 한도 끝 무렵 락을 얻어 다시 최대 15초 발급 → 요청 스레드 최악 점유가 수정 전 15초(대기 한도 뒤 503)에서 약 30초로 늘어남. C-02 가 줄이려던 서블릿 스레드 묶임이 장애 때 오히려 길어짐. D11 에도 요청 하나의 전체 상한이 없음 | should-fix | 이번 수정 | 아니오 | 중간 | 요청 하나의 상한을 한 값으로: 대기자가 락을 얻었을 때 남은 시간(deadline − now)이 발급 최악 소요보다 짧으면 발급하지 않고 락을 풀고 503, 또는 D11 에 «대기 + 발급 전체 상한»을 적고 사람이 값 결정 | `lockWaitMillis = 5_000`, sleeper 가 시계를 4,900ms 까지 돌린 뒤 락을 풀고 `FakeTossTokenIssuer(onIssue = { clock.addAndGet(15_000) })` → `getToken` 성공, 끝 시각 − 시작 ≈ 19,900ms |
| R1-03 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/exception/GlobalExceptionHandler.kt:104-107 | 표준 | CS24 | 새 `serviceUnavailableMessage` 가 에러 code 를 문자열 리터럴 `"TOSS_TOKEN_UNAVAILABLE"` 과 비교. 같은 code 가 `TossTokenUnavailableException.kt:9` 에도 리터럴로 따로 있어 한쪽 오타 · 개명이면 조용히 기본 문구로 떨어짐(지금은 시험이 문구를 확인해 잡음). 응답 문구가 code 정의와 떨어진 처리기 `when` 에만 있어 D15 의 `TOSS_IP_NOT_ALLOWED`(허용 IP 등록 안내 고정 문구) 같은 step 5 code 는 `when` 에 줄을 더하지 않으면 안내 없이 기본 문구로 나감 — 예외 생성자의 `message` 는 이제 응답에 안 쓰이는데 받는 모양은 그대로 | should-fix | 이번 수정 | 아니오 | 높음 | 응답 고정 문구를 code 와 한 자리에: 예외가 code 상수와 공개 문구 상수를 함께 들고(예: `ServiceUnavailableException` 에 `publicMessage`, 하위 예외가 상수로 채움) 처리기는 그 값만 씀, 원문 `message` 는 로그 전용 | — |
| R1-04 | backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/security/AesGcmTokenCipherTest.kt:113-118 | 회귀 시험 | label:test-asserts-nothing | 형식 오류 목록을 v2 로 옮기면서 옛 `"v1.!!!.@@@"`(IV · 본문 base64 해독 실패까지 가던 입력)가 빠지고, 대신 든 `"v2.!!!.@@@.###"` 은 모르는 키 식별자에서 먼저 거부돼 해독까지 안 감. 목록 9개 · 변조 · 다른 키 · AAD 시험 모두 `decryptParts` 의 `IllegalArgumentException → TokenCipherException` 변환(AesGcmTokenCipher.kt:52-53)에 닿지 않음 → 그 catch 를 지워도 시험이 초록. 그 경우 식별자는 맞고 base64 가 깨진 캐시 값이 `decryptOrNull`(TokenCipherException 만 잡음)을 빠져나가 `getToken` 이 `handleIllegalArgument` 400 `BAD_REQUEST`(JDK 문구 그대로)로 끝나고 재발급 복구도 안 됨 | should-fix | 이번 수정 | 아니오 | 높음 | 현재 키 식별자를 붙인 깨진 base64 입력 한 줄 추가: `"v2.${cipher.encrypt(plain, aad).split(".")[1]}.!!!.@@@"` | AesGcmTokenCipher.kt:52-53 catch 를 지운 사본에서 `AesGcmTokenCipherTest` 실행 → 14건 초록 예상(이 샌드박스는 Gradle 잠금 쓰기 EPERM 으로 미실행) |
| R1-05 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossProperties.kt:85-86 | 유지보수성 | label:hardcoded-value-not-ssot | 락 TTL 하한 15,000ms 가 «연결 5초 + 읽기 10초»(D11 이 근거로 든 `RestTemplateConfig.kt:13-14`)를 손으로 더한 숫자. 시간 제한을 바꾸면 하한이 조용히 어긋나 C-04 가 지키려던 «발급 도중 락 만료 금지»가 다시 깨짐 | nit | 이번 수정 | 아니오 | 중간 | 시간 제한을 한 곳의 상수로 두고 하한을 그 합으로 계산(토스 발급기가 쓸 시간 제한과 같은 값) | — |
| R1-06 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossTokenManager.kt:95 | 정확성 | label:missing-input-validation | `expires_in ≤ 0`(토스가 이미 만료라고 준 토큰)도 1초 캐시해 정상 토큰으로 돌려줌, 수정 전엔 503. 그 1초 동안의 요청은 401 → 무효화 → 재발급이 다시 0 을 받으면 같은 순환. D10 문구는 «`expires_in − 갱신 여유` 가 0 이하»만 말해 `expires_in` 자체가 0 이하인 응답을 정상으로 볼지 정하지 않았고, 시험(TossTokenManagerTest.kt:292 `0L to 1L`)이 정상으로 못박음 | nit | 이번 수정 | 아니오 | 중간 | `expires_in ≤ 0` 은 응답 이상으로 보고 D15 `TOSS_RESPONSE_INVALID` 로 거부할지 D10 재승인 때 사람 확인 | — |
| R1-07 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossProperties.kt:12 | 유지보수성 | label:stale-doc-after-change | 클래스 KDoc 이 «필수 4개 중 하나라도 비었거나 암호화 키 형식이 틀리면 토스만 비활성» 그대로인데 이번 수정으로 이전 키 형식 오류 · 선택 키 범위 오류도 비활성 사유가 됨. 같은 부류로 `TossTokenUnavailableException.kt:4` «(락 대기 한도 초과, 토큰 수명 이상)» 은 C-01 로 «토큰 수명 이상» 경로가 없어지고 «발급 도중 락 만료»가 생겼는데 안 고침 | nit | 이번 수정 | 아니오 | 높음 | — | — |
| R1-08 | backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/redis/RedisDistributedLockTest.kt:116 | 표준 | TT16 | 새 «락을 잃었으면 저장하지 않는다» 시험이 실제 시간 `Thread.sleep(250)` 으로 TTL 100ms 만료를 기다림. 이 시험이 보려는 것은 TTL 이 아니라 «소유자가 바뀐 뒤의 저장 거부»라 시간이 필요 없음(같은 상황을 TossTokenManagerTest 는 락 키 삭제 뒤 재획득으로 결정적으로 만듦). TTL 동작 자체는 기존 시험(:74)이 이미 실제 시간으로 봄 | nit | 이번 수정 | 아니오 | 높음 | 락 키를 지우고 새 소유자가 `tryAcquire` 하는 방식으로 | — |

## 2-1. 재리뷰 — 지난 지적 판정

1회차(`step4-code-review.md`) 막는 지적 0건 → 판정할 막는 행 없음.

참고 — 1회차 후속 9건 처리 확인(막는 행 아님, 원장 판정 아님):

| 후속 ID | 처리 | 근거 |
|---|---|---|
| C-01 | 고침 | `TossTokenManager.kt:92-98` `storageTtlSeconds` 가 0 이하이면 절반(최소 1초)으로 저장, warn 1회. 시험 TossTokenManagerTest.kt:290-303(TTL 값) · «수명이 짧은 토큰은 다음 getToken 이 캐시 적중»(발급 1회 · warn 1회). 같은 부류의 남은 꼴은 R1-01 |
| C-02 | 고침 | `TossTokenManager.kt:49-60` 루프가 매 회 `tryAcquire` 뒤 캐시 재조회. 시험 «대기 중 토큰도 락도 없으면 락을 다시 잡아 직접 발급한다»(polls < 5 · 발급 1회 · 락 해제). 남은 꼴 R1-02 |
| C-03 | 고침 | `RedisDistributedLock.kt:33-36 · 43-47` Lua «락 값 = 내 소유자일 때만 SET PX», `TossTokenManager.kt:83-86` 실패 시 캐시 재조회 또는 503. 시험 RedisDistributedLockTest storeIfHeld 3건, TossTokenManagerTest 락 상실 2건(덮어쓰지 않음 · 남의 락 유지 / 캐시 없음 → 503 · 저장 없음) |
| C-04 | 고침 | `TossProperties.kt:58-60` 갱신 여유 < 0 · 락 TTL ≤ 15,000ms · 대기 ≤ 0 이면 이름만 싣고 토스만 비활성. 시험 범위 오류 7건 · 경계 안 3건(15001 · 0 · 1), 값 미노출 단언. 하한 숫자 출처는 R1-05 |
| C-05 | 고침 | `TossProperties.kt:34` `by lazy` 로 한 번 계산 · 보관, `requireEnabled` · `integrationEnabled` · `@PostConstruct` 가 같은 값. 시험 `assertSame` |
| C-06 | 고침 | `RedisDistributedLock.kt:27-30` `deleteIfEquals` 하나를 `release` 와 `invalidate`(`TossTokenManager.kt:45`)가 함께 씀, 관리자의 중복 스크립트 삭제. 시험 «deleteIfEquals 는 값이 같을 때만 지운다» |
| C-07 | 고침 | `GlobalExceptionHandler.kt:96-102` `log.error(ex)` 1회, 응답은 고정 500. 시험 «잡이 처리기는 500 고정 응답을 주고 원인 예외를 error 로그로 한 번 남긴다» |
| C-08 | 고침 | `AesGcmTokenCipher.kt` 형식 `v2.<키 식별자>.<IV>.<본문>`, 식별자 = 키 SHA-256 앞 8바이트, 이전 키 선택(저장은 늘 현재 키), `TossProperties.kt:53-56` 이전 키 형식 검사, `TossTokenManager.kt:28-33` 전달, `application.yaml` 새 키. 시험 암호기 5건 · 관리자 2건 · 누출 1건 · 설정 2건. 롤링 배포 중 1회 재발급 잔여는 §3 |
| C-09 | 고침 | `GlobalExceptionHandler.kt:41 · 104-107` code 별 고정 문구, 예외 message 는 로그만. 시험 «범주 J 예외에 토스 원문이 실려도 응답에는 싣지 않고 로그에만 남긴다». 문구와 code 가 떨어진 구조는 R1-03 |

## 3. 범위 밖 (안 본 것)

- 잡이 처리기 로그(C-07)는 모든 엔드포인트에 걸림: 따로 처리기가 없는 Spring MVC 요청 오류(본문 해독 실패 · 지원 안 하는 메서드 · 필수 파라미터 누락 등)도 `GlobalExceptionHandler.kt:96` 이 잡아 500 + 이제 ERROR 스택 로그. 4xx 를 500 으로 내는 것은 기존 동작이고 이번 수정은 로그만 더함. 범주 매핑 문제는 `backend-api.md` 몫인데 이번 `--list` 에 없어 행으로 안 올림.
- C-08 키 교체: 한 번에 키를 바꾸는 롤링 배포에서는 옛 인스턴스(이전 키 설정 없음)가 새 키 암호문을 못 읽어, 새 인스턴스 저장 1회당 옛 인스턴스 재발급 1회는 남음. 옛 인스턴스가 쓴 값은 새 인스턴스가 이전 키로 읽어 왕복 반복은 끝남. 0회로 하려면 «현재 = 옛 키 · 이전 = 새 키 먼저 배포 → 그다음 현재 = 새 키 · 이전 = 옛 키» 2단계 — 운영 절차 문서 몫, 코드 결함 아님.
- 읽기 범위: 이번 요청에 `재리뷰 읽기 범위` 블록이 없어 지시(diff 변경 줄 + 그 줄이 깰 수 있는 호출처)대로 봄. diff 밖으로 연 파일 = `TossTokenUnavailableException.kt` · `ServiceUnavailableException.kt`(처리기가 읽는 예외 계약, R1-03 · R1-07 판단용).
- 시험 실행: 지시대로 전체 스위트 안 돌림. 개별 시험도 Gradle 잠금 EPERM 으로 못 돌려 작성자 xml 집계(104건, 실패 0)를 근거로 둠.
- `docs/` 변경(development-plan · tech-plan-approval · progress)은 코드 리뷰 범위 밖. D10 · D11 · D15 문구는 설계 기준으로만 대조.
- `skill-session.js open code-review` 는 실행 안 함(1회차에 샌드박스가 `~/.devkit` 쓰기를 막은 기록).

<!-- devkit:standard-list hex:7b22636861696e223a22646f63732f66656174757265732f72756e6e696e672f6d756c74692d62726f6b65722d6b69732d746f73732f7265766965772f73746570342d636f64652d7265766965772e6d64222c22726f756e6473223a7b2231223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2239313566313166653261646266313231353537323039333162616663653966373461333064633536227d2c2232223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2237636537613730613766343034353238303935323239383666333664636137393633666165613732227d7d7d -->
