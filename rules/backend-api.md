# Backend API — Kotlin·Spring 도메인 모델과 금액 (paper-trading)
<!-- devkit:pointer
path: ^backend/(trading-api|collector-api)/src/main/.+\.kt$
signal: @Entity|BigDecimal|class \w+\s*(protected )?constructor|private set|@ManyToOne|@OneToMany|@OneToOne|^import com\.papertrading\.api\.(application|infrastructure|presentation)\.|@Service|@Transactional|interface \w+Port\b
why: JPA 엔티티(애그리거트)·금액 계산·계층 간 import·포트를 다루는 Kotlin 코드
-->
<!-- devkit:form v2 -->

> 이 레포 전용 규칙 — devkit 팀 규칙 `backend-api.md`(DD4~DD14)를 이 레포의 Kotlin·Spring·JPA 에서 지키는 방법만 적는다. 원칙 자체는 팀 규칙이 SSoT 이고 여기서 다시 쓰지 않는다.
> devkit 은 이 레포의 Kotlin 파일에 팀 규칙을 자동으로 전달하지 않는다(rule-pointer 실측 2026-11-15: `always:dev-team` 만 나온다). 그래서 이 레포 Kotlin 작업·리뷰는 팀 `backend-api.md` 의 DD9~DD14(PTB1·PTB2·PTB3·PTB5·PTB7~PTB9 가 이 레포 형태로 적음)와, 이 문서가 다시 쓰지 않는 DD7(컨텍스트 사이는 명시 계약으로만)·AE5·AE8(예외→HTTP 응답은 전역 핸들러 한 곳, 예외 원문을 응답에 싣지 않음)을 직접 열어 따른다. devkit 쪽 Kotlin 신호 추가는 devkit 에 제안으로 올린다.
> 금액 규칙의 상위 원칙은 루트 `CLAUDE.md` «Financial Safety»(금액에 부동소수점 금지, Kotlin 은 BigDecimal).

## 도메인 모델과 영속 모델

<!-- gt:PTB1 -->
- 금액을 바꾸거나 상태 필드에 전이 규칙이 있는 애그리거트는 순수 Kotlin 도메인 모델로 둔다. 도메인 모델 파일은 `jakarta.persistence`·`org.hibernate`·`org.springframework` 를 import 하지 않고, JPA 엔티티와 그 사이 변환은 영속 어댑터에 두며, 리포지토리 포트는 도메인 모델을 주고받는다.
  Why: 팀 규칙 DD9(도메인·유스케이스가 ORM 모델을 직접 참조하지 않음)의 Kotlin·JPA 형태. 엔티티가 곧 도메인이면 지연 로딩·더티 체킹·프록시가 불변식 검사를 우회하는 경로가 된다(사람 결정 2026-11-15: 이 레포는 DD9 를 따라 분리). — enforce: review
  Exception: 금액도 전이 규칙도 없는 단순 CRUD·설정 성격 컨텍스트는 JPA 엔티티를 그대로 쓴다 — 팀 규칙 backend-api «No over-engineering»(단순 CRUD 에 전체 DDD 를 강요하지 않음)의 적용이지 이 레포가 만든 예외가 아니다.
<!-- gt:PTB2 -->
- 도메인 모델의 상태 필드는 `private set` 이고, 상태 변경은 허용된 이전 상태를 검사하는 이름 붙은 메서드로만 한다.
  Why: 팀 규칙 DD10(빈약한 모델 금지)·DD11(도메인 로직 누수 금지)의 Kotlin 형태. Kotlin `var` 는 기본이 public setter 라 서비스가 전이 검사를 건너뛰고 상태를 바꿀 수 있고, 루트 `CLAUDE.md` 의 «상태 전이는 명시적»이 깨진다. — enforce: review
