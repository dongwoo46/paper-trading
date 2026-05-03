# DDD Aggregate 경계 수정 — 기능 명세

## 개요

`trading-api`에서 Aggregate Root 경계가 약해져 애플리케이션 서비스가 내부 Entity Repository를 직접 조작하고, Aggregate 간 직접 객체 참조를 유지하고 있다.  
본 phase는 `Order`, `Account`를 Root로 고정하고, 내부 엔티티(`Execution`, `Settlement`, `RiskPolicy`, `PendingSettlement`, `OrderAmendment`, `SettlementExecution`) 접근을 Root 경유로 재구성한다.

## 범위 (In Scope)

1. 내부 Entity Repository 제거
- 대상: `ExecutionRepository`, `SettlementRepository`, `RiskPolicyRepository`, `PendingSettlementRepository`, `OrderAmendmentRepository`, `SettlementExecutionRepository`
- 의미: 외부 서비스/유스케이스에서 위 Repository를 직접 의존하지 않도록 구조 변경

2. Aggregate Root Repository 경유 접근 강제
- `OrderRepository`, `AccountRepository` 중심으로 조회/수정 경로 재정의
- 필요 시 Root Repository에 use-case 지향 메서드 추가 (예: `findByExternalExecutionId`, `findPendingSettlements` 등)

3. Aggregate 간 직접 객체 참조 제거
- `Execution.order: Order` -> `Execution.orderId: Long`
- `Settlement.order: Order` -> `Settlement.orderId: Long`
- `SettlementExecution.execution: Execution` -> `SettlementExecution.executionId: Long`
- `SettlementExecution.settlement: Settlement` -> `SettlementExecution.settlementId: Long`
- `PendingSettlement.account: Account` -> `PendingSettlement.accountId: Long` (Root 외부 엔티티 간 직접 참조 제거 일관성 확보)

4. 애플리케이션 서비스 재구성
- `ExecutionProcessor`, `SettlementCommandService`, `SettlementProcessor`, `RiskPolicyService`, `PendingSettlementQueryService`의 Repository 주입/호출 구조 변경

5. 테스트 갱신
- 컴파일/단위테스트 기준으로 경계 변경 회귀 검증

## 비범위 (Out of Scope)

1. `strategy-execution` 및 전략 도메인 리빌딩
2. `collector-api`, `quant-worker` 코드 수정
3. 세금 정산(`tax-summary`) 기능 구현
4. 거래 일지(`trading-journal`) 기능 구현
5. 새로운 도메인 규칙 추가(현재 동작 보존 우선)

## 현재 경계 위반 지점

다음 파일에서 내부 Entity Repository 직접 의존이 확인됨:

1. `backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt`
- `ExecutionRepository`, `PendingSettlementRepository`, `SettlementRepository`, `SettlementExecutionRepository` 직접 주입/호출

2. `backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/RiskPolicyService.kt`
- `RiskPolicyRepository` 직접 주입/호출

3. `backend/trading-api/src/main/kotlin/com/papertrading/api/application/settlement/SettlementCommandService.kt`
- `PendingSettlementRepository` 직접 주입/호출

4. `backend/trading-api/src/main/kotlin/com/papertrading/api/application/settlement/SettlementProcessor.kt`
- `PendingSettlementRepository` 직접 주입/호출

5. `backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/PendingSettlementQueryService.kt`
- `PendingSettlementRepository` 직접 주입/호출

다음 엔티티에서 Aggregate 간 직접 객체 참조가 확인됨:

1. `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/Execution.kt`
- `order: Order`, `account: Account`

2. `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/Settlement.kt`
- `order: Order`, `account: Account`

3. `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/SettlementExecution.kt`
- `settlement: Settlement`, `execution: Execution`

## 설계 원칙

1. 내부 Entity Repository 제거 원칙 (표현 정정)
- "물리적 완전 삭제"가 목표가 아니라, 내부 Entity Repository를 `Application` 계층에 노출하지 않는 것을 목표로 한다.
- 내부 구현 상세로 남을 수 있으나, 유스케이스 서비스에서 직접 의존/호출하지 않는다.

2. Root Repository 강제 범위
- Root Repository 강제는 `상태 변경(Command)` 경로에 한정한다.
- 조회(Query) 경로까지 무리하게 Root 경유를 강제하지 않는다.

3. 참조는 ID 우선
- Root 경계를 넘는 객체 포인터를 제거하고 FK ID만 보유한다.
- 조회는 Repository에서 명시적으로 수행한다.

4. Query 전용 Repository 허용
- `PendingSettlementQueryService`는 `ReadRepository/ProjectionRepository` 사용을 허용한다.
- CQRS 관점에서 Query 최적화 모델을 허용하되, Command 모델과 분리한다.

5. Execution/Settlement 멱등성
- `externalExecutionId`는 DB unique constraint로 중복 저장을 차단한다.
- 애플리케이션 계층에서 멱등성 검사(선조회/중복 처리)를 명시적으로 유지한다.

6. 동작 보존
- 주문 체결, 정산, 리스크 정책의 비즈니스 결과는 동일하게 유지한다.

## 목표 아키텍처

1. Application
- `ExecutionProcessor` -> `OrderRepository`, `AccountRepository`, `PositionRepository`, `AccountLedgerRepository`, `FeePolicyRepository` (+ 필요한 Root 기반 조회 메서드)
- `RiskPolicyService` -> `AccountRepository` only
- `SettlementCommandService/Processor` -> `AccountRepository` 중심 처리

2. Domain Model
- `Execution`, `Settlement`, `SettlementExecution`, `PendingSettlement`는 외부 Aggregate 객체 대신 ID 필드 보유

3. Infrastructure
- 내부 Entity Repository 삭제 또는 Root 내부 구현 상세로 캡슐화

## 검증 기준

1. 컴파일
```bash
cd .worktrees/trading-api-ddd-aggregate-fix/backend/trading-api
./gradlew compileKotlin
./gradlew compileTestKotlin
```

2. 회귀 테스트
```bash
cd .worktrees/trading-api-ddd-aggregate-fix/backend/trading-api
./gradlew test --tests "*ExecutionProcessorTest"
./gradlew test --tests "*SettlementCommandServiceTest"
./gradlew test --tests "*RiskPolicyServiceTest"
./gradlew test --tests "*PendingSettlementQueryServiceTest"
```

## 참고/가정

1. 요청된 `docs/phase/trading-api/ddd-aggregate-fix/index.json` 및 `backend/trading-api/graphify-out/graph.json`은 현재 워크트리에 부재하여, 본 명세는 실제 코드 스캔 결과를 기준으로 작성했다.
2. step 파일의 문서 경로 표기는 모두 메인 리포 루트 기준으로 고정한다.
