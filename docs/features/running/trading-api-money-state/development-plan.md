# Development Plan — trading-api 돈·상태 결함 바로잡기 (A 갈래)

> Tech Planner 산출물입니다. 감사 원장의 돈·상태 결함을 «어떻게 고칠지 + 지금 고칠 수 있나»로 정합니다.
> 기반: `docs/audits/latest/files.md` · `merged.json` · `report.md`(run 2026-11-15-0500, 커밋 bb93c07) · `rules/backend-api.md` · `rules/data-access.md` · `rules/failure-handling.md` · `rules/tests.md` · `rules/async-external.md` / 다음: state.md + steps/step-N.md
> 합의되면 고정합니다. 진행 상태는 여기 적지 않고 state.md 에 적습니다. 불가능한 부분(🔴)은 구현으로 넘기지 않고 §8-1 사람 결정으로 올립니다.
> 줄 번호 기준: worktree `fix/trading-api-money-state`, main e510634. 경로 줄임: `TA:` = `backend/trading-api/src/main/kotlin/com/papertrading/api/`, `TT:` = `backend/trading-api/src/test/kotlin/com/papertrading/api/`, `RES:` = `backend/trading-api/src/test/resources/`, `CFG:` = `backend/trading-api/config/`.
> 이름표: `#N` = 원장 발견 번호(`merged.json` findings.rows 의 N번째 = files.md 의 #N), `D` = 이 문서의 기술 결정, `DEC` = 사람이 정할 항목, `AS` = 사소해서 가정하고 진행하는 항목, `RISK` = 위험, `O` = 원장 밖 관찰(대상 코드를 열다 본 것 — 재감사 아님).

---

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 기능 | `trading-api-money-state` — A 갈래(돈·상태가 실제로 틀어지는 결함) |
| 기반 feature-spec | 없음 — 명세 = 감사 원장 결함 목록 + `rules/` (§1) |
| 작성자 | Claude (tech-planning, Tech Planner) |
| 날짜 | 2026-11-15 |
| tier | 반자동 (`.claude/stack-profile.json` `tier`) |
| 대상 repo / 브랜치 | paper-trading / `fix/trading-api-money-state` (worktree `.worktrees/trading-api-money-state`, main e510634) |
| 상태 | in-review — DEC-01~08 사람 결정 대기. PR-A1·A2 는 결정 없이 착수 가능 |

---

## 1. 구현 가능성 판단 (착수 전 점검)

명세 = 감사 원장 결함 목록 + `rules/`. 이 기능에는 feature-spec 이 없습니다. «무엇을»은 원장의 A 후보 발견(§1-1)과 레포 규칙 5개가 정하고, 이 계획은 «어떻게»만 정합니다. 갈래 순서(A 돈·상태 결함 → B 트랜잭션 안 외부 호출 X3 → C 포트 도입 L1 → D 도메인·영속 분리 D1)는 사람 결정이라 바꾸지 않습니다.

| 항목 | 값 |
|---|---|
| 명세 준비됨? | ready — 원장 후보 20건을 대상 코드를 열어 판정(§1-1), 재감사 없음 |
| 막힌 ASK | partial — DEC-01~05 가 PR-A3~A6 을 막음. PR-A1·A2 는 막힘 없음 |
| **판정** | 🟡 일부 조건부 (A 안에서 🔴 2곳은 B 로 넘김 — DEC-03·04) |
| 코멘트 | 계획을 좌우하는 사실 셋. 1. 부분 체결은 KIS 모드에서만 생깁니다. LOCAL 매칭은 남은 수량 전부를 한 번에 체결합니다(TA:application/order/LocalMatchingEngine.kt:36-37). 그래서 #8·#108·#253 은 KIS_PAPER·KIS_LIVE 에서 돈이 틀어지고 LOCAL 에서는 거의 안 드러납니다. 2. 올바른 해제 규칙 `Order.remainingLockedAmount()`(TA:domain/entity/order/Order.kt:157-159)가 이미 도메인에 있으나 main 호출처가 0건이고, 그 규칙이 기대는 `totalAmount` 가 #8 로 틀립니다 → #8 이 먼저입니다. 3. 기계 검사 두 가지가 고치는 모양을 묶습니다. ArchUnit 동결 저장소는 application→infrastructure 호출을 «호출하는 메서드 서명 + 호출 대상» 문구로 저장하고(RES:archunit_store/bb05cc60-ae2b-4a57-8f2e-cec4890e2499, 갱신·생성 금지 RES:archunit.properties), detekt 기준선은 함수 서명으로 ID 를 만듭니다(CFG:detekt/baseline.xml:9, :211). 리포지토리 호출을 새 메서드로 옮기거나 기준선에 오른 함수 서명을 바꾸면 새 위반이 됩니다(D-08) |

### 1-1. 원장 후보 분류

| # | 항목 | 판정 | 재현 시나리오 / 기각 이유 | 근거 file:line | 갈래 · PR |
|---|---|---|---|---|---|
| #8 | S6 평균가를 서비스가 계산 | 실제 | KIS_PAPER 지정가 매수 10@70000, 체결 4@69000 → 6@70000. 서비스가 누적 평균 69600 을 넘겨 totalAmount = 276000 + 6×69600 = 693600(정답 696000). OrderTest 는 체결가를 넘겨 696000 을 기대 — 시험 계약과 운영 계약이 다름 | TA:application/order/ExecutionProcessor.kt:114-120 · Order.kt:134-143(:140) · TT:domain/entity/order/OrderTest.kt:145-153 | A · PR-A1 |
| #127 | F1 `BigDecimal ==` | 실제(PTB4 위반) · 저장값 영향 없음 | DB 에서 읽은 filledQuantity 0E-8 ≠ BigDecimal.ZERO 라 가중평균 가지로 가지만, avg_filled_price 열이 scale 4 라 저장값은 같음 | ExecutionProcessor.kt:116 · Order.kt:81-83, :89-91 | A · PR-A1 (#8 수정으로 분기 자체가 사라짐) |
| #108 | D3 최종 체결 해제액 | 실제 | 잠금 700000 주문이 4@70000 → 6@70000 체결. 두 체결의 confirmBuy 가 잠금을 다 썼는데 FILLED 때 «잠금 − 마지막 체결 1건» = 280000 을 해제. 계좌에 다른 잠금이 없으면 unlockDeposit require 실패로 마지막 체결 트랜잭션 롤백(KIS 는 체결, 로컬은 PARTIAL 고착). 있으면 다른 주문 잠금 280000 이 풀려 가용 예수금이 부풂 | ExecutionProcessor.kt:203-206, :233-242 · TA:domain/entity/account/Account.kt:139-144, :158-163 · Order.kt:157-159(main 호출처 0, grep) | A · PR-A1 |
| #253 | D3 취소 해제액 | 실제 | 잠금 700000, 4@69000 체결(잠금 사용 276000 → 이 주문 몫 424000) 뒤 취소. 서비스 식 700000÷10×6 = 420000 만 해제 → 4000 영구 잠김. 시장가 매수가 잠금가보다 비싸게 부분 체결된 뒤 취소하면 반대로 과다 해제·require 실패 | TA:application/order/OrderCommandService.kt:250-266(:256-259) | A · PR-A2 |
| #252 | S6 상태를 읽고 cancel() | 중복은 실제 · «예외 타입이 갈린다»는 기각 | 서비스 check 와 Order.cancel() 둘 다 `check` → IllegalStateException, 전역 핸들러에서 둘 다 400. 돈·상태 영향 없음 | OrderCommandService.kt:243-248 · Order.kt:145-150 · TA:presentation/exception/GlobalExceptionHandler.kt:31-34 | A 밖 — PR-A2 가 같은 줄을 고치며 중복 상태 검사만 지움(소유권 검사는 남김) |
| #591 | E1 포지션 없으면 조용히 건너뜀 | 실제(관측성) · 돈·상태는 안 틀어짐 | 포지션이 없으면 풀 잠금도 없음. main 에 포지션 삭제 경로 0건(grep `positionRepository.delete`) → 데이터 손상 때만 도달 | OrderCommandService.kt:269-276 | A 밖 — 오류·관측성 정리(DEC-07 묶음) |
| #245 | D3 수수료 반올림 | 실제 | 요율 0.000015, 1@70001: 서비스 HALF_UP 1.0500, 도메인 UP 1.0501. 시험된 도메인 규칙이 운영에 안 쓰임(calculateFee main 호출처 0, grep) | ExecutionProcessor.kt:108-112 · TA:domain/entity/feepolicy/FeePolicy.kt:78-82 | A · PR-A5 (방향은 DEC-01) |
| #126 | F4 정책을 처리 시각으로 조회 | 실제 | 정책 P1 [t0, t1) · P2 [t1, ∞). executedAt < t1 인 KIS 체결 통지가 t1 뒤 도착하면 P2 로 계산 | ExecutionProcessor.kt:108 (executedAt 은 :93 에 있음) · TA:infrastructure/persistence/FeePolicyRepository.kt:13-25 | A · PR-A5. 정산 시각 `Instant.now()`(:185)는 금액이 아니라 시계 주입 문제 → C·D |
| #397 | X1 processFill 에 @Transactional 없음 | 기각(현재 결함 아님) | processFill 을 부르는 곳은 같은 클래스의 @Transactional fillLocal(:55, :61)·fillExternal(:70, :78)뿐이고, 둘은 다른 빈에서 프록시로 불림(LocalMatchingEngine.kt:37, TA:application/order/KisExecutionNoticeService.kt:46). 직접 호출처 0 | ExecutionProcessor.kt:88 | D — S1(#7) 분해 때 private 화. A 에서 안 하는 이유: detekt LongMethod·ReturnCount 기준선 ID 가 `fun processFill(` 서명이라(CFG:detekt/baseline.xml:9, :211) private 을 붙이면 새 위반이 돼 분해를 당겨옴 |
| #17 | I3 KIS 주문이 커밋 전 트랜잭션 안 | 실제 | KIS 접수 성공 뒤 응답 유실(읽기 타임아웃)이나 assignExternalOrderId 뒤 Slack 발행 예외 → placeOrder 전체 롤백 → KIS 엔 주문, DB 엔 흔적 없음 → 같은 키 재시도 시 이중 주문, 이후 체결 통지는 미매칭으로 버려짐. 호출 동안 계좌 행 락 보유 | OrderCommandService.kt:49-73(:55, :70), :171-177 · TA:application/order/KisLiveOrderExecutor.kt:25-43 · KisPaperOrderExecutor.kt:22-40 | B 권고 (DEC-03, D-10) |
| #20 | I3 KIS 취소가 로컬만 | 실제 | KIS_LIVE 주문 접수 → 사용자 DELETE → 로컬 CANCELLED·잠금 해제, KIS 주문은 살아 있음 → KIS 체결 통지는 PENDING·PARTIAL 만 찾아 버려짐 → 실계좌엔 주식, 로컬엔 없음. GTD 만료도 모드 구분 없이 같은 경로 | OrderCommandService.kt:238-280 · KisLiveOrderExecutor.kt:46-62 · KisPaperOrderExecutor.kt:42-58(cancel 호출처 0, grep) · TA:infrastructure/scheduler/LocalMatchingScheduler.kt:23-37 · TA:infrastructure/persistence/OrderRepositoryImpl.kt:56-65, :79-96 | A 임시 차단 + B 본 수정 (DEC-04) |
| #250 | I1 같은 키 다른 요청에 기존 주문 | 실제 | 같은 키로 수량 1 → 10 재요청 → 첫 주문(1주)이 201 로 돌아와 사용자는 10주 주문 성공으로 앎 | OrderCommandService.kt:51-52 | A · PR-A4 (DEC-02) |
| #249 | X10 멱등 키 충돌이 Spring 예외로 | 실제 · 돈은 안 틀어짐 | 같은 키 동시 2요청이 둘 다 락 전 조회 통과 → 둘째가 save 에서 유일 제약 위반 → 전역 핸들러 500. 유일 제약이 이중 주문은 막음 | OrderCommandService.kt:51-57, :112-133 · Order.kt:35 · GlobalExceptionHandler.kt:59-61 | A · PR-A4 — 락 뒤 재확인만. 어댑터의 예외 변환(PTB9)은 포트가 필요 → C |
| #22 | Q5 주문 목록 상한 없음 | 실제 · 돈·상태 아님 | 목록 호출 한 번이 계좌 주문 이력 전체를 응답 | TA:application/order/OrderQueryService.kt:25-29 · OrderRepositoryImpl.kt:19-31 · TA:presentation/controller/OrderController.kt:57-73 | A~D 밖 — 별도 항목(API 계약 변경이라 trading-web 주문 목록 작업과 같이) |
| #551 | T6 시험 이름 ↔ 단언 반대 | 실제 | 이름 «정산 생성 없음», 본문은 정산 저장 1회 단언 | TT:application/order/ExecutionProcessorTest.kt:147, :167, :171, :189 | A · PR-A1 (손대는 시험 파일) |
| #552 | T8 BigDecimal assertEquals | 실제 | scale 까지 비교 | ExecutionProcessorTest.kt:165, :188 | A · PR-A1 |
| #218 | T6 stale 이름, 본문은 시세 없음 | 실제 | stale 분기(잠금액 계산 안전장치) 시험 0 | TT:application/order/OrderCommandServiceTest.kt:105-119 · OrderCommandService.kt:103-105 | A · PR-A2 |
| #219 | T3 실행기 호출 횟수만 단언 | 실제 | 결과 상태 단언 없음 | OrderCommandServiceTest.kt:201-202, :222-223 | B — KIS 접수가 트랜잭션 밖으로 옮겨질 때 다시 씀 |
| #324 | T8 BigDecimal assertEquals | 실제 | scale 까지 비교 | OrderCommandServiceTest.kt:100, :181 | A · PR-A2 |

집계: A 에서 고침 13건(#8 #127 #108 #253 #245 #126 #250 #249 #551 #552 #218 #324, #20 임시 차단) · A PR 안에서 같은 줄로 정리 1건(#252 중복) · 기각 1건(#397)과 부분 기각 1건(#252 예외 타입 주장) · B 3건(#17, #20 본 수정, #219) · C 1건(#249 의 어댑터 변환) · D 1건(#397) · 별도 2건(#22, #591).

### 1-2. 원장 밖 관찰 (범위 편입은 DEC-08)

| O | 무엇이 틀리나 | 근거 |
|---|---|---|
| O1 | 매수 수수료가 예수금에서 안 빠짐. 매수 가지는 confirmBuy(cost)만 하고 recordFee 는 원장 행만 만듦. LOCAL 매도는 순대금으로 수수료를 뺌 → 매수·매도 비대칭. 정책 행이 있을 때만 드러남(없으면 수수료 0) | ExecutionProcessor.kt:139-148, :159, :164, :197-201 · Account.kt:196-206 |
| O2 | 시장가 매수 잠금 부족. 잠금 = 접수 시 시세 × 수량, 체결 = 다음 틱 시세. 체결가가 더 높으면 confirmBuy 가 계좌 전체 잠금에서 빼 다른 주문 잠금을 쓰거나, 없으면 require 실패로 매 틱 체결 실패 | OrderCommandService.kt:95-110 · LocalMatchingEngine.kt:46-49 · Account.kt:158-163 |
| O3 | 다건 체결 매도의 정산이 마지막 체결 1건만 담음 → 앞선 체결의 대금·세금 누락(부분 체결은 KIS 모드뿐) | ExecutionProcessor.kt:178-194 · TA:domain/entity/settlement/Settlement.kt:85-98 |
| O4 | #8 로 이미 저장된 orders.total_amount 오염(가격이 다른 다건 체결 KIS 주문). 고친 뒤 해제액이 이 값을 읽음 | DEC-05 |

### 1-3. 부분별 가능성

| 부분 | 가능? | 필요한 선행 작업 |
|---|---|---|
| PR-A1 체결 금액·최종 해제 (#8 #127 #108 #551 #552) | 🟢 | 없음 |
| PR-A2 취소 해제 (#253, #252 중복, #218 #324) | 🟢 | PR-A1 (totalAmount 정상화) |
| PR-A3 KIS 접수 주문의 사용자 취소 차단 (#20 임시) | 🟡 | DEC-04 |
| PR-A4 멱등 (#249 #250) | 🟡 | DEC-02 · Docker(Testcontainers 동시성 시험) |
| PR-A5 수수료 (#126 #245) | 🟡 | DEC-01 (#126 은 결정 없이 가능) |
| PR-A6 total_amount 보정 (O4) | 🟡 | DEC-05 — 운영 DB 읽기 전용 점검 결과 |
| #17 을 A 안에서 | 🔴 | 의도 행 먼저 커밋 + 트랜잭션 밖 KIS 호출 + SUBMITTING·UNKNOWN 상태(#95) + 확정 작업(PTF3)이 필요 = B 설계 그 자체. A 에서 «되는 것처럼» 못 함 → DEC-03 |
| #20 KIS 취소 실제 호출을 A 안에서 | 🔴 | PTD2(트랜잭션 안 외부 호출 금지)를 지키려면 B 의 트랜잭션 밖 호출 구조가 먼저 → DEC-04 |
| 손댄 파일 린트 정리 | 🟡 | DEC-06 |

---

## 2. 아키텍처·통합 결정

| 결정 | 선택 | 근거 | 기각한 대안 |
|---|---|---|---|
| D-01 체결 금액 계약 | `Order.applyExecution(executedQuantity, executedPrice, executedFee)` 가 이번 체결가를 받아 totalAmount += 수량×체결가, avgFilledPrice = totalAmount ÷ filledQuantity(`divide(x, 4, HALF_UP)`)를 스스로 계산하고 이번 체결 금액(수량×체결가, scale 4 HALF_UP — AS-01)을 돌려줍니다. 수량·가격 > 0 을 require 로 검사(평균 나눗셈 0 나눔 방지). 서비스의 평균가 계산(ExecutionProcessor.kt:114-118)은 지우고, 돌려받은 금액을 매수 confirmBuy·매도 대금에 씁니다 | 묻지 말고 시켜라(PTB2·팀 DD11). OrderTest 가 이미 이 계약(체결가)을 기대 — 운영만 다름. 주문 totalAmount 와 계좌 잠금 차감이 같은 값에서 나와야 D-02 해제액이 정확히 맞음 | 이름만 바꾸고 서비스가 체결가를 넘김 — 평균가 계산이 서비스에 남아 D3 반복. totalAmount 를 executions 합으로 매번 계산 — 조회 늘고 구조 변경(D 갈래) |
| D-02 잠금 해제액 한 곳 | 최종 체결(FILLED)과 취소 모두 `Order.remainingLockedAmount()`(잠금 − 누적 체결금액, 0 하한)만 씁니다. `releaseLockedExcess` 는 지우고 processFill 본문에서 그 값으로 recordBuyUnlock. 취소의 ÷수량×잔량 식(OrderCommandService.kt:256-259)도 지움 | 원장 #108·#253 의 고침과 같음, 규칙이 이미 도메인에 있음. releaseLockedExcess 를 서명만 바꿔 남기면 동결 문구(메서드 서명 + AccountLedgerRepository.save)가 새로 생겨 ArchitectureRulesTest 실패. processFill 은 같은 호출이 이미 동결돼 있어 본문으로 옮김 | `Account.releaseOrderLock(order)` — 계좌가 다른 애그리거트를 읽음(PTB3). 계좌 잠금을 주문별로 쪼개 저장 — 스키마 변경, A 밖 |
| D-03 수수료 | ExecutionProcessor 는 `feePolicy.calculateFee(이번 체결 금액)` 만 부르고 정책이 없을 때 0 처리만 남깁니다. 정책 조회는 `executedAt`. 반올림 방향은 FeePolicy.calculateFee 한 곳에만 두고 DEC-01 결과를 거기 반영 | #245·#126 고침. 규칙 SSoT 는 도메인(팀 DD11) | 서비스 식만 도메인 방향으로 맞춤 — 규칙 두 벌 유지. Clock 주입까지 — 시계 주입은 F4(#125·#251) 묶음(C·D) |
| D-04 멱등 재확인 | placeOrder 는 계좌 행 락(findByIdWithLock)을 먼저 잡은 뒤 멱등 조회를 한 번 합니다. 같은 키 기존 주문이 있으면 Order 가 «같은 조건인가»를 판정(`existing.hasSameTermsAs(candidate)` — candidate 는 command 로 `Order.create` 한 저장하지 않는 객체, 잠금액 0, 수량·가격은 compareTo). 같으면 기존 주문 반환, 다르면 `IdempotencyKeyReusedException`(ConflictException 상속, F-3 409) | 같은 계좌 요청은 계좌 행 락에서 줄 서므로 락 뒤 조회가 앞 요청의 커밋을 봄(PostgreSQL READ COMMITTED 는 문장마다 스냅샷) → 유일 제약 위반 자체가 안 생김. 조회·락 두 호출은 지금도 placeOrder 안이라 동결 문구 그대로(D-08) | 락 앞·뒤 두 번 조회 — 경로 둘, 이득은 재생 요청이 락을 안 기다리는 것뿐. 어댑터가 DataIntegrityViolation 을 바꿔 기존 주문 조회 — 위반 뒤 트랜잭션은 rollback-only 라 같은 트랜잭션에서 못 돌려줌(PTB9·PTD3), 포트 필요(C). 서비스가 필드 비교 — S6 반복. 요청 지문(해시) 열 저장 — 스키마 변경, 다음 단계 후보. 조건 8개짜리 새 값 객체 — detekt LongParameterList(생성자 7 초과) 새 위반 |
| D-05 KIS 접수 주문 취소 차단 (DEC-04 1안일 때) | `CancelOrderCommand` 에 취소 출처(사용자·시스템)를 기본값 없이 싣습니다. 사용자 취소인데 `order.isSubmittedToBroker()`(externalOrderId 있음)면 `BrokerOrderCancelNotSupportedException`(I-1 → 501). GTD 만료·IOC/FOK 즉시 취소(외부 접수 전)는 그대로. 호출처 4곳(OrderController.kt:87, LocalMatchingScheduler.kt:33, OrderCommandService.kt:159, :165)이 출처를 명시 | 팀 분류표 T7(계획했으나 안 만든 기능 → I). 출처 기본값을 두면 새 호출자가 조용히 로컬 취소로 떨어짐 | Order.cancel() 안에서 막음 — IOC/FOK·GTD 까지 막혀 동작 변경(1안 범위 넘음). 409(F-4) — 분류 어긋남. 만료 조회에서 KIS 제외 — KIS 가 장 마감에 죽인 주문의 잠금이 안 풀려 지금보다 나빠짐 |
| D-06 새 예외 자리 | 새 예외(#250 409, #20 501)는 새 파일 `TA:common/exception/OrderExceptions.kt` 에 둡니다. 409 는 기존 ConflictException 상속. 501 은 HttpStatus 없는 기반 예외로 두고 매핑은 GlobalExceptionHandler(presentation)에 추가. 메시지는 고정 문구(내부값 없음) | common 에 HttpStatus 를 쓰는 새 클래스를 두면 동결 규칙 «common → org.springframework»(RES:archunit_store/3baebd4b-ce02-4e9d-9ab2-4419048a522d)에 새 위반 → PTB7 지시대로 매핑을 presentation 으로. CommonApiExceptions.kt 는 ktlint 기준선 35건이라 건드리면 A 와 무관한 정리량이 커짐. 전역 핸들러가 예외 메시지를 응답에 실음(AE8 위반, 기존) | CommonApiExceptions.kt 에 추가. `ApiDomainException(…, HttpStatus.NOT_IMPLEMENTED)` 직접 상속 — 동결 새 위반 |
| D-07 시험 자리 | 돈 규칙은 도메인 시험(OrderTest)에서 먼저 빨갛게, 서비스 시험은 원장 재현 시나리오로 결과 상태(잠금·가용·totalAmount)를 compareTo 로 단언. 동시성·제약은 Testcontainers 통합 시험. 서비스 시험의 리포지토리 목(T2 #212·#216)은 그대로 | PTT1(락·제약은 실제 엔진), tests.md 예외(도메인·유스케이스 단위 시험은 가짜). 통합 시험 기반 있음(TT:presentation/controller/OrderControllerIntegrationTest.kt:34-54, Postgres·Redis 컨테이너) | 이번에 시험을 포트 가짜로 다시 짬 — C 범위 |
| D-08 기계 검사 경계 | application 클래스의 리포지토리 호출은 지금 있는 메서드 안에서만 고치고 새 메서드로 옮기지 않습니다. detekt 기준선에 서명이 오른 함수(processFill, placeOrder, cancelIfUnfillableIocFok, 두 서비스 생성자, Order.create)의 서명을 바꾸지 않습니다. 동결 저장소 갱신 옵션과 기준선 재생성으로 새 위반을 흡수하지 않습니다 | RES:archunit.properties(갱신·생성 금지) · backend/trading-api/build.gradle:97-154(verifyLintBaselinesNotGrown, detekt 는 기준 ref 에 없는 ID 가 생기면 실패) | 저장소·기준선 갱신 — 새 위반을 숨김 |
| D-09 PR 쪼개기 | 결함 묶음마다 PR 하나, PR 안은 «린트 정리 → 빨간 시험 → 고침 → 기준선 재생성» 커밋 | 루트 CLAUDE.md(작은 단위·TDD). 린트를 먼저 떼어 로직 diff 를 읽기 쉽게 | 한 PR — 결정 대기 항목이 전체를 막음. 린트를 마지막에 — 로직 diff 와 섞임 |
| D-10 #17 I3 자리 | B 갈래로 넘깁니다(사람 확인 DEC-03) | 고침이 X3 #19 와 같은 코드 이동(트랜잭션 쪼개기)이고 #95 상태 추가·확정 작업(PTF3)·결과 기록 유스케이스(PTD4 REQUIRES_NEW, PTB9 @Async)가 같이 와야 함 | A 에 넣기 — 사람이 정한 갈래 순서를 뒤집고 B 설계를 A 에서 먼저 정함. A 에서 의도 행만 먼저 커밋 — KIS 실패 때 그 행의 상태·잠금 해제·확정 작업이 같이 와야 해 결국 B 전체 |

| AS | 가정 | 바꾸려면 |
|---|---|---|
| AS-01 | 체결 금액·평균가 반올림 = scale 4 HALF_UP. 지금 매도 대금(ExecutionProcessor.kt:158)·평균가(:118) 규칙과 같고, 원화 정수 거래는 반올림이 안 일어남 | Order 한 곳 |
| AS-02 | `remainingLockedAmount` 이름은 유지, KDoc 만 «체결 완료·취소 때 돌려줄 잠금 잔액»으로 | Order.kt |
| AS-03 | 수수료 정책 없음 → 0 은 기존 동작이라 A 에서 안 바꿈(RISK-07) | 별도 결정 |

---

## 3. 바꿀 파일·모듈

| 경로 / 모듈 | 변경 종류 | 목적 | PR | 위험 |
|---|---|---|---|---|
| TA:domain/entity/order/Order.kt | 수정 | applyExecution 계약(D-01), hasSameTermsAs(D-04), isSubmittedToBroker(D-05), KDoc | A1·A4·A3 | 중간 — 체결 경로 전체가 씀 |
| TA:application/order/ExecutionProcessor.kt | 수정 | 평균가 계산 삭제, 해제액(D-02), 수수료(D-03) | A1·A5 | 높음 — 돈 흐름 핵심 |
| TA:application/order/OrderCommandService.kt | 수정 | 취소 해제(D-02)·중복 검사 삭제, 멱등(D-04), 취소 출처(D-05) | A2·A4·A3 | 높음 |
| TA:application/order/command/CancelOrderCommand.kt | 수정 | 취소 출처 필드 | A3 | 낮음 |
| TA:presentation/controller/OrderController.kt | 수정 | 사용자 출처 전달 | A3 | 낮음 |
| TA:infrastructure/scheduler/LocalMatchingScheduler.kt | 수정 | 시스템 출처 전달 | A3 | 낮음 |
| TA:common/exception/OrderExceptions.kt | 신설 | 409·501 예외(D-06) | A4·A3 | 낮음 |
| TA:presentation/exception/GlobalExceptionHandler.kt | 수정 | 501 매핑 | A3 | 중간 — 모든 오류 응답이 지남 |
| TA:domain/entity/feepolicy/FeePolicy.kt | 수정(DEC-01 이 2안일 때만) | 반올림 방향 | A5 | 낮음 |
| backend/trading-api/src/main/resources/db/migration/V13__*.sql | 신설(DEC-05 가 보정일 때만) | total_amount 재계산 | A6 | 중간 |
| TT:domain/entity/order/OrderTest.kt | 수정 | 체결 계약·조건 비교·브로커 접수 판정 시험 | A1·A4·A3 | 낮음 |
| TT:application/order/ExecutionProcessorTest.kt | 수정 | 매수 부분 체결 재현, 수수료, #551 #552 | A1·A5 | 낮음 |
| TT:application/order/OrderCommandServiceTest.kt | 수정 | 취소 해제, 멱등 불일치, KIS 취소 차단, #218 #324 | A2·A4·A3 | 낮음 |
| TT:presentation/controller/OrderControllerIntegrationTest.kt (또는 같은 기반의 새 통합 시험) | 수정 / 신설 | 동시 같은 키, 409, 501 | A4·A3 | 중간 — Docker 필요 |
| CFG:ktlint/baseline.xml · CFG:detekt/baseline.xml | 수정 | 고친 항목 제거(줄기만) | 전 PR | 낮음 |

손대지 않게 설계한 파일: Account.kt(메서드 변경 불필요), AccountTest.kt(ktlint 59), CommonApiExceptions.kt(ktlint 35), KIS 실행기 2개(B), FeePolicyTest.kt(DEC-01 1안일 때).

손댈 파일의 기존 린트 기준선(정리량 근거, DEC-06):

| 파일 | ktlint | detekt 구조 항목(기준선에 남길 후보) | detekt 국소 항목(고칠 것) |
|---|---|---|---|
| ExecutionProcessor.kt | 17 | LongMethod·ReturnCount processFill, LongParameterList 생성자 | MagicNumber 4, MaxLineLength 2 |
| OrderCommandService.kt | 6 | LongParameterList 2, ReturnCount 2, TooManyFunctions | MagicNumber 4(#253 고침으로 사라짐), MagicNumber 60 |
| Order.kt | 5 | LongParameterList create, ProtectedMemberInFinalClass(JPA 생성자) | MaxLineLength 1 |
| OrderTest.kt | 34 (함수 이름 21) | — | — |
| ExecutionProcessorTest.kt | 4 | — | MaxLineLength 1 |
| OrderCommandServiceTest.kt | 31 | — | MaxLineLength 1 |
| OrderControllerIntegrationTest.kt | 27 | — | MaxLineLength 1, UnusedPrivateProperty 1 |
| OrderController.kt | 6 | — | — |
| GlobalExceptionHandler.kt | 15 | — | MagicNumber 3, MaxLineLength 1, UnusedParameter 2 |
| LocalMatchingScheduler.kt | 0 | — | LoopWithTooManyJumpStatements 1 |
| FeePolicy.kt (DEC-01 2안일 때) | 4 | LongParameterList create, ProtectedMemberInFinalClass | MagicNumber 4, NewLineAtEndOfFile |

---

## 4. 데이터·API 작업

| 항목 | 필요한 변경 | 소유 | 막는가(blocking)? |
|---|---|---|---|
| `POST /api/v1/accounts/{accountId}/orders` 같은 키·다른 조건 | 201(기존 주문) → 409 `IDEMPOTENCY_KEY_REUSED` (F-3) | backend | 예 — DEC-02 |
| 같은 엔드포인트 같은 키 동시 요청 | 500 → 201(같은 주문) | backend | 아니오 |
| `DELETE /api/v1/accounts/{accountId}/orders/{orderId}` KIS 접수 주문 | 204 → 501 `BROKER_ORDER_CANCEL_NOT_SUPPORTED` (I-1) | backend | 예 — DEC-04 |
| 같은 DELETE·체결 처리, 부분 체결 시나리오 | «잠금 예수금이 부족합니다» IllegalArgumentException(400 / 체결 롤백)이 그 시나리오에서 사라짐 | backend | 아니오 |
| 클라이언트 영향(팀 AE20) | 호출처 0 — trading-web 은 재제작 중이라 주문 접수·취소 호출 없음(멱등 키 생성 `frontend/trading-web/src/shared/lib/idempotency.ts` 만), `backend/quant-*`·collector-api 에 주문 API 호출 0(grep). 커밋에 `App-Impact: none — 호출처 0(grep)`. 다음 trading-web 주문 화면은 409·501 의 `code` 로 분기 | frontend | 아니오 |
| OpenAPI | 명세 없음(springdoc 의존성 0) → openapi-diff 대상 없음. 계약 변경은 이 표와 커밋 기록으로 | backend | 아니오 |
| DB | 스키마 변경 없음. DEC-05 가 보정이면 V13 데이터 마이그레이션만(orders 는 원장 행이 아니라 PTF1 대상 아님, 원장 행은 손대지 않음) | backend | DEC-05 |

DEC-05 점검 쿼리(읽기 전용, 사람이 운영 DB 에서):

```sql
SELECT o.id, o.total_amount, ROUND(SUM(e.executed_price * e.executed_quantity), 4) AS exec_total
FROM orders o JOIN executions e ON e.order_id = o.id
GROUP BY o.id, o.total_amount
HAVING COUNT(*) > 1 AND o.total_amount <> ROUND(SUM(e.executed_price * e.executed_quantity), 4);
```

---

## 5. 구현 순서 개요

PR 안 커밋 순서(공통): 1. 린트 정리 — 손댈 파일만, 동작 불변(DEC-06) 2. 빨간 시험 — 원장 재현 시나리오 3. 고침 — 초록 4. 기준선 재생성 — 줄기만. 커밋 메시지는 한글.

1. PR-A1 체결 금액·최종 해제 — #8 #127 #108 + #551 #552. 빨간 시험: OrderTest «두 체결(4@69000 → 6@70000) 뒤 평균 69600·누적 696000», ExecutionProcessorTest «KIS_PAPER 매수 두 번 체결 뒤 잠금 0·가용 304000». 고침: D-01·D-02.
2. PR-A2 취소 해제 — #253(#252 중복 삭제) + #218 #324. 빨간 시험: «4@69000 부분 체결 뒤 취소하면 잠금 0·가용 = 예수금». 고침: D-02 취소 쪽.
3. PR-A3 KIS 접수 주문의 사용자 취소 차단 — #20 임시(DEC-04 1안일 때). 빨간 시험: «KIS 접수 주문 사용자 취소 → 501·상태 PENDING·잠금 그대로». 고침: D-05·D-06.
4. PR-A4 멱등 — #249 #250(DEC-02). 빨간 시험: 통합 «같은 키 동시 2요청 → 같은 주문·행 1·BUY_LOCK 1», 단위 «같은 키 다른 수량 → 409 예외». 고침: D-04·D-06.
5. PR-A5 수수료 — #126 다음 #245(DEC-01). 빨간 시험: «정책은 체결 시각으로 고름», «수수료 = 도메인 calculateFee 결과». 고침: D-03.
6. PR-A6 (조건부) total_amount 보정 — DEC-05 점검 결과가 1행 이상일 때.

순서 근거: 돈이 실제로 새는 순(A1 → A2), 결정 대기 PR 은 뒤. A1·A5 는 ExecutionProcessor 를, A2·A3·A4 는 OrderCommandService 를 같이 고쳐 차례로 합니다(병렬 안 함). A3 을 A4 앞에 둔 건 KIS_LIVE 실계좌 갈림이 멱등 오류보다 크기 때문입니다.

---

## 6. 테스트 계획

| 테스트 | 종류 | 명령 / 방법 | 기대 결과 |
|---|---|---|---|
| 체결 계약 | unit (도메인) | `cd backend/trading-api && ./gradlew test --tests '*OrderTest'` | 4@69000 → 6@70000: avgFilledPrice 69600, totalAmount 696000 (compareTo). 지금 코드는 평균이 70000 이라 빨강 |
| 매수 두 번 체결 (#8 #108) | unit (서비스) | `./gradlew test --tests '*ExecutionProcessorTest'` | KIS_PAPER 예수금 1000000, 10@70000 잠금 700000, fillExternal 4@69000 → 6@70000: totalAmount 696000, lockedDeposit 0, availableDeposit 304000, deposit 304000, BUY_UNLOCK 원장 4000 1건. 지금 코드는 unlockDeposit 예외로 빨강 |
| 같은 가격 두 번 체결 (#108) | unit | 같음 | 4@70000 → 6@70000: 예외 없음, lockedDeposit 0, 해제 원장 없음(잔액 0) |
| 단일 체결 가격 개선 (회귀) | unit | 같음 | 10@68000 한 번: 해제 20000 — 기존 동작 유지 |
| 부분 체결 뒤 취소 (#253) | unit | `./gradlew test --tests '*OrderCommandServiceTest'` | 잠금 700000, 4@69000 체결 상태에서 취소: BUY_UNLOCK 424000, lockedDeposit 0, available = deposit. 지금 코드는 4000 남아 빨강 |
| stale 시세 (#218) | unit | 같음 | updatedAt = 지금 − 1시간 → IllegalStateException(기존 동작 고정, 분류 변경은 DEC-07). 이름 «시세가 없으면 QuoteUnavailableException» 시험은 이름만 고침 |
| BigDecimal 단언 (#324 #552) · 이름 (#551) | unit | 같음 | compareTo 단언, 이름이 단언과 같은 말 |
| KIS 접수 주문 사용자 취소 (#20) | unit + integration | 단위 위와 같음 · `./gradlew test --tests '*OrderControllerIntegrationTest'` | 단위: BrokerOrderCancelNotSupportedException, 상태 PENDING, 원장 저장 0. 통합: DELETE → 501, 본문 code `BROKER_ORDER_CANCEL_NOT_SUPPORTED` |
| KIS IOC 접수 전 즉시 취소 (회귀) | unit | OrderCommandServiceTest | KIS_PAPER IOC 시세 없음 → CANCELLED, 실행기 호출 0 |
| 같은 키 다른 조건 (#250) | unit + integration | 위와 같음 | 수량 1 → 10: IdempotencyKeyReusedException · 통합 409 + code |
| 같은 키 같은 조건 (회귀) | unit (도메인) + integration | OrderTest · 기존 통합 시험 «주문 멱등성»(OrderControllerIntegrationTest.kt:212-249) | 수량 1 과 1.00000000 을 같은 조건으로 봄, 같은 주문 id |
| 같은 키 동시 요청 (#249) | integration (Testcontainers) | 위와 같음 | 시험 트랜잭션이 계좌 행을 잡은 채 두 스레드가 같은 키로 placeOrder → `pg_stat_activity` 에서 락 대기 2건 확인 뒤 풀기 → 둘 다 같은 id, orders 행 1, BUY_LOCK 원장 1. 지금 코드는 하나가 DataIntegrityViolationException 으로 빨강 |
| 정책은 체결 시각 (#126) | unit | ExecutionProcessorTest | `findActivePolicy(mode, market, executedAt)` → P1, 다른 시각 → P2 로 stub: 수수료 = P1.calculateFee |
| 수수료 = 도메인 (#245) | unit | 같음 | 요율 0.000015, 1@70001: fee compareTo policy.calculateFee(70001) |
| 계층 동결 | unit (ArchUnit) | `./gradlew test --tests '*ArchitectureRulesTest'` | 초록, 동결 저장소 diff 0 |
| 전체 게이트 | 전체 | `cd backend/trading-api && ./gradlew test ktlintCheck detekt verifyLintBaselinesNotGrown` (PR 전체 비교는 `-PlintBaselineRef=main` — 기본 ref HEAD 는 커밋 뒤 늘 통과) | 모두 초록, 손댄 파일 기준선 항목 수 감소(baseline.xml diff 로 확인) |
| KIS_PAPER 부분 체결 실측 | manual (선택, 사람) | 모의계좌에서 부분 체결 주문 | 최종 체결 반영·잠금 0 |

---

## 7. 도메인 제약 반영

| 차원 | 적용 | 구현·검증 방법 | review routing |
|---|---|---|---|
| 수치 무결성 | y | BigDecimal 만, compareTo 단언, 나눗셈은 `divide(x, 4, RoundingMode)` 명시, Kotlin `/`·`==` 금지(PTB4). 금액 계산은 Order·FeePolicy 한 곳. 원장 #8·#108·#253 수치를 그대로 시험 값으로 씀(golden) | 리뷰 |
| 단위·기준치 | y | 요율은 fee_policies 행에서 읽음(하드코딩 금지). 반올림 방향은 DEC-01 전까지 지금 도메인 규칙을 바꾸지 않음(미정 정책 하드코딩 금지) | DEC-01 |
| 결측·LOD/LOQ | n | 해당 없음. 수수료 정책 없음 → 0 은 기존 동작 유지(AS-03, RISK-07) | — |
| 규제·표현 | n | — | — |
| PHI | n | — | — |
| 감사·추적 (루트 CLAUDE.md Financial Safety) | y | 원장 행 UPDATE·DELETE 없음(PTF1). 해제 원장 멱등 키(`unlock-excess-{orderId}`, `cancel-unlock-{orderId}`) 유지. 상태 전이는 Order 메서드로만(PTB2) | 리뷰 |

---

## 8. 리스크·막힘

| ID | 리스크 / 막힘 | impact | 완화 / 필요한 결정 |
|---|---|---|---|
| RISK-01 | 과거 #108·#253 로 이미 어긋난 계좌 lockedDeposit 은 코드 수정으로 안 돌아옴 | 높음 (KIS 계좌) | DEC-05 1안에서 영향 계좌 기록, 대사(PTF2)는 별도 |
| RISK-02 | A 기간 동안 #17(이중 주문)·#20 GTD 만료 갈림 유지 | 높음 | DEC-03·04 |
| RISK-03 | O2 때문에 #108 을 고쳐도 시장가 매수 체결가가 잠금가보다 높으면 체결 실패·다른 주문 잠금 소진 | 중간 | DEC-08 |
| RISK-04 | ArchUnit 동결·detekt 서명 ID 가 고칠 자리를 제약 | 중간 | D-08, §9 멈춤 조건 |
| RISK-05 | 동시성 통합 시험의 비결정성 | 중간 | 고정 sleep 금지, `pg_stat_activity` 로 락 대기를 확인한 뒤 풀기 |
| RISK-06 | stale 시험이 벽시계 기준(시계 주입은 C·D) | 낮음 | 여유 1시간 |
| RISK-07 | 수수료 정책 없음 → 0 은 조용한 기본값(기존, ExecutionProcessor.kt:109-110) | 낮음 | A 에서 안 바꾸고 기록만 |
| RISK-08 | createAutoExitSellOrder(main 호출처 0, OrderCommandService.kt:211-236)는 수량을 orderableQuantity 로 다시 읽어 같은 키 재시도 때 DEC-02 1안에서 409 | 낮음 | 호출처 0 확인(grep). 지우기는 범위 밖 |
| RISK-09 | 손댄 시험 파일의 형식 정리 커밋이 큼(OrderTest 34·OrderCommandServiceTest 31·통합 27) | 낮음 | 동작 불변 커밋으로 분리, 시험 초록으로 확인 |
| RISK-10 | 전역 핸들러가 예외 메시지를 응답에 그대로 실음(팀 AE8, 기존) | 낮음 | 새 예외는 고정 문구(D-06) |

### 8-1. 사람 결정 필요

| DEC | 무엇 | 선택지 (추천 순 · 장단) | 추천 | 막는 PR |
|---|---|---|---|---|
| DEC-01 | 수수료 반올림 방향 (#245) | 1. 도메인 UP 유지, 서비스는 도메인만 부름 — 장: 문서화·시험된 규칙 그대로(FeePolicy.kt:74-82), FeePolicy·FeePolicyTest 안 건드림. 단: 운영 수수료가 체결마다 최대 0.0001 늘 수 있음. 2. 도메인을 HALF_UP 으로 — 장: 지금 운영 값과 이어지고 세금(HALF_UP)과 같음. 단: FeePolicy.kt·FeePolicyTest.kt 를 고쳐 둘의 린트(ktlint 4+31, detekt 4+2)까지 정리. 3. 실제 증권사 규칙(예: 원 미만 절사) — 장: 실계좌와 맞음. 단: KIS 규칙 근거 확인(research) 필요, 시장·모드별로 다를 수 있음 | 1 | PR-A5 의 #245 |
| DEC-02 | 같은 멱등 키·다른 요청 (#250) | 1. 409 `IDEMPOTENCY_KEY_REUSED`(F-3), 비교 = 요청 본문 전부(ticker·marketType·orderType·orderSide·orderCondition·quantity·limitPrice·expireAt·strategyId·signalId·orderGroupId, 수량·가격은 compareTo) — 장: 팀 분류표(F «현재 상태가 막음», 422 는 프레임워크 전용)와 IETF Idempotency-Key 초안의 «같은 키 다른 본문은 오류»와 같은 방향. 단: 201 을 기대하던 호출자가 있으면 깨짐(지금 0). 2. 409 + 돈·상태를 바꾸는 필드만 비교(strategyId·signalId·orderGroupId 제외) — 장: 메타데이터만 다른 재시도 통과. 단: 전략 추적 값이 첫 요청 것으로 조용히 남음. 3. 지금처럼 기존 주문 반환 — 장: 변경 없음. 단: #250 그대로, 팀 failure-handling 과 어긋남 | 1 | PR-A4 |
| DEC-03 | #17 I3 자리 | 1. B 갈래 첫 PR 로 — X3 #19·#18, 상태 #95(SUBMITTING·UNKNOWN), 확정 작업(PTF3), #219 와 함께. 장: 같은 코드를 한 번만 고침. 단: A 기간 동안 KIS 이중 주문 위험 유지. 2. 1 + A 기간 KIS_LIVE 주문 접수를 운영 설정으로 막음 — 장: 실돈 위험 0. 단: 실계좌 기능 정지(제품 결정). 3. A 에 넣음 — 장: 가장 큰 돈 위험을 먼저. 단: 갈래 순서를 뒤집고 B 설계를 A 에서 먼저 정함, A 가 커짐 | 1 (KIS_LIVE 를 실제로 쓰면 2) | 없음 (B 착수 시점) |
| DEC-04 | #20 KIS 취소 — a. A 임시 조치 | 1. 사용자 취소(DELETE)만 «KIS 접수 주문»이면 501 거절, GTD 만료·IOC/FOK 는 그대로(D-05) — 장: «취소됐다»고 믿는데 KIS 에서 체결되는 갈림을 바로 막음, 다른 경로 불변. 단: B 전까지 앱에서 KIS 주문 취소 불가 — HTS 에서 취소하면 로컬 잠금이 B 의 대사 전까지 남음, GTD 만료 갈림은 남음. 2. 모든 로컬 취소에서 KIS 접수 주문 거절(GTD 포함) — 장: 갈림 전부 차단. 단: KIS 주문은 늘 당일 주문이라(KisOrderRestClient.kt:42 가 주문 조건을 안 보냄) 장 마감에 KIS 가 죽인 주문의 잠금이 B 전까지 안 풀리고 스케줄러가 30초마다 실패 로그. 3. 지금처럼 두고 B 로 — 장: 변경 없음. 단: 갈림 지속 | 1 | PR-A3 |
| DEC-04 | #20 — b. B 에서 쓸 «KIS 취소 호출 실패·불확정 시 상태» | 1. CANCEL_REQUESTED 유지 + 조회로 확정(PTF3 — 타임아웃은 실패가 아님). 2. UNKNOWN 으로 두고 대사. 3. 원상태(PENDING) 복귀 + 오류 응답 — 실제로는 취소됐을 수 있어 재취소·이중 처리 위험. 참고: 지금 실행기 cancel 은 실패를 log.warn 으로 삼킴(KisLiveOrderExecutor.kt:59-61) → B 에서 그대로 쓰면 안 됨 | 1 | 없음 (B 입력) |
| DEC-05 | 기존 total_amount 보정 (O4) | 1. §4 점검 쿼리를 운영 DB 에서 먼저 돌려 행 수 확인 → 0 이면 보정 없음, 있으면 PR-A6 에서 V13 으로 재계산 + 그 계좌의 lockedDeposit 차이를 대사 항목으로 기록 — 장: 근거로 정함. 단: 사람이 운영 DB 를 봐야 함. 2. 보정 없이 앞으로만 — 장: 마이그레이션 0. 단: 진행 중(PARTIAL) KIS 주문은 고친 뒤에도 틀린 값으로 해제액 계산. 3. 무조건 V13 — 장: 확실. 단: 영향 0 이면 불필요한 데이터 변경 | 1 | PR-A6 |
| DEC-06 | 손댄 파일 린트 범위 (R3-1 정책 해석) | 1. ktlint 는 손댄 파일 전부, detekt 는 국소 항목만 고치고 구조 항목(§3 표)은 기준선에 남겨 C·D 로 — 장: 구조 항목을 고치는 일이 곧 S1·S3·S4·D1 리팩터링이라 갈래 순서를 지키고 기준선은 줄어듦. 단: 손댄 파일에 detekt 항목이 남음(같은 ID 라 «늘지 않음»). 2. ktlint·detekt 전부 — 장: 정책 글자 그대로. 단: processFill 115줄 분해·생성자 의존 11개 축소(포트 = C)가 A 로 당겨짐. 3. 새 줄만 깨끗이 — 장: diff 최소. 단: 정책과 어긋남 | 1 | 전 PR 의 린트 커밋 |
| DEC-07 | 손댄 파일 오류 분류 정리 (팀 AE19) | 1. 미룸 — A 커밋에 `API-Errors-Deferred: <path> — 오류 분류 정리는 별도 항목` 기록, docs/TODO.md 에 항목 등록. A 가 새로 만드는 예외(409·501)만 분류표대로 — 장: 사람이 정한 A 범위(돈·상태) 유지, 클라이언트 0 이라 급하지 않음. 단: OrderCommandService 의 check·require 400(남의 주문 → E-2 404, 취소 불가 상태 → F-2 409, 수량 부족 → F-5 409 등)이 남음, #591 도 이 묶음. 2. A 안 별도 PR 로 지금 고침(AE19 의 권장) — 장: 규칙 권장안. 단: 오류 응답 5곳 안팎이 바뀌어 기존 통합 시험 기대값(OrderControllerIntegrationTest.kt:134-135 «stale 시세면 400» 등)도 바뀜, A 범위 확대 | 1 | 없음 (커밋 기록 방식) |
| DEC-08 | 원장 밖 관찰 O1~O3 편입 | 1. 원장 보강(발견 등록) 뒤 A 다음 묶음으로 — 각각 정책이 필요(매수 수수료 차감 여부·모드별, 시장가 잠금 여유 또는 체결 때 추가 잠금, 정산 단위 = 체결별 또는 주문별). 장: A 를 원장 후보로 고정. 단: 그동안 결함 유지. 2. A 에 편입 — 장: 같은 성격의 돈 결함. 단: 정책 결정 3건이 A 를 막고 재감사 없이 범위가 늘어남. 3. 안 다룸 — 단: 돈 결함 방치 | 1 | 없음 |

**사람 결정 (2026-11-15, 팝업):** 계획 승인 «승인, A1부터». DEC-01 = 1(도메인 UP 유지) · DEC-02 = 1(409, 본문 전부 비교) · DEC-03 = 1(B 첫 PR) · DEC-04a = 1(사용자 취소만 501) · DEC-05~08 = 각 1안. DEC-04b 는 B 착수 때 확정.
**DEC-01 변경 (2026-11-15, 사람 «돈은 반올림같은거하면안되는데?» 뒤 팝업):** 1안 철회 → 3안(실제 증권사 수수료·세금 원 미만 처리 규칙을 출처와 조사한 뒤 결정). PR-A5 의 #245 는 그때까지 대기, #126(정책 조회 시각)은 영향 없음.

---

## 9. 구현자에게 넘기기 (Handoff)

> 구현자(사람·AI·Codex)는 아래 경계 안에서만 작업합니다. 실행은 steps/step-1.md 부터.

**이것만 만들어라 (승인된 범위):**
- §1-1 에서 «A · PR-…» 인 항목만, §5 순서로. PR-A1·A2 는 바로, PR-A3~A6 은 해당 DEC 가 정해진 안으로만.
- 결함마다 빨간 시험(원장 재현 시나리오) 커밋을 먼저, 그다음 고침. 돈 단언은 compareTo.
- 손댄 파일 린트는 DEC-06 결정 범위만, 고친 뒤 기준선 재생성으로 줄이기.

**이건 하지 마라 (범위 밖):**
- B·C·D 항목: KIS 접수를 트랜잭션 밖으로(#17·#19), collector 구독 HTTP(#9·#18), KIS 취소 실제 호출(#20 본 수정), 포트 도입(L1), 어댑터의 영속 예외 변환, 시계 주입(F4), processFill 분해(S1·S3·#397), JPA 분리(D1), 시험의 리포지토리 목 교체(T2), #219.
- #22 목록 상한, #591, O1~O3 (DEC 전).
- 동결 저장소 갱신 옵션(`-Darchunit.freeze.store.default.allowStoreUpdate=true` 등), 새 위반이 남은 채 `ktlintGenerateBaseline`·`detektBaseline` 실행.
- 프로젝트 전체 `ktlintFormat`(손 안 댄 파일까지 바뀜) — 손댄 파일만.
- 리포지토리 호출을 새 메서드로 옮기기, 기준선에 서명이 오른 함수 서명 바꾸기(D-08).
- 원장 행 UPDATE·DELETE, `git stash`.

**이때는 멈추고 물어봐라:**
- ArchitectureRulesTest 가 새 위반을 내고 코드 자리로 피할 수 없을 때.
- verifyLintBaselinesNotGrown 이 늘었다고 하고 구조 항목을 고쳐야만 풀릴 때(DEC-06 재확인).
- 재현 시험이 원장 시나리오대로 빨갛게 안 될 때 — 원장 판정이 틀렸을 수 있으니 고치지 말고 보고.
- 고치다 원장에 없는 돈 결함을 또 볼 때 — O 항목처럼 기록만 하고 보고.
- Docker 가 없어 통합 시험을 못 돌릴 때 — 건너뛰지 말고 보고.
- 손댄 파일만 형식 정리하는 방법이 없을 때.

**다음:** Maker 가 docs/TODO.md 에 항목을 먼저 등록하고(루트 CLAUDE.md), §5 를 5~7 단계로 나눠 state.md §2 로드맵과 step-1 을 `step-scaffold` 로 만든 뒤, 로드맵이 고정되면 `feature-approval set trading-api-money-state tech-plan --by <승인자>` 로 설계 승인 흔적을 남깁니다. 이 계획의 교차 검토(cross-check, 다른 모델)는 사용자가 직접 요청할 때만 돌립니다.
