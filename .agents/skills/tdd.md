Skill: TDD + 코리코프 원칙 (Vladimir Khorikov - Unit Testing)

Red-Green-Refactor 사이클 — 각 작업 단위마다 반드시 이 순서를 지킨다

1. Red: 실패하는 테스트 먼저 작성 → 테스트 실패 확인 필수 (실패 확인 없이 구현 진행 금지)
2. Green: 최소 구현으로 테스트 통과
3. Refactor: 중복 제거, 가독성 개선

코리코프 4대 원칙 — 좋은 테스트의 기준

1. 회귀 방지: 코드 변경 시 버그를 잡아낸다 (많은 코드·복잡한 로직 커버)
2. 리팩토링 내성: 구현 세부가 바뀌어도 테스트가 깨지지 않는다 (거짓 양성 방지)
3. 빠른 피드백: 빠르게 실행되어야 한다
4. 유지보수성: 읽기 쉽고, 실행 비용이 낮아야 한다

단위(Unit)의 정의

- 코드 단위(클래스/메서드)가 아닌 동작 단위(Behavior)
- 하나의 테스트는 하나의 비즈니스 동작만 검증한다

테스트 스타일 3종 — 코리코프 분류, 우선순위 순서

1. Output-based (출력 기반): 입력 → 함수 → 출력만 검증. 부수효과 없음. 리팩토링 내성 최고. 함수형 코어에 적용.
2. State-based (상태 기반): 동작 후 객체/DB 상태 검증. 도메인 객체·통합 테스트에 적용.
3. Communication-based (커뮤니케이션 기반): 외부 시스템과의 호출 검증 (Mock 사용). 리팩토링 내성 최저. 외부 out-of-process 의존성에만 제한 사용.

Mock vs Stub 구분 — 코리코프 핵심

- Stub: 테스트에 필요한 데이터를 제공. 검증 대상이 아님. Stub 검증 = 구현 세부 결합 = 리팩토링 내성 파괴.
- Mock: 외부로 나가는 커뮤니케이션(side effect) 검증. 이메일 발송, 외부 API 호출 등.
- Stub에 대해 verify() 호출 금지

테스트 작성 원칙

- given / when / then 구조 (AAA: Arrange / Act / Assert)
- 테스트명은 한글 동작 중심: 주문_수량이_보유잔고를_초과하면_예외를_던진다
- Observable Behavior만 검증 (구현 세부 검증 = 리팩토링 내성 파괴)
- Humble Object로 외부 의존성 격리
- 고전파(Classical School) 우선: 실제 객체 사용, 외부 의존성만 Mock

Mock 사용 기준

- Mock 대상: 외부 시스템 (DB, Redis, 외부 API, 이메일 등 out-of-process)
- Mock 금지: 도메인 객체, Value Object, 내부 협력 객체
- Mock 남용 = 리팩토링 내성 파괴 → 구현 세부에 결합됨

테스트 계층

- Unit: 도메인 로직. MockK / JUnit5. Spring 컨텍스트 없음. 빠름.
- Integration+E2E: @SpringBootTest + Testcontainers (DB·Redis 실제 연결)

우선순위: Happy Path → 핵심 비즈니스 로직 → 경계값/예외

금지

- 구현 코드 먼저 작성 후 테스트 작성 (TDD 역순 금지)
- 실패 확인 없이 Green 진행
- private 메서드 직접 테스트 (Observable Behavior가 아님)
- 구현 세부 검증 (리팩토링 내성 파괴)
- 도메인 객체 Mock
- Stub에 대한 verify() 호출
- 한 테스트에서 다중 동작 검증
- 테스트 편의를 위한 프로덕션 코드 변경
