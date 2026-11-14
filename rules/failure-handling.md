# Failure handling — 원장·대사·불확정 응답 (paper-trading)
<!-- devkit:pointer
path: ^backend/(trading-api|collector-api)/src/main/.+\.kt$
signal: Ledger|ledger|Execution|Settlement|Balance|balance|Deposit|Cash|Reconcil|reconcil|timeout|Timeout|@Scheduled|KisClient|kis
why: 원장·체결·잔고·정산이나 외부(KIS) 호출 결과를 다루는 Kotlin 코드
-->
<!-- devkit:form v2 -->

> 이 레포 전용 사본. 세 조항은 스택 무관이라 devkit 팀 규칙 제안으로도 올렸고(제안 financial-integrity-eda-followup-2026-11-15), 그 제안이 머지되면 이 사본은 지운다. 멱등 쓰기·소비자 중복 제거의 원칙은 팀 규칙 `failure-handling.md`·`data-access.md` DA9 가 SSoT.

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

<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd.md -->
