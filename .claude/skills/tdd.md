Skill: TDD (코리코프 원칙 — Vladimir Khorikov, Unit Testing)

## Red-Green-Refactor 사이클 (순서 이탈 금지)

1. Red: 실패하는 테스트 작성 → 실패 확인 필수 (실패 미확인 시 Green 진행 금지)
2. Green: 최소 구현으로 테스트 통과
3. Refactor: 중복 제거, 가독성 개선 후 재실행 확인

## 테스트 작성 원칙

- given / when / then 구조 (AAA: Arrange / Act / Assert)
- 테스트명 한글 동작 중심: 주문_수량이_보유잔고를_초과하면_예외를_던진다
- Observable Behavior만 검증 (구현 세부 검증 = 리팩토링 내성 파괴)
- 하나의 테스트는 하나의 비즈니스 동작만 검증

## Mock vs Stub

- Stub: 테스트에 필요한 데이터 제공. 검증 대상 아님. Stub에 verify() 금지.
- Mock: 외부로 나가는 side effect 검증 (이메일 발송, 외부 API 호출 등)
- Mock 대상: 외부 시스템만 (DB, Redis, 외부 API — out-of-process)
- Mock 금지: 도메인 객체, Value Object, 내부 협력 객체

## 테스트 계층

- Unit: 도메인 로직. JUnit5/MockK. Spring 컨텍스트 없음. 빠름.
- Integration+E2E: @SpringBootTest + Testcontainers (DB·Redis 실제 연결)

## 우선순위

Happy Path → 핵심 비즈니스 로직 → 경계값/예외

## 금지

- 구현 코드 먼저 작성 후 테스트 (TDD 역순)
- 실패 확인 없이 Green 진행
- private 메서드 직접 테스트
- 구현 세부 검증 (리팩토링 내성 파괴)
- 도메인 객체 Mock
- Stub에 verify() 호출
- 한 테스트에서 다중 동작 검증
- 테스트 편의를 위한 프로덕션 코드 변경
