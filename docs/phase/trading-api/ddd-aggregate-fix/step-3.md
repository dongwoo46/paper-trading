# Step 3: 테스트 보강 및 회귀 검증
Assigned agent: test-engineer

## Working Directory
`.worktrees/trading-api-ddd-aggregate-fix`

## Files to Read

- `docs/phase/trading-api/ddd-aggregate-fix/spec.md`
- `backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/ExecutionProcessorTest.kt`
- `backend/trading-api/src/test/kotlin/com/papertrading/api/application/settlement/SettlementCommandServiceTest.kt`
- `backend/trading-api/src/test/kotlin/com/papertrading/api/application/settlement/SettlementProcessorTest.kt`
- `backend/trading-api/src/test/kotlin/com/papertrading/api/application/account/RiskPolicyServiceTest.kt`
- `backend/trading-api/src/test/kotlin/com/papertrading/api/application/account/PendingSettlementQueryServiceTest.kt`

## Tasks

1. 경계 변경에 맞춰 테스트 fixture 수정
- 엔티티 생성 fixture에서 객체 참조 대신 ID 세팅으로 교체
- `Execution`, `Settlement`, `PendingSettlement`, `SettlementExecution` 관련 fixture 정리

2. 유스케이스 테스트 보강
- `ExecutionProcessorTest`:
  - 외부 체결 ID 중복 방지 로직 회귀
  - SELL FILLED 시 정산/연결 레코드 생성 회귀
- `SettlementCommandServiceTest`, `SettlementProcessorTest`:
  - 계좌 조회 및 정산 완료 상태전환 회귀
- `RiskPolicyServiceTest`:
  - 활성 정책 교체(upsert) 동작 회귀

3. 제거된 내부 Repository 참조가 테스트 코드에 남지 않도록 정리
4. 정책 검증 추가
- `PendingSettlementQueryService`가 Query 전용 Repository(읽기 모델) 허용 정책과 일치하는지 검증
- `externalExecutionId` 중복 처리에 대해:
  - 애플리케이션 멱등성 검사 동작
  - DB unique constraint 전제 동작
  를 테스트/검증 시나리오에 명시

## Verification Commands

```bash
cd .worktrees/trading-api-ddd-aggregate-fix/backend/trading-api
./gradlew compileTestKotlin
./gradlew test --tests "*ExecutionProcessorTest"
./gradlew test --tests "*SettlementCommandServiceTest"
./gradlew test --tests "*SettlementProcessorTest"
./gradlew test --tests "*RiskPolicyServiceTest"
./gradlew test --tests "*PendingSettlementQueryServiceTest"
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
