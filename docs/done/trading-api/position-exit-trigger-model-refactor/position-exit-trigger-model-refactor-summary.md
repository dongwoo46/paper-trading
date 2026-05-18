# Position Exit Trigger Model Refactor Summary

## Result

Completed on 2026-05-19.

`PositionExitTrigger` now represents one automatic exit condition per row. The old combined position trigger API shape was replaced with plural single-trigger APIs, and automatic exit orders now support grouped trigger execution with `orderGroupId`.

## Implemented

- Replaced combined stop-loss/take-profit trigger row shape with a single-condition entity.
- Added `PriceBasisPolicy` with `FIXED_PRICE`, `AVG_PRICE_AT_CREATION`, and `FOLLOW_AVG_PRICE`.
- Added dynamic `exitRatioPercent`, single trigger `state`, `skipReason`, evaluation metadata, and optimistic versioning.
- Rebuilt position trigger command/query/controller DTO flow around `/api/positions/{positionId}/exit-triggers`.
- Added grouped orchestration by `positionId + triggerType`.
- Added pending sell quantity subtraction before automatic order quantity calculation.
- Added grouped auto-exit `orderGroupId` and trigger audit input handling.
- Split grouped order placement into a `REQUIRES_NEW` transaction boundary so order failures do not roll back trigger `FAILED`/`SKIPPED` state updates.
- Updated focused command/evaluator/orchestrator/controller/persistence tests for the new model.

## Verification

- `./gradlew test --tests "*PositionExitTrigger*"`: PASS
- `./gradlew compileKotlin`: PASS
- Changed files TODO/FIXME scan: no unresolved entries found

## Commits

- `66fb6ce` 작업중: 포지션 청산 트리거 리팩터링 변경사항 보존
- `cfa710c` 포지션 청산 트리거 도메인 모델 재정의
- `dacab14` 포지션 청산 트리거 API 단일 조건 구조로 변경
- `722ac79` 포지션 청산 트리거 그룹 주문 처리 구현
- `ac6258e` 포지션 청산 트리거 단일 조건 테스트 보강
- `fdb4bec` 자동 청산 주문 트랜잭션 경계 분리

## Residual Notes

- `AccountExitTriggerDefault` remains intentionally out of scope and keeps the existing default trigger model.
- The branch still includes the initially preserved collector-api quote event changes from the WIP commit.
- PR creation is pending user confirmation.
