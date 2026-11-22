# PR-A1 재리뷰 (3회차, 합치기 9fdbd37 포함)

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 리뷰 대상 | PR #53 `fix/trading-api-money-state` · 합치기 커밋 `9fdbd37`(`git diff 9fdbd37^1 9fdbd37`) + PR 전체 `git diff origin/main...HEAD -- backend/` |
| 기준 | `rule-pointer --list` (ExecutionProcessor.kt · KisExecutionWebSocketManager.kt · Order.kt): dev-team architecture·backend-api·events·async-external·common·failure-handling + 레포 rules/backend-api(PTB*)·data-access(PTD*)·failure-handling(PTF5·PTF6)·async-external(PTA*)·domains/trading·conduct·security-privacy-config·workflow |
| 프로필 | consumer |
| 리뷰어 | code-reviewer (구현자와 독립, 읽기 전용) |
| 날짜 | 2026-11-22 |

읽기 범위 밖으로 연 파일: OrderCommandService.kt(MARKET 매수 잠금 계산으로 초과 체결 도달 가능성 확인), Settlement.kt·Position.kt·FeePolicy.kt(중간 반올림 점검 — 요청 3번), common/exception/*(합친 뒤 예외 상속 확인).

## 1. 요약

| 항목 | 값 |
|---|---|
| 리뷰한 렌즈 | 정확성 · 표준 · 회귀(합치기 해소) · 근본성 |
| 수정 지킴 확인 | 확인 불가(샌드박스 — gradle 캐시 쓰기 불가, 전체 시험 재실행 금지 지시) · 작성자 증거: build/test-results 의 ExecutionProcessorTest 27·KisExecutionWebSocketManagerTest 7·ExecutionMoneyPrecisionPersistenceTest 3 실패 0·건너뜀 0 |
| 발견 | 2개 (막음 1 — 회귀 예라 판정 도구가 막음으로 셈 · 후속 후보 1) |
| 머지 판정 | 머지 판정: 불가 — 막는 지적 1건 · 지난 지적 미해결 0건 · 후속 후보 1건 (재리뷰 · 3회차) |
| 한 줄 | 합치기 해소는 RISK-11 대로 맞음(TOSS_LIVE·UPBIT_LIVE 입금 경로 0). 다만 main 의 Account 예외 타입 전환이 R2-N1 고침(IllegalArgumentException 만 잡음)의 덮는 범위를 좁혀, KIS 매수 잠금 부족 거부가 다시 세션을 닫는다 |

## 1-1. consumer 프로필

- 1. 규칙 준수 — 대조 조항: FH1·FH6·FH7, PTB4(BigDecimal compareTo·divide scale 명시), DD13(float 금지), PTD5, CS14(예외 삼킴), domains/trading(BigDecimal·명시 전이). 위반: C3-01(FH6).
- 2. 사이드 이펙트 — 합치기 자동 병합 파일 점검:
  - ExecutionProcessor.kt: 진입부 `rejectIfBrokerFillNotSupported`(ExecutionProcessor.kt:116, 256-261)가 `applyExecution`·체결 저장·포지션·원장보다 먼저라 TOSS_LIVE·UPBIT_LIVE 는 어떤 쓰기도 없이 거부. 매도 `when`(164-177)은 else 없는 전수이고 두 모드는 throw 뿐 — 입금 경로 0. `releaseLockedExcess` 없음, 최종 해제는 `remainingLockedAmount`(214-219) 한 곳. main 쪽 `ensureSellApplied`·`SellExecutionApplyFailedException`(250-253) 유지. origin/main 대비 이 파일의 차이는 PR 의 금액·원장·로그 변경뿐(main 변경 유실 없음).
  - Account.kt: origin/main 대비 차이는 열 정밀도 3줄뿐(Account.kt:60-70). main 의 범주 예외 전환은 그대로 — 그러나 이것이 C3-01 의 원인.
  - KisExecutionNoticeService.kt·KisExecutionWebSocketManager.kt: 합치기에서 바뀐 줄 없음(main 이 merge-base 뒤 손대지 않음). 의미 충돌은 C3-01 하나.
  - archunit_store: bb05cc60 에서 `releaseLockedExcess` 행 제거는 PR 의도와 맞음, 나머지 제거·추가 행(KisPaper/LiveOrderExecutor → BrokerOrderSubmitter, KisAccountQueryService 생성자)은 main 쪽 그대로. 3baebd4b·5b47cd17 은 main 변경 그대로.
  - ExecutionProcessorTest.kt: main 의 6개 시험(SELL_EXECUTION_APPLY_FAILED, 미지원 모드 SELL·BUY, 모드 누락 표 2, 이벤트) 모두 남음(ExecutionProcessorTest.kt:432-567), KIS 매도 2건은 이름·단언(compareTo)만 PR 판으로.
- 3. 버그·장애 위험 — 돈 계산: 부동소수점 0(Double·Float 없음). 중간 반올림: 체결 금액·누적 총액·원장·잠금 해제·정산 realizedPnl·netPnl 은 반올림 없음(Order.kt:380-386, ExecutionProcessor.kt:161-162, Settlement.kt:95-98). 반올림이 남은 곳은 수수료(ExecutionProcessor.kt:123, 최종값)·평균가(Order.kt:386, 표시)·세금(FeePolicy calculateTax scale 4, 최종값)·외화 krwNetPnl(Settlement.kt:108, 환산 최종값)·포지션 avgBuyPrice(Position.kt:155, 표시) — 모두 저장 직전 최종값이라 중간 반올림 아님. 멱등: `findByExternalExecutionId` 선확인(ExecutionProcessor.kt:99), 끝난 주문 무시(102-109), 최종 해제는 FILLED 전이 때 한 번·키 `unlock-excess-$orderId`. 취소 해제의 scale 4 나눗셈(OrderCommandService.kt:282-284)은 PR-A2 범위의 기존 코드라 지적하지 않음.

## 2. 발견

| ID | file:line | 렌즈 | 근거 조항 | 무엇이 틀렸나 | 등급 | 영향 | 출처 | 회귀 | confidence | 구조 고침 | 재현 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| C3-01 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:244 | 회귀 | FH6 | 통지 경계가 `IllegalArgumentException` 만 잡는데, 합치기로 들어온 main 의 Account 가 `require` 를 범주 예외로 바꿔(Account.kt:170-171 `confirmBuy` → `LockedDepositInsufficientException`, InternalErrorException → RuntimeException, IAE 아님) 합치기 전엔 잡히던 거부가 다시 경계 밖으로 나간다. 도달 경로: KIS MARKET 매수는 잠금 = 시세 × 수량(OrderCommandService.kt:111-120)이라 실제 체결가가 더 높고 계좌에 다른 잠금이 없으면 `recordBuyExecution` → `confirmBuy` 가 던짐 → 세션 SERVER_ERROR 종료·재연결, 기록은 주문 id·체결 id 없는 한 줄(R2-N1 증상 그대로). 시험(KisExecutionWebSocketManagerTest «rejected execution notice…»)은 IAE 만 던져 이 회귀를 못 잡음. 잠금 초과 경고 시험도 `account.lockDeposit(200000)` 로 다른 잠금을 깔아 이 경로를 피함(ExecutionProcessorTest.kt:413) | should-fix | 버그·장애 | 이번 수정 | 예 | 높음 | 경계 catch 를 «한 통지의 도메인 거부» 범주(IAE + ApiDomainException·InternalErrorException 등, 또는 경계 catch-all 기록 후 가둠)로 넓히고, 잠금 부족 예외를 던지는 시험 1건 추가 | `grep -n "catch (rejected" backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt; grep -n "LockedDepositInsufficientException()" backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt; grep -n "RuntimeException" backend/trading-api/src/main/kotlin/com/papertrading/api/common/exception/InternalErrorException.kt` |
| C3-02 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:376 | 표준 | label:error-category | PR 이 새로 넣은 초과 수량 거부가 `require`(IAE)인데 합친 main 은 같은 계층 Account 의 거부를 범주 예외(K·400 묶음)로 정렬(커밋 7111818). 통지 경계(C3-01)가 IAE 라는 타입에 기대는 것도 이 불일치 때문 — 둘 중 하나로 맞춰야 경계 계약이 한 모양이 됨 | nit | 동작 무관 | 이번 수정 | 아니오 | 중간 | — | — |

## 2-1. 재리뷰 — 지난 지적 판정

| 지난 ID | 판정 | 근거 |
|---|---|---|
| R2-N1 | 고침 | 합치기 전 고침은 맞음 — KisExecutionWebSocketManager.kt:241-250 가 경계에서 기록·세션 유지, Order.kt:376-379 메시지에 orderId·executedQty·remainingQty, KisExecutionWebSocketManagerTest 7건 실패 0. 합치기가 범위를 좁힌 부분은 새 행 C3-01 |
| R2-N2 | 고침 | 돈 열 numeric(28,12)(Account.kt:60-70, AccountLedger.kt:50-55, Order.kt:96·105, Position.kt:76, Settlement.kt:52·64·76, V13__widen_money_columns.sql), ExecutionMoneyPrecisionPersistenceTest «끝수 0점00005» 실제 PostgreSQL 3건 실패 0·건너뜀 0 |
| R2-N3 | 고침 | docs/TODO.md:105 «해소: … numeric(28,12)로 확장(리뷰 P-05·R2-N2·R2-N3)» |
| R2-N4 | 고침 | ExecutionProcessor.kt:105 `maskKisAccountNumber(externalExecutionId)`, KisExecutionNoticeService.kt:27·46-47 도 가림, ExecutionProcessorTest.kt:400 단언 |
| R2-N5 | 고침 | docs/TODO.md:117 P-14 항목 |

- 참고(지적 아님): build/test-results xml 시각 2026-11-22T12:11Z(21:11 KST)는 합치기 커밋 시각 21:26 KST 보다 앞섬 — 해소한 작업 트리에서 돌린 뒤 커밋한 것으로 보이고, 작업 트리에 추적 파일 변경 없음. 커밋 뒤 재실행 증거가 필요하면 의심 클래스만 `--tests` 로.
- 기존 결함(기록됨, 이 PR 밖): LOCAL 매도 대금 미입금 의심(docs/TODO.md:113-115, `applySellExecutionAtomically` clearAutomatically 뒤 분리된 account 변경).

<!-- devkit:standard-list hex:7b22636861696e223a22646f63732f66656174757265732f72756e6e696e672f74726164696e672d6170692d6d6f6e65792d73746174652f7265766965772f70616e656c2e6d64222c22726f756e6473223a7b2231223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2266623233646633396166393135663036633837313363343439353336666235366134353235363934227d2c2232223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2232663638653033303166353266373130353961643761626335356239663963343266323763336230227d2c2233223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2239666462643337323836353238386437643031343930336538353563363838343730356461313238227d7d7d -->
