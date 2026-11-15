# 코드 리뷰 — PR-A1 체결 금액·최종 잠금 해제 (e510634..fb23df3)

- 프로필: consumer (rule-pointer --list) · 리뷰어: code-reviewer · 읽기 전용
- 적용 규칙(--list 그대로): dev-team failure-handling FH1~6, tests TT*, 레포 backend-api PTB1~9, data-access PTD1~5, failure-handling PTF1~6, tests PTT1, always:dev-team(주석 C12·시험 먼저·KST)
- 영향 범위 도구: serena Kotlin 언어 서버 기동 실패(LanguageServerTerminatedException, serena 루트도 worktree 아닌 본 저장소) → grep 로 대신함
  - Order.applyExecution main 호출처 1곳(ExecutionProcessor.kt:115), remainingLockedAmount 1곳(ExecutionProcessor.kt:199), 시험은 OrderTest 만
  - processFill 은 fillLocal(LocalMatchingEngine.kt:37)·fillExternal(KisExecutionNoticeService.kt:46) 로만 들어옴 → 둘 다 @Transactional(ExecutionProcessor.kt:56, :71)

## 1. 규칙 준수

- PTB4: `==` 비교 삭제(#127), avg `divide(x, 4, HALF_UP)`, 금액 비교 전부 compareTo 연산자 → 위반 없음
- PTB2: 평균가 계산이 Order 로 들어감(D-01), 상태 필드 private set 유지 → 위반 없음
- PTB7·PTB8: 새 application→infrastructure 의존 없음. 동결 위반 4→3(releaseLockedExcess 줄 삭제)
- TT33: 새 require 2개를 찍는 시험 없음 → R1
- C12(always): Order.kt:137 종결어미 → R4
- TT7: 새 시험이 프로세스 안 리포지토리 11개를 mockk 로 바꿈(기존 파일 방식) → R7
- PTF1(원장 UPDATE·DELETE 금지): 원장 행 추가만 함 → 위반 없음
- 린트 기준선: ktlint +1 −66, detekt −5. ktlint +1 줄은 Order.kt 기존 항목이 179→192 로 줄 번호만 옮겨진 것(증가 아님)

## 2. 부작용

- 매도 원장 저장 순서(정산 저장 뒤로): 값·롤백·이벤트 영향 없음(근거는 질문 1)
- 배포 주의(계획 O4·PR-A6·DEC-05 에 이미 있음): 옛 식으로 부분 체결이 2번 이상 된 채 PARTIAL 인 KIS 주문은 totalAmount 가 틀린 값임(예: 693600). 배포 뒤 최종 체결 해제액은 700000 − 693600 = 6400 으로 계산되는데, 이 주문 몫 잠금은 4000 뿐 → 다른 주문 잠금을 2400 쓰거나, unlockDeposit require 실패로 롤백됨. 옛 코드(280000 해제 시도)보다 나으니 회귀는 아님. DEC-05 읽기 전용 점검을 A1 배포 전에 돌리길 권합니다

## 3. 버그·장애 위험

- 차단할 결함 없음. 원장 수치 재계산은 질문 2에 있음
- 잔량 초과 체결과 0 하한 깎기가 넘친 잠금 사용을 조용히 숨김 → R2·R3(기존, 후속)

## 질문별 판정

### 1. 원장 저장을 한 곳으로 합친 것 — 정당한 정리. 기계 검사 우회 아님

- ArchUnit 1.5.1 바이트코드 확인(FreezingArchRule$CategorizedViolations 생성자): 저장 줄 하나는 실제 위반 하나에만 맞고 `Iterator.remove` 뒤 break → 호출 개수를 셈
- 해결된 위반이 생기면 `removeObsoleteViolationsFromStore` 가 save 를 부름 → allowStoreUpdate=false 라 `Updating frozen violations is disabled` 로 실패
  - processFill 의 AccountLedgerRepository.save 는 정확히 3개여야 함(4개 = 새 위반, 2개 = 해결 위반 → 실패)
  - releaseLockedExcess 1줄 삭제는 선택이 아니라 필수(메서드가 없어져 해결 위반이 됨)
- 우회가 아닌 이유
  - 새 의존 종류 없음
  - 저장소에 줄을 더하지 않았고 갱신 옵션도 안 씀
  - 전체 동결 위반이 4→3 으로 줄어듦
  - 계획 D-08(«지금 있는 메서드 안에서만 고침»)과 맞음
- 순서 변경 영향
  - 값: 원장 객체 생성(recordSellExecution, :170)은 그대로 정산 앞이라 balanceAfter 는 같은 시점 값. Settlement.createFromExecution(Settlement.kt:85-114)은 계좌를 안 바꿈
  - 롤백: 한 트랜잭션이라 어느 save 가 실패해도 전부 롤백. 순서 무관
  - 이벤트: ExecutionFilledEvent 발행(:210)은 모든 save 뒤 그대로
  - 감사: 원장 행끼리 순서(SELL_EXECUTE → FEE → BUY_UNLOCK)는 같음. 달라지는 것은 정산 행과 원장 행 사이 IDENTITY 삽입 순서·created_at 몇 μs 뿐
- 문서 어긋남: 계획 §6(development-plan.md:206)은 «동결 저장소 diff 0» → R6

### 2. 돈 계산 — 맞음

- scale·반올림: 이번 체결 금액은 `setScale(4, HALF_UP)`(orders 열 scale 4와 같음). 평균가 = 누적 금액 ÷ 누적 수량 `divide(4, HALF_UP)` → 옛 반올림 평균의 누적 오차가 사라짐
- 0 나눔: require(qty > 0) 덕분에 filledQuantity > 0 이 보장됨 → 불가
- 재계산(잠금 700000, 예수금 1,000,000, 가용 300000에서 시작)

| 단계 | 체결 금액 | totalAmount | avg | lockedDeposit | deposit | 가용 |
|---|---|---|---|---|---|---|
| 4@69000 | 276000.0000 | 276000.0000 | 69000.0000 | 424000 | 724000 | 300000 |
| 6@70000 | 420000.0000 | 696000.0000 | 69600.0000 | 4000 | 304000 | 300000 |
| FILLED 해제 | 700000 − 696000 = 4000 | | | 0 | 304000 | 304000 |

- 옛 코드는 700000 − 420000 = 280000 해제를 시도 → lockedDeposit 4000 < 280000 이라 require 실패 → 롤백(#108)
- 같은 가격(4@70000 + 6@70000): 잔액 0 → `> 0` 조건으로 해제 원장 없음(unlockDeposit 의 require(>0)를 피함). 가용 300000
- 가격 개선(10@68000): 잔액 20000 해제, 가용 320000 → 옛 식과 같은 값
- 매도: lockedAmount 0 → 잔액 0 하한 → 해제 없음(옛 동작과 같음)
- 남은 점
  - 소수 수량에서는 체결마다 HALF_UP 을 하므로, 잠금(단가 × 수량, DB 에서 scale 4 반올림)과 최대 n × 0.00005 차이가 날 수 있음. KRW 정수 수량·가격에서는 정확함
  - 수수료 기준액(:112)은 반올림 전 `fillPrice × fillQty` 로 따로 계산함 → D-03(PR-A5)에서 calculateFee(이번 체결 금액)로 합칠 예정

### 3. 기존 시험 변경 3건 — 약화 아님

- OrderTest.kt:116: `assertEquals(BigDecimal("70000"), avg)` → compareTo. 새 평균가가 70000.0000(scale 4)이라 바꿀 수밖에 없음. 값은 그대로 고정. scale 은 더 안 보지만 PTB4 가 compareTo 를 요구함
- ExecutionProcessorTest.kt:218-219, :242-243: `assertEquals(initialDeposit, availableDeposit)` → compareTo(#552). 같은 값을 단언
- 덤: OrderTest.kt:337 은 assertZero 를 블록 본문으로만 바꿈(내용 같음). 시험 이름 두 개 바꿈(#551)은 이름을 본문(정산 1건 저장 단언)에 맞춘 것 → 약화 아님
- 회귀: `git diff 1dd52a5 fb23df3 -- src/test` 는 매개변수 이름(newAvgPrice→executedPrice)과 위 compareTo 1줄뿐 → 빨강 커밋의 실패 3건 증거가 최종 시험에도 그대로 해당됨

### 4. 멱등·상태 전이 — 이번 diff 로 바뀐 것 없음, 충분

- 같은 체결이 두 번 오면 다음 중 하나가 막음
  - :96 외부 체결 ID 조회
  - `uk_executions_external_id`(Execution.kt:30)
  - 주문 @Lock(OPTIMISTIC)(OrderRepository.kt:15)
  - 계좌 비관적 락
  - 결과: 두 번째는 되돌아가거나 롤백. 효과는 한 번
- FILLED 해제 원장 키 `unlock-excess-$orderId` 는 `uk_account_ledger_account_id_idempotency` 로 주문당 1행
- 전이: applyExecution 이 PENDING·PARTIAL 을 check 한 뒤 FILLED·PARTIAL 을 명시적으로 정함. 잔량 초과만 안 막음 → R2
- 중복 경로 단위 시험은 없음(기존, 이번 diff 밖)

## Findings

| ID | file:line | 근거 조항 | 무엇이 틀렸나 | 등급 | 출처 | 회귀 | 확신도 |
|---|---|---|---|---|---|---|---|
| R1 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:142-143 | TT33 | 새 require(수량 > 0)·require(가격 > 0)를 찍는 시험이 없음(OrderTest·ExecutionProcessorTest 에 0·음수 체결 입력 0건, grep) → 두 줄을 지워도 시험이 안 깨질 것으로 추론됨(미실행). 첫 체결 수량 0 이면 메시지 없는 ArithmeticException(:148 divide)으로 바뀜. «수량 0 → IllegalArgumentException»·«가격 0 → IllegalArgumentException» 시험 2개 | should-fix | 이번 diff | 아니오 | 높음 |
| R2 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:150 | label:domain-invariant (rules/backend-api.md PTB2 «허용된 이전 상태를 검사하는 메서드») | 잔량을 넘는 체결(executedQty > quantity − filledQuantity)도 받아 FILLED 로 넘김 → totalAmount > lockedAmount → 넘친 금액은 confirmBuy 가 계좌 전체 잠금(다른 주문 몫)에서 뺌. 예: 10주 주문에 4주 → 7주 통지 | nit(후속 후보) | 기존(D-01 은 > 0 만 정함) | 아니오 | 중간 |
| R3 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:169 | label:silent-clamp (core 6 조용한 대체 금지) | 0 하한 깎기가 «주문 몫보다 더 쓴 잠금»(시장가 체결가 > 접수 시세 O2, R2 초과 체결)을 기록 없이 숨김. 옛 `if (excess <= 0) return` 과 같은 동작이라 회귀는 아님. 하한에 걸리면 log.warn 이나 정합성 기록 | nit(후속 후보) | 기존 | 아니오 | 중간 |
| R4 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:137 | C12 (always:dev-team 주석 표지) | KDoc «… 돌려준다» 가 종결어미이고 코드를 옮긴 말임. «반환: 이번 체결 금액(수량 × 체결가, scale 4 HALF_UP)» 같은 명사형으로 | nit | 이번 diff | 아니오 | 높음 |
| R5 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:166 | label:comment-accuracy | KDoc 은 «체결 완료·취소 때 돌려줄 잠금 잔액»인데 취소 경로(OrderCommandService.kt:256-261)는 아직 자체 식을 씀 → PR-A2 전까지 사실과 다름(AS-02 승인 문구). PR-A2 가 바로 이어지면 무시해도 됨 | nit | 이번 diff | 아니오 | 중간 |
| R6 | docs/features/running/trading-api-money-state/development-plan.md:206 | label:plan-drift | 계획 §6 은 «동결 저장소 diff 0», 실제 diff 는 해결 위반 1줄 삭제(ArchUnit 갱신 금지 설정상 필수). state.md·step-1.md 에도 기록 없음 → 다음 리뷰어가 D-08 위반으로 오판할 수 있음. «해결된 줄 삭제만 허용»으로 고쳐 적기 | nit | 이번 diff | 아니오 | 높음 |
| R7 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/ExecutionProcessorTest.kt:114-131 | TT7 | 새 매수 시험 3개가 프로세스 안 리포지토리(원장·주문·포지션 등)를 mockk 로 바꿈 → 저장 순서·제약(유일 키)은 안 보임. 기존 파일 방식을 넓힌 것이라 이번 PR 에서 고칠 일은 아님. 가짜 저장소 도입은 C·D 갈래 후보 | nit(후속 후보) | 기존 방식 확장 | 아니오 | 중간 |

## 확인 불가

- 확인 불가(회귀 guards-fix): 샌드박스 탓. gradle 이 ~/.gradle 에 써야 하고 네트워크도 꺼져 있음. 대신 정적 비교로 빨강 커밋 이후 시험 변경이 위 2가지뿐임을 확인함
- 확인 불가(뮤테이션): mutation-run exit 4 «뮤테이션 대상 없음»(Kotlin 엔진 없음)
- CI 상태(gh checks)는 안 봄 → 작성자 증거(BUILD SUCCESSFUL, 418 시험, ArchitectureRulesTest 초록)만 있음

## 병합 판정

- blocker 0 · should-fix 1(R1) · nit 6(그중 후속 후보 R2·R3·R7)
- R1 시험 2개를 넣으면 머지할 만함. 머지 승인은 사람이 함
- 후속 후보
  1. R2 잔량 초과 체결 막기
  2. R3 하한 깎기 기록
  3. 수수료 기준액을 이번 체결 금액으로 통일(PR-A5 D-03 에 포함)
  4. DEC-05 점검을 A1 배포 전에
