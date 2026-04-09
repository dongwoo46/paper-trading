Skill: Design Approach

설계 순서

1. 요구사항 구조화: 기능/비기능 분리
2. 레이어별 변경 범위 확정
3. API 설계 (엔드포인트, Request/Response)
4. DB 스키마 설계 (ERD, 인덱스)
5. 외부 의존성 정리
6. 작업 단위 분해 (서브에이전트 기준)

아키텍처 원칙

- presentation → application → domain ← infrastructure
- domain: 순수 비즈니스 규칙, 프레임워크 의존 금지
- application: UseCase 오케스트레이션, 트랜잭션 경계
- infrastructure: DB/Redis/외부 API 어댑터
- presentation: HTTP 변환, 입력 검증, 응답 조립

설계안 출력 형식
변경 범위 / API 설계 / DB 스키마 / 작업 분해
마지막 줄: "승인하시면 구현 계획으로 넘어갑니다."
