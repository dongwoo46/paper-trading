# Step 4: 내부 Entity Repository 제거 및 의존성 정리
Assigned agent: fullstack-dev

## Working Directory
`.worktrees/trading-api-ddd-aggregate-fix`

## Files to Read

- `docs/phase/trading-api/ddd-aggregate-fix/spec.md`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/ExecutionRepository.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/SettlementRepository.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/RiskPolicyRepository.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PendingSettlementRepository.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderAmendmentRepository.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/SettlementExecutionRepository.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepository.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/AccountRepository.kt`

## Tasks

1. 내부 Repository 제거
- 아래 파일 삭제 또는 Root 내부 구현으로 완전 비노출화:
  - `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/ExecutionRepository.kt`
  - `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/SettlementRepository.kt`
  - `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/RiskPolicyRepository.kt`
  - `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PendingSettlementRepository.kt`
  - `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderAmendmentRepository.kt`
  - `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/SettlementExecutionRepository.kt`

2. Root Repository 메서드 보강
- 파일: `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepository.kt`
  - 체결 중복 방지/정산 연결에 필요한 조회 메서드 추가
- 파일: `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/AccountRepository.kt`
  - 활성 리스크 정책/미정산 내역 조회에 필요한 메서드 추가

3. DI 그래프 정리
- 서비스 생성자에서 제거된 Repository 주입이 남지 않도록 전수 정리

## Verification Commands

```bash
cd .worktrees/trading-api-ddd-aggregate-fix/backend/trading-api
./gradlew compileKotlin
./gradlew test --tests "*ExecutionProcessorTest"
./gradlew test --tests "*RiskPolicyServiceTest"
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
