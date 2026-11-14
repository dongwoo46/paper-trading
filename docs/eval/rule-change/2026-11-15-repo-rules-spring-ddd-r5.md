# 레포 규칙 5차 수정 — 독립 리뷰 지적 반영 증거 (2026-11-15)

대상: `rules/data-access.md`(PTD1~PTD5·포인터 signal·머리말), `rules/backend-api.md`(PTB4·PTB6~PTB9·머리말), `rules/failure-handling.md`(PTF5·머리말), `rules/async-external.md`(PTA2·머리말), `rules/tests.md`(PTT1), 증거 문서 r4·1차.

출처: PR #48 독립 리뷰(2026-11-15, 약화 1건·should-fix 12건·nit 7건)와 사람 결정 2026-11-15. 사람 결정은 네 가지다. 1. 지적을 전부 이 PR 에서 고친다. 2. 매칭은 복구가 필요하다(시세 메시지는 유실 허용, 미체결 지정가·청산 조건은 주기 재대조로 복구). 3. 계층 지도는 엄격하게 적는다(presentation→domain 은 enum·값 타입만, infrastructure→application 은 port 패키지만, common 은 domain 과 같이 Spring 금지. r6 정정: «값 타입»은 판정 불가라 `domain.enums`·`domain.event` 패키지로 고침). 4. 여러 컨텍스트가 같이 쓰는 포트는 `application/common/port`.

조항을 지운 것이 아니라 고쳤다. 조항 Why 줄은 모두 남기고 고쳤다. 팀 규칙 모순 대조 표는 1차 증거 문서(`2026-11-15-repo-rules-spring-ddd.md`)의 바뀐 조항 행을 이번에 갱신했다.

## 지적별 처분 표

