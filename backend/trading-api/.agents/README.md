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
compileJava 빌드 성공

Phase 3 API 개발 다음 작업
계좌 관리 API
주문 API(시장가/지정가 매수 매도, 취소 정정)
체결 처리 엔진(로컬 체결 시뮬레이터)
포트폴리오 조회 API
정산 손익 조회 API
전략 등록 실행 API

Phase 4 외부 연동
Redis 시세 조회 연동
KIS 모의투자 어댑터
KIS 실거래 어댑터
Upbit 실거래 어댑터

Phase 5 테스트
도메인 단위 테스트
주문 체결 통합 테스트
동시성 테스트(낙관적 락 검증)
