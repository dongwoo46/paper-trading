# Root-cause chain — KIS 체결 통지의 초과 체결 거부가 세션을 닫고, 체결 id 로그에 계좌번호가 남음

status: resolved

> PR-A1 재리뷰 2회차 R2-N1·R2-N4. 앞 회차 고침(초과 체결 거부, 끝난 주문 경고 로그)이 만든 회귀.

## 1. Chain

| Layer | Content |
|---|---|
| Symptom | KIS 체결 통지가 남은 수량보다 많으면(IllegalArgumentException) 예외가 WebSocket 처리 밖으로 나가 세션이 닫히고 재연결된다. 기록은 주문 id·체결 id·수량 없는 한 줄뿐이다. 끝난 주문 경고·통지 중복·미매칭 로그는 KIS 체결 id(계좌번호 포함)를 그대로 찍는다 |
| Direct cause | KisExecutionWebSocketManager.kt handleRawMessage 가 noticeService.handle 의 거부 예외를 잡지 않는다. Order.applyExecution 거부 메시지에 값이 없다. ExecutionProcessor·KisExecutionNoticeService 의 로그가 externalExecutionId·accountScope 를 그대로 문자열에 넣는다 |
| Enabling structure | 통지 처리 경계(WebSocket 핸들러)에 «한 통지의 거부가 세션을 죽이지 않고 어느 건이 왜 거부됐는지 남긴다»는 계약이 없었다(거부 경로가 이번에 처음 생김). 계좌번호를 가리는 규칙도 한 곳에 없어 로그마다 id 를 직접 찍었다 |

## 2. Fix

| Item | Value |
|---|---|
| Site | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt#KisExecutionWebSocketManager |
| Fixed layer | Enabling structure |
| Clause | FH6 |
| Changed files | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt · backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisExecutionNoticeService.kt · backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt · backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt · backend/trading-api/src/main/kotlin/com/papertrading/api/common/LogMasking.kt |
| Recurrence |  |
| Why the upper layer stays | n/a |

## 3. Siblings (other sites sharing the same cause)

| Item | Value |
|---|---|
| Search | `externalExecutionId`·`accountScope`·`accountNumber`·`externalAccountId` 를 로그 문자열에 넣는 곳을 backend/trading-api/src/main/kotlin 전체에서 grep -rnE 로 찾음(개행으로 나뉜 로그 포함 확인). 통지 처리 경계는 KisExecutionWebSocketManager.handleRawMessage 한 곳 |

| Site (file:line) | Fixed / Left | Reason |
|---|---|---|
| backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:247 | Fixed | 거부를 경계에서 error 로 기록하고 세션 유지 |
| backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisExecutionNoticeService.kt:27 | Fixed | 중복·미매칭 통지 로그 마스킹 |
| backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:103 | Fixed | 끝난 주문 경고 로그 마스킹 |
| backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:148 | Fixed | 거부 메시지에 주문 id·체결 수량·남은 수량 |
| backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionNoticeParser.kt:19 | Left | 파싱 실패 통지를 runCatching 으로 조용히 버린다(기록 없음). 이번 변경 밖 기존 결함이라 별도 항목으로 보고 |
