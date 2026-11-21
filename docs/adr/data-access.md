# 결정 이력 — 데이터 접근·트랜잭션 (paper-trading)
<!-- devkit:applies-to ^backend/(trading-api/src/main/.+\.kt|quant-[a-z]+/.+\.py)$ -->

> 옛 `docs/ADR.md`·`backend/*/docs/ADR.md` 의 결정을 관심사별로 모았다(2026-11-21 규칙 이사). 아래 글은 원문 그대로다.

아래는 옛 `backend/trading-api/docs/ADR.md` 에서 온 결정이다.

# ADR — trading-api

## ADR-001: JPA + QueryDSL 선택
**결정**: Spring Data JPA + QueryDSL Projections으로 조회 구현
**이유**: 도메인 Entity와 DB 스키마를 객체로 관리. 복잡한 조회(원장 페이징, 포지션 집계)는 QueryDSL Projection으로 DTO 직접 매핑.
**트레이드오프**: Hibernate 프록시 이슈로 data class 사용 불가. N+1 주의 필요. JOOQ 대비 타입 안전 쿼리 작성이 번거로움.

## ADR-002: 비관적 락 vs 낙관적 락 혼용
**결정**: 입출금 → 비관적 락(SELECT FOR UPDATE), 주문 → 낙관적 락(@Version)
**이유**: 입출금은 충돌 시 재시도가 불가능한 금융 연산이므로 비관적 락 필수. 주문은 동시 수정 빈도가 낮아 낙관적 락으로 충분.

## ADR-004: 체결 처리 단일 트랜잭션
**결정**: Order + Execution + Position + AccountLedger 를 하나의 트랜잭션으로 처리
**이유**: 부분 성공 상태(주문은 체결됐는데 포지션 미반영 등)가 발생하면 복구 불가능한 데이터 불일치.

아래는 옛 `backend/quant-worker/docs/ADR.md` 에서 온 결정이다.

## ADR-003: Redis 의존성 제거
**결정**: 초기 Redis 기반 메타데이터 관리를 제거하고 PostgreSQL catalog 단일 소스로 전환
**이유**: Redis와 DB 간 동기화 불일치 발생. catalog의 watermark(fetchedUntilDate)를 DB에서 직접 읽는 게 단순하고 신뢰성 높음.
**트레이드오프**: 수집 상태 조회 시 매번 DB 쿼리 필요. Redis 캐시 없어 반복 조회 시 부하.

## ADR-004: ON CONFLICT DO UPDATE (Upsert)
**결정**: 동일 (source, symbol, trade_date) 데이터 재수집 시 UPDATE로 덮어씀
**이유**: 수집 중단 후 재실행 시 중복 INSERT 오류 없이 안전하게 재적재 가능.
**트레이드오프**: 의도치 않은 데이터 덮어씌움 위험. 원본 데이터 보존이 필요하면 별도 이력 테이블 필요.
