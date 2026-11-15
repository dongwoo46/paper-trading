# Root-cause chain — A1 고침이 체결 금액을 메모리에서 반올림해 소수 수량 체결이 실패하고 초과 수량을 받아들임

status: resolved

> PR-A1 합동 리뷰 P-01·P-02·P-03(회귀)과 같은 구조의 P-04·P-06·P-10·P-11. 사람 결정 2026-11-15 «메모리 반올림 제거»(원칙: 돈은 반올림하면 안 된다).

## 1. Chain

| Layer | Content |
|---|---|
| Symptom | 소수 수량 단일 체결의 주문 평균가가 체결가와 다르고(70000.1234 → 70000.1231), 극소 수량 체결은 금액이 0 으로 반올림돼 «매수 확정 금액은 0보다 커야 합니다.» 로, 소수 수량 체결은 «잠금 예수금이 부족합니다.» 로 실패한다. 남은 수량보다 많은 체결은 받아들여져 다른 주문 잠금을 쓰고, 끝난 주문에 온 새 체결과 해제액 0 하한 깎기는 로그 없이 지나간다 |
| Direct cause | Order.kt applyExecution 이 이번 체결 금액을 setScale(4, HALF_UP) 로 반올림하고 평균가를 그 반올림된 누적 금액으로 나눴다. 계좌 잠금은 반올림 없는 수량×가격이라 둘이 어긋난다. 남은 수량 검사가 없고, ExecutionProcessor.kt processFill 이 끝난 주문·0 하한 경우를 기록하지 않는다 |
| Enabling structure | 금액 계약에 «체결 금액은 메모리에서 반올림하지 않는다»가 없었다. A1 고침이 한 곳(applyExecution)에 4자리 반올림을 넣었고, 같은 금액을 다루는 계좌 잠금·원장·매도 대금은 반올림 없는 값과 반올림된 값이 섞였다. 주문 불변식(수량 상한)도 도메인 메서드 밖에서만 지켜졌다(PTB5) |

## 2. Fix

| Item | Value |
|---|---|
| Site | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt#Order |
| Fixed layer | Enabling structure |
| Clause | PTB4 |
| Changed files | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt · backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt |
| Recurrence | different defect — 474e066 이 넣은 메모리 반올림 회귀(P-01~P-03)이고, 그 커밋이 고친 원래 결함(#8 #108 잘못된 평균가·해제액)과 다르다 |
| Why the upper layer stays | n/a |

## 3. Siblings (other sites sharing the same cause)

| Item | Value |
|---|---|
| Search | `setScale`·`RoundingMode`·`HALF_UP` 를 Order.kt·ExecutionProcessor.kt 안에서 읽어 전부 확인(backend/trading-api/src/main). 같은 금액 계산이 있는 Settlement.createFromExecution·FeePolicy 는 열어서 반올림 위치만 확인 |

| Site (file:line) | Fixed / Left | Reason |
|---|---|---|
| backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:145 | Fixed | 이번 체결 금액 반올림 제거, 평균가만 4자리 HALF_UP, 수량 상한 검사 |
| backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:159 | Fixed | 매도 순대금 setScale 제거(원장·예수금 입금이 반올림 없는 값), 끝난 주문·0 하한 경고 로그 |
| backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:118 | Left | 수수료 rawFee 의 setScale(4, HALF_UP) 은 수수료 규칙(DEC-01 확정: 원 미만 절사, FeePolicy 한 곳) 자체라 PR-A5 에서 FeePolicy 로 옮겨 고친다 |
| backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/Settlement.kt:95 | Left | 정산 행 금액(realizedPnl·net)의 setScale 은 정산 저장 열 scale 4 규칙이라 TODO 별도 항목 «금액 열 자리수 확장»(P-05)에서 함께 다룬다 |
