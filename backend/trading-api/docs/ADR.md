# ADR — trading-api

## ADR-001: JPA + QueryDSL 선택
**결정**: Spring Data JPA + QueryDSL Projections으로 조회 구현
**이유**: 도메인 Entity와 DB 스키마를 객체로 관리. 복잡한 조회(원장 페이징, 포지션 집계)는 QueryDSL Projection으로 DTO 직접 매핑.
**트레이드오프**: Hibernate 프록시 이슈로 data class 사용 불가. N+1 주의 필요. JOOQ 대비 타입 안전 쿼리 작성이 번거로움.

## ADR-002: 비관적 락 vs 낙관적 락 혼용
**결정**: 입출금 → 비관적 락(SELECT FOR UPDATE), 주문 → 낙관적 락(@Version)
**이유**: 입출금은 충돌 시 재시도가 불가능한 금융 연산이므로 비관적 락 필수. 주문은 동시 수정 빈도가 낮아 낙관적 락으로 충분.
**트레이드오프**: 비관적 락은 데드락 위험. Account 락 먼저 → Position 락 나중 순서를 코드 레벨에서 강제해야 함.

## ADR-003: Redis를 시세 조회 전용으로만 사용
**결정**: trading-api는 Redis에서 시세를 읽기만 함. 쓰기는 collector-api 전담.
**이유**: 책임 분리. collector-api가 Redis 시세 포맷의 단일 소유자.
**트레이드오프**: Redis 키 구조(quote:{ticker} Hash) 변경 시 두 서비스 동시 수정 필요.

## ADR-004: 체결 처리 단일 트랜잭션
**결정**: Order + Execution + Position + AccountLedger 를 하나의 트랜잭션으로 처리
**이유**: 부분 성공 상태(주문은 체결됐는데 포지션 미반영 등)가 발생하면 복구 불가능한 데이터 불일치.
**트레이드오프**: 트랜잭션 범위가 커서 DB 락 경합 가능성 증가. 체결 처리 중 외부 API 호출 금지.

## ADR-005: KIS 토큰 Redis 공유
**결정**: KIS access token을 Redis kis:token:{mode} 에 저장, collector-api와 공유
**이유**: KIS는 하루 발급 한도가 있어 서비스별로 각자 발급하면 한도 초과.
**트레이드오프**: Redis 장애 시 KIS 연동 전체 중단. 토큰 TTL 관리 로직이 한 서비스(collector-api)에 집중.
