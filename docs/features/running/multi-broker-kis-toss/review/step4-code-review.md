# 코드 리뷰 결과 — multi-broker-kis-toss step 4 «토스 토큰 기반» (1회차)

범위: `변경분` (HEAD 915f11f 위 미커밋 변경 전부, `git status --short backend/`)

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 리뷰 대상 | worktree `.worktrees/multi-broker-kis-toss` 미커밋 변경: main 신설 10파일(`common/exception` 3 · `infrastructure/security` 2 · `infrastructure/redis/RedisDistributedLock.kt` · `infrastructure/toss` 4), 수정 `GlobalExceptionHandler.kt`·`application.yaml`·`config/ktlint/baseline.xml`, 시험 신설 8파일, 수정 `GlobalExceptionHandlerTest.kt` |
| 기준 | `rule-pointer --list` 가 낸 목록 그대로: `rules/dev-team/common.md`(AE18·AE21·CS1~CS25·C12·DD1~DD6) · `rules/dev-team/failure-handling.md`(FH1~FH6 + Diff checks 4행) · `rules/dev-team/tests.md`(TT 조항) · 레포 `rules/tests.md`(PTT1) · `always:dev-team` |
| 프로필 | consumer (`profile:consumer` 행) |
| 리뷰어 | code-reviewer (구현자와 독립, 읽기 전용) |
| 날짜 | 2026-11-15 |

## 1. 요약

| 항목 | 값 |
|---|---|
| 리뷰한 렌즈 | 정확성 · 표준 · 유지보수성 · 보안 · 성능 · 회귀 시험 (근본성: bugfix 아님 · 의도: 미커밋이라 커밋 의도 블록 없음) |
| 수정 지킴 확인 | 확인 불가 — 변경이 미커밋이라 merge-base diff 가 비고, 탐침 샌드박스에서 Gradle 캐시 쓰기와 Docker 소켓(Testcontainers)이 막혀 `guards-fix` 를 돌릴 수 없음. 통과 아님. 작성자 기록(step-4 §5·§6: Red = 새 클래스 미해결 참조 컴파일 실패, 변이 실험 1건)은 읽었으나 측정값으로 대신하지 않음 |
| 뮤테이션 | 확인 불가(뮤테이션) — `mutation-run.js --base main` exit 4, 목록 없이 «뮤테이션 대상 없음». 대상 파일이 미커밋·새 파일이라 범위에 안 잡힌 것으로 보여 통과로 보지 않음. 커밋 뒤 재실행 필요 |
| 재사용 점검 | `reuse-inventory --target` (RedisDistributedLock·TossTokenManager·AesGcmTokenCipher) 재생성 의심 0건. 의미 중복 1건은 C-06 |
| 발견 | 9개 (막음 0 · 후속 후보 9) |
| 머지 판정 | `review-verdict.js` 첫 줄: 머지 판정: 가능 — 막는 지적 0건 · 후속 후보 9건 |
| 한 줄 | 보안 바닥(평문 저장·로그 0, GCM 사용, 소유자 확인 해제, 락 안 재확인)은 맞게 섰고 막는 지적 없음. 남은 위험은 실패 경로 설계: 수명 짧은 토큰을 발급하고 버림(C-01), 발급자 실패 뒤 대기자가 한도까지 묶임(C-02), 락 만료 뒤 늦은 저장(C-03), 선택 키 범위 미검증(C-04) |

기타: `skill-session.js open code-review` 는 샌드박스가 `~/.devkit` 쓰기를 막아 EPERM 으로 실패(리뷰 절차 자체는 진행).

## 1-1. 프로필별 검토 절 (consumer)

