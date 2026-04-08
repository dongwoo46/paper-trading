# .agents 운영 가이드 — trading-api

## 목적
이 폴더는 기능 보고서, 재발 방지 규칙을 관리한다.

## 구조
- `AGENTS.md`: 서비스별 규칙 (항상 먼저 읽기)
- `.agents/feature/`: 기능 정의/변경 보고서
- `.agents/feature/README.md`: API 인덱스 (API 개발 완료 시 갱신)
- `.agents/rule/`: 버그/장애 재발 방지 규칙

## 핵심 운영 원칙
- 기능 개발 후 반드시 `./gradlew compileJava`로 검증한다.
- API 추가/수정 시 `.agents/feature/README.md`를 즉시 갱신한다.

---

## 개발 이정표

### Phase 1: DB 설계 ✅ 완료

- [x] ERD 설계 (23개 테이블)
- [x] DB 스키마 확정 및 사용자 승인
- [x] 설계 문서: `.agents/feature/erd-design-v1.md`

### Phase 2: JPA Entity + Repository ✅ 완료

- [x] 설계 결정: Entity = 도메인 모델 통합 (`domain/model/`에 `@Entity` 직접 배치, 매퍼 없음)
- [x] ENUM 타입 14개 — `domain/enums/`
- [x] JPA Entity 23개 — `domain/model/` (도메인 메서드 포함)
- [x] Repository 인터페이스 23개 — `infrastructure/persistence/`
- [x] `./gradlew compileJava` 빌드 성공
- [x] `ddl-auto=create` 로 PostgreSQL 스키마 자동 생성 (`application.yaml`)

### Phase 3: API 개발 (다음 세션 즉시 시작)

- [ ] 계좌 관리 API (계좌 생성, 예수금 입출금, 잔고 조회)
- [ ] 주문 API (시장가/지정가 매수·매도, 주문 취소/정정)
- [ ] 체결 처리 엔진 (로컬 체결 시뮬레이터)
- [ ] 포트폴리오 조회 API (보유 종목, 평가손익)
- [ ] 정산/손익 조회 API (실현손익, 기간별 수익률)
- [ ] 전략 등록/실행 API

### Phase 4: 외부 연동

- [ ] Redis 시세 조회 연동 (collector-api → Redis → trading-api)
- [ ] KIS 모의투자 어댑터
- [ ] KIS 실거래 어댑터
- [ ] Upbit 실거래 어댑터

### Phase 5: 테스트

- [ ] 도메인 단위 테스트
- [ ] 주문·체결 통합 테스트
- [ ] 동시성 테스트 (낙관적 락 검증)
