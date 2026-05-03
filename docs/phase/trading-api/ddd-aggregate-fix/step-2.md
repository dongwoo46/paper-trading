# Step 2: 구현 — Aggregate 경계 재구성 (TDD)
Assigned agent: fullstack-dev

## Working Directory
`.worktrees/trading-api-ddd-aggregate-fix`

## Files to Read

- `CODEX.md`
- `docs/TODO.md`
- `docs/phase/trading-api/ddd-aggregate-fix/spec.md`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/Execution.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/Settlement.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/SettlementExecution.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/PendingSettlement.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/RiskPolicyService.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/settlement/SettlementCommandService.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/settlement/SettlementProcessor.kt`

## Tasks (Red -> Green)

1. 엔티티 참조를 ID 참조로 전환
- 파일: `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/Execution.kt`
  - `order: Order?` 제거, `orderId: Long` 필드 도입
  - `account: Account?` 제거, `accountId: Long` 필드 도입
- 파일: `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/Settlement.kt`
  - `order: Order?` 제거, `orderId: Long` 필드 도입
  - `account: Account?` 제거, `accountId: Long` 필드 도입
- 파일: `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/SettlementExecution.kt`
  - `settlement: Settlement?`, `execution: Execution?` 제거
  - `settlementId: Long`, `executionId: Long` 도입
- 파일: `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/PendingSettlement.kt`
  - `account: Account?` 직접 참조 제거, `accountId: Long` 도입

2. `ExecutionProcessor`를 Command 경로 기준으로 재구성
- 파일: `backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt`
  - 생성자에서 내부 Entity Repository의 Application 노출 의존 제거
  - 상태 변경 경로는 `orderRepository`, `accountRepository` 중심으로 유지
  - `externalExecutionId` 멱등성 검사 로직을 명시적으로 유지
  - DB unique constraint 기대 동작(중복 삽입 방지)과 충돌하지 않도록 처리

3. `RiskPolicyService`에서 내부 Repository 제거
- 파일: `backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/RiskPolicyService.kt`
  - `RiskPolicyRepository` 의존 제거
  - 활성 정책 비활성화/신규 생성을 `Account` Aggregate 도메인 동작으로 이관

4. 정산 유스케이스를 Command/Query 분리 기준으로 변경
- 파일: `backend/trading-api/src/main/kotlin/com/papertrading/api/application/settlement/SettlementCommandService.kt`
- 파일: `backend/trading-api/src/main/kotlin/com/papertrading/api/application/settlement/SettlementProcessor.kt`
  - 상태 전환(Command) 경로는 `AccountRepository` 중심으로 유지
  - 조회(Query) 경로는 `ReadRepository/ProjectionRepository` 허용 정책과 충돌 없게 유지

5. 내부 Repository 처리 원칙 반영
- 파일: `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/*.kt`
  - 대상: `ExecutionRepository`, `SettlementRepository`, `RiskPolicyRepository`, `PendingSettlementRepository`, `OrderAmendmentRepository`, `SettlementExecutionRepository`
  - 물리적 삭제 강제가 아니라 Application 비노출을 목표로 정리
  - 우선 compileKotlin 통과

## Verification Commands

```bash
cd .worktrees/trading-api-ddd-aggregate-fix/backend/trading-api
./gradlew compileKotlin
```

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
