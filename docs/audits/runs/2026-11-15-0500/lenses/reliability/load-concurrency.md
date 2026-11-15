---
lens: reliability/load-concurrency
dept: reliability
agent: reliability-auditor
title: 부하·동시성 (종목 직렬화·Redis Pub/Sub·스레드풀)
repo-checks: C11
target-signal: @Scheduled|@Async|@EnableAsync|Executor|ThreadPool|Thread\b|synchronized|@Synchronized|ReentrantLock|Mutex|Semaphore|Concurrent[A-Z]|Atomic[A-Z]|@Volatile|@Lock\(|LockModeType|@Version|MessageListener|onMessage\(|RedisMessageListenerContainer|SseEmitter|WebSocket|CompletableFuture|parallelStream|launch\s*\{|async\s*\{|runBlocking|Dispatchers|suspend fun|tryMatch|[Mm]atching|PESSIMISTIC
target-path: /src/main/resources/application[^/]*\.ya?ml$
concerns: 장애
---
# Lens — 부하·동시성 (reliability · Spring 스케줄러/`@Async`/Redis Pub/Sub/SSE/WebSocket)

## Standard
- 레포 규칙 [async-external.md PTA2](../../../../../../rules/async-external.md) — **PR #50 판본**: Redis Pub/Sub 리스너(`onMessage`)는 받아 넘기기만 하고 무거운 처리(매칭·DB 쓰기)는 큐 크기 상한이 있는 실행기로(LA21). **같은 종목 키 메시지는 종목 단위로 직렬 처리**(종목 키 단일 소비자 또는 종목 락)해 두 메시지가 같은 종목을 동시에 매칭하지 못하게 하고, `updatedAt` 비교·갱신(종목별 compare-and-set)은 **낡은 시세를 버리는 데만** 쓴다(CAS 는 처리를 직렬화하지 않는다). 시세 유실은 허용하지만, 시세가 촉발하는 매칭·청산 판정은 주기 작업이 놓친 구간의 고가·저가로 미체결 지정가·청산 조건을 다시 대조하는 복구 경로를 둔다.
- 레포 규칙 PTA1 — `suspend` 함수 안 JPA·JDBC·블로킹 I/O 는 `withContext(Dispatchers.IO)`, 요청 스레드에서 `runBlocking`·`.block()` 금지.
- 레포 규칙 [data-access.md PTD3](../../../../../../rules/data-access.md) — 읽고-고쳐-쓰기 보호(`@Version`·비관 락·조건부 UPDATE).
- 레포 규칙 [failure-handling.md PTF6](../../../../../../rules/failure-handling.md) — 예수금 홀드는 요청 경로에서 즉시(큐 뒤 차감 금지).
- devkit 팀 규칙 `rules/dev-team/async-external.md` — LA1(비동기 경로의 블로킹 I/O) · LA3(시작한 작업의 핸들·예외) · LA4(입력 비례 외부 호출 상한) · LA8(스레드에서 도는 핸들러는 호출마다 자기 세션·가변 객체) · LA9(«진행 중» 플래그·사용자 상태를 프로세스 메모리에 두지 않음) · LA10(잃으면 안 되는 일은 내구성 있는 작업으로) · LA16(외부 클라이언트는 프로세스당 하나, 연결 상한 명시) · LA18(커넥션 풀 예산) · LA21(큐 크기 상한·최대 전달·DLQ·나이 기반 폐기) · LA22(무거운 일은 요청 경로 밖, 유실 허용 여부) · LA24(주기·재시도 지터) · LA25(다중 인스턴스 예약 작업은 분산 단일 실행 임대) · LA26(요청 타임아웃·정상 종료·동시성 한도 명시).
- Spring Framework 문서 — `RedisMessageListenerContainer` 에 `taskExecutor` 를 지정하지 않으면 `SimpleAsyncTaskExecutor`(메시지마다 새 스레드, 상한 없음) · `@Async` 기본 실행기 · `@Scheduled` 기본 단일 스레드 스케줄러.
- Amazon Builders' Library «Timeouts, retries, and backoff with jitter» → C9.
- 알려진 규칙 충돌: main 판본 PTA2 는 «종목 단위 직렬 처리 **또는** 종목별 CAS» 로 둘 중 하나를 허용한다. 이 렌즈는 PR #50 판본(직렬화 필수, CAS 는 낡은 값 폐기 전용)으로 판정 — CAS 만 있고 직렬화가 없으면 A2 발견으로 남기고 `ruleConflicts` 에 `rules/async-external.md PTA2 (main 판본)` 행을 단다.

## Deterministic first
| Tool | Command | Proves | If absent |
|---|---|---|---|
| grep (실행기·스레드) | `grep -nE 'RedisMessageListenerContainer\|setTaskExecutor\|ThreadPoolTaskExecutor\|SimpleAsyncTaskExecutor\|Executors\.\|@Async\|@EnableAsync\|@Scheduled\|setPoolSize\|queueCapacity\|setQueueCapacity' <chunk-files>` | A1·A3·C1 후보 | 읽는다 |
| grep (공유 상태·락) | `grep -nE 'ConcurrentHashMap\|mutableMapOf\|mutableListOf\|mutableSetOf\|synchronized\|ReentrantLock\|Mutex\|AtomicReference\|AtomicBoolean\|@Volatile\|companion object' <chunk-files>` | A4·A5 후보 | 읽는다 |
| LLM-only | 종목 직렬화 여부(A2)·복구 경로(C11)·다중 인스턴스 단일 실행(C3) | — | 발견의 `confidence` 에 «LLM-only» |

명령 칸의 `\|` 는 `|` 의 마크다운 이스케이프다. `<chunk-files>` = 내 인스턴스의 파일 목록.

## Checks
| ID | Signal — what you see in the file | Why it matters | Default severity |
|---|---|---|---|
| A1 | Redis `MessageListener.onMessage`·`@RedisListener` 성격 처리기가 매칭·DB 쓰기·외부 호출을 직접 함, 또는 `RedisMessageListenerContainer` 에 `setTaskExecutor` 가 없음(기본 `SimpleAsyncTaskExecutor`)·실행기 큐에 상한(`queueCapacity`)과 거부 정책이 없음 (PTA2·LA21) | 메시지마다 새 스레드 — 시세 폭주 때 스레드 수 무제한, OOM·커넥션 풀 고갈 | high |
| A2 | 같은 종목의 시세·매칭이 직렬화되지 않음: 종목 키 단일 소비자·종목 락(`ConcurrentHashMap<String, Mutex/Lock>`·Redis 락·`SELECT … FOR UPDATE` on 종목 행) 없이 여러 스레드가 `tryMatchPendingOrders(symbol)`·청산 판정을 동시에 탈 수 있음; `updatedAt` CAS 만으로 동시 매칭을 막는다고 가정 (PTA2 PR #50) | 두 메시지가 같은 종목을 동시에 매칭해 한 주문이 두 번 체결되거나 낡은 가격으로 매칭된다 | high |
| A3 | `@Async`·`CompletableFuture.runAsync`·`Thread { }.start()`·`Executors.newFixedThreadPool` 이 기본 실행기·무제한 큐로 돈다; 시작한 작업의 결과·예외를 아무도 읽지 않음(`AsyncUncaughtExceptionHandler` 없음, 반환 `CompletableFuture` 버림) (LA3·LA21) | 예외가 로그 없이 사라지고, 큐가 무한히 쌓여 메모리가 마른다 | med |
| A4 | 공유 가변 상태: 싱글턴 빈(`@Service`·`@Component`)의 `var` 필드·`mutableMapOf`·`mutableListOf`·`HashMap` 을 여러 스레드(요청·스케줄러·리스너)가 쓰는데 `ConcurrentHashMap`·락·불변 교체가 없음; `companion object` 가변 상태 (LA8·LA9) | 경쟁 조건 — `ConcurrentModificationException` 이나 조용한 갱신 유실 | high |
| A5 | «진행 중» 플래그·대기 집합(`@Volatile var running`·`processingIds`)을 프로세스 메모리에 둬 중복 실행을 막는다고 가정; `finally` 에서 안 풂 (LA9) | 인스턴스 둘이면 서로의 플래그를 못 보고, 예외 한 번이면 플래그가 영원히 남는다 | med |
| A6 | 확인-후-행동: 존재 조회 → 삽입·갱신, 또는 `entity.qty = entity.qty - n` 후 `save` 를 유일 제약·`@Version`·`@Lock`·조건부 UPDATE·종목 락 없이 동시 경로(매칭·자동 청산·주문 접수)에서 (PTD3) | 동시 매칭·청산이 같은 포지션을 두 번 줄인다 | high |
| A7 | `suspend` 함수 안 JPA 리포지토리·JDBC·`RestTemplate`·`Thread.sleep` 호출이 `withContext(Dispatchers.IO)` 밖; 요청 처리 스레드의 `runBlocking`·`.block()` (PTA1·LA1) | 기본 디스패처·이벤트 루프 스레드를 막아 그 스레드를 쓰는 모든 요청이 멈춘다 | high |
| C1 | `@Scheduled` 작업이 기본 단일 스레드 스케줄러(`spring.task.scheduling.pool.size` 미설정)를 공유하는데 한 작업이 외부 호출·대량 처리로 오래 걸림 — 다른 작업(매칭·KIS 폴링·알림)이 밀림 | 느린 작업 하나가 매칭 스케줄을 굶겨 체결 지연이 생긴다 | med |
| C2 | 주기 작업 한 번의 비용이 변경량·실패 상태에 비례: 모든 미체결 주문·모든 포지션을 매 틱 전체 조회·평가(배치 상한 없음), 실패하면 다음 틱에 전체 재처리 (LA23) | 장중 주문 수가 늘면 틱이 다음 틱을 넘어 쌓인다 | med |
| C3 | 다중 인스턴스에서 같이 도는 `@Scheduled`(매칭·KIS 폴링·스냅샷·세금 배치·Slack 발송)가 분산 단일 실행 임대(ShedLock·Redis `SET NX PX`·PG advisory lock·행 점유)가 없음, 인스턴스 수 가정이 코드·설정에 없음 (LA25) | 인스턴스 둘이면 같은 매칭·같은 알림이 두 번 — 이중 체결 | high |
| C4 | 연결별 장기 자원: `SseEmitter` 레지스트리가 타임아웃·완료·오류 콜백에서 제거하지 않음(`onCompletion`·`onTimeout`·`onError`), 연결 수 상한 없음; KIS WebSocket 재연결이 지터 없는 고정 간격·무한 재시도 (LA16·LA24) | 끊긴 emitter 가 쌓여 메모리·스레드가 새고, 재연결 폭주가 KIS 제한을 친다 | med |
| C5 | 입력 비례 외부 호출: 주문·종목 목록을 돌며 KIS·collector-api 를 건당 호출(`forEach { kisClient.… }`)하는데 개수 상한·동시성 상한·일괄 API 가 없음 (LA4) | 요청 하나가 외부 호출 N 개가 되어 KIS 초당 호출 제한에 걸린다 | med |
| C6 | 런타임 설정 부재(`application.yaml`): `spring.datasource.hikari.maximum-pool-size`·`connection-timeout` 미설정 또는 근거 없음, `server.tomcat.threads.max`·`spring.lifecycle.timeout-per-shutdown-phase`·`server.shutdown: graceful` 미설정 (LA18·LA26) | 기본값에 기대면 풀·스레드 상한이 인스턴스 수와 DB 연결 한도에 맞지 않고, 배포 때 처리 중 매칭이 끊긴다 | low |
| C11 | 레포 단위 복구 경로(PTA2 PR #50): 놓친 시세 구간의 고가·저가로 미체결 지정가·손절선 도달을 다시 판정하는 주기 작업이 있는가, 재시작·Redis 단절 뒤 그 구간을 메우는가 — `(repo)` 에 기록 | Pub/Sub 은 at-most-once — 놓친 틱이 지정가를 건드렸다 돌아오면 체결·청산이 영구히 빠진다 | high |

## Disprove before reporting
- A1: `onMessage` 가 역직렬화 후 상한 있는 실행기·종목별 큐에 넘기기만 하면 통과.
- A2: 매칭이 단일 스레드 스케줄러 한 곳에서만 돌고 리스너는 시세 저장만 한다면 직렬화가 구조로 보장 — 근거(호출 경로)를 `notes` 에 적고 통과.
- A4: 생성 후 바뀌지 않는 `val` 맵·`ConcurrentHashMap` 의 원자 연산(`compute`·`merge`)만 쓰면 통과. 스레드 하나만 쓰는 필드는 그 근거를 적고 통과.
- C3: 배포가 단일 인스턴스로 고정된 근거(Dockerfile·배포 설정·문서)가 있으면 low 로 낮춘다 — 근거 없으면 그대로.
- C6: 값이 있으면 근거 주석이 없어도 통과(근거 없음은 low 상한).

## Out of scope
- 트랜잭션 범위·`@Version` 매핑 자체 → data/transaction-boundary·data/schema-data-model. N+1·조회 상한 → data/query.
- 타임아웃·재시도·멱등·대사 → reliability/failure-handling. 큐 깊이·작업 지연 측정 → observability.
