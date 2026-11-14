# Data access — Spring 트랜잭션·JPA 함정 (paper-trading)
<!-- devkit:pointer
path: ^backend/(trading-api|collector-api)/src/main/.+\.kt$
signal: @Transactional|@TransactionalEventListener|JpaRepository|@Query|@Modifying|@Lock|@Version|EntityManager|fetch join|@EntityGraph|\.save\(|\.saveAll\(|JPAQueryFactory|import com\.papertrading\.api\.domain\.entity\.\w+\.Q[A-Z]\w+|\.fetchJoin\(\)|\.offset\(
why: Spring 트랜잭션·JPA·Querydsl 조회와 쓰기를 다루는 Kotlin 코드
-->
<!-- devkit:form v2 -->

> 이 레포 전용 규칙 — devkit 팀 규칙 `data-access.md`(DA1~)를 Spring·JPA 에서 지키는 방법만 적는다. 원칙은 팀 규칙이 SSoT.
> devkit 은 이 레포의 Kotlin 파일에 팀 규칙을 자동으로 전달하지 않는다(rule-pointer 실측 2026-11-15: 팀 규칙 신호가 Kotlin 을 잡지 않아 `always:dev-team` 만 나온다). 그래서 이 레포 Kotlin 작업·리뷰는 팀 `data-access.md` 의 DA6·DA7(PTD4)·DA9(PTD2)·DA10(PTD3)을 이 문서의 해당 조항으로 읽고, 이 문서가 다시 쓰지 않는 DA11(«없으면 삽입»은 DB 유일 제약으로 막는다)·DA19(입력 크기가 정하는 조회에 상한)·DA23(집계·정렬·필터는 DB 에서)은 팀 문서를 직접 열어 따른다. devkit 쪽 Kotlin 신호 추가는 devkit 에 제안으로 올린다.

## 트랜잭션 경계

<!-- gt:PTD1 -->
- `@Transactional` 은 프록시를 거쳐 호출되는 public 유스케이스 메서드(다른 빈이 부르는 메서드)에만 건다. 스케줄러·Redis 리스너 같은 외부 진입점과 `AFTER_COMMIT` 리스너 메서드에는 걸지 않고 그 진입점·리스너가 유스케이스를 불러 경계를 유스케이스에 둔다(`backend-api.md` PTB9, 이 문서 PTD4). 같은 클래스 안에서 부르는 메서드나 private 메서드에 걸린 것은 경계로 치지 않는다.
  Why: Spring 트랜잭션은 프록시가 건다. 같은 클래스 안 호출(self-invocation)은 프록시를 안 거쳐 어노테이션이 있어도 트랜잭션이 없고, 오류도 안 난다. 진입점마다 경계를 따로 걸면 같은 유스케이스가 경로에 따라 트랜잭션 범위가 달라진다. — enforce: review
<!-- gt:PTD2 -->
- 열린 트랜잭션 안(`@Transactional` 메서드, 트랜잭션 안에서 도는 리스너, 그 둘이 부르는 어노테이션 없는 메서드 포함)에서 KIS·외부 HTTP(`WebClient`·`RestClient`)·Redis 발행을 부르지 않는다. 외부 호출은 트랜잭션 밖 유스케이스에서 하고, 결과 반영은 새 트랜잭션으로 한다.
  Why: 팀 규칙 DA1·DA9 의 Spring 형태. 외부 지연이 DB 커넥션과 락을 붙잡아 무관한 요청까지 막는다. 트랜잭션 안에서 부르는 어노테이션 없는 메서드도 같은 트랜잭션에 묶이므로 대상에 넣는다. — enforce: review

## 동시 갱신

<!-- gt:PTD3 -->
- 잔고·예수금·포지션·주문 상태처럼 읽고 고쳐 쓰는 값은 `@Version` 낙관적 락, `@Lock(LockModeType.PESSIMISTIC_WRITE)`, 조건부 UPDATE 중 하나로 보호한다. 조회 뒤 `save()` 로 덮어쓰지 않는다. 영속 예외(`@Version` 충돌, 제약 위반)는 어댑터가 쓰기 뒤 `saveAndFlush`·`flush` 로 어댑터 안에서 일으켜 애플리케이션 예외로 바꾸고(PTB9), 재시도하거나 실패로 돌려주는 일은 트랜잭션을 연 유스케이스 메서드를 부르는 바깥 경계에서 그 애플리케이션 예외를 잡아 새 트랜잭션으로 한다.
  Why: 팀 규칙 DA10 의 JPA 형태. 기본 격리 수준에서 동시 요청이 서로의 갱신을 덮어써 체결 금액이 사라진다. flush 를 어댑터 밖에 두면 충돌이 커밋 시점에 유스케이스의 `@Transactional` 프록시에서 Spring 타입으로 터져 어댑터가 바꿀 기회가 없고(PTB9 와 어긋남), 롤백 표시가 된 트랜잭션 안에서는 재시도할 수 없다. — enforce: review

## 커밋 뒤 처리

<!-- gt:PTD4 -->
- 본 쓰기와 함께 성공·실패해야 하는 DB 쓰기는 트랜잭션 안에서 도는 리스너(`@EventListener`, `@TransactionalEventListener(phase = BEFORE_COMMIT)`, application 에 둔다 — `backend-api.md` PTB7·PTB9)에 두고 커밋하지 않는다(팀 규칙 DA6·CS11). `AFTER_COMMIT` 리스너는 커밋 뒤 부수효과(팀 규칙 DA7)만 맡고, 리스너 메서드 자신에는 `@Transactional` 을 걸지 않는다. 그 리스너가 한 외부 부수효과의 결과를 기록해야 할 때만 `@Transactional(propagation = REQUIRES_NEW)` 를 선언한 유스케이스 메서드를 불러 새 짧은 트랜잭션으로 그 결과만 쓰고(팀 규칙 DA1·DA9 3단계) 원본 id 로 중복을 거른다. 리스너가 부르는 쓰기 유스케이스는 기본 전파(`REQUIRED`)가 아니라 `REQUIRES_NEW` 다. 본 쓰기와 같은 DB 의 부수 쓰기는 `AFTER_COMMIT` 에 두지 않는다.
  Why: AFTER_COMMIT 시점의 원래 트랜잭션은 이미 커밋됐지만 트랜잭션 자원은 아직 살아 있어, 기본 전파(`REQUIRED`) 유스케이스를 부르면 그 자원에 참여만 하고 커밋이 뒤따르지 않아 오류 없이 쓰기가 사라진다(Spring `TransactionSynchronization.afterCommit` 은 여기서 부르는 트랜잭션 작업에 `REQUIRES_NEW` 를 쓰라고 한다). 그렇다고 리스너 메서드에 `@Transactional(REQUIRES_NEW)` 를 걸면 리스너 안의 외부 호출(KIS·Slack·Redis 발행)까지 트랜잭션에 들어가 PTD2 가 깨지므로, 새 트랜잭션은 외부 호출이 끝난 뒤 결과를 쓰는 유스케이스 메서드에만 건다(PTD1·`backend-api.md` PTB9 와 같은 자리). 본 쓰기와 원자적이어야 하는 쓰기를 AFTER_COMMIT 에 두면 DA6 이 막는 «본 쓰기 실패와 별개로 살아남는 부수 쓰기»가 반대로 «본 쓰기 성공 뒤 사라지는 쓰기»가 된다. `REQUIRES_NEW` 를 쓰기 종류 제한 없이 열면 DA6·CS11 이 허용하지 않는 커밋하는 동일 DB 핸들러가 생기므로, 새 트랜잭션은 외부 호출 결과 기록(DA1·DA9 3단계)에만 허용한다. — enforce: review

## 조회

<!-- gt:PTD5 -->
- 컬렉션 fetch join 과 페이징을 같이 쓰지 않는다(`@BatchSize` 나 id 먼저 조회로 나눈다. Querydsl `.fetchJoin()` 과 `.offset()` 의 조합도 같다). `@Modifying` 벌크 쿼리는 `flushAutomatically = true, clearAutomatically = true` 를 함께 건다(`PositionRepository` 의 `@Modifying` 이 이미 그렇다).
  Why: 컬렉션 fetch join + 페이징은 전체를 메모리로 읽어 자르고(HHH90003004 경고뿐), 벌크 쿼리는 영속성 컨텍스트를 건너뛰어 같은 트랜잭션에서 낡은 엔티티를 읽는다. clear 만 하면 실행 전 자동 flush 가 그 쿼리가 건드리는 테이블에만 걸려, 같은 트랜잭션의 다른 엔티티(예: Account 잔고) 변경이 오류 없이 버려진다. flush 를 함께 걸어야 한다. — enforce: review

<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd.md -->
<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd-r5.md -->
<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd-r6.md -->
<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd-r7.md -->