- 1. 규칙 준수 — 대조 출처: `rule-pointer --list` 목록만.
  - common.md: AE21·CS14 — `decryptOrNull` catch 는 좁은 `TokenCipherException` 만 잡고 warn 로그 뒤 «캐시 없음 → 재발급»으로 실제 복구(성공처럼 보이는 대체값 아님, D10 설계 그대로). `fromBase64Key` 는 다른 예외로 다시 던짐(원인을 버린 이유 주석 있음). CS21·CS22 — 좁은 예외만 잡음, `decryptParts` try 몸통에 `Cipher.getInstance`·`init` 이 함께 있으나 Java 21 에서 AES-GCM 은 항상 있어 지적 안 함. CS14 위반 1건(C-04). CS1 의미 중복 1건(C-06). CS25 — 새 함수 전부 40줄·중첩 3단계 이하. C12 — 새 주석 전부 명사형 한 줄, 종결어미·`**` 없음. CS15 — TODO·FIXME 없음. CS24 — 상태 문자열 비교 없음(에러 code 는 생성자 상수).
  - failure-handling.md: Diff checks 4행 — 돈·주문·발송 POST, 웹훅, 메시지 소비자, 건별 루프 모두 이 diff 에 없음 → 해당 없음. FH1~FH6 해당 없음(토큰 발급은 외부 효과지만 락·재확인으로 1회화, 이 문서 범위 밖).
  - tests.md: TT1 — 새 소스마다 시험 있음. TT7·TT48 — 프로세스 밖 경계(토스 발급)만 `TossTokenIssuer` 계약에서 파생한 가짜로 대체. TT44 — `callCount` 단언은 «발급 1회»(RULE-06) 자체가 경계 계약이라 허용. TT16 — 시계·sleeper 주입, 락 TTL 시험만 실제 시간(실제 Redis TTL 이라 불가피). TT17 — 공유 컨테이너지만 시험마다 키 삭제, 병렬 실행 설정 없음. TT33 — 작성자 Red 기록만 있고 측정 불가(위 «수정 지킴 확인»).
  - 레포 rules/tests.md PTT1 — 락·TTL·Lua 해제·토큰 캐시를 Testcontainers `redis:7-alpine` 실제 엔진으로 시험. 충족.
  - always:dev-team — 재사용·함수 길이·주석·시험 먼저 위와 같음. 시간: epoch 밀리초만 다루고 벽시계 시간대 없음.
- 2. 사이드 이펙트 — `TossTokenManager` 는 아직 호출처 없음(빈 미등록, FLAG-14). 새 빈 `RedisDistributedLock`(@Component) 은 `StringRedisTemplate` 만 받음, 컨텍스트 기동 시험 통과 기록. `GlobalExceptionHandler` 에 `ServiceUnavailableException` 처리기 추가 — 기존 처리기와 형 겹침 없음(RuntimeException 직계), MVC 경유 시험 있음. API 계약: 새 code `TOSS_TOKEN_UNAVAILABLE`(503)·`TOSS_INTEGRATION_DISABLED`(500) 추가만, 기존 응답 변화 없음. `application.yaml` 새 키 7개는 환경변수, 필수 4개 빈 기본값. ktlint 기준선 15건 줄 번호 이동은 새 파일 기준으로 하나씩 맞춰 봄(앞 2건 +2, 나머지 +12 — import 1줄·`@Suppress` 1줄·새 처리기 10줄) — 일치. 레포 밖 서비스·앱 영향 없음.
- 3. 버그·장애 위험 — 보안: Redis 값은 `v1.<IV>.<암호문+태그>` 만, 로그·예외 문구에 토큰·secret·키 없음(`TossIssuedToken.toString` 도 가림), GCM 은 `SecureRandom` 12바이트 IV·128비트 태그·AAD=키 이름·버전 확인으로 맞음. 액추에이터는 노출 설정이 없어 configprops 값 노출 없음. 동시성: 소유자 UUID + Lua 해제로 TTL 만료 뒤 남의 락 해제 불가(시험 있음), 락 안 재확인 있음(시험 있음). 남은 위험은 C-01~C-03(발급·대기·저장 순서), C-04(설정), C-07(Redis 장애가 로그 없는 500), C-08(키 교체 중 인스턴스 간 토큰 폐기 반복).
- 조항을 어기지 않는 코드 품질 지적은 `should-fix` 이하로 둠.

## 2. 발견 (findings)

