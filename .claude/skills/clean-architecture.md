Skill: Clean Architecture

## 레이어 구조

```
presentation → application → domain ← infrastructure
```

- domain: 순수 비즈니스 규칙. 프레임워크·DB·외부 의존 금지. 가장 안정적인 레이어.
- application: UseCase 오케스트레이션. 트랜잭션 경계 관리. 도메인 객체 조율.
- infrastructure: DB/Redis/외부 API 어댑터. 도메인 인터페이스(Port) 구현.
- presentation: HTTP 변환, 입력 검증, 응답 조립. 도메인 로직 포함 금지.

## 의존성 규칙

- 의존 방향은 항상 바깥 → 안쪽 (presentation → domain)
- domain은 어떤 레이어도 의존하지 않음
- infrastructure는 domain 인터페이스를 구현 (의존 역전)
- presentation은 application UseCase만 호출

## 레이어별 책임

**Domain**
- Entity, Value Object, Aggregate, Domain Service, Domain Event
- 비즈니스 불변식 보호 (상태 변경은 도메인 메서드로)
- 외부 의존 없이 단위 테스트 가능

**Application**
- UseCase 클래스 단위로 오케스트레이션
- 트랜잭션 경계는 Application에서만 (@Transactional)
- 도메인 객체 → DTO 변환 후 presentation으로 반환

**Infrastructure**
- Repository 구현체 (JPA, Redis 등)
- 외부 API 클라이언트 구현
- 타임아웃, Circuit Breaker 설정

**Presentation**
- Controller: HTTP 요청 수신 → UseCase 호출 → 응답 반환
- 입력 검증 (@Valid, BindingResult)
- 예외 처리: @ControllerAdvice로 일괄 처리

## 핵심 규칙

- DTO ↔ Entity 혼용 금지. 레이어 경계에서 변환.
- 생성자 주입만 사용 (@Autowired 필드 주입 금지)
- 도메인 예외는 도메인에서 정의
- 매직 넘버 금지 (상수화)
- N+1 금지 (fetch join / @EntityGraph)
- 금액·수량은 BigDecimal만 (double/float 금지)
