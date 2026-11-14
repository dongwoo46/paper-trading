# Data access — Spring 트랜잭션·JPA 함정 (paper-trading)
<!-- devkit:pointer
path: ^backend/(trading-api|collector-api)/src/main/.+\.kt$
signal: @Transactional|@TransactionalEventListener|JpaRepository|@Query|@Modifying|@Lock|@Version|EntityManager|fetch join|@EntityGraph|\.save\(|\.saveAll\(
why: Spring 트랜잭션·JPA 조회와 쓰기를 다루는 Kotlin 코드
-->
<!-- devkit:form v2 -->

> 이 레포 전용 규칙 — devkit 팀 규칙 `data-access.md`(DA1~)를 Spring·JPA 에서 지키는 방법만 적는다. 원칙은 팀 규칙이 SSoT.

## 트랜잭션 경계

<!-- gt:PTD1 -->
- `@Transactional` 은 다른 빈에서 부르는 public 메서드에만 건다. 같은 클래스 안에서 부르는 메서드나 private 메서드에 걸린 것은 경계로 치지 않는다.
  Why: Spring 트랜잭션은 프록시가 건다. 같은 클래스 안 호출(self-invocation)은 프록시를 안 거쳐 어노테이션이 있어도 트랜잭션이 없고, 오류도 안 난다. — enforce: review
<!-- gt:PTD2 -->
- `@Transactional` 메서드 안에서 KIS·외부 HTTP(`WebClient`·`RestClient`)·Redis 발행을 부르지 않는다. 외부 호출은 트랜잭션 밖 유스케이스에서 하고, 결과 반영은 새 트랜잭션으로 한다.
  Why: 팀 규칙 DA1·DA9 의 Spring 형태. 외부 지연이 DB 커넥션과 락을 붙잡아 무관한 요청까지 막는다. — enforce: review

## 동시 갱신

<!-- gt:PTD3 -->
- 잔고·예수금·포지션·주문 상태처럼 읽고 고쳐 쓰는 값은 `@Version` 낙관적 락, `@Lock(LockModeType.PESSIMISTIC_WRITE)`, 조건부 UPDATE 중 하나로 보호한다. 조회 뒤 `save()` 로 덮어쓰지 않는다. 낙관적 락 충돌은 유스케이스 경계에서 재시도하거나 실패로 돌려준다.
  Why: 팀 규칙 DA10 의 JPA 형태. 기본 격리 수준에서 동시 요청이 서로의 갱신을 덮어써 체결 금액이 사라진다. — enforce: review

## 커밋 뒤 처리

<!-- gt:PTD4 -->
- 본 쓰기와 함께 성공·실패해야 하는 DB 쓰기는 트랜잭션 안에서 도는 리스너(`@EventListener`, `@TransactionalEventListener(phase = BEFORE_COMMIT)`)에 두고 커밋하지 않는다(팀 규칙 DA6). `AFTER_COMMIT` 리스너는 커밋 뒤 부수효과(팀 규칙 DA7)만 맡고, 그 안에서 DB 를 써야 하면 `@Transactional(propagation = REQUIRES_NEW)` 로 감싸고 원본 id 로 중복을 거른다.
  Why: AFTER_COMMIT 시점의 원래 트랜잭션은 이미 커밋돼 새 트랜잭션 없이 쓰면 오류 없이 아무것도 저장되지 않는다. 본 쓰기와 원자적이어야 하는 쓰기를 거기 두면 DA6 이 막는 «본 쓰기 실패와 별개로 살아남는 부수 쓰기»가 반대로 «본 쓰기 성공 뒤 사라지는 쓰기»가 된다. — enforce: review

## 조회

<!-- gt:PTD5 -->
- 컬렉션 fetch join 과 페이징을 같이 쓰지 않는다(`@BatchSize` 나 id 먼저 조회로 나눈다). `@Modifying` 벌크 쿼리는 `clearAutomatically = true` 로 영속성 컨텍스트를 비운다.
  Why: 컬렉션 fetch join + 페이징은 전체를 메모리로 읽어 자르고(HHH90003004 경고뿐), 벌크 쿼리는 영속성 컨텍스트를 건너뛰어 같은 트랜잭션에서 낡은 엔티티를 읽는다. — enforce: review

<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd.md -->
