---
lens: backend/layering
dept: backend
agent: backend-auditor
title: 계층·도메인 순수성 (Kotlin·Spring 클린 아키텍처)
repo-checks: L9
target-path: /src/main/kotlin/(?!.*/domain/enums/)
concerns: 구조
---
# Lens — 계층·도메인 순수성 (backend · Kotlin/Spring Boot 3/JPA)

## Standard
- 레포 규칙 [backend-api.md PTB1](../../../../../../rules/backend-api.md) — **PR #50 판본 기준**(사람 결정 2026-11-15 «paper-trading은 클린아키텍쳐로 만들건데»): 모든 도메인 모델은 순수 Kotlin. domain 파일은 `jakarta.persistence`·`org.hibernate`·`org.springframework` 를 import 하지 않고, JPA 엔티티와 변환은 infrastructure 영속 어댑터에만, 리포지토리 포트는 도메인 모델을 주고받는다. 예외는 단순 CRUD 의 «필드만 있는 얇은 도메인 모델»뿐이고 JPA 엔티티를 infrastructure 밖에 두는 예외는 없다.
- 레포 규칙 PTB2·PTB3·PTB5·PTB6 — 상태 필드 `private set` + 이전 상태를 검사하는 이름 붙은 메서드 · 다른 애그리거트는 id 로만 · 생성 시점 불변식(`init { require }`·private 생성자 + 팩토리) · 컬렉션은 복사본으로.
- 레포 규칙 PTB7(PR #50 판본) — 허용 방향: presentation → application, presentation → `domain.enums`·`domain.event`, application → domain, infrastructure → domain, infrastructure → `application.<ctx>.port`·`application.common.port` 만, 모든 계층 → 최상위 `common`. 스케줄러·Redis 리스너 같은 진입점은 presentation 에서 유스케이스만 부른다. 트랜잭션 안 리스너는 application. `AFTER_COMMIT` 리스너 메서드엔 `@Transactional` 을 걸지 않고, 유스케이스(결과 기록은 `REQUIRES_NEW` 유스케이스)를 부르면 application, 어댑터만 부르면 infrastructure. `common` 은 `org.springframework` 를 import 하지 않는다.
- 레포 규칙 PTB8 — 출력 포트는 `application/<ctx>/port`(공유는 `application/common/port`), 포트 요청·결과 타입도 같은 `port` 패키지. 유스케이스는 Spring Data 리포지토리·외부 클라이언트를 직접 주입받지 않는다.
- 레포 규칙 PTB9(PR #50 판본) — application 이 쓰는 Spring 은 `@Service`·`org.springframework.transaction.annotation`·`@EventListener`·`@TransactionalEventListener`·`TransactionPhase` 뿐. 영속 예외는 어댑터가 `saveAndFlush`·`flush` 뒤 애플리케이션 예외로 바꾼다.
- devkit 팀 규칙 `rules/dev-team/backend-api.md` — DD9(도메인·유스케이스가 ORM·HTTP·SDK 를 모름) · DD10 빈약한 모델 · DD11 로직 누수 · DD12 한 트랜잭션 한 애그리거트 · DD13 원시 타입 집착 · DD14 계층 건너뛰기·순환 금지 · DD7 컨텍스트 사이는 명시 계약 · AE5 전역 예외 핸들러 하나.
- devkit 팀 규칙 `rules/dev-team/common.md` CS7 — 포트는 DB·HTTP·SDK 경계에만, 포트는 안쪽·어댑터는 바깥.
- Robert C. Martin «Clean Architecture»(의존 규칙: 소스 의존은 안쪽으로만) · Alistair Cockburn «Hexagonal Architecture»(포트는 애플리케이션 소유).
- 카카오뱅크 기술 블로그 — 모듈 경계는 CI 에서 실패하는 시험으로 검증 → L9 — https://tech.kakaobank.com/posts/2507-legacy-to-modular-monolith-with-spring-modulith/
- 계층 지도(레포 상대, `<api>` = `backend/trading-api/src/main/kotlin/com/papertrading/api`): domain = `<api>/domain/**` · application = `<api>/application/**`(`application.common` 포함) · infrastructure = `<api>/infrastructure/**` · presentation = `<api>/presentation/**` · common = `<api>/common/**` 만.
- 알려진 규칙 충돌: main 판본 PTB1 Exception 은 단순 CRUD 컨텍스트의 JPA 엔티티를 domain 에 두는 것을 허용한다. 이 렌즈는 PR #50 판본으로 판정하므로, 단순 CRUD 엔티티가 `domain.entity` 에 있으면 발견으로 남기고 `ruleConflicts` 에 `rules/backend-api.md PTB1 (main 판본 Exception — PR #50 미머지)` 행을 단다.

## Deterministic first
| Tool | Command | Proves | If absent |
|---|---|---|---|
| ArchUnit | `cd backend/trading-api && ./gradlew test --tests '*ArchitectureRulesTest*' --console=plain` | PTB1·PTB7·PTB8·PTB9 규칙 13개가 동결 저장소 기준 통과 · 출력에 동결된(기존) 위반이 규칙별·파일별로 있음 — 내 묶음 파일로 걸러 읽는다 | import 를 읽는다 |
| grep (import 간선) | `grep -nE '^import (jakarta\.persistence\|org\.hibernate\|org\.springframework\|com\.papertrading\.api\.(application\|infrastructure\|presentation))' <chunk-files>` | 내 묶음의 D1·L1~L5 후보 줄 | import 를 읽는다 |
| LLM-only | 빈약한 모델(D3)·애그리거트 참조(D4)·불변식 자리(D5)·진입점 자리(L6) | — | 발견의 `confidence` 에 «LLM-only» 를 적는다 |

명령 칸의 `\|` 는 `|` 의 마크다운 이스케이프다. `<chunk-files>` = 내 인스턴스의 파일 목록.

## Checks
| ID | Signal — what you see in the file | Why it matters | Default severity |
|---|---|---|---|
| D1 | `domain/**` 파일이 `jakarta.persistence.*`·`org.hibernate.*`·`org.springframework.*` 를 import 하거나 `@Entity`·`@Table`·`@Column`·`@Embeddable`·`@MappedSuperclass`·`@EntityListeners`·`@CreatedDate` 를 단다; 도메인 타입에서 Querydsl `Q*` 클래스가 생성된다 (PTB1 PR #50) | 도메인 모델이 곧 ORM 행이라 지연 로딩·더티 체킹·프록시가 불변식 메서드를 우회한다(DD9). ArchUnit 동결 목록에 있어도 발견이다(동결 ≠ 해결) | high |
| D2 | 애그리거트 상태 캡슐화 깨짐: 도메인 모델의 상태·금액 필드가 public setter 를 가진 `var`(`private set`·`protected set` 없음)이거나 서비스·어댑터가 `order.status = …` 처럼 대입으로 상태를 바꾼다 — 허용된 이전 상태를 검사하는 이름 붙은 메서드가 아님 (PTB2); 또는 `MutableList`·`MutableMap`·`MutableSet` 을 속성·반환 타입으로 내주거나 `toList()`·`toMap()` 없이 내부 컬렉션을 돌려주거나, 가변 컬렉션을 담은 `data class` 를 `copy()` 로 상태 분리한다 (PTB6) | 호출자가 전이 검사와 애그리거트 메서드를 건너뛴다 — CLAUDE.md «상태 전이는 명시적»이 깨지고, `copy()` 는 얕은 복사라 원본과 사본이 리스트를 공유한다 | high (상태·금액 setter) · med (컬렉션) |
| D3 | 빈약한 모델: 도메인 클래스의 필드를 application 서비스 2곳 이상이 읽어 분기(상태 검사·금액 산술·한도 검사)하는데 그 규칙의 메서드가 클래스에 없다 (DD10·DD11) | 불변식이 데이터와 떨어져 서비스마다 다시 구현하고 하나는 빠뜨린다 | med |
| D4 | `@ManyToOne`·`@OneToMany`·`@OneToOne`·`@ManyToMany` 또는 다른 애그리거트 루트 타입의 속성(`Order` 안 `val account: Account`)이 애그리거트를 넘는다 — 부모 → 자기 자식만 허용 (PTB3·DD12) | 한 애그리거트를 읽으면 다른 것이 딸려 오고 잠기며, 한 트랜잭션이 불변식 둘을 바꾼다 | med |
| D5 | 불변식(수량 ≥ 0, 가격 > 0, 상태·필드 정합)이 있는 도메인 클래스인데 public 주 생성자에 `init { require(...) }` 도 private 생성자 + `companion object` 팩토리도 없고 검증은 서비스에만 있다 (PTB5) | 서비스를 안 거치는 호출자(다른 유스케이스·매퍼·픽스처)가 잘못된 인스턴스를 만든다 | med |
| D6 | 금액·가격·수량·요율이 `Double`·`Float`(속성·인자·반환)이다; 같은 타입 id 인자 둘 이상이 나란히 온다(`fun transfer(fromId: Long, toId: Long)`) (DD13, CLAUDE.md Financial Safety) | 부동소수 금액은 조용히 정밀도를 잃고, 같은 타입 id 는 호출부에서 뒤바뀐다 | high (부동소수 금액) · low (id) |
| L1 | `application/**` 파일이 `com.papertrading.api.infrastructure.*`·`presentation.*` 를 import 하거나, 유스케이스 생성자가 `JpaRepository`·`CrudRepository` 를 확장한 `*Repository`, `RestTemplate`·`RestClient`·`WebClient`·`StringRedisTemplate`·`RedisTemplate`, infrastructure 클래스(`Kis*Client`·`*Adapter`)를 주입받는다 (PTB7·PTB8) | 유스케이스가 세부에 묶여 단위 시험이 DB·HTTP 를 요구하고 포트가 우회된다 | high |
| L2 | `application/**` 파일이 허용 목록(`stereotype.Service`·`transaction.annotation.*`·`context.event.EventListener`·`transaction.event.*`) 밖의 `org.springframework.*` 를 import 한다 — 예 `dao.DataIntegrityViolationException`·`data.*`(`Pageable`·`Sort`)·`http.*`·`web.*`·`scheduling.*` (PTB9) | 프레임워크 세부가 업무 분기가 되고, 유스케이스 안에서 잡은 영속 예외는 커밋 때 `UnexpectedRollbackException` 이 된다 | high (`DataIntegrityViolationException` 잡음) · med (그 밖) |
| L3 | 출력 포트(리포지토리·외부 API·캐시·발행) 인터페이스가 `application/<ctx>/port`·`application/common/port` 밖에 있다 — 예 `domain/port/*`, `application/portfolio/tax/*ReadRepository` — 또는 포트 시그니처가 자기 `port` 패키지·domain 밖 타입을 쓴다 (PTB8) | 포트 자리가 두 곳으로 흩어지고 infrastructure 가 port 밖 application 패키지를 import 하게 된다(PTB7) | med |
| L4 | `infrastructure/**` 가 `presentation.*` 를 import 하거나, `*.port` 밖 application 패키지(`application.<ctx>` 서비스·`application.common.result`)를 import 하거나, 유스케이스·서비스 클래스를 직접 호출한다 (PTB7) | 어댑터가 유스케이스를 몬다 — 의존 역전이 뒤집히고 순환이 자란다 | high |
| L5 | `presentation/**` 가 `infrastructure.*` 나 `domain.entity.*` 를 import 하거나, 컨트롤러 메서드가 도메인 엔티티(또는 엔티티를 감싼 result)를 받거나 돌려준다 — application result·query 타입이나 presentation DTO 가 아님 (PTB7) | 지연 로딩·내부 필드가 API 계약이 되어 컬럼 이름 변경이 클라이언트를 깬다 | high |
| L6 | 밖에서 들어오는 진입점이 infrastructure 에 있다: `@Scheduled` 메서드·Redis `MessageListener.onMessage`·WebSocket 핸들러·유스케이스를 부르는 `@EventListener(ApplicationReadyEvent)`; 유스케이스를 부르는 `AFTER_COMMIT` `@TransactionalEventListener` 가 infrastructure 에 있다 (PTB7 PR #50) | 진입점이 application 서비스를 import 해 PTB7 이 깨지고 유스케이스가 그 어댑터로만 닿는다 | med |
| L7 | 최상위 `common/**` 가 `org.springframework.*`(예 `HttpStatus`)나 계층 패키지를 import 한다; domain 파일이 HTTP 상태를 품은 그 `common` 예외를 던지거나 import 한다 (PTB7) | HTTP 의미가 `common` 을 거쳐 domain 에 새어 «domain 에 Spring 없음»이 우회로 깨진다 | med |
| L8 | application 컨텍스트 둘 사이의 import 순환(`application.order` ↔ `application.position`), 또는 컨텍스트 A 가 B 의 내부(port·result 가 아닌) 클래스를 import 한다 (DD7·DD14) | 컨텍스트 경계가 사라져 B 내부 변경이 A 를 고친다 | med |
| L9 | 레포 단위: `ArchitectureRulesTest` 가 위 PTB1·PTB7·PTB8·PTB9 방향을 모두 덮고, CI(`.github/workflows/*` 가 `test`·`check` 실행)에서 돌며, 동결 저장소가 읽기 전용(`archunit.properties` 의 `allowStoreCreation=false`·`allowStoreUpdate=false`)이고 `stored.rules` 의 규칙 문장마다 시험이 있다 — `(repo)` 에 기록 | 실패하는 검사로 지키지 않는 경계는 한 스프린트 안에 무너진다(카카오뱅크). 쓰기 가능한 저장소는 새 위반을 조용히 동결한다 | med |

## Disprove before reporting
- D1: `jakarta.annotation.*`(`@PostConstruct`)는 영속이 아니다 — Spring 금지만 본다. 동결 위반도 발견이지만(저장소 줄 인용) D1 과 동결 목록으로 같은 클래스를 두 번 올리지 않는다 — 파일당 하나.
- D2: infrastructure 의 JPA 엔티티 `var` 를 매퍼가 필드 복사만 하면 어댑터 몫 — 통과. 발견은 도메인 모델(또는 아직 `domain.entity` 에 있는 엔티티)의 상태를 밖에서 쓰는 경우다.
- D3·D5: 단순 CRUD 의 필드만 있는 모델(전략 로그·알림 설정)은 PTB1 PR #50 예외 — 통과, `notes` 에 한 번 적는다.
- D4: 부모 → 자기 자식(`Order` → 자기 라우트가 없는 `OrderAmendment`)은 통과. 자기 리포지토리·라우트로 따로 다뤄지는 자식은 루트다.
- D6: 금액이 아닌 측정값(화면용 비율·백분위)의 `Double` 은 통과. presentation DTO 의 `BigDecimal` 은 통과.
- L2: `@Transactional`·`@Service`·`@TransactionalEventListener`·`TransactionPhase` 는 허용 import — 통과.
- L5: 컨트롤러가 `domain.enums`·`domain.event` 를 쓰는 것은 PTB7 허용.

## Out of scope
- `@Transactional` 자리·전파·자기 호출·트랜잭션 안 원격 호출 → data/transaction-boundary. D4 가 부르는 지연 로딩 N+1 → data/query.
- 함수 길이·중첩·클래스 SRP → code-shape. 패키지·클래스 이름 → naming-terminology.
- `BigDecimal` 비교·나눗셈·scale(PTB4) → code-shape F1. 컬럼 precision·scale → data/schema-data-model.
