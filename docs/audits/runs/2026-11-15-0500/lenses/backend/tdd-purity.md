---
lens: backend/tdd-purity
dept: backend
agent: backend-auditor
title: 시험 품질 (JUnit5·MockK·Testcontainers)
repo-checks: T13
target-path: /src/test/kotlin/
concerns: 테스트
---
# Lens — 시험 품질 (backend · Kotlin/JUnit5/MockK/Spring Boot Test/Testcontainers)

## Standard
- 레포 규칙 [tests.md PTT1](../../../../../../rules/tests.md) — PostgreSQL·Redis 어댑터의 쿼리·락·TTL·제약은 Testcontainers 로 띄운 실제 엔진으로 시험한다. H2·인메모리 가짜·리포지토리 목으로 이 동작을 대신 검증하지 않는다. Exception: 도메인 모델·유스케이스 단위 시험은 포트를 가짜로 바꿔 엔진 없이 돌린다.
- 레포 규칙 [backend-api.md PTB8](../../../../../../rules/backend-api.md) — 유스케이스는 포트만 주입받는다 → 단위 시험의 가짜는 포트 구현이어야 한다(Spring Data 리포지토리 목이 아님).
- 레포 규칙 PTB4 — `BigDecimal` 은 `compareTo` 로 비교 → 시험 단언도 같다(`assertEquals(BigDecimal("1.0"), x)` 는 scale 까지 비교).
- 레포 규칙 [data-access.md PTD3·PTD4](../../../../../../rules/data-access.md)(PTD4 는 PR #50 판본) — 동시 갱신 보호와 `AFTER_COMMIT` + `REQUIRES_NEW` 는 실제 트랜잭션에서만 드러나므로 시험도 실제 커밋 경로를 탄다.
- devkit 팀 규칙 `rules/dev-team/tests.md` — TT6·TT7 가짜는 외부 경계에서만 · TT44 구현(private·호출 순서)에 묶지 않음 · TT48 가짜는 실제 계약에서 유도 · TT16 결정성 · TT17 격리 · TT20 시험 하나 = 행동 하나 · TT21 이름 = 상황_행동_기대 세 토막(한글 허용) · TT23 분기 커버리지 · TT26 가짜 초록 금지 · TT40 시험 먼저.
- devkit 팀 규칙 `rules/dev-team/common.md` CS7 — DB·HTTP·SDK 경계에 포트를 두어 가짜가 설 수 있게.
- 우아한형제들 기술 블로그 — 시험 이름은 정책을 문장으로 → T6 — https://techblog.woowahan.com/5825/
- 강남언니 기술 블로그 — 가짜 Redis 가 음수 TTL 운영 버그를 가린 사례(PTT1 근거).
- 외부 경계 허용 목록(T1·T3): `infrastructure/kis/**`·`infrastructure/notification/SlackWebhookNotifier`·`infrastructure/client/**` 의 HTTP·WebSocket, `RestTemplate`·`SseEmitter`·`StringRedisTemplate`. 그 밖의 프로젝트 클래스(서비스·도메인 엔티티·리포지토리)는 내부다.
- 알려진 규칙 충돌: PTT1 Exception 은 유스케이스 단위 시험에서 포트 가짜를 허용하는데, 이 레포는 아직 포트 대신 Spring Data 리포지토리를 유스케이스에 주입한다(PTB8 기존 위반). 그래서 `mockk<AccountRepository>` 는 «포트 가짜»가 아니라 T2 발견이다 — 사람이 «포트 이전 전까지 허용»으로 고르면 `ruleConflicts` 행으로 남긴다.

## Deterministic first
| Tool | Command | Proves | If absent |
|---|---|---|---|
| ArchUnit | `cd backend/trading-api && ./gradlew test --tests '*ArchitectureRulesTest*' --console=plain` | 아키텍처 시험이 실제로 수집·실행됨(JUnit XML 의 tests 수) — T13 의 근거 | 읽는다 |
| grep (목 대상) | `grep -nE 'mockk<[A-Za-z]+>\|@MockkBean\|@MockBean\|@MockitoBean\|every \{\|verify \{\|verifyOrder\|verifySequence' <chunk-files>` | T1·T2·T3 후보 줄 | 시험 파일을 읽는다 |
| grep (비결정·엔진 대체) | `grep -nE 'Thread\.sleep\|\.now\(\)\|randomUUID\|H2\|jdbc:h2\|@DataJpaTest\|@AutoConfigureTestDatabase\|PostgreSQLContainer\|GenericContainer\|@ServiceConnection' <chunk-files>` | T4·T5·T9 후보 줄 | 읽는다 |
| grep (BigDecimal 단언) | `grep -nE '(assertEquals\|assertThat\|shouldBe\|isEqualTo)[^\n]*BigDecimal' <chunk-files>` | T8 후보 줄 | 읽는다 |

명령 칸의 `\|` 는 `|` 의 마크다운 이스케이프다. `<chunk-files>` = 내 인스턴스의 파일 목록.

## Checks
| ID | Signal — what you see in the file | Why it matters | Default severity |
|---|---|---|---|
| T1 | 내부 협력자(도메인 엔티티·같은 레포 서비스·`*Calculator`·`*Evaluator`)를 `mockk<…>`·`@MockkBean`·`@MockBean`·`@MockitoBean` 으로 갈음하거나 `every { entity.status } returns …` 로 도메인 객체 동작을 지어낸다(외부 경계 허용 목록 제외) (TT6·TT7) | 시험이 구현에 묶여 리팩터링마다 깨지고, 가짜 엔티티는 불변식을 안 탄다 | high |
| T2 | 유스케이스 단위 시험이 포트 가짜 대신 Spring Data 리포지토리(`mockk<OrderRepository>`·`mockk<AccountRepository>`)를 목으로 쓰며 그 목의 반환으로 쿼리 동작(필터·락·정렬·유일 제약 위반)을 흉내 낸다 (PTB8·PTT1) | 쿼리·락의 실제 동작은 한 번도 시험되지 않고, 목이 «있다고 믿는» 쿼리 결과가 운영과 갈린다 | med |
| T3 | `verify { … }`·`verify(exactly = n)`·`verifyOrder`·`verifySequence` 로 내부 협력자 호출 횟수·순서를 단언하는 것이 결과 단언을 대신한다; private 필드를 리플렉션으로 읽어 단언한다 (TT44) | 상호작용을 시험해 행동이 틀려도 초록이다 | med |
| T4 | 시험에 `Thread.sleep(`; `LocalDateTime.now()`·`LocalDate.now()`·`Instant.now()` 를 고정된 `Clock` 없이 단언과 비교; `UUID.randomUUID()`·`Random` 값을 단언에 씀; 비동기(`@Async`·`SseEmitter`·스케줄러) 결과를 고정 대기로 기다림 (TT16) | 비결정 = 깜빡임 = 기능 버그. 자정·연말 경계(정산일·세금연도)에서만 깨진다 | high |
| T5 | PostgreSQL·Redis 어댑터(리포지토리 `*Impl`·`@Query`·`@Lock`·`@Modifying`·`@Version`·Redis 어댑터)의 시험이 H2(`jdbc:h2`·`@AutoConfigureTestDatabase(replace = ANY)`)·인메모리 가짜·목으로 쿼리·락·TTL·제약을 검증한다, 또는 그런 어댑터에 Testcontainers 시험이 아예 없다 (PTT1) | H2 는 `FOR UPDATE SKIP LOCKED`·격리 수준·부분 인덱스·시퀀스를 흉내 못 내 통과한 시험이 운영에서 깨진다 | high |
| T6 | 시험 이름(백틱 이름 또는 `@DisplayName`)이 상황·행동·기대 세 토막이 아니다 — `test1`·`성공`·`works`·`정상 동작`·대상 메서드 이름만 (TT21) | 빨간 시험이 어떤 정책이 깨졌는지 말하지 못해 진단 비용을 치른다 | low |
| T7 | 단언이 0개인 `@Test`(예외 없음만 확인, `assertDoesNotThrow` 만 예외로 인정), `@Disabled` 에 이유 문자열 없음, 본문 맨 위 `return` (TT26) | 가짜 초록 — 그 경우를 더는 지키지 않는다 | high (단언 없음) · med (`@Disabled`) |
| T8 | `BigDecimal` 단언이 `assertEquals`·`isEqualTo`·`shouldBe`·`==` 로 scale 까지 비교하거나(`isEqualByComparingTo`·`compareTo` 가 아님), 기댓값을 `BigDecimal(0.1)`·`Double` 로 만든다 (PTB4) | scale 만 달라도 빨강, 또는 `Double` 기댓값이 이진 오차를 정답으로 굳힌다 — 금액 시험이 금액을 지키지 못한다 | med |
| T9 | 시험 격리 깨짐: `@SpringBootTest` + 실제 DB 시험이 `@Transactional` 롤백·`@Sql` 정리·truncate 없이 행을 남김; `companion object` 의 가변 상태를 시험끼리 공유; `@TestMethodOrder` 로 순서에 기댐 (TT17) | 혼자는 통과하고 CI 에서 순서에 따라 깨진다 | high |
| T10 | 커밋 경로를 우회한 시험: `AFTER_COMMIT` 리스너·`REQUIRES_NEW`·`@Version` 충돌·`saveAndFlush` 예외 번역을 검증한다면서 클래스 전체 `@Transactional`(커밋 안 함) 안에서 돌리거나 리스너를 직접 호출한다 (PTD3·PTD4 PR #50) | 커밋이 일어나지 않으면 `AFTER_COMMIT` 은 실행되지 않고 버전 충돌도 안 난다 — 시험이 거짓 초록 | high |
| T11 | 손으로 베낀 가짜: `object : SomePort { … }`·`Fake*` 클래스·`mockk` 반환값이 외부 응답(KIS JSON·Slack 응답) 필드·코드 문자열을 그대로 적었는데 그 값의 출처(KIS 문서·녹취 픽스처 파일)가 시험에 없다 (TT48) | 시험과 코드가 같은 믿음을 복사해 둘 다 초록인 채 외부 계약과 어긋난다 | med |
| T12 | 시험 하나가 대상 호출 2번 이상에 각자 단언을 달거나 단언 8개 이상 (TT20) | 첫 실패가 나머지를 가리고 이름이 둘을 못 담는다 | low |
| T13 | 레포 단위: 분기 커버리지 도구(JaCoCo·Kover)가 `build.gradle` 에 없거나 분기 기준 없이 줄 기준만; 로직(`if`·`when`·`for`·`try`)이 있는 `application/**`·`domain/**` 클래스 중 같은 이름 `*Test`·`*IntegrationTest` 가 없는 것의 목록 — `(repo)` 에 기록 (TT23·TT40) | 회귀선 없는 로직이 silent-wrong 의 자리다. 측정 없는 커버리지는 주장일 뿐이다 | med |

## Disprove before reporting
- T1·T3: 허용 목록의 외부 경계(`RestTemplate`·`SseEmitter`·KIS 클라이언트·Slack 노티파이어) 목과 그 호출 검증은 허용된 런던식 사용 — 통과.
- T2: 포트 인터페이스(`*Port`)의 가짜·목은 PTT1 Exception — 통과.
- T4: `Clock.fixed(...)` 를 주입하고 그 시각과 비교하면 통과. 실패 시험의 `Thread.sleep` 이 아니라 Testcontainers 기동 대기는 대상 아님.
- T5: 컨트롤러 슬라이스(`@WebMvcTest`)의 서비스 목은 T5 가 아니다(영속 동작을 검증하지 않음).
- T6: 한글 백틱 이름은 허용(TT21). 세 토막이 한 문장으로 녹아 있으면 통과.
- T9: 클래스에 `@Transactional` 이 있어 매 시험 롤백되면 격리는 통과 — 단 T10 에 해당하는지 따로 본다.

## Out of scope
- 제품 코드의 `now()`·`randomUUID()` 직접 읽기 → code-shape F4. 제품 코드 구조 → layering·code-shape.
- 시험 대상 코드의 트랜잭션 모양 → data/transaction-boundary(T10 은 «시험이 커밋을 우회함»만).
- 외부 호출 재시도·타임아웃 → reliability/failure-handling.
