# Async·external — Kotlin 코루틴·Redis Pub/Sub (paper-trading)
<!-- devkit:pointer
path: ^backend/(trading-api|collector-api)/src/main/.+\.kt$
signal: suspend fun|runBlocking|Dispatchers|\.block\(|WebClient|MessageListener|RedisMessageListenerContainer|convertAndSend|\.publish\(|onMessage\(
why: 코루틴·블로킹 호출이나 Redis Pub/Sub 발행·구독을 다루는 Kotlin 코드
-->
<!-- devkit:form v2 -->

> 이 레포 전용 규칙 — devkit 팀 규칙 `async-external.md`(LA1~)를 Kotlin·Spring 에서 지키는 방법만 적는다. 원칙은 팀 규칙이 SSoT.
> devkit 은 이 레포의 Kotlin 파일에 팀 규칙을 자동으로 전달하지 않는다(rule-pointer 실측 2026-11-15: `always:dev-team` 만 나온다). 그래서 이 레포 Kotlin 작업·리뷰는 팀 `async-external.md` 의 LA1(PTA1)과 LA21(큐 상한)·LA22(무거운 일을 요청 경로 밖으로, 유실 허용 여부)·LA11(원격 호출 연결·읽기 타임아웃 명시)을 직접 열어 따른다. devkit 쪽 Kotlin 신호 추가는 devkit 에 제안으로 올린다.

## 블로킹

<!-- gt:PTA1 -->
- `suspend` 함수 안의 JPA·JDBC·블로킹 I/O 호출은 `withContext(Dispatchers.IO)` 로 감싼다. 요청을 처리하는 스레드에서 `runBlocking` 과 `.block()` 을 쓰지 않는다.
  Why: 팀 규칙 LA1 의 Kotlin 형태. 기본 디스패처나 이벤트 루프 스레드를 막으면 그 스레드를 쓰는 모든 요청이 함께 멈춘다. — enforce: review

## Redis Pub/Sub

<!-- gt:PTA2 -->
- Redis Pub/Sub 리스너(`onMessage`)는 메시지를 받아 넘기기만 하고 무거운 처리(매칭·DB 쓰기)는 큐 크기에 상한이 있는 실행기로 넘기며(팀 규칙 LA21), 같은 종목 키의 메시지는 종목 단위로 직렬 처리해(종목 키 단일 소비자 또는 종목 락) 두 메시지가 같은 종목을 동시에 매칭하지 못하게 하고, 그 직렬 처리 안에서 이미 처리한 `updatedAt` 보다 새롭지 않은 메시지는 버린다(낡은 시세 폐기, 직렬 처리와 `updatedAt` 비교 둘 다 필요하다). 시세 메시지 자체는 유실을 허용한다(다음 값이 덮어쓴다). 그러나 시세 외 메시지로 돈·주문·포지션 상태를 바꾸는 Pub/Sub 처리는 이 통로에만 기대지 않고 원본과 대조하는 복구 경로를 둔다. 시세가 촉발하는 매칭·청산 판정(미체결 지정가, 청산 조건)도 주문·포지션 상태를 바꾸는 처리라서, 주기 작업이 놓친 구간의 고가·저가(데이터 출처는 구현 때 정한다)로 지정가·손절선 도달을 판정해 미체결 주문과 청산 조건을 다시 대조하는 복구 경로를 둔다.
  Why: 이 레포의 `RedisListenerConfig` 는 `RedisMessageListenerContainer` 에 실행기를 지정하지 않아 기본 `SimpleAsyncTaskExecutor` 가 메시지마다 새 스레드를 만든다. 실제 위험은 스레드 수가 무제한이라는 것과 같은 종목의 시세가 순서가 뒤바뀌어 낡은 가격으로 매칭하거나 같은 종목을 동시에 매칭하는 것이다. 종목 락이나 단일 소비자는 동시 실행만 막고 순서는 지키지 않는다. 실행기 스레드가 여럿이면 t=5 와 t=6 메시지가 각자 스레드에서 락을 다투다 t=6 이 먼저 매칭하고 이어 t=5 가 낡은 호가로 지정가를 체결할 수 있고, 단일 소비자도 `onMessage` 가 여러 스레드에서 불리면 큐에 넣는 순서부터 뒤바뀐다. 그래서 동시 매칭은 직렬 처리로 막고 순서 역전은 직렬 처리 안의 `updatedAt` 비교로 막는다(직렬 처리 안이라 compare-and-set 없이 단순 비교로 충분하고, compare-and-set 은 받아들일 메시지를 고를 뿐 처리를 직렬화하지 않아 앞 메시지가 매칭 중일 때 더 새 메시지도 통과하므로 직렬화 대용이 못 된다). Pub/Sub 은 구독자가 끊긴 동안의 메시지를 버리는 통로(at-most-once)라 재시작·네트워크 단절 중 시세나 신호는 흔적 없이 사라진다. 시세처럼 다음 값이 곧 덮어쓰는 데이터는 유실을 받아들여도 되지만, 체결·잔고는 안 된다. 팀 규칙 LA22 가 인정하는 «유실 허용»은 시세 메시지 자체에만 적용하고, 그 메시지가 곧 매칭·청산 트리거(`QuoteEventListener` 가 `tryMatchPendingOrders` 와 `onQuote` 를 부른다)이므로 놓친 틱은 미체결 주문 체결 누락으로 이어진다(사람 결정 2026-11-15: 매칭은 복구가 필요하다). 복구를 최신 가격 한 점으로 대조하면 놓친 틱이 지정가나 손절선을 건드렸다가 되돌아온 경우(지정가 매수 100, 놓친 askp1 99, 최신 101) 조건이 거짓이라 체결·청산이 영구히 빠지므로, 놓친 구간의 고가·저가로 도달을 판정한다(사람 결정 2026-11-15: 출처는 구현 때 정한다). — enforce: review

<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd.md -->
<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd-r5.md -->
<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd-r6.md -->
<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd-r7.md -->
<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd-r8.md -->
