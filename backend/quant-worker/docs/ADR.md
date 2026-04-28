# ADR — quant-worker

## ADR-001: Python + FastAPI 선택
**결정**: pykrx/yfinance 수집 워커를 Python FastAPI로 구현
**이유**: pykrx, yfinance가 Python 전용 라이브러리. Kotlin에서 호출하려면 subprocess나 별도 프로세스 필요 — 차라리 Python 서비스로 분리.
**트레이드오프**: Kotlin 메인 스택과 언어 분리. 타입 공유 불가, HTTP API 계약으로만 통신.

## ADR-002: 외부 트리거 방식 (스케줄러 미내장)
**결정**: 워커 자체에 스케줄러를 두지 않고 collector-api가 HTTP로 트리거
**이유**: 수집 시점을 collector-api가 제어하면 트리거 조건(장 마감 후, 수동 등) 변경 시 워커 코드 수정 불필요.
**트레이드오프**: collector-api 장애 시 수집이 아예 안 됨. 독립 실행이 필요한 경우 main.py CLI로 직접 실행해야 함.

## ADR-003: Redis 의존성 제거
**결정**: 초기 Redis 기반 메타데이터 관리를 제거하고 PostgreSQL catalog 단일 소스로 전환
**이유**: Redis와 DB 간 동기화 불일치 발생. catalog의 watermark(fetchedUntilDate)를 DB에서 직접 읽는 게 단순하고 신뢰성 높음.
**트레이드오프**: 수집 상태 조회 시 매번 DB 쿼리 필요. Redis 캐시 없어 반복 조회 시 부하.

## ADR-004: ON CONFLICT DO UPDATE (Upsert)
**결정**: 동일 (source, symbol, trade_date) 데이터 재수집 시 UPDATE로 덮어씀
**이유**: 수집 중단 후 재실행 시 중복 INSERT 오류 없이 안전하게 재적재 가능.
**트레이드오프**: 의도치 않은 데이터 덮어씌움 위험. 원본 데이터 보존이 필요하면 별도 이력 테이블 필요.
