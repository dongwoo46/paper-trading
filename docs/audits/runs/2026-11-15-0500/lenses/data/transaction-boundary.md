---
lens: data/transaction-boundary
dept: data
agent: data-auditor
title: 트랜잭션 경계 (Spring @Transactional·JPA)
repo-checks: X13
target-signal: @Transactional|TransactionalEventListener|@EventListener|TransactionTemplate|TransactionSynchronization|PlatformTransactionManager|JpaRepository|EntityManager|\.save(AndFlush|All)?\(|\.flush\(\)|@Modifying|@Lock\(|@Version|@Scheduled|onMessage\(|publishEvent\(|RestTemplate|RestClient|WebClient|WebSocket|convertAndSend|SseEmitter|Notifier|Executor\b
concerns: 장애, 정합성
---
# Lens — 트랜잭션 경계 (data · Spring Boot 3/JPA/Hibernate)

## Standard
- 레포 규칙 [data-access.md PTD1](../../../../../../rules/data-access.md) — **PR #50 판본**: `@Transactional` 은 프록시를 거쳐 호출되는 public 유스케이스 메서드에만. 스케줄러·Redis 리스너 같은 외부 진입점과 `AFTER_COMMIT` 리스너 메서드에는 걸지 않고, 진입점·리스너가 유스케이스를 불러 경계를 유스케이스에 둔다. 같은 클래스 안 호출·private 메서드의 `@Transactional` 은 경계로 치지 않는다(self-invocation 은 오류 없이 트랜잭션이 없다).
- 레포 규칙 PTD2 — 열린 트랜잭션 안(`@Transactional` 메서드, 트랜잭션 안 리스너, 그 둘이 부르는 어노테이션 없는 메서드 포함)에서 KIS·외부 HTTP(`RestTemplate`·`WebClient`·`RestClient`)·Redis 발행을 부르지 않는다. 외부 호출은 트랜잭션 밖, 결과 반영은 새 트랜잭션.
- 레포 규칙 PTD3 — 잔고·예수금·포지션·주문 상태 같은 읽고-고쳐-쓰기는 `@Version`·`@Lock(PESSIMISTIC_WRITE)`·조건부 UPDATE 중 하나로 보호. 영속 예외는 어댑터가 `saveAndFlush`·`flush` 로 어댑터 안에서 일으켜 애플리케이션 예외로 바꾸고, 재시도·실패 응답은 트랜잭션을 연 유스케이스 바깥에서 새 트랜잭션으로.
- 레포 규칙 PTD4 — **PR #50 판본**: 본 쓰기와 함께 성패해야 하는 DB 쓰기는 트랜잭션 안 리스너(`@EventListener`·`BEFORE_COMMIT`, application)에 두고 커밋하지 않는다. `AFTER_COMMIT` 리스너는 커밋 뒤 부수효과만 맡고 **리스너 메서드 자신엔 `@Transactional` 을 걸지 않는다**. 외부 부수효과 결과 기록만 `@Transactional(propagation = REQUIRES_NEW)` 를 선언한 유스케이스 메서드를 불러 쓰고 원본 id 로 중복을 거른다. 리스너가 부르는 쓰기 유스케이스는 `REQUIRED` 가 아니라 `REQUIRES_NEW`.
- 레포 규칙 [backend-api.md PTB9](../../../../../../rules/backend-api.md)(PR #50 판본) — `@Transactional` 은 유스케이스에만, 진입점·`AFTER_COMMIT` 리스너에는 없음 · 유스케이스는 바뀐 애플리케이션 예외만, 실패한 트랜잭션 밖에서 잡는다.
- 레포 규칙 [failure-handling.md PTF6](../../../../../../rules/failure-handling.md) — 예수금 차감·홀드는 주문을 받는 요청 경로 트랜잭션 안에서 즉시.
- devkit 팀 규칙 `rules/dev-team/data-access.md` — DA1(트랜잭션 밖 외부 I/O) · DA3(쓰기 경로의 커밋 보장, 행 수는 성공 증거 아님) · DA4(DB 오류 뒤 롤백·세션 재사용 금지, 일괄은 원자) · DA5(한 트랜잭션 = 한 애그리거트 한 전이) · DA6(이벤트 핸들러는 커밋하지 않음) · DA7(되돌릴 수 없는 부수효과는 커밋 뒤) · DA9(의도 기록 → 트랜잭션 밖 호출 → 결과 반영) · DA10(읽고-고쳐-쓰기 보호) · DA15(일괄 작업은 행을 점유하고 «진행 중» 커밋 뒤 부수효과).
- Spring Framework 문서 `TransactionSynchronization.afterCommit` — 여기서 트랜잭션 작업은 `PROPAGATION_REQUIRES_NEW` 를 쓴다(PTD4 근거).
- Amazon Builders' Library «Making retries safe with idempotent APIs» — 동시 읽고-고쳐-쓰기엔 버전이 필요 → X8.
- 알려진 규칙 충돌: main 판본 PTD4 는 «`AFTER_COMMIT` 리스너가 `@Transactional(REQUIRES_NEW)` 로 결과를 쓴다»로 읽혀 리스너 메서드 자신의 `@Transactional(REQUIRES_NEW)` 를 허용하는 것처럼 보인다. 이 렌즈는 PR #50 판본으로 판정하므로 그 모양을 X5 발견으로 남기고 `ruleConflicts` 에 `rules/data-access.md PTD4 (main 판본)` 행을 단다.

## Deterministic first
| Tool | Command | Proves | If absent |
|---|---|---|---|
| ArchUnit | `cd backend/trading-api && ./gradlew test --tests '*ArchitectureRulesTest*' --console=plain` | PTB9 의 application Spring 허용 목록 위반(동결 목록 포함)이 파일별로 출력에 있음 — X10 근거 | 읽는다 |
| grep (경계·리스너) | `grep -nE '@Transactional\|@TransactionalEventListener\|@EventListener\|TransactionPhase\|propagation\|REQUIRES_NEW\|readOnly' <chunk-files>` | X1~X5 의 모집단 | 읽는다 |
| grep (원격 호출) | `grep -nE 'restTemplate\.\|RestTemplate\|WebClient\|RestClient\|convertAndSend\|\.send\(\|Notifier\|Executor\.\|execute\(' <chunk-files>` | X3 의 원격 호출 자리 — 열린 트랜잭션과 교차해 본다 | 읽는다 |
| LLM-only | 자기 호출(X2)·트랜잭션 범위와 원격 호출(X3)·애그리거트 대응(X7)·점유 뒤 부수효과(X11) | — | 발견의 `confidence` 에 «LLM-only» |

명령 칸의 `\|` 는 `|` 의 마크다운 이스케이프다. `<chunk-files>` = 내 인스턴스의 파일 목록.

## Checks
| ID | Signal — what you see in the file | Why it matters | Default severity |
|---|---|---|---|
| X1 | `@Transactional` 이 진입점에 있음: `@Scheduled` 메서드·Redis `onMessage`·WebSocket 핸들러·컨트롤러 메서드·`@EventListener(ApplicationReadyEvent)`; 또는 쓰기 유스케이스가 `@Transactional` 없이 여러 `save` 를 호출(각 리포지토리 호출이 따로 커밋) (PTD1·PTB9) | 같은 유스케이스가 진입 경로마다 다른 범위로 돈다. 경계가 없으면 중간 실패에 앞 쓰기만 커밋된다(DA3) | high (경계 없음) · med (진입점) |
| X2 | 자기 호출: 같은 클래스의 `this.method()` 로 `@Transactional`·`@Transactional(propagation = REQUIRES_NEW)` 메서드를 부름, `private`·`protected` 메서드에 `@Transactional` (PTD1) | 프록시를 안 거쳐 어노테이션이 있어도 트랜잭션이 없고 오류도 안 난다 — `REQUIRES_NEW` 를 믿은 결과 기록이 바깥 롤백에 같이 사라진다 | high |
| X3 | 열린 트랜잭션 안(어노테이션 메서드, 그것이 부르는 어노테이션 없는 메서드, `BEFORE_COMMIT` 리스너)에서 KIS 주문·조회(`Kis*Client`·`*Executor`)·`RestTemplate`·`WebClient`·Slack 웹훅·Redis `convertAndSend`·`SseEmitter.send` 호출 (PTD2·DA1) | 외부 지연이 커넥션·행 락을 붙잡아 무관한 요청까지 막고, 원격 성공 + 커밋 실패 = 고아 부수효과(이중 주문의 씨앗) | high |
| X4 | 본 쓰기와 원자여야 하는 같은 DB 쓰기(원장 행·체결 반영·잔고 갱신)가 `AFTER_COMMIT` 리스너·`@Async`·별도 스레드에 있음; 또는 트랜잭션 안 리스너(`@EventListener`·`BEFORE_COMMIT`)가 스스로 커밋(`REQUIRES_NEW`·`TransactionTemplate`)함 (PTD4·DA6) | 본 쓰기 성공 뒤 사라지는 쓰기, 또는 본 쓰기 실패와 별개로 살아남는 부수 쓰기 — 원장과 잔고가 어긋난다 | high |
| X5 | `AFTER_COMMIT` 리스너 메서드 자신에 `@Transactional`(어떤 전파든)이 있음; 또는 리스너가 부르는 쓰기 유스케이스가 기본 `REQUIRED` 라 커밋이 뒤따르지 않음; 또는 `REQUIRES_NEW` 결과 기록이 원본 id 로 중복을 거르지 않음 (PTD4 PR #50) | `REQUIRED` 는 이미 커밋된 자원에 참여만 해 오류 없이 쓰기가 사라지고, 리스너 자체 `@Transactional` 은 리스너 안 외부 호출까지 트랜잭션에 넣어 PTD2 를 깬다 | high |
| X6 | 영속 예외를 유스케이스 안에서 잡고 계속 진행: `catch (e: DataIntegrityViolationException)`·`OptimisticLockingFailureException`·`ObjectOptimisticLockingFailureException` 을 같은 `@Transactional` 메서드 안에서 잡아 대체 경로·재시도; `save()` 후 `flush` 없이 어댑터 밖으로 나가 커밋 시점에 터짐 (PTD3·PTB9) | 참여 트랜잭션은 rollback-only 가 되어 커밋 때 `UnexpectedRollbackException` — 잡았다고 믿은 실패가 500 으로 나간다 | high |
| X7 | 한 `@Transactional` 메서드가 애그리거트 루트 2개 이상의 불변식을 바꿈(주문 상태 + 계좌 잔고 + 포지션 수량을 한 커밋에) — 원장·감사 행 추가는 제외 (DA5·DD12) | 불변식 둘이 한 락에 묶여 락 범위가 넓어지고 독립적으로 실패할 수 없다 | med |
| X8 | 잔고·예수금·포지션 수량·주문 상태를 읽고 고쳐 쓰는데 `@Version`·`@Lock(LockModeType.PESSIMISTIC_WRITE)`·조건부 UPDATE(`WHERE status = :expected`·`SET x = x + :n`) 중 무엇도 없음 — 조회 뒤 `save()` 로 덮어씀 (PTD3·DA10) | 기본 격리 수준(READ COMMITTED)에서 동시 요청이 서로의 갱신을 덮어써 체결 금액이 사라진다 | high |
| X9 | 예수금 차감·홀드(주문 가능 금액 감소)가 주문 요청 트랜잭션 밖 — `@Async`·이벤트 리스너·스케줄러·매칭 엔진 체결 시점에서만 차감 (PTF6) | 처리 전에 같은 예수금으로 다음 주문이 통과해 초과 주문이 난다 | high |
| X10 | 어댑터가 영속 예외를 바꾸지 않고 Spring 타입(`DataIntegrityViolationException`·`DataAccessException`)이 application 으로 올라옴, 또는 `@Modifying` 벌크 쿼리 뒤 같은 트랜잭션이 영속성 컨텍스트의 낡은 엔티티를 읽음 (PTB9·PTD5) | JPA 세부가 업무 분기로 새고, 벌크 쿼리 뒤 낡은 잔고로 계산한다 | med |
| X11 | 스케줄·일괄 작업이 행을 점유(`@Lock`·`FOR UPDATE SKIP LOCKED`·상태 `PENDING → PROCESSING` 조건부 UPDATE)하지 않고 부수효과(KIS 주문·알림 발송·스냅샷 기록)를 냄, 또는 점유 커밋 전에 부수효과 (DA15) | 인스턴스 둘·재시작에서 같은 부수효과가 두 번 난다 | high |
| X12 | 호출자가 한 작업으로 보는 일괄 안에서 항목마다 `REQUIRES_NEW`·`TransactionTemplate` 커밋, 또는 루프 안 `saveAndFlush` 로 부분 커밋 — 응답은 전체 실패 (DA4) | 1..k-1 이 커밋된 채 실패 응답 — 부분 상태 | med |
| X13 | 레포 단위: `spring.jpa.open-in-view` 가 `false` 로 명시됐는가 · 트랜잭션 정책이 한 가지인가(`@Transactional` 과 `TransactionTemplate`·수동 `EntityManager.getTransaction()` 혼용 여부) · `@EnableAsync` 스레드에서 영속 엔티티를 넘기는 경로가 있는가 — `(repo)` 에 기록 | open-in-view 는 요청 끝까지 커넥션을 붙잡고 지연 로딩을 트랜잭션 밖에서 허용한다. 정책이 둘이면 새 코드가 짐작한다 | med |

## Disprove before reporting
- X1: 읽기 전용 조회 컨트롤러가 `@Transactional(readOnly = true)` 서비스를 부르는 것은 통과 — 발견은 진입점 자신의 어노테이션.
- X2: 같은 클래스 호출이라도 바깥 메서드가 이미 `@Transactional` 이고 안쪽이 같은 범위를 기대하면 실해 없음 — `REQUIRES_NEW`·`readOnly` 차이를 기대할 때만 발견.
- X3: `AFTER_COMMIT` 리스너 안 외부 호출은 트랜잭션 밖 — 통과(단 X5 를 본다). 같은 프로세스 지역 함수 호출은 원격이 아니다.
- X7: 부모 + 자기 자식(`Order` + `OrderAmendment`)은 한 애그리거트. 설정 성격 컨텍스트는 n/a.
- X8: 단일 경로에서 원자적 `SET x = x + :n` 으로만 바뀌는 열은 버전이 필요 없다 — 통과.
- X9: 체결 시 확정 차감과 별개로 주문 접수 시 홀드가 이미 트랜잭션 안에 있으면 통과.

## Out of scope
- N+1·벌크 쿼리 비용·조회 상한 → data/query. 락이 잡힌 동안의 비용(Q 계열) → data/query.
- 멱등 키·재시도 안전성·불확정 응답(UNKNOWN) → reliability/failure-handling. 종목 직렬화·스레드풀 → reliability/load-concurrency.
- 리스너·진입점의 패키지 자리 → backend/layering L6.
