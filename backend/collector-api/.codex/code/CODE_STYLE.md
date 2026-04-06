# market-collector 코드 스타일 가이드

## 언어 및 기준
- Kotlin 1.9+, Java 21, Spring Boot 3.x를 기준으로 한다.
- 파일 인코딩은 UTF-8, 줄바꿈은 LF, 들여쓰기는 공백 4칸을 사용한다.
- 코드와 주석은 팀 일관성을 위해 원칙적으로 영어를 유지한다.

## 네이밍
- 패키지명은 소문자만 사용한다. (`com.papertrading.collector.source.upbit`)
- 클래스/객체명은 `PascalCase`를 사용한다.
- 함수/프로퍼티/변수명은 `camelCase`를 사용한다.
- 상수명은 `UPPER_SNAKE_CASE`를 사용한다.
- 책임에 따라 접미사를 명시한다.
- `*Config`, `*Properties`, `*Client`, `*Collector`, `*Repository`, `*Service`

## 파일 및 패키지 구조
- 파일당 하나의 주요 클래스를 둔다.
- 먼저 도메인/소스 기준으로 나누고, 그 안에서 레이어를 구분한다.
- 권장 패턴:
- `source/<source>/config`
- `source/<source>/client`
- `source/<source>/collector`
- `source/<source>/normalize`

## Kotlin 규칙
- 가변(`var`)보다 불변(`val`)을 우선한다.
- 함수는 짧고 단일 책임을 유지한다.
- 전송/도메인 페이로드는 `data class`를 우선 사용한다.
- 정말 선택적인 경우가 아니면 nullable 타입을 지양한다.
- 제어된 이벤트/결과 타입은 `sealed interface/class`를 사용한다.
- 가독성이 유지되면 식(Expression) 스타일을 우선한다.

## Spring 규칙
- 의존성 주입은 생성자 주입만 사용한다.
- 외부 설정은 `@ConfigurationProperties`로 관리한다.
- 필드 주입과 정적 상태를 피한다.
- 시작 시 동작은 숨겨진 부작용 없이 명시적으로 작성한다.

## 리액티브/동시성 규칙
- WebSocket/스트림 코드는 재연결 + 백오프 정책을 반드시 포함한다.
- 리액티브 파이프라인 내부에서 블로킹 호출을 금지한다.
- IO 경계의 타임아웃은 명시적으로 설정한다.
- 로그는 source, symbol, event time을 포함한 구조화 로그를 사용한다.

## 예외 처리 및 로깅
- 예외를 삼키지 않는다.
- 외부 API 실패는 소스별 컨텍스트를 포함해 래핑한다.
- 라이프사이클은 `info`, 복구 가능한 이슈는 `warn`, 실패는 `error`를 사용한다.
- 시크릿/자격증명 원문은 절대 로그에 남기지 않는다.

## 테스트
- 정규화/매핑 로직은 단위 테스트를 작성한다.
- 가능하면 클라이언트 어댑터 통합 테스트를 작성한다.
- 테스트 이름은 Given-When-Then 스타일을 따른다.
- 새로운 수집 경로마다 최소 1개의 정상 경로(happy path) 테스트를 추가한다.

## 금지 사항
- 비대해진 God 클래스 작성 금지
- 하나의 패키지에 도메인 관심사 혼합 금지
- 코드에 엔드포인트/키 하드코딩 금지
- 실제 두 번째 사용 사례가 없는데 과도한 추상화 금지
