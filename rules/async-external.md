# Async·external — Kotlin 코루틴·Redis Pub/Sub (paper-trading)
<!-- devkit:pointer
path: ^backend/(trading-api|collector-api)/src/main/.+\.kt$
signal: suspend fun|runBlocking|Dispatchers|\.block\(|WebClient|MessageListener|RedisMessageListenerContainer|convertAndSend|\.publish\(|onMessage\(
why: 코루틴·블로킹 호출이나 Redis Pub/Sub 발행·구독을 다루는 Kotlin 코드
-->
<!-- devkit:form v2 -->

> 이 레포 전용 규칙 — devkit 팀 규칙 `async-external.md`(LA1~)를 Kotlin·Spring 에서 지키는 방법만 적는다. 원칙은 팀 규칙이 SSoT.

## 블로킹

<!-- gt:PTA1 -->
- `suspend` 함수 안의 JPA·JDBC·블로킹 I/O 호출은 `withContext(Dispatchers.IO)` 로 감싼다. 요청을 처리하는 스레드에서 `runBlocking` 과 `.block()` 을 쓰지 않는다.
  Why: 팀 규칙 LA1 의 Kotlin 형태. 기본 디스패처나 이벤트 루프 스레드를 막으면 그 스레드를 쓰는 모든 요청이 함께 멈춘다. — enforce: review

## Redis Pub/Sub

<!-- gt:PTA2 -->
- Redis Pub/Sub 리스너(`onMessage`)는 메시지를 받아 넘기기만 하고 무거운 처리(매칭·DB 쓰기)는 별도 실행기로 넘긴다. Pub/Sub 은 구독자가 끊긴 동안의 메시지를 버리는 통로(at-most-once)이므로, 돈이나 주문 상태를 바꾸는 처리는 이 통로에만 기대지 않고 원본과 대조하는 복구 경로를 둔다.
  Why: 리스너 스레드가 막히면 다음 메시지가 밀리고, 재시작·네트워크 단절 중 시세나 신호는 흔적 없이 사라진다. 시세처럼 다음 값이 곧 덮어쓰는 데이터는 유실을 받아들여도 되지만, 체결·잔고는 안 된다. — enforce: review

<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd.md -->
