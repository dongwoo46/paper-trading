Skill: Code Review

## 검토 순서

1. git diff로 변경 범위 파악
2. spec.md로 설계 의도 파악
3. 체크리스트 순서대로 검토
4. 결과 출력: 🔴 필수 수정 / 🟡 권장 개선 / 🟢 확인 완료

## 아키텍처

- 레이어 의존 방향 준수 (presentation → application → domain ← infrastructure)
- 도메인에 프레임워크 의존 없음
- DTO ↔ Entity 혼용 없음
- 트랜잭션 경계 Application에서만

## 코드 품질

- SRP/OCP/DIP 준수
- 중복 로직 없음
- 매직 넘버 없음 (상수화)
- 도메인 용어 기반 네이밍
- 불필요한 주석 없음 (코드가 자기 설명적이어야 함)
- 주석 처리된 코드 없음
- 트랜잭션 경계 확인

## 안정성

- 금액·수량 BigDecimal 사용
- N+1 쿼리 없음
- 외부 API 타임아웃 명시
- 예외 처리 누락 없음
- 널 처리 일관성

## 보안

- 시크릿/자격증명 원문 로그 없음
- 토큰 평문 저장 없음
- SQL Injection, XSS 방어
- 입력 유효성 검증 누락 없음

## 테스트 품질

- Observable Behavior만 검증 (구현 세부 검증 없음)
- 도메인 객체 Mock 없음
- AAA 패턴 적용
- Stub에 verify() 없음
- 경계값/예외 케이스 커버
- 테스트명이 동작을 명확히 설명

## 퀀트 로직 (해당 시)

- 수식과 코드 일치 여부
- look-ahead bias 없음
- 거래비용 반영 여부
- 결측값/이상값 처리
- 결과 재현성 (random seed)
