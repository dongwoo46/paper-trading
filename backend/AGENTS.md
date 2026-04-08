# Backend 공통 규칙
> trading-api, collector-api, collector-worker 공통

루트 규칙: @../AGENTS.md

---

## 아키텍처

의존 방향: interfaces → application → domain ← infrastructure

- domain: 순수 비즈니스 규칙만, 프레임워크 의존 금지
- application: UseCase 오케스트레이션, 트랜잭션 경계 관리
- infrastructure: DB/Redis/외부 API 어댑터
- interfaces: HTTP 변환/검증/응답 조립

---

## 코드 퀄리티

- SRP/OCP/DIP 준수
- 중복 로직 금지, 도메인 용어 기반 네이밍
- 입력 불변식은 조기 검증 (require*)

---

## 안정성 규칙

- 금액·수량은 BigDecimal만 사용 (double/float 금지)
- DTO ↔ Entity 혼용 금지, 레이어 경계에서 변환
- 트랜잭션 경계는 Application 레이어에서만 관리
- N+1 금지 (fetch join / @EntityGraph)
- 외부 API는 타임아웃 필수, 필요 시 Circuit Breaker 적용
- 시크릿/자격증명 원문 로그 금지

---

## 구현 규칙

- 생성자 주입만 사용 (@Autowired 필드 주입 금지)
- val 우선, !! 금지
- 도메인 예외는 도메인에서 정의, @ControllerAdvice로 일괄 처리
- 매직 넘버 금지 (const/상수화)

---

## 검증 명령

- trading-api: `./gradlew compileJava`
- collector-api: `./gradlew compileKotlin`
- collector-worker: `python -m py_compile {파일}`
