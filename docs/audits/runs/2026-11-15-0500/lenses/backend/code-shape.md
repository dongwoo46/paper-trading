---
lens: backend/code-shape
dept: backend
agent: backend-auditor
title: 코드 모양 (Kotlin 함수·클래스 크기·SRP·금액 연산)
repo-checks: S11
target-signal: ^(?![\s\S]*(import org\.junit|import io\.mockk|@ArchTest))[\s\S]*\n[ \t]*((private|internal|protected|public|override|open|suspend|inline|abstract|operator|infix|tailrec)[ \t]+)*fun[ \t]
concerns: 구조
---
# Lens — 코드 모양 (backend · Kotlin/Spring Boot 3)

## Standard
- devkit 팀 규칙 `rules/dev-team/common.md` CS25 — 함수 40줄이 잣대, 중첩 3단 이상은 조기 반환·추출, 한 함수 = 한 추상화 수준. CS18 — 주인공 시험(public 메서드마다 존재 이유인 애그리거트 하나, 이름이 둘이면 분리). CS7 — 경계가 아닌 인터페이스에 구현 하나면 만들지 않음. CS8 — 상속 깊이 > 2 면 합성. CS16 — 흐름 제어용 예외 금지. CS24 — 상태·코드 값은 enum·상수.
- devkit 팀 규칙 `rules/dev-team/tests.md` TT16 — 결정성(제품 코드가 시각·난수를 직접 읽으면 시험이 고정 못 함).
- 레포 규칙 [backend-api.md PTB4](../../../../../../rules/backend-api.md) — `BigDecimal` 은 `compareTo` 비교(`==`·`equals` 금지), 문자열·정수로 생성(`BigDecimal(0.1)` 금지), 나눗셈은 `/` 연산자 금지 · `divide(x, scale, RoundingMode)` 또는 `divide(x, MathContext)`, `BigDecimal` 을 담은 값 객체 `data class` 는 scale 정규화·검증과 `equals`·`hashCode` 일치.
- 레포 규칙 PTB2 — 상태 변경은 이름 붙은 메서드로만(여기서는 «묻지 말고 시켜라» 모양으로 본다).
- detekt 기본 규칙 세트(`buildUponDefaultConfig = true`): `LongMethod`(60줄)·`NestedBlockDepth`(4)·`LongParameterList`(함수 6·생성자 7)·`TooManyFunctions`(11)·`CyclomaticComplexMethod`(15)·`ComplexCondition`(4). 이 렌즈는 팀 규칙 수치(40줄·중첩 3)로 더 엄격하다 — detekt 통과가 곧 통과가 아니다.
- Kotlin 공식 코딩 컨벤션 — https://kotlinlang.org/docs/coding-conventions.html (표현식 본문·불변 우선·`val` 우선).
- 우아한테크코스 pr_checklist — 도메인 getter/setter 금지(S7 근거만 인용) — https://github.com/woowacourse/woowacourse-docs/blob/main/cleancode/pr_checklist.md
- 알려진 규칙 충돌: detekt 기본값(LongMethod 60·NestedBlockDepth 4)은 팀 규칙 CS25(40·3)보다 느슨하다 — S11 이 레포 단위로 판정한다. 규칙 쪽 충돌은 없다.

## Deterministic first
| Tool | Command | Proves | If absent |
|---|---|---|---|
| detekt | `cd backend/trading-api && ./gradlew detekt --console=plain` | 기준선 밖 새 위반 수(출력·`detekt.md`) · 기준선(`config/detekt/baseline.xml`)의 기존 위반이 파일별로 출력에 있음 — 내 파일로 걸러 S1·S2·S5 후보로 쓴다 | 읽어서 센다 |
| ktlint | `cd backend/trading-api && ./gradlew ktlintCheck --console=plain` | 서식 위반 · 기준선(`config/ktlint/baseline.xml`) 항목이 파일별로 출력에 있음 — 서식은 이 렌즈 판정 대상이 아니고 참고만 | 생략 |
| grep (BigDecimal 함정) | `grep -nE 'BigDecimal\((-?[0-9]+\.[0-9]+\|[a-z][A-Za-z]*\.toDouble\(\))\)\|\.toDouble\(\)\|\.divide\([^,()]*\)\|[A-Za-z)]\s/\s[A-Za-z(]\|\.equals\(\|==\s*BigDecimal\.' <chunk-files>` | F1 후보 줄 — 산술 `/` 는 타입을 읽어 `BigDecimal` 인지 확인 | 읽는다 |
| grep (시각·난수 직접 읽기) | `grep -nE '(LocalDateTime\|LocalDate\|Instant\|ZonedDateTime\|OffsetDateTime)\.now\(\)\|UUID\.randomUUID\(\)\|System\.currentTimeMillis\(\)' <chunk-files>` | F4 후보 줄 | 읽는다 |

