# ADR — Paper Trading System

## ADR-001: 모노레포 구조 채택
**결정**: 단일 Git 레포 안에 backend(Kotlin) + frontend(Next.js) + worker(Python) 통합
**이유**: 서비스 간 API 변경 시 단일 PR로 추적 가능. 초기 단계에서 팀이 1인이므로 오버헤드 최소화.
**트레이드오프**: 서비스별 독립 배포 파이프라인 설정이 복잡해짐. 레포 규모 커지면 빌드 시간 증가.

## ADR-002: 언어/프레임워크 혼용 (Kotlin + Python)
**결정**: trading-api/collector-api → Kotlin/Spring Boot, collector-worker → Python/FastAPI
**이유**: OHLCV 수집은 pykrx/yfinance가 Python 전용 라이브러리. 금융 도메인 로직은 Kotlin 타입 안전성이 유리.
**트레이드오프**: 두 언어 유지 비용. 서비스 간 타입 공유 불가, API 스펙으로만 계약.

## ADR-003: Redis를 시세 캐시 + Pub/Sub 버스로 사용
**결정**: 실시간 시세는 Redis Hash(quote:{ticker}) 저장 + Redis Pub/Sub으로 trading-api에 전달
**이유**: 별도 메시지 브로커(Kafka 등) 없이 Redis 하나로 캐시와 이벤트 버스 역할 통합. 운영 복잡도 최소화.
**트레이드오프**: Redis 장애 시 시세 수신과 체결 엔진 동시 중단. 메시지 영속성 없음(재시작 시 유실 가능).

## ADR-004: PostgreSQL 단일 DB (서비스 간 스키마 공유)
**결정**: trading-api / collector-api / collector-worker 모두 동일 PostgreSQL 인스턴스 사용
**이유**: 초기 단계에서 DB 분리 운영 비용 대비 이점 없음. 조인 없이 각 서비스가 자기 스키마만 접근.
**트레이드오프**: 서비스 독립 확장 어려움. 한 서비스의 쿼리 폭주가 전체 DB에 영향.

## ADR-005: 거래 모드 Strategy 패턴
**결정**: LOCAL / KIS_PAPER / KIS_LIVE를 OrderExecutionPort 인터페이스로 추상화, 계좌별로 모드 결정
**이유**: 모드 추가(Upbit 등) 시 기존 코드 변경 없이 구현체만 추가. 테스트 시 LocalOrderExecutor로 격리.
**트레이드오프**: 인터페이스 추상화 레이어 증가. KIS_LIVE와 LOCAL의 체결 타이밍 차이를 동일 인터페이스로 감추는 것이 부자연스러울 수 있음.
