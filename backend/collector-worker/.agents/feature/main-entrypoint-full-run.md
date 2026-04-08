Title
main.py 전체 수집 실행 엔트리포인트 추가

Date
2026-04-05

Owner
Codex

Goal
python main.py 한 번으로 카탈로그 기반 전체 일봉 수집(yfinance + pykrx)을 실행한다.

Scope
포함:
루트 엔트리포인트 main.py
기존 카탈로그 배치 로직 재사용
제외:
수집 로직 변경

Design (Clean Architecture boundary)
main.py는 실행 위임만 담당
실제 orchestration은 scripts/fetch_daily_from_catalog.py 유지

SRP checklist
엔트리포인트 책임 단일화: Yes
기존 배치 로직 재사용(중복 제거): Yes

Readability checklist
최소 코드/명확한 흐름: Yes

Test plan
python main.py
python main.py --provider yfinance --start 2012-01-01 --end 2020-12-31

Review result
전체 실행 진입점 확보
기존 옵션 체계 그대로 사용 가능
