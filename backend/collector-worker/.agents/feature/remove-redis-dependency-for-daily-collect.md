Title
일봉 수집 Redis 의존 제거

Date
2026-04-06

Owner
Codex

Goal
수집 주기가 낮은 운영 조건에 맞춰 Redis 연동 없이 DB 직접 조회 기반으로 단순화한다.

Scope
포함:
구독목록 Redis 캐시 제거
실행 메타 Redis 저장 제거
API/CLI 옵션에서 Redis 관련 인자 제거
제외:
주기 자동 수집 로직

Design (Clean Architecture boundary)
src/application/daily_fetch_service.py: DB 기반 조회/수집만 수행
src/interfaces/api/app.py: 수집 요청/주기 실행 엔트리 유지

SRP checklist
수집 책임 단순화: Yes
외부 캐시 의존 제거: Yes

Readability checklist
불필요 옵션 제거: Yes
실행 경로 단순화: Yes

Test plan
python -m compileall src scripts api_main.py main.py
API 수집 요청 정상 여부 확인

Review result
Redis 없이 동작 가능
일 1~2회 수집 운영에 적합한 구조로 단순화 완료
