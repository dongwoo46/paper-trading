# .agents 운영 가이드 — trading-api

## 목적
이 폴더는 기능 보고서, 재발 방지 규칙을 관리한다.

## 구조
- `AGENTS.md`: 서비스별 규칙 (항상 먼저 읽기)
- `.agents/feature/`: 기능 정의/변경 보고서
- `.agents/feature/api.md`: API 명세서 (API 개발 완료 시 생성)
- `.agents/rule/`: 버그/장애 재발 방지 규칙

## 핵심 운영 원칙
- 기능 개발 후 반드시 `./gradlew compileJava`로 검증한다.
- API 추가/수정 시 `.agents/feature/api.md`를 즉시 갱신한다.

---

## 개발 이정표

### Phase 1: DB 설계 (현재 단계)

- [ ] ERD 설계 (계좌, 주문, 체결, 포지션, 정산, 전략)
- [ ] DB 스키마 확정 및 사용자 승인
- [ ] Flyway 마이그레이션 스크립트 작성

### Phase 2: 도메인 모델

- [ ] Account 도메인 (계좌, 예수금, 거래 모드)
- [ ] Order 도메인 (주문, 상태 머신)
- [ ] Execution 도메인 (체결)
- [ ] Position 도메인 (보유 종목, 평균단가)
- [ ] Settlement 도메인 (정산, 실현손익)

### Phase 3: API 개발

- [ ] 계좌 관리 API
- [ ] 주문 API (시장가/지정가 매수·매도)
- [ ] 체결 처리 엔진
- [ ] 포트폴리오 조회 API
- [ ] 정산/손익 조회 API
- [ ] 전략 등록/실행 API

### Phase 4: 외부 연동

- [ ] Redis 시세 조회 연동 (collector-api → Redis → trading-api)
- [ ] KIS 모의투자 어댑터
- [ ] KIS 실거래 어댑터

### Phase 5: 테스트

- [ ] 도메인 단위 테스트
- [ ] 주문·체결 통합 테스트
- [ ] 동시성 테스트 (낙관적 락 검증)