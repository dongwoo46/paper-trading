.agents trading-api

구조
feature/README.md: 현재 API 인덱스
feature/{기능명}.md: 기능 보고서
rule/{이슈}.md: 재발 방지 기록

개발 이정표

완료
ERD 설계 완료(23개 테이블)
DB 스키마 확정 및 사용자 승인 완료
ENUM 타입 14개 완료
JPA Entity 23개 완료(도메인 메서드 포함)
Repository 인터페이스 23개 완료
계좌 API (Phase 3) 완료
주문/포지션 API (Phase 4) 완료
  - LOCAL / KIS_PAPER / KIS_LIVE 3모드
  - LocalMatchingEngine, ExecutionProcessor
  - KisTokenManager (Redis 공유), KisOrderRestClient
  - RedisMarketQuoteAdapter, QuoteEventListener
  - 통합/단위 테스트 완료

다음 작업
정산 손익 조회 API (Phase 4 잔여)
포트폴리오 조회 API
전략 등록/실행 API
Upbit 실거래 어댑터
동시성 테스트 (낙관적 락 검증)