<!-- gt:PTB3 -->
- 다른 애그리거트는 id 로만 참조한다. JPA 연관관계(`@ManyToOne`·`@OneToMany`·`@OneToOne`)는 같은 애그리거트 안의 자식에만 쓴다.
  Why: 팀 규칙 DD12(한 트랜잭션은 한 애그리거트)를 지키는 구조. 애그리거트를 객체로 이으면 한 트랜잭션이 여러 애그리거트를 읽고 잠근다(devkit 제안 BiocomKR/devkit#2471 의 DD15 와 같은 내용 — 그 제안이 머지되면 이 조항은 지운다). — enforce: review
<!-- gt:PTB5 -->
- 도메인 모델은 private 생성자와 팩토리, 또는 `init { require(...) }` 로 생성 시점에 불변식을 검증해 잘못된 인스턴스가 존재할 수 없게 한다.
  Why: 검증을 서비스에 두면 그 서비스를 거치지 않는 호출자(다른 유스케이스, 어댑터의 변환, 테스트 픽스처)가 건너뛴다. 팀 규칙 DD13(원시 타입 집착 금지)의 값 객체도 이 방식으로 만든다. — enforce: review
<!-- gt:PTB6 -->
- 도메인 모델은 내부 컬렉션을 복사본(`toList()`·`toMap()`)으로 내주고 `MutableList`·`MutableMap` 으로 밖에 내주지 않으며, 가변 컬렉션을 담은 `data class` 의 `copy()` 를 상태 분리 수단으로 쓰지 않는다.
  Why: 밖으로 나간 가변 컬렉션은 호출자가 애그리거트 메서드(PTB2)를 거치지 않고 내용을 바꿀 수 있고, `copy()` 는 얕은 복사라 원본과 사본이 같은 컬렉션을 공유해 한쪽 변경이 다른 쪽 불변식을 깬다(LINE 공개 리뷰 기준). Kotlin 읽기 전용 `List`·`Map` 타입은 같은 가변 컬렉션의 뷰라 캐스팅이나 Java 호출자가 고칠 수 있어, 타입만 좁히지 말고 복사본으로 내준다. — enforce: review

## 계층과 포트 (trading-api)

<!-- gt:PTB7 -->
- trading-api 의 계층 지도는 아래 방향만 허용하고 나머지는 모두 금지한다. 허용: presentation → application, presentation → domain 의 `domain.enums`·`domain.event` 패키지만, application → domain, infrastructure → domain, infrastructure → application 의 `port` 패키지(`application.<컨텍스트>.port` 와 `application.common.port` 의 포트와 그 시그니처 타입)만, 모든 계층 → 최상위 `com.papertrading.api.common`. 금지: domain → application·infrastructure·presentation, application → infrastructure·presentation, presentation → infrastructure, infrastructure → presentation, infrastructure → application 의 `port` 밖(유스케이스·서비스 호출 포함, `application.common.result` 같은 `application.common` 의 다른 패키지도 application 의 일부라 여기 든다), common → 어느 계층이든 import. 여기서 `common` 은 최상위 `com.papertrading.api.common` 만이고 `application.common` 은 application 의 일부다. `common` 은 domain 과 같이 `org.springframework` 를 import 하지 않는다(예외가 HTTP 상태를 알아야 하면 그 매핑은 presentation 의 예외 핸들러로 옮긴다). presentation 은 엔티티·애그리거트를 요청·응답에 쓰지 않고 application 의 result·query 타입이나 presentation DTO 를 쓴다. 스케줄러·Redis 리스너처럼 밖에서 들어오는 진입점은 infrastructure 가 아니라 presentation 에 두고 유스케이스만 부르며, 같은 트랜잭션 안에서 도는 도메인 이벤트 리스너(`@EventListener`, `@TransactionalEventListener(phase = BEFORE_COMMIT)`)는 application 에 둔다(PTB9). 커밋 뒤(`AFTER_COMMIT`) 리스너는 유스케이스를 부르거나 결과 기록용 새 트랜잭션을 열면 application 에, 어댑터만 부르는 부수효과면 infrastructure 에 둔다.
  Why: 팀 규칙 backend-api 잠금 원칙(의존은 안쪽으로)과 DD14(계층 건너뛰기 금지)를 이 레포 패키지 이름으로 적은 지도다. 지도에 빠진 방향은 판정 기준이 없어 PTB1 을 우회한다: common 의 예외가 `org.springframework.http.HttpStatus` 를 import 하고(`ApiDomainException`·`CommonApiExceptions`) domain 2파일이 그 예외를 써서 «domain 은 Spring 금지»가 새고, presentation 이 엔티티를 직접 응답에 쓰면 지연 로딩·내부 필드가 API 계약이 된다(사람 결정 2026-11-15: 엄격하게 적는다). 실측 2026-11-15: application→infrastructure 28파일, infrastructure→presentation 2파일, presentation→domain 14파일(`domain.entity` import 5파일, `domain.event` 1파일, 나머지는 `domain.enums`), infrastructure→application 18파일 중 `port` 밖 import 15파일(`application.common.result` 1파일 포함, 스케줄러·리스너·웹소켓 4파일이 유스케이스 서비스를 직접 호출). 리스너는 현재 모두 infrastructure 에 있고(`SlackNotificationEventHandler`·`ExecutionSseEventHandler` 는 AFTER_COMMIT, `KisExecutionWebSocketManager` 는 기동 이벤트) 트랜잭션 안 리스너는 아직 없다. 기존 위반은 ArchUnit 동결 목록으로 두고 새 위반만 막는다. 사람 결정 2026-11-15: 클린 아키텍처(포트·어댑터). — enforce: archunit · review
<!-- gt:PTB8 -->
- 출력 포트(리포지토리, 외부 API, 캐시, 메시지 발행 인터페이스)는 그것을 쓰는 유스케이스 쪽 `application/<컨텍스트>/port` 에 인터페이스로 두되, 여러 컨텍스트가 같이 쓰는 포트는 `application/common/port` 에 두고, 포트가 주고받는 요청·결과 타입도 같은 `port` 패키지에 둔다. 구현은 `infrastructure` 어댑터가 한다. 유스케이스는 Spring Data 리포지토리나 외부 클라이언트 클래스를 직접 주입받지 않는다.
  Why: 유스케이스가 어댑터 클래스를 직접 쓰면 PTB7 방향이 깨지고, 단위 시험이 DB·외부 API 없이 돌지 못한다. 포트가 `domain/port` 와 `application/*/port` 두 곳에 흩어진 상태를 한 곳으로 정한다. 여러 컨텍스트가 쓰는 포트(예: `MarketQuotePort` 는 order·portfolio·position 세 컨텍스트가 쓴다)는 «쓰는 유스케이스 쪽»만으로 자리가 정해지지 않아 공유 자리를 따로 정했고, 포트 시그니처 타입을 `port` 패키지에 두는 것은 infrastructure 가 PTB7 에서 `port` 패키지만 import 하도록 하기 위해서다(사람 결정 2026-11-15). — enforce: archunit · review
<!-- gt:PTB9 -->
- application 계층이 쓰는 Spring 은 `@Service`, `org.springframework.transaction.annotation`(`@Transactional`), 도메인 이벤트 리스너 어노테이션(`org.springframework.context.event` 의 `@EventListener`, `org.springframework.transaction.event` 의 `@TransactionalEventListener` 와 `TransactionPhase`)뿐이다. `@Transactional` 은 유스케이스에만 두고 스케줄러·Redis 리스너 같은 진입점에는 두지 않는다. 그 밖의 Spring 타입(예: `DataIntegrityViolationException`)이 필요하면 어댑터가 애플리케이션 예외나 포트 결과로 바꿔 넘기고, 영속 예외(`@Version` 충돌, 제약 위반)는 어댑터가 쓰기 뒤 `saveAndFlush`·`flush` 로 어댑터 안에서 일으켜 바꾼다(`data-access.md` PTD3). 유스케이스는 바뀐 애플리케이션 예외만 잡고, 계속 진행하거나 재시도하려면 실패한 트랜잭션 밖(`data-access.md` PTD3 의 바깥 경계)에서 잡는다.
  Why: 트랜잭션 경계는 유스케이스에 둔다(사람 결정 2026-11-15). 같은 트랜잭션 안에서 도는 리스너는 유스케이스를 부르는 쪽이라 infrastructure 에 두면 PTB7 에 걸리므로 application 에 두고 그 어노테이션을 허용한다(사람 결정 2026-11-15). 참여 중인 트랜잭션에서 영속 예외가 나면 그 트랜잭션은 rollback-only 가 돼, 유스케이스 안에서 예외를 잡고 계속 진행하면 커밋 때 `UnexpectedRollbackException` 이 난다. 영속 예외를 유스케이스가 직접 잡으면 JPA 구현 세부가 업무 분기로 새어 팀 규칙 DD9 가 무너진다. `@Version` 충돌과 지연 제약 위반은 flush 를 어댑터 안에서 하지 않으면 커밋 시점에 유스케이스의 `@Transactional` 프록시에서 Spring 타입으로 터져 어댑터가 바꿀 기회가 없으므로 flush 위치까지 함께 정한다. 현재 `SnapshotJobService`·`TaxSummaryCommandService`·`PositionExitTriggerOrchestrator` 3파일이 application 에서 `DataIntegrityViolationException` 을 직접 잡는다(기존 위반). — enforce: archunit · review

## 금액

<!-- gt:PTB4 -->
- `BigDecimal` 은 `compareTo` 로 비교하고(`==`·`equals` 금지), 문자열이나 정수로 만들며(`BigDecimal("0.1")`, `BigDecimal(0.1)` 금지), 나눗셈은 Kotlin `/` 연산자를 쓰지 않고 `divide(x, scale, RoundingMode)`(지표 계산처럼 유효숫자로 재면 `divide(x, MathContext)`)로 scale 이나 정밀도와 반올림을 명시한다. `BigDecimal` 을 담은 `data class` 값 객체(PTB5 의 금액 값 객체 등)는 private 생성자와 팩토리에서 scale 을 정규화하거나 `init` 에서 `require` 로 scale 을 검증하고, `equals` 를 바꾸면 `hashCode` 도 `stripTrailingZeros()` 기준으로 함께 바꾼다.
  Why: Kotlin `==` 는 `equals` 라 scale 까지 비교해 `1.0 != 1.00` 이 되고, `Double` 생성자는 이진 오차를 그대로 싣고, Kotlin `/` 는 `divide(other, RoundingMode.HALF_EVEN)` 이라 예외 없이 피제수의 scale 로 반올림한다(`BigDecimal("10") / BigDecimal("4")` 는 2). scale 없는 `.divide(x)` 는 무한소수에서 예외를 던지니 이쪽은 시끄럽게 실패하지만 `/` 는 조용히 틀린다. `data class` 의 `==`·`Set`·`Map` 키도 scale 까지 비교해 같은 금액이 서로 다른 값이 된다. `init` 에서는 `val` 속성을 다시 대입할 수 없어 정규화는 팩토리에서 하고, `equals` 만 바꾸면 `data class` 가 생성하는 `hashCode` 는 scale 을 반영한 채라 `1.0` 과 `1.00` 이 같다고 하면서 hash 는 달라 `Set`·`Map` 키 문제가 남는다. 컴파일은 통과하고 금액이 조용히 틀린다. — enforce: review

<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd.md -->
<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd-r3.md -->
<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd-r4.md -->
<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd-r5.md -->
<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd-r6.md -->
