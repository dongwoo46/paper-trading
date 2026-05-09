# Settlement Cleanup (ReceivableSettlement Removal + LOCAL Tax Fix)

## Core Feature
KIS 계좌의 중복 정산 경로를 제거하고 LOCAL 전용 정산/세금 계산으로 단순화해 정산 일관성과 세금 집계 정확도를 확보한다.

## Considerations
- TODO 범위를 그대로 반영한다: `ReceivableSettlement` 제거, KIS 분기 단순화, LOCAL 매도세금 0.2% 반영, TaxSummary LOCAL-only.
- ADR-005(거래 모드 Strategy 패턴)를 준수해 `TradingMode` 기반 분기를 명확히 유지한다.
- 기존 `Settlement` 엔티티는 삭제하지 않고 LOCAL 모드 실현손익/세금 기록 저장소로 유지한다.
- 삭제 범위가 넓으므로 API/예외/레포지토리/테스트를 함께 정리해야 컴파일/회귀 리스크를 줄일 수 있다.

## Trade-offs
- Option A: `ReceivableSettlement` 경로를 유지하고 KIS 분기만 완화
  - 장점: 변경량이 작음
  - 단점: KIS에서 불필요한 내부 정산 상태가 계속 남아 설계 복잡도 유지
- Option B (선택): `ReceivableSettlement + SettlementProcessor + SettlementCommandService + BusinessDayCalculator` 제거, KIS는 외부(KIS) 정산 신뢰
  - 장점: 도메인 경계 명확, 중복 정산 제거, 유지보수 비용 감소
  - 단점: 정산 관련 API/테스트의 일괄 개편 필요

## Implementation Approach
- Domain
  - `ReceivableSettlement` 엔티티 제거.
  - `Settlement`/`SettlementExecution`은 LOCAL 실현손익 기록 용도로 유지.
- Application
  - `ExecutionProcessor`에서 SELL 처리 시 `TradingMode` 분기를 단순화:
    - `LOCAL`: 기존 계좌 반영 + `Settlement` 기록 유지, 단 매도세금 0.2% 반영.
    - `KIS_PAPER`/`KIS_LIVE`: 계좌 예수금/정산 엔티티 생성 로직 제거.
  - TaxSummary 집계는 LOCAL 계좌만 대상으로 제한.
- Infrastructure
  - `ReceivableSettlementRepository` 계열 제거.
  - Flyway로 `receivable_settlements` 관련 스키마 드롭 및 참조 정리.
- Presentation
  - `ReceivableSettlementController` 및 DTO/Query API 제거.
  - TaxSummary API 계약은 유지하되 KIS 계좌 요청 처리 정책(빈 결과/오류)을 명확히 고정.

## Workflow
1. 주문 SELL 체결 발생.
2. `ExecutionProcessor`가 계좌 `TradingMode` 판별.
3. `LOCAL`이면 실현손익 계산 시 `tax = grossProceeds * 0.0020` 적용 후 `Settlement` 저장 및 계좌 반영.
4. `KIS_PAPER`/`KIS_LIVE`이면 내부 정산 생성/입금 처리 없이 체결 상태 동기화만 수행.
5. TaxSummary 재계산 시 LOCAL 계좌의 `Settlement`만 집계한다.

## DDD Model
- Bounded Context: `Trading` 내 `Order/Execution/Settlement/Tax`.
- Aggregate
  - Root: `Account`, `Order`.
  - Policy: LOCAL 모드에서만 내부 정산 원장(`Settlement`)을 생성한다.
- Entity
  - 유지: `Settlement`, `SettlementExecution`, `TaxSummary`, `TaxSummaryRun`.
  - 제거: `ReceivableSettlement`.
- Domain Event
  - 유지: 체결 이벤트 기반 후처리.
  - 제거 영향: receivable settlement 완료 이벤트 계열(있다면) 제거.

## API
`DELETE SCOPE (internal cleanup)`
- 제거 대상: Receivable settlement 조회 API
- Endpoints
  - `GET /api/accounts/{accountId}/receivable-settlements`

`GET /api/accounts/{accountId}/tax-summaries/{taxYear}`
- 설명: LOCAL 계좌의 연도별 세금 요약 조회.
- Request: `accountId: Long`, `taxYear: Int`
- Response: 기존 스키마 유지 (`accountId`, `taxYear`, `totalRealizedPnl`, `taxablePnl`, `estimatedTax`, `computedAt`, `status`)
- Errors
  - `400 INVALID_ACCOUNT_MODE_FOR_TAX_SUMMARY` (KIS 계좌 요청 시)
  - `404 TAX_SUMMARY_NOT_FOUND`

`POST /api/accounts/{accountId}/tax-summaries/{taxYear}/recalculate`
- 설명: LOCAL 계좌만 재계산 허용.
- Request: `accountId: Long`, `taxYear: Int`, optional body `force`
- Response: 기존 TaxSummary 응답
- Errors
  - `400 INVALID_ACCOUNT_MODE_FOR_TAX_SUMMARY`
  - `409 TAX_SUMMARY_ALREADY_RUNNING`

## DB
- Drop table
  - `receivable_settlements`
- Drop/cleanup related indexes and foreign keys tied to `receivable_settlements`
- Keep
  - `settlements` (LOCAL 기록)
  - `settlement_executions`
  - `tax_summaries`
  - `tax_summary_runs`
- Optional query optimization (LOCAL-only 집계)
  - `accounts(trading_mode)` 조건 활용
  - `settlements(account_id, settled_at)` 인덱스 점검

## Concrete File Scope
- 제거 후보
  - `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/ReceivableSettlement.kt`
  - `backend/trading-api/src/main/kotlin/com/papertrading/api/application/settlement/SettlementCommandService.kt`
  - `backend/trading-api/src/main/kotlin/com/papertrading/api/application/settlement/SettlementProcessor.kt`
  - `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/BusinessDayCalculator.kt`
  - `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/ReceivableSettlementRepository.kt`
  - `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/ReceivableSettlementRepositoryCustom.kt`
  - `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/ReceivableSettlementRepositoryImpl.kt`
  - `backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/ReceivableSettlementQueryService.kt`
  - `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/ReceivableSettlementController.kt`
  - `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/account/ReceivableSettlementDto.kt`
- 수정 핵심
  - `backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt`
  - `backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/tax/TaxSummaryCommandService.kt`
  - `backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/tax/SettlementTaxReadRepository.kt`
  - `backend/trading-api/src/main/kotlin/com/papertrading/api/common/exception/CommonApiExceptions.kt`
- 테스트/마이그레이션
  - `backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/ExecutionProcessorTest.kt`
  - `backend/trading-api/src/test/kotlin/com/papertrading/api/application/settlement/*` (삭제/대체)
  - `backend/trading-api/src/main/resources/db/migration/*` (receivable settlement 제거)

## Non-Functional Requirements
- 빌드/테스트 기준
  - `./gradlew compileKotlin`
  - `./gradlew compileTestKotlin`
  - `./gradlew test --tests "*ExecutionProcessorTest" --tests "*TaxSummary*"`
- 회귀 안전성
  - LOCAL/KIS SELL 체결 경로 분기 테스트를 분리해 반드시 유지.
  - 세금 계산값 scale/rounding 규칙을 테스트로 고정.