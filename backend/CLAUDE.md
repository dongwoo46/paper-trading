@../CLAUDE.md

## Backend 공통 규칙
> trading-api, collector-api, collector-worker 공통

## 아키텍처
interfaces → application → domain ← infrastructure

## 코드 규칙
- 생성자 주입만 사용 (@Autowired 필드 주입 금지)
- val 우선, !! 금지
- 도메인 예외는 도메인에서 정의, @ControllerAdvice로 일괄 처리
- 입력 불변식 조기 검증 (require*)
- JPA @Entity와 DDD Entity 분리 (ORM 관심사가 도메인 오염 금지)
