Title
Docker Compose 기본값 반영 + Redis 메타데이터 연동

Date
2026-04-06

Owner
Codex

Goal
docker-compose 설정값과 맞는 기본 DB 연결값을 사용하고, 배치 실행 결과를 Redis에 기록한다.

Scope
포함:
PG 기본 env 값 반영 (localhost:5432, paper/paper/paper)
Redis 설정 로더 추가
배치 실행 후 Redis 메타데이터 저장
Redis 비활성화 옵션 추가 (--disable-redis)
제외:
Redis 큐/락 기반 오케스트레이션

Design (Clean Architecture boundary)
src/runtime/redis_run_metadata.py: Redis 기록 책임
scripts/fetch_daily_from_catalog.py: 설정 로딩/실행 연결 책임

SRP checklist
Redis 책임 분리: Yes
스크립트는 orchestration만 담당: Yes

Readability checklist
기본값 명시: Yes
실패 시 비차단 처리: Yes

Test plan
python main.py --provider all
Redis 미기동 상태에서 redis=skip 로그 확인
python main.py --disable-redis 실행 확인

Review result
Compose 로컬 환경에서 추가 env 없이 기본 실행 가능
Redis 실패가 수집 배치를 막지 않도록 처리
