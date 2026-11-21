# 결정 이력 — 서비스 구성·아키텍처 (paper-trading)
<!-- devkit:applies-to ^backend/(trading-api|collector-api|quant-[a-z]+)/ -->

> 옛 `docs/ADR.md`·`backend/*/docs/ADR.md` 의 결정을 관심사별로 모았다(2026-11-21 규칙 이사). 아래 글은 원문 그대로다.

아래는 옛 `docs/ADR.md` 에서 온 결정이다.

# ADR — Paper Trading System

## ADR-001: 모노레포 구조 채택
**결정**: 단일 Git 레포 안에 backend(Kotlin) + frontend(Next.js) + worker(Python) 통합
**이유**: 서비스 간 API 변경 시 단일 PR로 추적 가능. 초기 단계에서 팀이 1인이므로 오버헤드 최소화.
**트레이드오프**: 서비스별 독립 배포 파이프라인 설정이 복잡해짐. 레포 규모 커지면 빌드 시간 증가.

## ADR-002: 언어/프레임워크 혼용 (Kotlin + Python)
**결정**: trading-api/collector-api → Kotlin/Spring Boot, quant-worker → Python/FastAPI
**이유**: OHLCV 수집은 pykrx/yfinance가 Python 전용 라이브러리. 금융 도메인 로직은 Kotlin 타입 안전성이 유리.
**트레이드오프**: 두 언어 유지 비용. 서비스 간 타입 공유 불가, API 스펙으로만 계약.

## ADR-004: PostgreSQL 단일 DB (서비스 간 스키마 공유)
**결정**: trading-api / collector-api / quant-worker 모두 동일 PostgreSQL 인스턴스 사용
**이유**: 초기 단계에서 DB 분리 운영 비용 대비 이점 없음. 조인 없이 각 서비스가 자기 스키마만 접근.
**트레이드오프**: 서비스 독립 확장 어려움. 한 서비스의 쿼리 폭주가 전체 DB에 영향.

## ADR-005: 거래 모드 Strategy 패턴
**결정**: LOCAL / KIS_PAPER / KIS_LIVE를 OrderExecutionPort 인터페이스로 추상화, 계좌별로 모드 결정
**이유**: 모드 추가(Upbit 등) 시 기존 코드 변경 없이 구현체만 추가. 테스트 시 LocalOrderExecutor로 격리.
**트레이드오프**: 인터페이스 추상화 레이어 증가. KIS_LIVE와 LOCAL의 체결 타이밍 차이를 동일 인터페이스로 감추는 것이 부자연스러울 수 있음.

아래는 옛 `backend/quant-worker/docs/ADR.md` 에서 온 결정이다.

# ADR — quant-worker

## ADR-001: Python + FastAPI 선택
**결정**: pykrx/yfinance 수집 워커를 Python FastAPI로 구현
**이유**: pykrx, yfinance가 Python 전용 라이브러리. Kotlin에서 호출하려면 subprocess나 별도 프로세스 필요 — 차라리 Python 서비스로 분리.
**트레이드오프**: Kotlin 메인 스택과 언어 분리. 타입 공유 불가, HTTP API 계약으로만 통신.

## ADR-002: 외부 트리거 방식 (스케줄러 미내장)
**결정**: 워커 자체에 스케줄러를 두지 않고 collector-api가 HTTP로 트리거
**이유**: 수집 시점을 collector-api가 제어하면 트리거 조건(장 마감 후, 수동 등) 변경 시 워커 코드 수정 불필요.
**트레이드오프**: collector-api 장애 시 수집이 아예 안 됨. 독립 실행이 필요한 경우 main.py CLI로 직접 실행해야 함.

아래는 옛 `backend/collector-api/docs/ADR.md` 에서 온 결정이다.

## ADR-004: 수요 기반 자동 구독 (Demand-driven)
**결정**: trading-api가 주문/포지션 생성 시 collector-api에 구독 요청, 종료 시 해제
**이유**: 불필요한 종목까지 상시 구독하면 KIS WebSocket 41슬롯 낭비.
**트레이드오프**: 내부 API 결합 발생. trading-api 장애 시 구독 해제 누락 가능성. 슬롯 관리 로직 복잡.

아래는 옛 `backend/trading-api/docs/ADR.md` 에서 온 결정이다.

## ADR-003: Redis를 시세 조회 전용으로만 사용
**결정**: trading-api는 Redis에서 시세를 읽기만 함. 쓰기는 collector-api 전담.
**이유**: 책임 분리. collector-api가 Redis 시세 포맷의 단일 소유자.
**트레이드오프**: Redis 키 구조(quote:{ticker} Hash) 변경 시 두 서비스 동시 수정 필요.
