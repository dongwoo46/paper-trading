Title
DB Catalog 기반 일봉 수집 (yfinance + pykrx)

Date
2026-04-05

Owner
Codex

Goal
PostgreSQL의 yfinance_symbol_catalog, pykrx_symbol_catalog에서 종목을 읽어 일봉을 수집한다.

Scope
포함:
DB 종목 카탈로그 조회
provider별 일봉 수집 실행
기간 선택(기본: 2010-01-01 ~ 오늘)
심볼별 CSV + 요약 CSV 출력
제외:
DB 적재(저장)는 Spring 처리

Design (Clean Architecture boundary)
src/catalog/*: DB 종목 조회 책임
src/jobs/catalog_daily_fetch_job.py: 오케스트레이션 책임
scripts/fetch_daily_from_catalog.py: 실행 엔트리포인트/인자 파싱

SRP checklist
DB 조회 / 수집 / 실행 엔트리 분리: Yes
provider별 수집 로직 재사용: Yes

Readability checklist
명확한 파라미터 이름: Yes
기본값 정책(2010~오늘) 명시: Yes
검증 로직(start <= end) 포함: Yes

Test plan
python scripts/fetch_daily_from_catalog.py --provider yfinance
python scripts/fetch_daily_from_catalog.py --provider pykrx --only-default
python scripts/fetch_daily_from_catalog.py --provider all --start 2015-01-01 --end 2020-12-31

Review result
카탈로그 기반 배치 실행 가능
Spring이 종목 제어를 맡는 구조와 충돌 없음