| 지적(리뷰 번호) | 처분 | 고친 조항 | 근거 |
|---|---|---|---|
| 팀 규칙이 Kotlin 에 전달되지 않는데 머리말은 SSoT 라고만 함(S1) | 고침 | 네 문서 머리말 | 머리말에 «자동 전달되지 않음»과 직접 열어 따를 팀 조항을 적음. data-access 는 DA11·DA19·DA23, backend-api 는 DD7·AE5·AE8, async-external 은 LA11·LA21·LA22, failure-handling 은 FH1·LA11. 각 ID 는 devkit 0.95.0 `rules/dev-team` 원문에서 실재 확인. devkit 쪽 Kotlin 신호 추가는 메인 세션이 제안으로 올린다고 한 줄 적음 |
| 포인터 signal 이 Querydsl 코드를 못 잡음(S2) | 고침 | data-access 포인터 signal, why | `JPAQueryFactory`·`^import .*\.Q[A-Z]\w+`·`\.fetchJoin\(\)`·`\.offset\(` 추가(r6 정정: Q-import 대안은 `^` 가 파일 맨 앞에만 맞아 영원히 안 걸렸다. rule-pointer 는 signal 을 플래그 `i` 하나로 컴파일한다. r6 에서 `import com\.papertrading\.api\.domain\.entity\.\w+\.Q[A-Z]\w+` 로 고쳐 Q-import 파일 9개 모두 node 로 매칭됨을 확인). 실측: `JPAQueryFactory` 를 쓰는 파일 10개(`QueryDslConfig` 1 + `*RepositoryImpl` 9), `.offset(` 은 `TradingJournalRepositoryImpl.kt:23`·`AccountLedgerRepositoryImpl.kt:38`, `.fetchJoin()` 은 `OrderRepositoryImpl.kt:71` 등 |
| `@Modifying` 에 clear 만 지시(B1) | 고침 | PTD5 | `flushAutomatically = true, clearAutomatically = true` 를 함께. 근거 `PositionRepository.kt:32` 가 이미 두 옵션을 씀. Why 에 flush 없이 clear 하면 같은 트랜잭션의 다른 엔티티 변경이 버려진다고 적음 |
| Why 가 실제 구성과 다름, 순서·상한 없음(B2) | 고침 | PTA2 | `RedisListenerConfig.kt:17` 이 `RedisMessageListenerContainer()` 에 실행기를 지정하지 않아 기본 `SimpleAsyncTaskExecutor` 가 메시지마다 새 스레드를 만든다. Why 를 스레드 무제한과 같은 종목 순서 뒤바뀜으로 고치고, 지시문에 상한 있는 실행기(팀 LA21)와 종목 키별 직렬 처리 또는 `updatedAt` 낡은 값 버리기를 넣음 |
| 시세가 유실 허용인지 복구 대상인지 판정 불가(B3) | 고침 | PTA2 | `QuoteEventListener.kt:34` `tryMatchPendingOrders`, `:49` `onQuote` 가 시세 메시지에서 매칭·청산을 촉발한다. 사람 결정에 따라 «메시지는 유실 허용(LA22 이 인정), 촉발되는 매칭·청산은 주기 재대조로 복구»로 가름. 현재 코드에는 이 복구 경로가 없다: `LocalMatchingScheduler` 의 설명 주석은 재매칭을 말하지만 `@Scheduled` 는 `cancelExpiredGtdOrders` 하나뿐(실측, `@Scheduled` 는 이 파일과 `SlackNotificationScheduler` 둘뿐)이라 규칙은 구현이 따라올 목표이지 기존 사실이 아니다 |
| PTD3 재시도와 PTB9 어댑터 변환이 충돌(B4) | 고침 | PTD3, PTB9 | 영속 예외는 어댑터가 쓰기 뒤 `saveAndFlush`·`flush` 로 어댑터 안에서 일으켜 애플리케이션 예외로 바꾸고, 재시도는 트랜잭션을 연 유스케이스 메서드를 부르는 바깥 경계에서 그 예외를 잡아 새 트랜잭션으로 한다. 현재 application 이 `DataIntegrityViolationException` 을 직접 잡는 파일은 `SnapshotJobService.kt:71`, `TaxSummaryCommandService.kt:68`, `PositionExitTriggerOrchestrator.kt:155` 3개(기존 위반으로 PTB9 Why 에 적음) |
| 반열림 규칙이 이산 날짜·연도까지 덮음(B5) | 고침 | PTF5 | 대상을 `Instant`·`LocalDateTime` 연속 시각으로 좁히고, `LocalDate`·연도는 닫힌 구간이 맞다고 적음. `...Between` 파생 메서드와 Querydsl `.between(` 도 대상에 포함. 근거 `DailyBalanceRepositoryImpl.kt:17`, `TaxSummaryRepositoryImpl.kt:28`, `DailyBalanceRepository.kt:10` |
| 계층 지도에 의존 세 갈래가 빠짐(B6) | 고침 | PTB7 | 허용·금지 방향을 모두 열거. common 은 domain 과 같이 Spring 금지(`common/exception/ApiDomainException.kt:3`·`CommonApiExceptions.kt:3` 의 `HttpStatus`, domain 의 `Account.kt`·`PositionExitTrigger.kt` 가 common 예외를 씀), presentation→domain 은 enum·값 타입만(실측 14파일 중 엔티티 import 5파일), infrastructure→application 은 port 패키지만(18파일 중 port 밖 import 14파일). r6 정정: port 밖 import 는 `application.common.result` 1파일을 포함해 15파일이고 유스케이스 서비스를 직접 호출하는 파일은 5가 아니라 4개다(`SlackNotificationEventHandler` 는 이벤트 타입만 import). presentation→domain 은 `domain.entity` 5파일, `domain.event` 1파일, 나머지 `domain.enums` 로 세어 «값 타입»을 패키지로 바꿈. 이 지도를 따르려면 유스케이스를 호출하는 스케줄러·Redis 리스너(infrastructure 의 5파일)의 진입점을 presentation 쪽에 두어야 하므로 그것도 한 문장으로 적었다. 이 한 문장은 사람 결정에서 이어진 귀결을 내가 적은 것이라 사람 확인이 필요하다(보고에 올림) |
| Kotlin `/` 나눗셈 함정과 값 객체 scale 이 빠짐(B7) | 고침 | PTB4 | `/` 는 `divide(other, RoundingMode.HALF_EVEN)` 이라 피제수 scale 로 반올림(`BigDecimal("10") / BigDecimal("4")` 는 2)하므로 금지하고 `divide(x, scale, RoundingMode)` 만 허용. Why 의 «예외를 던진다»를 scale 없는 `.divide(x)` 한정으로 정확히 고침. 값 객체는 `init` 에서 scale 정규화나 `compareTo` 기반 `equals`. 실측 `.divide(` 호출: trading-api 11건 중 9건이 scale·반올림을 명시하고(`Position.kt:155` 등), 2건은 `percent.divide(HUNDRED)`(`PositionExitTriggerCommandService.kt:140`, `PositionExitTriggerEvaluator.kt:71`)로 scale 이 없다. 100 으로 나누는 값은 종료 소수라 예외가 나지 않지만 지시문 글자와는 어긋나므로 기존 위반 후보다. collector-api 는 scale·반올림 명시(`OrderbookRedisStore.kt:28` 등) 또는 `MathContext`(지표 계산기 `RsiCalculator.kt:23` 등)라 지시문에 `MathContext` 형태도 허용으로 적음. Kotlin `/` 연산자 사용처는 이번에 측정하지 않았다(grep 으로 `BigDecimal` 피연산자를 가를 수 없음) |
| AFTER_COMMIT 에서 REQUIRES_NEW 가 쓰기를 한정하지 않아 DA6 을 약화(B8, 약화 1건) | 고침 | PTD4, 1차 표 PTD4 행 | 새 트랜잭션은 그 리스너가 한 외부 부수효과의 결과 기록(DA1·DA9 3단계)에만 허용하고, 본 쓰기와 같은 DB 의 부수 쓰기는 AFTER_COMMIT 에 두지 않는다고 명시(DA6·CS11 과 일치). 약화 지적이었으므로 문장을 팀 문면 안으로 좁혔다 |
| 머리말 «세 조항»과 실제 조항 수 불일치(B9) | 고침 | failure-handling 머리말 | 머지 뒤 지울 조항 PTF1~PTF5 와 남길 조항 PTF6 을 ID 로 적음. PTF1~PTF3 은 제안 financial-integrity-eda-followup-2026-11-15, PTF4·PTF5 는 BiocomKR/devkit#2471 2차(1차 증거 표 PTF4·PTF5 행), PTF6 은 어느 제안에도 없음 |
| 여러 컨텍스트가 쓰는 포트의 자리 없음(B10) | 고침 | PTB8 | `application/common/port`(사람 결정). 실측: `MarketQuotePort` 는 order·portfolio·position 세 컨텍스트(`OrderCommandService`·`PositionQueryService`·`PortfolioSnapshotCommandService`)가 씀. PTB7 의 port 패키지만 import 규칙 때문에 포트 시그니처 타입도 같은 port 패키지에 두라고 적음(이 한 줄도 귀결이라 사람 확인 대상) |
| PTD2 대상이 팀 DA1 보다 좁음(N1) | 고침 | PTD2 | «열린 트랜잭션 안(트랜잭션 안 리스너, 그 둘이 부르는 어노테이션 없는 메서드 포함)» |
| PTD1 이 리스너·`@Scheduled` 를 포함하는지 불명(N2) | 고침 | PTD1 | «프록시를 거쳐 호출되는 public 메서드(다른 빈이 부르는 메서드, Spring 이 부르는 리스너·`@Scheduled`)» |
| 읽기 전용 List 는 뷰라 캐스팅으로 고쳐짐(N3) | 고침 | PTB6 | 복사본(`toList()`·`toMap()`)으로 내준다고 지시문을 고치고 Why 에 뷰 위험 추가 |
| Testcontainers 는 trading-api 만 맞음(N4) | 고침 | PTT1 | trading-api 는 `build.gradle:47` 에서 `org.testcontainers:postgresql` 사용, collector-api 는 `build.gradle.kts` 에 Testcontainers 가 없고 `mockk<StringRedisTemplate>`(`MarketBarRedisRepositoryTest.kt:26` 등)을 씀. 모듈 범위를 적고 collector-api Redis 어댑터 시험은 옮길 대상이라고 적음 |
| 1차 증거 표 중간 빈 줄(N5) | 고침 | 1차 증거 문서 | PTF3 행과 PTF4 행 사이 빈 줄 삭제 |
| 1차 증거 표 대조 부정확(N6) | 고침 | 1차 증거 문서 PTD5·PTT1·PTB9 행 | PTD5 는 DA23 과 맞닿음, PTT1 은 TT7(프로세스 안은 진짜나 가짜, 프로세스 밖만 대역)과의 대조로 다시 씀, PTB9 는 잠금 원칙 «프레임워크는 바깥 세부»보다 `@Service`·`@Transactional` 을 허용하는 만큼 느슨하다고 적음 |
| r4 포트 수가 파일 수(N7) | 고침 | r4 증거 문서 | 파일은 5개·3개, 인터페이스는 4개·2개로 구분해 표기(`ls` 실측, `LivePositionSnapshot`·`SlackNotificationRuntimeConfig` 는 data class) |

