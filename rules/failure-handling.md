# Failure handling — 시간 경계·예수금 홀드 (paper-trading)
<!-- devkit:pointer
path: ^backend/(trading-api|collector-api)/src/main/.+\.kt$
signal: Ledger|ledger|Execution|Settlement|Balance|balance|Deposit|Cash|Reconcil|reconcil|timeout|Timeout|@Scheduled|KisClient|kis
why: 원장·체결·잔고·정산이나 외부(KIS) 호출 결과를 다루는 Kotlin 코드
-->
<!-- devkit:scope narrows DT9 -->
<!-- devkit:form v2 -->

> 이 레포 전용 규칙. 원장·대사·불확정 응답·계산 근거(옛 PTF1~PTF4)는 devkit 1.2.0 팀 규칙 `failure-handling.md` FH7~FH10 이 SSoT 라 지웠고, PTF5 는 원칙을 devkit `time.md` DT9 에 맡기고 이 레포 고유 부분만 남겼다. PTF6(예수금 홀드를 요청 경로에서)은 devkit 에 없는 이 레포 조항이다. 멱등 쓰기·소비자 중복 제거의 원칙은 팀 규칙 `failure-handling.md`·`data-access.md` DA9 가 SSoT.
> devkit 은 이 레포의 Kotlin 파일에 팀 규칙을 자동으로 전달하지 않는다(rule-pointer 실측 2026-11-21, devkit 1.2.0: `DailyBalanceRepositoryImpl.kt` 에 `architecture.md` 와 레포 규칙만 나온다). 그래서 주문·결제성 쓰기와 원장·대사·외부 호출 결과를 만지는 Kotlin 작업·리뷰는 팀 `failure-handling.md` FH1(생성 요청은 `Idempotency-Key` 나 자연 키 upsert)·FH7(원장 행은 UPDATE·DELETE 하지 않고 반대 행으로 정정)·FH8(파생 잔고·포지션 주기 대사)·FH9(불확정 응답은 UNKNOWN 으로 두고 확정 뒤 재시도)·FH10(계산 금액에 요율·정책 버전 저장)과 `async-external.md` LA11(원격 호출 연결·읽기 타임아웃 명시), `time.md` DT9(연속 시각 반열림)를 직접 열어 따른다. devkit 쪽 Kotlin 신호 추가는 devkit 에 제안으로 올린다.

## 시간 경계

<!-- gt:PTF5 -->
- 연속 시각(`Instant`·`LocalDateTime`) 구간은 devkit `time.md` DT9(반열림 `[start, end)`, `>= start AND < end`, `BETWEEN` 금지)를 따르고, 이 레포에선 Querydsl `.between(` 과 Spring Data 파생 메서드 `...Between` 도 DT9 가 막는 `BETWEEN` 으로 친다. 이산 날짜·연도 API(`DailyBalanceRepositoryImpl` 의 `businessDate.between(from, to)`, `TaxSummaryRepositoryImpl` 의 `taxYear.between`)는 DT9 Exception 의 닫힌 구간이라 `< to` 로 바꾸지 않는다.
  Why: 파생 메서드 `...Between` 과 Querydsl `.between(` 은 코드에 `BETWEEN` 글자가 없어 리뷰에서 놓치기 쉽고, 이산 날짜 API 를 글자 그대로 `< to` 로 바꾸면 마지막 날이 빠진다(우아한형제들 공개 사례). DT9 는 이 레포 Kotlin 파일에 자동으로 실리지 않는다(머리말). — enforce: review

## 예수금 홀드

<!-- gt:PTF6 -->
- 주문 가능 금액을 줄이는 판단(예수금 차감, 홀드)은 비동기 큐 뒤가 아니라 주문을 받는 요청 경로의 트랜잭션 안에서 즉시 하고, 큐와 이벤트는 그 결과를 알리는 데만 쓴다.
  Why: 큐 뒤에서 차감하면 메시지가 처리되기 전에 같은 예수금으로 다음 주문이 통과해 초과 주문이 난다(우아한형제들 공개 사례). 동시 요청 보호는 `data-access.md` PTD3. — enforce: review

<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd.md -->
<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd-r3.md -->
<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd-r5.md -->
<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd-r6.md -->
