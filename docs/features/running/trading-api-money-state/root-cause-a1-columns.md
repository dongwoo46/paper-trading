# Root-cause chain — 돈 열 자리수(소수 4자리)가 반올림 없는 체결 금액을 못 담아 원장·예수금·잠금이 어긋남

status: resolved

> PR-A1 재리뷰 2회차 R2-N2·R2-N3 와 1회차 P-05. 사람 결정 2026-11-15 «금액 열 확장» (계획 §8-1).

## 1. Chain

| Layer | Content |
|---|---|
| Symptom | 매수 체결 금액 끝수가 0.00005 인 체결(0.5 × 100.0001 = 50.00005)을 저장하면 원장 BUY_EXECUTE 금액은 50.0001 인데 예수금은 50.0000 만 줄어 0.0001 이 어긋난다. 소수 수량을 세 번 나눠 체결하면 매 체결 사이 DB 재조회 값이 달라 FILLED 뒤에도 잠금 예수금 0.0002 가 남는다. 매도 정산 실현손익도 소수 수량에서 원장 금액과 갈린다 |
| Direct cause | 돈 열이 NUMERIC(20,4) 이라(Account.kt deposit·available_deposit·locked_deposit, AccountLedger.kt amount·balance_after, Order.kt total_amount·locked_amount, Settlement.kt 정산 3열, Position.kt total_buy_amount) 반올림 없는 값(수량 scale 8 × 가격 scale 4 = scale 12)이 저장 때 열마다 따로 4자리로 잘린다. Settlement.createFromExecution 도 자체 setScale(4) 로 같은 값을 따로 반올림했다 |
| Enabling structure | 저장 열의 자리수 정책이 값 계약과 따로 정해졌다. 체결 금액·잠금·원장은 같은 곱셈(수량 × 가격)에서 나오는데 열 정밀도가 그 곱을 담지 못해, 메모리 반올림을 없앤 A1 계약(사람 원칙 «돈은 반올림하면 안 된다»)이 DB 에서 다시 깨진다 |

## 2. Fix

| Item | Value |
|---|---|
| Site | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt#Account |
| Fixed layer | Enabling structure |
| Clause | PTB4 |
| Changed files | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt · backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/AccountLedger.kt · backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt · backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/Settlement.kt · backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/Position.kt · backend/trading-api/src/main/resources/db/migration/V13__widen_money_columns.sql |
| Recurrence |  |
| Why the upper layer stays | n/a |

## 3. Siblings (other sites sharing the same cause)

| Item | Value |
|---|---|
| Search | `precision = 20, scale = 4` 와 `setScale(` 를 backend/trading-api/src/main/kotlin 전체에서 grep -rn 한 뒤 열마다 돈·체결 금액인지 읽어 분류. 마이그레이션은 backend/trading-api/src/main/resources/db/migration 의 numeric 선언(V2~V12)도 확인(체결 금액 열은 JPA 가 만들며 SQL 파일엔 없음) |

| Site (file:line) | Fixed / Left | Reason |
|---|---|---|
| backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:49 | Fixed | 예수금 3열 numeric(28,12) |
| backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/AccountLedger.kt:50 | Fixed | 원장 amount·balance_after numeric(28,12) |
| backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:96 | Fixed | total_amount·locked_amount numeric(28,12) |
| backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/Settlement.kt:52 | Fixed | 정산 실현손익·순손익·원화 순손익 numeric(28,12), 실현손익·순손익 setScale(4) 제거 |
| backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/Position.kt:76 | Fixed | total_buy_amount numeric(28,12) (매수 누적 금액이 수량 × 가격 합) |
| backend/trading-api/src/main/resources/db/migration/V13__widen_money_columns.sql | Fixed | 기존 DB 수동 적용 스크립트 |
| backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/Position.kt:84 | Left | evaluation_amount·unrealized_pnl 은 시세 기반 파생 평가값이라 체결 금액 계약과 무관(돈인지 평가인지 사람 확인 필요) |
| backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:92 | Left | avg_filled_price 는 사람 결정으로 4자리 표시 평균가 |
| backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Execution.kt:56 | Left | 체결가·수수료 열은 가격·수수료 규칙이라 가격 scale 4 유지(수수료 규칙은 PR-A5) |
| backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/DailyBalance.kt:40 | Left | cash_balance 는 예수금의 일일 보고 사본이라 저장 때 4자리로 반올림됨 — 보고 열 정책은 별도 결정 |