## 약화 판정

PTD4 둘째 문장의 약화 1건은 팀 문면(DA6·CS11 은 커밋하지 않음, 허용은 DA1·DA9 3단계의 결과 기록)에 맞춰 좁혔다. r5 는 그 밖의 수정이 모두 조항을 팀 문면보다 엄격하게 하거나 이 레포 사실에 맞추는 방향이라고 적었으나 이는 틀렸다(r6 정정). PTA2 가 약화였다. 이전 문장은 «돈이나 주문 상태를 바꾸는 처리는 복구 경로»라고만 해서 시세 메시지가 거기에 드는지 판정 불가였고, r5 는 «시세 메시지는 유실 허용»을 명시하면서 복구 요구를 «시세가 촉발하는 매칭·청산»으로만 좁혔다. 그 결과 옛 일반 조항이 사라져 시세 외 메시지(체결·잔고·신호)를 받는 새 구독자에게 복구를 요구하는 지시가 없었다. r6 에서 일반 조항을 되살렸다.

## 삭제 줄 대조

지운 줄은 모두 같은 자리의 줄을 다시 쓴 것이다(조항 본문과 Why 줄을 고침). 조항을 없앤 것은 0개다. `rule-change-gate` 가 자리 수정을 뺀 순삭제로 잡은 줄은 둘이고, 줄마다 따로 판정한다.

