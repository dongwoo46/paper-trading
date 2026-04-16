Skill: DDD (Domain-Driven Design)

## 핵심 개념

- Bounded Context: 모델이 유효한 명확한 경계. 각 BC는 자체 Ubiquitous Language.
- Entity: ID로 동일성 판단.
- Value Object: 값으로 동일성 판단. 불변. (금액, 수량, 날짜 범위 등)
- Aggregate: 일관성 경계. Root를 통해서만 접근. 타 Aggregate 참조는 ID로만.
- Domain Service: 어떤 Entity/VO에도 자연스럽게 속하지 않는 도메인 연산.
- Domain Event: 도메인에서 발생한 사실. 과거형 명사 (OrderPlaced, TradeExecuted).
- Repository: Aggregate Root 단위로만 정의.
- Factory: 복잡한 Aggregate/Entity 생성 캡슐화.
- Ubiquitous Language: 코드·문서·대화에서 동일한 도메인 용어 사용.

## Context Map 패턴

- Anti-Corruption Layer(ACL): 외부 BC 모델이 내부 도메인 오염 방지. 번역 레이어.
- Shared Kernel: 두 BC가 공유하는 작은 모델. 변경 시 양측 합의 필수.
- Customer-Supplier: 상류(Supplier)가 하류(Customer) 요구를 반영.

## 전략적 설계

- Core Domain: 경쟁 우위의 핵심. 최대 투자. (이 프로젝트: 주문 체결·포지션 관리)
- Supporting Subdomain: Core 지원. 직접 구현하되 단순하게.
- Generic Subdomain: 범용 솔루션 존재. 외부 라이브러리로 대체.

## 구현 원칙

- 도메인 레이어: 프레임워크 의존 금지, 순수 비즈니스 로직만
- 상태 변경은 도메인 메서드로: order.cancel(), account.lockDeposit(amount)
- 비즈니스 규칙 위반은 도메인에서 즉시 예외: require(amount > 0) { "..." }
- Aggregate 경계 밖에서 내부 객체 직접 수정 금지
- JPA @Entity와 DDD Entity 분리 (ORM 관심사가 도메인 오염 금지)

## 이 프로젝트 적용

- trading-api ↔ collector-api: 별도 Bounded Context. 시세 수신 시 ACL 적용.
- trading-api: domain 패키지 = 순수 도메인 모델
- collector-api: domain 패키지 = 순수 도메인 모델
- collector-worker: dataclass = Value Object 역할
