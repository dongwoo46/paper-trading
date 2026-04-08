Title
pykrx 일봉 수집 (2010-01-01 ~ 현재)

Date
2026-04-05

Owner
Codex

Goal
pykrx에서 KRX 종목 일봉(OHLCV)을 2010-01-01부터 현재까지 수집하고 CSV로 저장한다.

Scope
포함:
pykrx 일봉 조회 모듈
CLI 실행 스크립트
yfinance와 동일한 공통 스키마 정규화
제외:
DB 적재
멀티 심볼 배치 오케스트레이션

Design (Clean Architecture boundary)
src/collectors/pykrx_daily_collector.py
수집 요청 모델(DailyCollectRequest)
수집기(PykrxDailyCollector)
pykrx 응답 정규화
scripts/fetch_pykrx_daily.py
실행 입력 파싱
파일 저장 경로 결정

SRP checklist
수집 책임과 CLI 책임 분리: Yes
정규화 책임 단일 메서드 유지: Yes
저장 책임 분리: Yes

Readability checklist
의미 있는 이름 사용: Yes
가드 절 사용: Yes
흐름 단순화: Yes

Test plan
수동 테스트:
python scripts/fetch_pykrx_daily.py --symbol 005930
python scripts/fetch_pykrx_daily.py --symbol 000660 --start 2015-01-01 --end 2020-12-31
검증 포인트:
CSV 생성 여부
컬럼/정렬/행 수 확인
빈 응답 처리 확인

Review result
yfinance 일봉과 동일 출력 스키마로 통일
pykrx 버전 차이를 고려해 adjusted 인자 폴백 적용
