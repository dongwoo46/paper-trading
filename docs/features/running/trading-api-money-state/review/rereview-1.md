# PR-A1 재리뷰 (2회차, QA)

- 범위: `fb23df3..2f68e03` (수정 커밋만), 리뷰어 tester
- 실행: 사본에서 `./gradlew test --tests '*OrderTest' --tests '*ExecutionProcessorTest' --tests '*QaEdge*'` 95건 중 4건 실패(E03·S16 은 옛 계약 기대값, R10·R11 은 새 결함), 대조 사본(Order.kt·ExecutionProcessor.kt 만 fb23df3) QaEdgeR2Test 11 중 5 실패
- R09~R11 의 저장 값은 reflection 으로 4자리 HALF_UP 모사(실제 DB 아님)

## 지난 ID 판정

| 지난 ID | 판정 | 근거 |
|---|---|---|
| P-01 | 고침 | S17·OrderTest 초록, 단일 체결 평균가 = 체결가 70000.1234 |
| P-02 | 고침 | E04·S07 초록, 극소 수량 체결 금액 > 0 |
| P-03 | 고침 | S05 초록, 소수 수량 단일 체결 잠금 0 |

그 밖 1회차 후속 후보: P-04·P-06~P-13 고침, P-05·P-14 수용(사람 결정 2026-11-15 «메모리 반올림 제거», 범위 밖).

## 이번 수정이 만든 것

| ID | file:line | 근거 조항 | 무엇이 틀렸나 | 등급 | 출처 | 회귀 |
|---|---|---|---|---|---|---|
| R2-N1 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:144 | FH6 | KIS 초과 체결 거부 예외가 WebSocket handle 밖으로 나가 세션을 닫고 재연결, 기록은 주문 id·체결 id·수량 없는 한 줄 | should-fix | 이번 수정 | 예 |
| R2-N2 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:148 | PTF2 | 매수 체결 금액 끝수 0.00005 일 때 4자리 저장 뒤 원장 50.0001 인데 예수금은 50.0000 만 줄어 0.0001 불일치 | should-fix | 이번 수정 | 예 |
| R2-N3 | docs/TODO.md:106 | label:progress-doc | P-05 증상이 «마지막 체결 실패»에서 «FILLED 뒤 잠금 0.0002 잔존(경고만)»으로 바뀌었는데 TODO 는 옛 증상 | should-fix | 이번 수정 | 예 |
| R2-N4 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:101 | label:pii-log | 끝난 주문 경고가 externalExecutionId 를 그대로 찍어 KIS 계좌번호가 로그에 남음 | nit | 이번 수정 | 예 |
| R2-N5 | docs/TODO.md:102 | label:progress-doc | P-14 가 TODO 항목에 없음 | nit | 이번 수정 | 아니오 |

<!-- devkit:standard-list hex:7b22636861696e223a22646f63732f66656174757265732f72756e6e696e672f74726164696e672d6170692d6d6f6e65792d73746174652f7265766965772f70616e656c2e6d64222c22726f756e6473223a7b2231223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2266623233646633396166393135663036633837313363343439353336666235366134353235363934227d2c2232223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2232663638653033303166353266373130353961643761626335356239663963343266323763336230227d7d7d -->
