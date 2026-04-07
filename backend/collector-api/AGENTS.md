@../../AGENTS.md

---

# collector-api 서비스 규칙

> 공통 규칙은 루트 `AGENTS.md`를 참조한다. 이 파일은 collector-api 전용 규칙이다.

## 서비스 개요

- **역할**: 외부 시장 데이터(KIS, Upbit, yfinance, pykrx, FRED) 수집 + Redis 반영 + PostgreSQL 히스토리 적재
- **언어/기술**: Kotlin 1.9 + Spring Boot 3.x (MVC + WebFlux 혼용) + JPA + Redis
- **검증 명령**: `./gradlew compileKotlin`

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

## Kotlin 코딩 규칙

- `var`보다 `val` 우선 사용
- `data class`를 전송/도메인 페이로드에 우선 사용
- nullable 타입은 정말 필요한 경우만 사용
- 의존성 주입은 생성자 주입만 사용 (`@Autowired` 필드 주입 금지)
- 외부 설정은 `@ConfigurationProperties`로 관리

## 리액티브/동시성 규칙

- WebSocket/스트림 코드는 재연결 + 백오프 정책을 반드시 포함한다
- 리액티브 파이프라인 내부에서 블로킹 호출 금지
- IO 경계의 타임아웃은 명시적으로 설정한다
- 로그는 source, symbol, event time을 포함한 구조화 로그 사용

## 데이터 수집 규칙

- Redis: 최신 시세 상태만 저장 (캐시, 단기)
- PostgreSQL: 히스토리 데이터 (장기 이력, 분석용)
- 외부 API 호출 실패 시 소스별 컨텍스트를 포함해 로그 남긴다
- FRED API Rate Limit(429) 발생 시 반드시 지수 백오프(exponential backoff) 적용

## .agents 폴더 관리

- 기능 완료 시 `.agents/feature/{날짜}-{기능명}.md` 생성
- API 추가/수정 시 `.agents/feature/README.md` 즉시 갱신
- `.agents/feature/README.md`는 최신 상태만 유지 (과거 상태 남기지 않음)
- 버그/장애 발생 시 `.agents/rule/` 에 재발 방지 기록