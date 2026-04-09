계좌 API (Phase 3)

아키텍처
presentation → application → domain ← infrastructure
AccountCommandService: 계좌 생성/수정/비활성화, 입출금(멱등)
AccountQueryService: 계좌 조회, 원장 페이징(QueryDSL Projection)
RiskPolicyService: 리스크 정책 upsert/조회 (Account Aggregate Root 통해 생성)
PendingSettlementQueryService: 정산 예정 조회(QueryDSL Projection)

핵심 설계 결정
- 입출금 멱등: findByIdWithLock → 락 안에서 idempotencyKey 체크 (동시성 안전)
- 비관적 락: findByIdWithLock + jakarta.persistence.lock.timeout=3000ms
- dirty checking 활용: 조회 후 필드 변경만으로 UPDATE (save 불필요)
- RiskPolicy: Account Aggregate 내부, Account.createRiskPolicy()로만 생성. isActive 이력 보존
- 원장/정산예정 조회: QueryDSL + Projections.constructor() (presentation DTO 의존 없음)
- 데드락 방지: 항상 Account 락 먼저, Position 락 나중 순서 준수

테스트
AccountCommandServiceTest: 단위 8개 (MockK, Spring 컨텍스트 없음)
AccountQueryServiceTest: 단위 5개
RiskPolicyServiceTest: 단위 5개
PendingSettlementQueryServiceTest: 단위 2개
AccountControllerIntegrationTest: 통합 6개 (@SpringBootTest + Testcontainers PostgreSQL)