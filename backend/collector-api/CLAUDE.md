# CLAUDE.md — collector-api 공통 규칙

## 프로젝트 개요
Spring Boot + Kotlin 기반 시장 데이터 수집 서비스.
KIS(한국투자증권) WebSocket/REST + Upbit WebSocket으로 실시간 시세 수집.
Redis를 캐시 레이어로 사용하고 JPA(PostgreSQL)로 구독 목록을 영속.

## 아키텍처 레이어 규칙

```
presentation  →  application (service/pipeline/runtime)  →  domain  ←  infra
```

- `domain`은 외부 의존성(Spring, Redis, WebClient 등)을 가지지 않는다.
- `infra`는 외부 시스템 연동 구현체만 둔다 (persistence, rest client, ws client).
- `application`은 유스케이스 오케스트레이션만 한다 — DB/Redis를 직접 알지 않는다.
- `presentation`은 HTTP 변환, 입력 정규화(lowercase, trim), 응답 조립만 한다.

## 코딩 컨벤션

### mode 문자열 처리
- mode는 항상 `"paper"` 또는 `"live"` 두 값만 허용한다.
- `KisProperties.normalizedModes()`를 통과한 값만 사용하고, 내부에서 재검증하지 않는다.
- `if (mode == "live") ... else ...` 분기는 `KisProperties` 내부에만 집중시킨다.
  - 새 URL/키 분기가 필요하면 `KisProperties`에 함수를 추가한다.

### DTO 네이밍
- Kotlin/Java 클래스 필드는 camelCase를 사용한다.
- 외부 API와 snake_case 필드명이 달라야 하면 `@JsonProperty("field_name")`을 사용한다.
- 현재 `TokenRequest`, `ApprovalRequest` 등에 snake_case 필드가 직접 사용됨 — 신규 DTO는 반드시 `@JsonProperty` 방식으로 작성한다.

### 로거
- `mu.KotlinLogging.logger {}` 또는 `LoggerFactory.getLogger(javaClass)` 중 파일 내에서 하나만 통일해 사용한다.
- 현재 혼용 중 — 신규 파일은 `mu.KotlinLogging.logger {}`를 사용한다.

### 서비스 반환 타입
- 상태를 반환해야 하는 서비스 메서드는 `SubscriptionChangeStatus` 같은 sealed enum을 사용한다.
- Boolean 반환은 성공/실패만 알 수 있어 호출자가 이유를 알 수 없다 — 피한다.
- `KisRestWatchlistService.addSymbol/removeSymbol`은 Boolean → SubscriptionChangeStatus로 교체 대상.

### 캐시 초기화 일관성
- `initCache(mode)` 호출은 블로킹 또는 논블로킹 방식 중 하나로 통일한다.
- 현재 `KisWebSocketCollector.start()`에서 `restWatchlistService.initCache`는 fire-and-forget,
  `wsSubscriptionService.initCache`는 `.then()` 체인으로 awaited — 불일치. 신규 코드는 awaited로 작성한다.

## 테스트
- 테스트는 `src/test/kotlin` 아래 동일 패키지 구조로 작성한다.
- 단위 테스트: MockK 사용. Spring 컨텍스트 로딩 금지.
- 통합 테스트: `@SpringBootTest` + Testcontainers (Redis, PostgreSQL).

## 금지 사항
- 주석 처리된 코드를 커밋하지 않는다 (`// upbitWebSocketCollector.start()` 등).
- 미구현 stub을 `TODO` 주석 없이 빈 메서드로 두지 않는다 (`RawEventPipeline.publish` 참고).
- `TokenCacheSnapshot`, `TokenCacheEntry` 같은 내부 구현 DTO는 파일 외부로 노출하지 않는다 (internal/private 처리).