- PTA2 의 옛 Why 줄 «Why: 리스너 스레드가 막히면 다음 메시지가 밀리고, 재시작·네트워크 단절 중 시세나 신호는 흔적 없이 사라진다. 시세처럼 다음 값이 곧 덮어쓰는 데이터는 유실을 받아들여도 되지만, 체결·잔고는 안 된다. — enforce: review»
  → 소실: «리스너 스레드가 막히면 다음 메시지가 밀린다» 한 구절. 이 레포는 `RedisListenerConfig.kt:17` 에서 실행기를 지정하지 않아 기본 `SimpleAsyncTaskExecutor` 가 메시지마다 새 스레드를 만들기 때문에 사실과 달라서 의도적으로 뺐고, 실제 위험(스레드 수 무제한, 같은 종목 순서 뒤바뀜)으로 바꿔 적었다(리뷰 B2).
  → 잔존(r6 정정: 일부만 잔존이었다. 옛 Why 의 «신호»와 «체결·잔고는 안 된다», 옛 지시문의 일반 복구 문장은 r5 에서 소실됐고 r6 에서 되살림): «재시작·네트워크 단절 중 시세는 흔적 없이 사라진다»는 PTA2 새 Why 의 «구독자가 끊긴 동안의 메시지를 버리는 통로(at-most-once)라 재시작·네트워크 단절 중 시세는 흔적 없이 사라진다». «시세는 유실을 받아들여도 된다, 체결·잔고는 안 된다»는 지시문의 «시세 메시지 자체는 유실을 허용한다 … 시세가 촉발하는 매칭·청산 판정은 … 복구하는 경로를 둔다»와 Why 의 LA22 문장으로 남았다(체결·잔고를 복구 대상으로 보는 뜻은 같고, 매칭·청산을 복구 쪽에 넣어 판정 가능하게 했다, 리뷰 B3). `— enforce: review` 도 그대로다.
