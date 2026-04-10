Skill: DDD

핵심 개념

- Bounded Context: 모델이 유효한 명확한 경계. 각 BC는 자체 Ubiquitous Language를 가짐.
- Context Map: Bounded Context 간 관계 패턴.
  - Anti-Corruption Layer(ACL): 외부 BC 모델이 내부 도메인 오염 방지. 번역 레이어.
  - Shared Kernel: 두 BC가 공유하는 작은 모델. 변경 시 양측 합의 필수.
  - Customer-Supplier: 상류(Supplier)가 하류(Customer) 요구를 반영.
  - Conformist: 하류가 상류 모델을 그대로 수용 (협상력 없을 때).
- Entity: ID로 동일성 판단.
- Value Object: 값으로 동일성 판단. 불변. (금액, 수량, 날짜 범위 등)
- Aggregate: 일관성 경계. Root를 통해서만 접근. 불변식은 Root가 보장. 타 Aggregate 참조는 ID로만.
- Factory: 복잡한 Aggregate/Entity 생성 캡슐화. 생성 로직이 복잡하거나 외부에 노출되면 안 될 때 사용.
- Domain Service: 어떤 Entity/VO에도 자연스럽게 속하지 않는 도메인 연산.
- Domain Event: 도메인에서 발생한 사실. 과거형 명사로 명명 (OrderPlaced, TradeExecuted). Aggregate 간 결합도 낮추는 핵심 수단.
- Repository: Aggregate Root 단위로만 정의.
- Ubiquitous Language: 코드·문서·대화에서 동일한 도메인 용어 사용.

전략적 설계 — Core/Supporting/Generic Subdomain

- Core Domain: 경쟁 우위의 핵심. 가장 많은 투자와 집중. (이 프로젝트: 주문 체결·포지션 관리)
- Supporting Subdomain: Core를 지원하지만 차별화 요소 아님. 직접 구현하되 단순하게.
- Generic Subdomain: 범용 솔루션 존재. 외부 라이브러리·서비스로 대체 가능. (인증, 알림 등)

원칙

- 도메인 레이어는 프레임워크 의존 없음 (순수 비즈니스 로직만)
- 상태 변경은 반드시 도메인 메서드로: order.cancel(), account.lockDeposit(amount)
- 비즈니스 규칙 위반은 도메인에서 즉시 예외: require(amount > 0) { "..." }
- Aggregate 경계 바깥에서 내부 객체 직접 수정 금지
- JPA @Entity와 DDD Entity는 분리 원칙. @Entity가 붙은 클래스는 ORM 제약(기본 생성자, 프록시 등)을 따르므로 순수 도메인 모델과 혼용 시 인프라 관심사가 도메인을 오염시킴.

TDD와 조합

- /design: DDD로 도메인 모델 설계 (Bounded Context 경계 확정 → Entity, VO, Aggregate 확정)
- /build: TDD로 도메인 모델 구현. 도메인 테스트는 Mock 없이 순수 단위 테스트.

이 프로젝트 적용 현황

- trading-api ↔ collector-api: 별도 Bounded Context. 시세 데이터 수신 시 ACL 적용 권장.
- trading-api: domain 패키지 = 순수 도메인 모델 (JPA @Entity와 분리 권장)
- collector-api: domain 패키지 = 순수 도메인 모델
- collector-worker: dataclass = Value Object 역할
