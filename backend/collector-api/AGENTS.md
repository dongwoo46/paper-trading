@../../AGENTS.md
@../AGENTS.md

---

# collector-api 서비스 규칙

외부 시장 데이터 수집 + Redis 반영 + PostgreSQL 히스토리 적재.
**Kotlin 1.9 / Spring Boot 3.x (MVC + WebFlux 혼용) / JPA / Redis**
검증: `./gradlew compileKotlin`

---

## 패키지 구조

```
com.papertrading.collector
├── domain/           # 순수 도메인 모델 (외부 의존 없음)
├── application/      # UseCase, 스케줄러, 파이프라인 오케스트레이션
│   ├── fred/
│   ├── kis/
│   ├── market/
│   └── upbit/
├── infrastructure/   # JPA 구현체, Redis 클라이언트, 외부 API 클라이언트
└── interfaces/       # REST Controller, DTO
```

---

## 데이터 흐름 원칙

- **Redis**: 최신 시세 상태만 저장 (캐시, 단기).
- **PostgreSQL**: 히스토리 데이터 (장기 이력, 분석용).
- 외부 API 호출 실패 시 source, symbol, event time 포함한 구조화 로그 남긴다.
- FRED API Rate Limit(429): 반드시 지수 백오프(exponential backoff) 적용.

---

## 리액티브 / WebSocket 규칙

- WebSocket/스트림 코드는 재연결 + 백오프 정책 필수.
- 리액티브 파이프라인 내부에서 블로킹 호출 금지.
- IO 경계의 타임아웃은 명시적으로 설정.
- `initCache()` 호출은 블로킹/논블로킹 중 하나로 통일. 신규 코드는 awaited 방식.

---

## Kotlin 코딩 주의사항

- DTO 필드는 camelCase. 외부 API와 다를 경우 `@JsonProperty("snake_case")` 사용.
- 서비스 반환 타입: Boolean 대신 sealed enum(`SubscriptionChangeStatus` 등) 사용.
  → Boolean은 성공/실패만 전달되고 이유를 알 수 없어 호출자가 분기 불가.
- 주석 처리된 코드 커밋 금지.
- 미구현 stub을 `TODO` 주석 없이 빈 메서드로 두지 않는다.
- 로거: 신규 파일은 `mu.KotlinLogging.logger {}` 사용.

---

## mode 처리 규칙

- mode는 `"paper"` 또는 `"live"` 두 값만 허용.
- URL/키 분기 로직은 `KisProperties` 내부에만 집중. 외부에서 재검증 금지.

---

## .agents 관리

- 기능 완료 시 `.agents/feature/{날짜}-{기능명}.md` 생성.
- API 추가/수정 시 `.agents/feature/README.md` 즉시 갱신 (최신 상태만 유지).
- 버그/장애 발생 시 `.agents/rule/`에 재발 방지 기록 (핵심만 짧게).
