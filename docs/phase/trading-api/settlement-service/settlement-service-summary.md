# Settlement Service — Implementation Summary

Phase: trading-api/settlement-service
Branch: feature/trading-api-settlement-service
Date: 2026-04-28
Status: COMPLETED

---

## What Was Built

T+2 정산 처리 시스템. 매도 체결 후 한국 주식시장 규칙에 따라 영업일 기준 2일 후 대금을 계좌에 입금하는 흐름을 구현.

### Option C Design Decision

LOCAL 모드와 KIS 모드의 UX/현실성 트레이드오프를 분리:

- **LOCAL** (모의투자): 매도 체결 즉시 `account.receiveSellProceeds()` 호출 → 즉시 잔액 반영
- **KIS_PAPER / KIS_LIVE**: `PendingSettlement` 생성 (settlementDate = T+2 영업일) → 스케줄러가 기준일에 처리

이 결정은 LOCAL 모드의 빠른 피드백 루프를 보존하면서 실제 KIS 정산 규칙을 정확히 반영한다.

---

## Key Design Decisions

1. **Batch transaction isolation (REQUIRES_NEW)**: `SettlementCommandService.processSettlements`는 `@Transactional` 없이 루프만 돌고, 실제 처리는 `SettlementProcessor.processOne`(`@Transactional(REQUIRES_NEW)`)에 위임. 한 건 실패가 전체 배치를 롤백시키지 않는다.

2. **Idempotency**: `AccountLedger` 저장 시 `idempotencyKey = "settlement-{id}"` — 중복 처리 방지.

3. **Pessimistic lock**: `accountRepository.findByIdWithLock(id)` — 동시 정산 처리 시 계좌 잔액 double-credit 방지.

4. **BusinessDayCalculator MVP**: 주말 스킵만 구현, 공휴일 미지원. 한국 공휴일 지원은 별도 이슈로 분리.

5. **Explicit save**: `accountRepository.save(account)` — REQUIRES_NEW 트랜잭션에서 엔티티 detach 위험을 방지하기 위해 명시적으로 저장.

---

## Files Created

| File | Description |
|------|-------------|
| `backend/trading-api/src/main/kotlin/.../domain/util/BusinessDayCalculator.kt` | 영업일 계산 유틸 (주말 스킵) |
| `backend/trading-api/src/main/kotlin/.../application/settlement/SettlementProcessor.kt` | 단건 정산 처리 빈 (REQUIRES_NEW) |
| `backend/trading-api/src/main/kotlin/.../application/settlement/SettlementCommandService.kt` | 배치 + 단건 재처리 오케스트레이터 |
| `backend/trading-api/src/main/kotlin/.../infrastructure/scheduler/SettlementScheduler.kt` | KST 16:30 MON-FRI 정산 스케줄러 |
| `backend/trading-api/src/test/kotlin/.../domain/util/BusinessDayCalculatorTest.kt` | 5 test cases |
| `backend/trading-api/src/test/kotlin/.../application/settlement/SettlementProcessorTest.kt` | 4 test cases |
| `backend/trading-api/src/test/kotlin/.../application/settlement/SettlementCommandServiceTest.kt` | 5 test cases |
| `backend/trading-api/src/test/kotlin/.../application/order/ExecutionProcessorTest.kt` | Option C 분기 검증 2 test cases |

## Files Modified

| File | Description |
|------|-------------|
| `backend/trading-api/src/main/kotlin/.../application/order/ExecutionProcessor.kt` | Option C 분기 추가 (LOCAL 즉시 / KIS 지연) |

---

## Test Results

```
SettlementCommandServiceTest  5/5  PASS
SettlementProcessorTest       4/4  PASS
BusinessDayCalculatorTest     5/5  PASS (separate test file)
ExecutionProcessorTest        2/2  PASS (Option C branching)
Full test suite               BUILD SUCCESSFUL
```

---

## Code Review Notes

- 1차 리뷰: 🔴 배치 트랜잭션 격리 실패(REQUIRES_NEW 필요) + 🔴 accountRepository.save 누락 → step 2b rework
- 2차 리뷰: PASS. 🟡 processSettlement 불필요한 @Transactional, test assertion gap — 비블로킹
- Finding 3 (realizedPnl = grossProceeds - fee): spec 의도적 MVP 간소화, 수정 불필요