명령 칸의 `\|` 는 `|` 의 마크다운 이스케이프다. `<chunk-files>` = 내 인스턴스의 파일 목록.

## Checks
| ID | Signal — what you see in the file | Why it matters | Default severity |
|---|---|---|---|
| S1 | 함수·메서드 본문이 빈 줄·주석·KDoc 을 빼고 40줄을 넘는다(CS25). 컨트롤러 핸들러·`@Scheduled` 메서드도 센다 | 한 화면에 안 읽히고 거의 항상 추상화 수준이 둘이다 | med |
| S2 | 중첩 깊이 > 3 — `if`·`when` 분기·`for`·`while`·`try`·`?.let {}`·`run {}`·`also {}` 같은 블록 람다를 센다, 함수 본문 = 0 (CS25) | 단계마다 시험이 덮어야 할 경로가 곱해진다. 조기 반환·추출이 답 | med |
| S3 | 클래스·서비스가 주인공 시험에 진다: public 메서드마다 존재 이유인 애그리거트를 적으면 서로 다른 이름이 2개 이상(읽기 전용 조회·단순 CRUD·설정 홀더 제외) (CS18) — 예 주문 서비스가 포지션 청산 규칙까지 가짐 | 바꿀 이유가 둘이라 한 애그리거트 변경이 다른 쪽을 건드린다 | med |
| S4 | public 함수 10개 초과 클래스 또는 생성자 주입 의존 7개 이상인 `@Service`·`@Component` | 넓은 표면이 책임 여럿을 숨겨 S3 를 무력화한다 | low |
| S5 | 인자 > 5 개 함수(`this`·수신 객체 제외). 컨트롤러 핸들러의 `@PathVariable`·`@RequestParam`·`@RequestBody`·`@RequestHeader` 인자만으로 5개를 넘으면 n/a | 위치 결합 — 인자 객체(`data class` 명령)가 빠졌다 | low |
| S6 | 묻지 말고 시켜라 위반: 호출자가 한 객체의 속성 2개 이상을 읽고 `if`/`when` 으로 정한 뒤 그 객체에 다시 쓰거나 그 결정으로 그 객체 메서드를 부른다(예 `if (order.status == PENDING && order.qty > filled) order.status = …`) (PTB2) | 결정은 객체의 것이다 — 호출자마다 다시 구현한다 | med |
| S7 | 도메인 클래스의 상태 필드에 public setter(`var` + `set` 공개) 또는 `setX`/`getX` 쌍 함수; DTO·`@ConfigurationProperties`·infrastructure JPA 엔티티는 제외 | setter 는 이름만 다를 뿐 필드를 공개한다(우아한테크코스) | low |
| S8 | 상속 깊이 > 2(프로젝트 클래스만 셈 — `BaseTimeEntity` → `BaseAuditEntity` → 엔티티는 셈, `RuntimeException`·`JpaRepository` 루트는 안 셈) 또는 코드 재사용만을 위한 `open`/`abstract` 상속 (CS8) | is-a 가 아닌 재사용 상속은 LSP 를 조용히 깬다 | med |
| S9 | 인터페이스(또는 `abstract class`)에 구현 1개뿐이고 그 구현이 DB·HTTP·Redis·SDK 를 감싸지 않으며 `src/test` 에 가짜 구현도 없다 (CS7) — 포트는 제외 | 바꿀 것이 없는 추상화의 유지 비용 | low |
| S10 | 흐름 제어용 예외: 같은 호출 사슬에서 던지고 잡아 분기하는 사용자 예외, `runCatching { … }.getOrNull()`·`try { … } catch (e: NoSuchElementException) { default }` 로 한 식으로 검사 가능한 조건(`firstOrNull`·`?: `·`in`)을 대신함 (CS16) | 숨은 제어 경로이고 같은 타입의 진짜 오류까지 삼킨다 | med |
| S11 | 레포 단위: detekt 가 `buildUponDefaultConfig` 기본값(LongMethod 60·NestedBlockDepth 4)만 쓰고 팀 수치(40·3)로 조인 설정이 없음, 또는 detekt·ktlint 기준선(`baseline.xml`) 항목이 늘 수 있음(`verifyLintBaselinesNotGrown` 이 CI 에서 안 돎) — `(repo)` 에 기록 | 도구가 팀 기준보다 느슨하면 리뷰만이 지키고, 기준선 증가는 새 위반의 흡수다 | low |
| F1 | `BigDecimal` 함정(PTB4): `==`·`equals` 로 비교 · `BigDecimal(0.1)`·`BigDecimal(x.toDouble())`·`.toDouble()` 후 산술 · Kotlin `/` 연산자로 나눔 · scale·`RoundingMode` 없는 `divide(x)` · `setScale(n)` 에 `RoundingMode` 없음 · `BigDecimal` 을 담은 `data class` 가 scale 정규화(팩토리)·`require` 검증 없이 `==`·`Set`·`Map` 키로 쓰임 | `1.0 != 1.00`, 이진 오차, `/` 는 피제수 scale 로 조용히 반올림(`10/4 = 2`) — 컴파일은 통과하고 금액이 조용히 틀린다 | high |
| F2 | 질의 이름(`get*`·`find*`·`calculate*`·`compute*`·`to*`·`is*`·`has*`·`validate*`) 함수가 쓴다: `repository.save`·`delete`·엔티티 상태 변경·`publishEvent`·외부 POST·컬렉션 `add` (≥ 1) | 이름은 조회를 약속해 호출자가 재시도·캐시하고, 쓰기가 두 번 돈다 | high |
| F3 | 인자로 받은 객체(`MutableList`·엔티티)를 바꾸는데 함수 이름이 `update*`·`apply*`·`add*`·`remove*`·`mark*` 가 아니거나, 인자를 바꾸면서 값을 돌려준다 | 호출자 객체가 등 뒤에서 바뀐다 | med |
| F4 | `application/**`·`domain/**` 함수 본문이 `LocalDateTime.now()`·`LocalDate.now()`·`Instant.now()`·`UUID.randomUUID()`·`System.currentTimeMillis()` 를 직접 읽는다 — `Clock`·시각 인자·`IdGenerator` 주입이 아님 (TT16) | 그 함수를 시험이 고정할 수 없어 시험마다 비결정이 된다(정산일·세금연도 경계 버그가 숨는다) | med |

