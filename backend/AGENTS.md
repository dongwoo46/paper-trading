# AGENTS.md — Backend 공통 규칙

> trading-api, collector-api, collector-worker 에서 공통 적용되는 백엔드 규칙.
> 루트 규칙: `@../AGENTS.md`
> 서비스별 세부 규칙은 각 서비스 내부 `AGENTS.md` 참고.

---

## 아키텍처

- 레이어 의존 방향: `interfaces → application → domain ← infrastructure`
- `domain`: 순수 비즈니스 규칙. Spring/JPA 등 외부 프레임워크 의존 금지.
- `application`: UseCase 오케스트레이션. 트랜잭션 경계 소유.
- `infrastructure`: Port 인터페이스 구현체. DB, Redis, 외부 API 어댑터.
- `interfaces`: HTTP 변환, 입력 정규화, 응답 조립만.

레이어 역방향 의존 발견 시 즉시 지적하고 수정한다.

---

## Kotlin / Java 공통 규칙

- 생성자 주입만 사용. `@Autowired` 필드 주입 금지.
- `BigDecimal` 전용 금액·수량 계산. `Double`/`Float` 금지. 나눗셈 시 `RoundingMode` 명시.
- `val` 우선. 변경이 반드시 필요한 경우만 `var`.
- `null` 안전: `!!` 사용 금지. `?: throw`, `?.let`, `requireNotNull()` 활용.
- DTO ↔ Entity 혼용 금지. 레이어 경계에서 반드시 변환. Entity를 Controller까지 노출 금지.
- 외부 설정은 `@ConfigurationProperties`로 관리.

---

## 트랜잭션 / 동시성

- 트랜잭션 경계: Application(UseCase) 레이어에서 관리.
- 주문 상태 변경: 낙관적 락 (`@Version`).
- 예수금 차감/반환: 비관적 락.
- N+1 금지: fetch join 또는 `@EntityGraph`.

---

## 외부 API 연동

- 타임아웃 필수. 필요 시 Resilience4j Circuit Breaker 도입.
- WebSocket/스트림: 재연결 + 지수 백오프 정책 반드시 포함.
- 리액티브 파이프라인 내부에서 블로킹 호출 금지.
- 시크릿/자격증명 원문 로그 금지. 민감 정보는 마스킹 후 기록.

---

## 예외 처리

- 도메인 예외는 도메인 레이어에서 던진다.
- Controller에서 직접 잡지 않고 `@ControllerAdvice`로 일괄 처리.
- 매직 넘버 금지. `companion object` 또는 `const val`로 상수화.

---

## 검증 명령

| 서비스 | 명령 |
|--------|------|
| trading-api | `./gradlew compileJava` |
| collector-api | `./gradlew compileKotlin` |
| collector-worker | `python -m py_compile {파일}` |