- PTB7 의 옛 Why 줄 «Why: 팀 규칙 backend-api 잠금 원칙(의존은 안쪽으로)과 DD14(계층 건너뛰기 금지)를 이 레포 패키지 이름으로 적은 지도다. 지도가 없으면 DD14 위반을 판정할 기준이 없다(실측 2026-11-15: application→infrastructure 28파일, infrastructure→presentation 2파일). 사람 결정 2026-11-15: 클린 아키텍처(포트·어댑터). — enforce: archunit · review»
  → 잔존: 새 PTB7 Why 가 첫 문장(팀 규칙 backend-api 잠금 원칙(의존은 안쪽으로)과 DD14(계층 건너뛰기 금지)를 이 레포 패키지 이름으로 적은 지도)을 그대로 두고, 실측 수치(application→infrastructure 28파일, infrastructure→presentation 2파일)를 새 실측 항목들과 같이 적었으며, 「사람 결정 2026-11-15: 클린 아키텍처(포트·어댑터)」와 `— enforce: archunit · review` 도 남겼다. 바뀐 것은 «지도가 없으면 판정 기준이 없다» 문장이 «지도에 빠진 방향은 판정 기준이 없어 PTB1 을 우회한다»로 구체화된 것뿐이고, 의미는 줄지 않았다(리뷰 B6).

## 공식 근거 대조

근거 문서: devkit `docs/research/ai-doc-official-guidance.md` §1~§4.

- 이유(rationale) 보존: 고친 조항 모두 `Why:` 줄을 남기고, 새로 정해진 내용마다 실패 메커니즘을 Why 에 더했다(flush 없는 clear, 스레드 무제한과 순서 뒤바뀜, 어댑터 밖 flush 의 커밋 시점 예외, 반열림 off-by-one, `/` 의 조용한 반올림, 계층 우회 경로). §1 「explaining the why behind constraints」와 devkit 실측(근거를 지운 판본 준수 90% → 10%)을 따랐다.
- 강조 서식: 다섯 규칙 파일에 굵게, IMPORTANT, CRITICAL, MUST 가 0개다(grep 확인). §2 「piling on emphasis mostly makes Claude over-apply the rule」에 맞춰 평문 명령형으로 썼다.
- 골격(헤딩·불릿): 헤딩 아래 `gt:` 앵커와 불릿 한 줄 조항과 `Why:` 줄 구조(form v2)를 그대로 유지했다. PTB7 은 허용 목록과 금지 목록을 한 조항 안에서 열거해 판정할 수 있게 했다. §3 「group related instructions under markdown headers and bullets」를 따랐다.
- 분량 판정: 규칙 파일 합계가 200줄 상한 아래임을 `wc -l` 로 확인했다(r5 시점 184줄, r6 시점 188줄). 모델이 이미 아는 일반 설명은 넣지 않았고, 새로 늘어난 글은 이 레포 코드에서 확인한 값(패키지 이름, 파일 수, 줄 번호, 설정 이름)과 컴파일을 통과하고도 조용히 틀리는 함정에 한했다. 머리말의 «자동 전달되지 않음» 두 줄은 모델이 코드만 보고는 알 수 없는 사실이라 넣었다.
- 모순: §4 「if two instructions contradict each other, Claude may pick one arbitrarily」에 따라 PTD3 와 PTB9 의 충돌(B4), PTA2 안의 유실 허용과 복구 요구의 충돌(B3)을 해소했고, 바뀐 조항의 팀 규칙 대조는 1차 증거 문서 표를 갱신해 다시 했다.

## 검증

- 레포 사실은 이 worktree 의 `backend/trading-api`·`backend/collector-api` 소스를 직접 열거나 `grep -rn` 으로 확인했다(파일·줄 번호는 위 표). 기능 코드는 바꾸지 않았다.
- `rule-pointer --list` 를 바꾼 파일에 돌려 레포 규칙이 걸리는지 확인했다(r6 시점 `QuoteEventListener.kt`·`TradingJournalRepositoryImpl.kt` 에 async-external·backend-api·failure-handling·data-access 네 규칙이 모두 걸림).
- 이 규칙의 강제 도구(ArchUnit 동결 목록, ktlint, detekt)는 `chore/trading-api-arch-guard` 브랜치의 별도 PR 이 맡는다.