## Disprove before reporting
- S1: 한 `when` 이 enum 을 평면으로 매핑하는 45줄은 추상화 수준이 둘일 때만 발견이다. Querydsl 조건 조립이 한 표현식이면 줄 수만으로 올리지 않는다.
- S3·S9: 단순 CRUD 서비스(설정·전략 로그)는 팀 규칙 «No over-engineering» — n/a, `notes` 에 한 번.
- S5: 생성자 주입 인자는 S5 가 아니라 S4 로 센다.
- S7: infrastructure 의 JPA 엔티티 `var` 는 JPA 요구 — 통과. 아직 `domain.entity` 에 있는 엔티티는 layering D2 가 판정하므로 여기서는 n/a.
- F1: `BigDecimal.ZERO.compareTo(x) == 0` 은 통과. `BigDecimal("0.1")`·`BigDecimal.valueOf(long)` 은 통과. `Int`·`Long` 의 `/` 는 대상 아님 — 타입을 확인한다.
- F4: `Clock` 을 생성자로 받아 `LocalDateTime.now(clock)` 이면 통과. 감사 필드(`@CreatedDate`)는 F4 가 아니다.
- 도구 기준선에 이미 있는 항목도 판정은 이 렌즈 수치로 한다 — 기준선은 «안다»이지 «괜찮다»가 아니다.

## Out of scope
- 계층 import·도메인 순수성·엔티티 setter 자체 → layering. 이름 짓기 → naming-terminology.
- 시험 쪽 시각 고정(`Thread.sleep`·`now()` 비교) → tdd-purity. 트랜잭션 경계 → data/transaction-boundary.
- 예외 삼킴·로그 없는 `catch` → reliability/failure-handling (S10 은 «예외를 if 로 쓰는 모양»만).