| ID | file:line | 렌즈 | 근거 조항 | 무엇이 틀렸나 | 등급 | 출처 | 회귀 | confidence | 구조 고침 | 재현 |
|---|---|---|---|---|---|---|---|---|---|---|
| C-01 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossTokenManager.kt:66-71 | 정확성 | label:redundant-work-per-item | `expires_in ≤ 갱신 여유`면 `issuer.issue()` 로 이미 발급한 토큰을 버리고 503. 토스는 클라이언트당 토큰 1개만 유효라, 발급 자체가 직전 토큰을 폐기함. 캐시가 계속 비므로 이후 모든 `getToken` 이 다시 발급하고 다시 503 → 토스 쪽 수명이 여유(기본 60초) 이하로 오면 요청마다 발급 호출(속도 제한 위험)하고 토스 연동 전체가 503 고정. 토스 문서에 만료 값이 없어 가능성을 배제할 수 없음. step-4 §8 은 «open-questions flag» 로 재확인한다고 적었으나 open-questions 에 해당 행 없음 | should-fix | 이 변경 | 아니오 | 중간 | TTL 이 0 이하이면 버리지 말고 줄인 TTL(예: `expires_in` 의 절반, 최소 1초)로 저장해 돌려주고 warn 1회. 처리 방식은 open-questions 에 행으로 올려 사람 확인 | — |
| C-02 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossTokenManager.kt:79-87 | 정확성 | label:required-state-not-designed | 락을 못 얻은 쪽은 한도(15초)까지 토큰 키만 읽고 락 재획득을 시도하지 않음. 락 보유자의 발급이 실패하면(`finally` 로 락은 풀림, 토큰 없음) 그 사이 들어온 대기자 전원이 락이 비었는데도 15초 내내 요청 스레드에서 `Thread.sleep` 하다 503. 토스 장애 때 토큰이 필요한 요청마다 서블릿 스레드가 15초씩 묶여 LOCAL·KIS 경로까지 스레드 고갈 위험. 작성자가 §8 에서 D11 문구를 근거로 step 5 판단으로 미룸(사람 결정 아님) | should-fix | 이 변경 | 아니오 | 중간 | 대기 루프에서 매 회 `readCached()` 가 비고 락 키도 없으면 `tryAcquire` 재시도(D11 문구 수정이라 사람 승인 필요) | — |
| C-03 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossTokenManager.kt:72-73 | 정확성 | label:concurrent-write-conflict | 저장 직전에 락을 아직 쥐고 있는지 확인하지 않음. 발급이 락 TTL(20초)을 넘기면(GC 정지·Redis 지연·step 5 발급기 재시도) 락이 만료돼 다른 인스턴스가 새 토큰 T2 를 발급·저장한 뒤, 늦게 끝난 쪽이 무조건 `set` 으로 T1 을 덮어씀. 토스에서 T1 이 먼저 발급돼 폐기된 상태면 Redis 에 폐기된 토큰이 남아 다음 401 까지 실패. 소유자 확인 해제(Lua)는 있지만 저장에는 같은 확인이 없음 | should-fix | 이 변경 | 아니오 | 중간 | 저장을 Lua 한 번으로: `GET lock == owner` 일 때만 `SET token PX` (락 소유 확인과 저장을 원자적으로) | — |
| C-04 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossProperties.kt:25-28 | 정확성 | CS14 | 선택 키 3개를 환경변수로 덮을 수 있는데 범위 검증이 없음. `TOSS_TOKEN_LOCK_TTL_MILLIS` 를 15000 이하로 주면 주석이 말하는 «연결 5초 + 읽기 10초보다 길게»(D11) 불변식이 조용히 깨져 발급 도중 락 만료 → 이중 발급 → `token-revoked`. `TOSS_TOKEN_REFRESH_SKEW_SECONDS` 가 음수면 TTL 이 `expires_in` 보다 길어져 만료된 토큰을 캐시 적중으로 돌려줌. 시험은 기본값만 확인(`TossPropertiesTest.kt:128-130`) | should-fix | 이 변경 | 아니오 | 높음 | `disabledKeyNames()` 판정에 범위 위반 키 이름을 더해 토스만 비활성(EDGE-01 과 같은 길), 또는 `@Validated` + 하한 | — |
| C-05 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossProperties.kt:43-46 | 성능 | label:redundant-work-per-item | `getToken`·`invalidate` 마다 `requireEnabled()` → `disabledKeyNames()` 가 목록을 새로 만들고 키를 base64 디코드한 뒤 `AesGcmTokenCipher`(새 `SecureRandom` 포함)를 매번 생성. 설정은 기동 때 한 번 묶이므로 토스 API 호출마다 같은 일을 되풀이함 | nit | 이 변경 | 아니오 | 높음 | 활성 판정을 `@PostConstruct` 에서 한 번 계산해 보관 | — |
| C-06 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossTokenManager.kt:93-96 | 유지보수성 | CS1 | «값이 같을 때만 DEL» Lua 가 `RedisDistributedLock.kt:27-30` 의 `RELEASE_SCRIPT` 와 글자까지 같음. 한쪽만 고치면 갈라짐 | nit | 이 변경 | 아니오 | 높음 | `RedisDistributedLock` 에 `deleteIfEquals(key, expected)` 를 두고 `release` 와 `invalidate` 가 함께 씀 | — |
| C-07 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/exception/GlobalExceptionHandler.kt:95-99 | 정확성 | AE21 | 잡이 처리기(`Exception`)가 500 만 돌려주고 기록하지 않음. 이 변경으로 `TossTokenManager` 의 Redis 장애(`RedisConnectionFailureException` 등)가 그대로 여기로 와 응답·로그 어디에도 원인이 안 남음. 고친 줄이 아니라 기존 줄 | should-fix | 기존 | 아니오 | 높음 | 잡이 처리기에서 `log.error(ex)` 1회 | — |
| C-08 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/toss/TossTokenManager.kt:57-63 | 정확성 | label:required-state-not-designed | 복호화 실패 = 캐시 없음 → 재발급(D10). 롤링 배포로 키를 바꾸는 동안 옛 키·새 키 인스턴스가 함께 돌면 서로의 암호문을 못 읽어 각자 재발급 → 토스에서 상대 토큰 폐기 → 상대의 `invalidate` 도 복호화 실패로 false → 다시 재발급, 공존 시간 내내 반복. 대기자는 100ms 마다 같은 warn 을 남김. 암호문 형식에 키 식별자가 없어 이전 키로 읽는 길이 없음 | should-fix | 범위 밖 | 아니오 | 낮음 | 형식에 키 식별자를 넣고 이전 키 1개로 복호화 허용(D10 설계 변경, 사람 결정) | — |
| C-09 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/exception/GlobalExceptionHandler.kt:41 | 보안 | label:rule-lives-in-prose-not-tool | 처리기가 모든 `ServiceUnavailableException` 의 `ex.message` 를 응답에 그대로 실음. «토스 원문은 응답에 싣지 않음»(D15)은 주석과 하위 예외 작성자의 습관에만 있고, `TossTokenUnavailableException(reason: String)` 도 아무 문자열을 받음. step 5 에서 원인 예외 문구를 `reason` 으로 넘기면 토스 원문이 응답으로 나감. 지금 문구에 비밀값은 없음 | nit | 이 변경 | 아니오 | 중간 | 하위 예외가 고정 문구만 받도록(사유를 enum·상수로) 하거나 처리기가 code 별 고정 문구를 씀 | — |

## 3. 범위 밖 (안 본 것)

- `TossTokenIssuer` 실제 HTTP 구현·401 재시도(D12 나머지)·`TossApiClient` — step 5, 이 diff 에 없음.
- 사람이 정한 것: 선택 키 기본값(FLAG-13), 빈 등록 시점(FLAG-14) — 지적 대상에서 뺌. C-04 는 기본값이 아니라 덮어쓴 값의 범위 검증.
- 시험 실행: 지시대로 전체 스위트는 안 돌림. 개별 시험도 샌드박스에서 Gradle·Docker 가 막혀 못 돌려 작성자 xml 집계(71건, 실패 0)를 근거로 둠.
- `docs/` 문서(state.md·open-questions.md·tech-plan-approval.md·progress.md) 변경은 코드 리뷰 범위 밖. 단 C-01 의 open-questions 행 누락은 코드 동작과 묶여 적음.

<!-- devkit:standard-list hex:7b22636861696e223a22646f63732f66656174757265732f72756e6e696e672f6d756c74692d62726f6b65722d6b69732d746f73732f7265766965772f73746570342d636f64652d7265766965772e6d64222c22726f756e6473223a7b2231223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2239313566313166653261646266313231353537323039333162616663653966373461333064633536227d7d7d -->
