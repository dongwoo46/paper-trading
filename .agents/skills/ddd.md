Skill: DDD

핵심 개념

- Entity: ID로 동일성 판단. 상태 변경은 도메인 메서드로만.
- Value Object: 값으로 동일성 판단. 불변. (금액, 수량, 날짜 범위 등)
- Aggregate: 일관성 경계. Root를 통해서만 내부 접근.
- Domain Service: 여러 Aggregate에 걸친 비즈니스 로직.
- Repository: Aggregate Root 단위로만 정의.
- Ubiquitous Language: 코드·문서·대화에서 동일한 도메인 용어 사용.

원칙

- 도메인 레이어는 프레임워크 의존 없음 (순수 비즈니스 로직만)
- 상태 변경은 반드시 도메인 메서드로: order.cancel(), account.lockDeposit(amount)
- 비즈니스 규칙 위반은 도메인에서 즉시 예외: require(amount > 0) { "..." }
- Aggregate 경계 바깥에서 내부 객체 직접 수정 금지

TDD와 조합

- /design: DDD로 도메인 모델 설계 (Entity, VO, Aggregate 확정)
- /build: TDD로 도메인 모델 구현. 도메인 테스트는 Mock 없이 순수 단위 테스트.

이 프로젝트 적용 현황

- trading-api: @Entity = 도메인 모델 (JPA + DDD 통합)
- collector-api: domain 패키지 = 순수 도메인 모델
- collector-worker: dataclass = Value Object 역할
