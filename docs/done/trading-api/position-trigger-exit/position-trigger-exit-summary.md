# Position Trigger Exit Summary

- Project: trading-api
- Phase: position-trigger-exit
- Completed date: 2026-05-08
- Branch: feature/trading-api-position-trigger-exit

## Scope Delivered
- 포지션 퍼센트 기반 자동청산(손절/익절) 도메인/애플리케이션 흐름 구현
- OCO 가드(한쪽 TRIGGERED 시 반대편 CANCELED) 및 중복 실행 방지(idempotency key + unique scope) 적용
- 계좌 기본값/포지션 override API 및 유효성/오류 계약(400/409/422) 구현
- 실패 시 FAILED 전이, 재시도 경로, 실패 알림 이벤트 흐름 보강
- duplicate-key 충돌 시 기존 idempotent order 존재 확인 후 TRIGGERED 확정(불확정 시 FAILED+알림)으로 결정성 보장

## PR-Ready Change Summary
- Migration
  - `V6__create_account_exit_trigger_defaults.sql`
  - `V7__create_position_exit_triggers.sql`
  - `V8__add_order_idempotency_scope.sql`
- Domain
  - `PositionExitTrigger`, `TriggerState`, `TriggerType`, `AccountExitTriggerDefault` 추가/확장
  - `PositionExitTriggerRepository`, `AccountExitTriggerDefaultRepository` 계약 추가
- Application
  - `PositionExitTriggerCommandService`, `PositionExitTriggerQueryService`, `PositionExitTriggerEvaluator`, `PositionExitTriggerOrchestrator`
  - `OrderCommandService` 자동청산 엔트리포인트 연동
  - stale version/position not eligible 예외 및 핸들링 경로 추가
- API
  - `PositionExitTriggerController` + DTOs
  - `AccountExitTriggerDefaultController` + DTOs
  - `GlobalExceptionHandler`에 400/409/422 매핑 보강
- Test
  - trigger evaluator/command/orchestrator/controller 테스트 추가 및 보강
  - duplicate-key 결정성, closed-position 422, concurrency, failure path 재검증

## Validation
- PASS: `./gradlew test --tests "*PositionExitTrigger*Test"`
- PASS: `./gradlew test --tests "*QuoteEventListenerTest"`
- PASS: `./gradlew compileKotlin`

## Risk Notes
- 전체 통합 테스트(`./gradlew test`)는 환경(Testcontainers/Docker) 의존 구간에 따라 별도 확인이 필요함.
- 운영 환경에서 retry/notification 소비자 설정 상태를 배포 체크리스트로 점검 필요.

## Rollback Notes
- 기능 롤백 시 `position_exit_triggers`, `account_exit_trigger_defaults`, orders idempotency scope 변경(V6~V8)을 역마이그레이션 정책에 따라 되돌려야 함.
- API 롤백 시 신규 엔드포인트 비활성화와 함께 자동청산 오케스트레이션 리스너를 차단해야 함.

## PR Status
- Pending (PR not created yet)
