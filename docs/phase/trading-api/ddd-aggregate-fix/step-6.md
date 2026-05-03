# Step 6: DI 충돌 수정 및 재검증
Assigned agent: fullstack-dev

## Working Directory
`.worktrees/trading-api-ddd-aggregate-fix`

## Files to Read

- `docs/phase/trading-api/ddd-aggregate-fix/spec.md`
- `docs/phase/trading-api/ddd-aggregate-fix/step-2.md`
- `docs/phase/trading-api/ddd-aggregate-fix/step-3.md`
- `docs/phase/trading-api/ddd-aggregate-fix/step-4.md`
- `docs/phase/trading-api/ddd-aggregate-fix/step-5.md`
- 구현/테스트/리뷰 단계에서 수정된 실제 코드 파일 일체

## Tasks
1. `PendingSettlementReadRepository` 주입 충돌 해결
- `PendingSettlementQueryService` 생성자 주입 대상이 단일 빈으로 결정되도록 수정
- 필요 시 `@Primary` 또는 `@Qualifier` 적용, 또는 중복 구현 제거

2. 회귀 검증
```bash
cd .worktrees/trading-api-ddd-aggregate-fix/backend/trading-api
./gradlew compileKotlin --no-daemon
./gradlew test --tests "*PendingSettlementQueryServiceTest" --no-daemon
./gradlew test --tests "*TradingApiApplicationTests" --no-daemon
```

## Acceptance Criteria
- DI 충돌(`NoUniqueBeanDefinitionException`) 해소
- 지정 테스트 통과

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
