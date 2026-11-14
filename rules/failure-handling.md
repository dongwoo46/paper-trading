# Failure handling — 원장·대사·불확정 응답 (paper-trading)
<!-- devkit:pointer
path: ^backend/(trading-api|collector-api)/src/main/.+\.kt$
signal: Ledger|ledger|Execution|Settlement|Balance|balance|Deposit|Cash|Reconcil|reconcil|timeout|Timeout|@Scheduled|KisClient|kis
why: 원장·체결·잔고·정산이나 외부(KIS) 호출 결과를 다루는 Kotlin 코드
-->
<!-- devkit:form v2 -->

> 이 레포 전용 사본. 이 문서 조항 가운데 PTF1~PTF5 는 스택 무관이라 devkit 팀 규칙 제안으로도 올렸고(PTF1~PTF3 은 제안 financial-integrity-eda-followup-2026-11-15, PTF4·PTF5 는 BiocomKR/devkit#2471 2차), 그 제안이 머지되면 PTF1~PTF5 는 지운다. 다만 PTF5 의 이산 날짜·연도 닫힌 구간 예외는 제안 판본에 없으면 지우지 않고 남긴다. PTF6(예수금 홀드를 요청 경로에서)은 어느 제안에도 없는 이 레포 조항이라 남긴다. 멱등 쓰기·소비자 중복 제거의 원칙은 팀 규칙 `failure-handling.md`·`data-access.md` DA9 가 SSoT.
> devkit 은 이 레포의 Kotlin 파일에 팀 규칙을 자동으로 전달하지 않는다(rule-pointer 실측 2026-11-15: `always:dev-team` 만 나온다). 그래서 주문·결제성 쓰기를 만지는 Kotlin 작업·리뷰는 팀 `failure-handling.md` FH1(생성 요청은 `Idempotency-Key` 나 자연 키 upsert)과 `async-external.md` LA11(원격 호출 연결·읽기 타임아웃 명시)을 직접 열어 따른다. devkit 쪽 Kotlin 신호 추가는 devkit 에 제안으로 올린다.

## 원장

<!-- gt:PTF1 -->
- 돈의 이동, 체결, 예수금 변동을 기록한 원장 행은 UPDATE·DELETE 하지 않는다. 정정은 원래 행을 가리키는 반대 행을 추가해서 한다.
  Why: 고쳐 쓰면 감사에 필요한 흔적이 사라지고 대사가 원장으로 잔고를 다시 계산할 수 없다. 루트 `CLAUDE.md` «모든 금융 상태 변경은 감사·추적 가능» 의 구조다(토스·우아한형제들 공개 사례). — enforce: review

## 대사

<!-- gt:PTF2 -->
- 원장에서 파생되는 잔고·포지션은 주기 작업이 원본(원장, 외부 체결 내역)으로 다시 계산해 대조하고, 불일치는 기록을 남겨 보정하거나 알린다.
  Why: 중복되거나 유실된 이벤트(Redis Pub/Sub 유실 포함, `async-external.md` PTA2)는 잔고를 조용히 어긋나게 하고 아무 시험도 그것을 잡지 못한다(토스 공개 사례). — enforce: review

## 불확정 응답

<!-- gt:PTF3 -->
- 외부 호출(KIS 주문 등)이 타임아웃되거나 확정 응답 없이 끝나면 그 의도를 실패가 아닌 UNKNOWN 상태로 두고, 재시도하기 전에 조회나 취소로 결과를 확정한다.
  Why: 타임아웃은 실패가 아니다. 상대가 처리를 끝냈을 수 있어 실패로 보고 재시도하면 이중 주문·이중 체결이 난다. 팀 규칙 DA9(의도 기록 → 트랜잭션 밖 호출 → 결과 반영)의 결과 반영 단계를 강화한다(카카오페이·토스 공개 사례). — enforce: review

## 계산 근거와 시간 경계

<!-- gt:PTF4 -->
- 수수료, 세금, 이자처럼 계산한 금액을 저장하는 행에는 그때 쓴 요율과 정책 버전을 함께 저장하고, 나중의 재계산과 감사는 저장된 요율을 읽는다(현재 설정을 다시 읽지 않는다).
  Why: 요율은 바뀐다. 과거 체결을 오늘 요율로 다시 계산하면 이력이 조용히 바뀌고 대사(PTF2)가 거짓 불일치를 낸다(토스 공개 사례). — enforce: review
<!-- gt:PTF5 -->
- 연속 시각(`Instant`·`LocalDateTime`) 구간은 반열림 `[start, end)` 으로 다룬다. 조회는 `>= start AND < end` 로 쓰고 `BETWEEN`·Querydsl `.between(`·Spring Data 파생 메서드 `...Between` 을 쓰지 않으며, 다음 구간은 앞 구간의 end 에서 시작한다. 이산 날짜·연도(`LocalDate`·`Int` 연도) 구간은 양끝을 포함하는 닫힌 구간이 맞고, 이 조항 대상이 아니다.
  Why: `BETWEEN` 은 양끝을 포함해 경계 시각의 체결이 두 구간(일봉, 정산일)에 겹쳐 세지고, 끝을 23:59:59 로 잡으면 그 사이의 행을 잃는다(우아한형제들 공개 사례). 날짜는 하루 단위라 «끝 날짜 포함»이 정의이므로, 이산 날짜 API(`DailyBalanceRepositoryImpl` 의 `businessDate.between(from, to)`, `TaxSummaryRepositoryImpl` 의 `taxYear.between`)를 글자 그대로 `< to` 로 바꾸면 마지막 날이 빠진다. 파생 메서드의 `...Between` 은 `BETWEEN` 글자가 없어 리뷰에서 놓치기 쉬워 대상으로 적는다. — enforce: review

## 예수금 홀드

<!-- gt:PTF6 -->
- 주문 가능 금액을 줄이는 판단(예수금 차감, 홀드)은 비동기 큐 뒤가 아니라 주문을 받는 요청 경로의 트랜잭션 안에서 즉시 하고, 큐와 이벤트는 그 결과를 알리는 데만 쓴다.
  Why: 큐 뒤에서 차감하면 메시지가 처리되기 전에 같은 예수금으로 다음 주문이 통과해 초과 주문이 난다(우아한형제들 공개 사례). 동시 요청 보호는 `data-access.md` PTD3. — enforce: review

<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd.md -->
<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd-r3.md -->
<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd-r5.md -->
<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd-r6.md -->
