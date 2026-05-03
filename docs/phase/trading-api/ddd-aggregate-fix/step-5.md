# Step 5: 코드 리뷰 — 경계 위반 재발 방지 확인
Assigned agent: code-reviewer

## Working Directory
`.worktrees/trading-api-ddd-aggregate-fix`

## Files to Read

- `docs/phase/trading-api/ddd-aggregate-fix/spec.md`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/RiskPolicyService.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/settlement/SettlementCommandService.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/settlement/SettlementProcessor.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/Execution.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/Settlement.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/SettlementExecution.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/PendingSettlement.kt`

## Tasks

1. Aggregate 경계 준수 리뷰
- 애플리케이션 서비스가 내부 Entity Repository를 직접 의존하지 않는지 검토
- Root Repository(`OrderRepository`, `AccountRepository`) 경유가 일관적인지 검토

2. 도메인 참조 규칙 리뷰
- Aggregate 간 객체 참조 제거가 완전한지 검토
- ID 참조 기반으로 전환되며 lazy loading 부작용이 사라졌는지 검토

3. 금융 안전성 리뷰
- 금액 연산에서 `BigDecimal` scale 일관성 확인
- 체결/정산의 idempotency key 동작이 회귀하지 않았는지 확인

4. 테스트 공백 점검
- 경계 변경으로 누락된 테스트 케이스 제시

## Verification Commands

```bash
cd .worktrees/trading-api-ddd-aggregate-fix/backend/trading-api
./gradlew test
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
