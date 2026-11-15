# Root-cause chain — KIS 매수 부분 체결에서 주문 누적 금액이 틀리고 최종 체결 때 잠금 해제액이 틀림

status: resolved

> PR-A1 (감사 원장 #8 #127 #108). 계획: `development-plan.md` §1-1·§2 D-01·D-02.

## 1. Chain

| Layer | Content |
|---|---|
| Symptom | KIS_PAPER·KIS_LIVE 지정가 매수 10@70000 이 4@69000 → 6@70000 으로 나눠 체결되면 주문 누적 금액이 693600(정답 696000)으로 저장되고, 최종 체결 트랜잭션이 «잠금 예수금이 부족합니다.» 로 롤백된다(계좌에 다른 잠금이 있으면 그 잠금이 풀려 가용 예수금이 부풂) |
| Direct cause | 서비스가 누적 평균가를 계산해 `Order.applyExecution` 에 «평균가»로 넘기고, Order 는 그 값에 이번 수량을 곱해 totalAmount 에 더한다(ExecutionProcessor.kt processFill 평균가 산출 → Order.kt applyExecution). 최종 체결 해제액은 «주문 잠금 − 마지막 체결 1건 금액»(ExecutionProcessor.kt releaseLockedExcess)이라 앞선 체결이 이미 쓴 잠금을 또 풀려 한다 |
| Enabling structure | 체결 금액 계약이 도메인에 없다 — 평균가·금액 계산이 서비스에 있고 Order 는 서비스가 넘긴 값을 그대로 쌓는다. 잠금 해제 규칙도 두 벌이다: 도메인 `Order.remainingLockedAmount()` 는 main 호출처가 0건이고 서비스가 자기 식으로 해제액을 계산한다. 도메인 시험(OrderTest)은 체결가를 넘기는 계약으로 짜여 운영 계약과 달랐다 |

## 2. Fix

| Item | Value |
|---|---|
| Site | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt#ExecutionProcessor |
| Fixed layer | Enabling structure |
| Clause | DD11 |
| Changed files | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt · backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt |
| Recurrence |  |
| Why the upper layer stays | n/a |

## 3. Siblings (other sites sharing the same cause)

| Item | Value |
|---|---|
| Search | `applyExecution`·`remainingLockedAmount`·`releaseLockedExcess` 를 backend/trading-api/src 전체에서 grep -rn (main·test). serena find_referencing_symbols 는 Kotlin 언어 서버 기동 실패로 못 썼다. 결과: applyExecution 호출처 = ExecutionProcessor 1곳, remainingLockedAmount 의 main 호출처 = 0건(시험만), releaseLockedExcess = ExecutionProcessor 안 1곳 |

| Site (file:line) | Fixed / Left | Reason |
|---|---|---|
| backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:115 | Fixed | 평균가 산출 삭제, 최종 체결 해제를 Order.remainingLockedAmount() 로 |
| backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:256 | Left | 취소 해제액이 같은 구조(서비스가 자기 식으로 계산 — 잠금 ÷ 수량 × 잔량)를 탄다. 계획 §5 의 PR-A2(#253)에서 고친다 |
| backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:108 | Left | 수수료 계산이 서비스에 있다(#245 #126). 계획 §5 의 PR-A5 범위 |
