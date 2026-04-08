Title
수집 데이터 market_daily_ohlcv 업서트 저장

Date
2026-04-06

Owner
Codex

Goal
pykrx, yfinance 수집 결과를 market_daily_ohlcv 테이블에 저장한다.

Scope
포함:
일봉 row upsert repository 추가
수집 job에서 DB 저장 수행
symbol catalog 워터마크 갱신 시 실제 수집된 fetched_until_date 사용
제외:
대량 벌크 튜닝

Design (Clean Architecture boundary)
src/repositories/market_daily_ohlcv_repository.py: OHLCV 저장 책임
src/jobs/catalog_daily_fetch_job.py: 수집 후 저장 orchestration
scripts/fetch_daily_from_catalog.py: repository wiring

SRP checklist
저장 책임 분리: Yes
수집/증분/상태갱신 역할 분리: Yes

Readability checklist
저장 context 구조체 명시: Yes
summary에 fetched_until_date 표시: Yes

Test plan
python main.py --provider yfinance --only-default
SELECT count(*) FROM market_daily_ohlcv WHERE source='yfinance'
동일 실행 재호출 후 중복 없이 upsert 확인

Review result
CSV + DB upsert 동시 처리 가능
