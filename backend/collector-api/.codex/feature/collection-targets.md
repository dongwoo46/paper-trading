# 기능: 수집 대상 체크리스트

상태 값: `TODO`, `IN_PROGRESS`, `DONE`

## 즉시 우선순위
- [ ] `TODO` 국장 OHLCV 수집 파이프라인 구축 (`pykrx`, 일봉/분봉)
- [ ] `TODO` 미장 OHLCV 수집 파이프라인 구축 (`Tiingo` 또는 `yfinance`, 일봉/분봉)
Blocker: `yfinance`는 Python 워커 분리 설계 이후 적용, 그 전에는 Tiingo 우선 검토
- [ ] `TODO` 현재가(또는 최신 close) 수집 경로 구축 (체결 시 가격 계산용)

## 실시간 (WebSocket 우선)
- [x] `DONE` Upbit WebSocket 실시간 시세/체결/호가 수집
- [x] `DONE` KIS WebSocket 실시간 시장 데이터 수집

## OHLCV
- [ ] `TODO` FinanceDataReader 기반 국내 OHLCV 수집
- [ ] `TODO` yfinance(Python 워커) 기반 미국 OHLCV 수집
Blocker: Python 워커 분리 설계 이후에 진행 (현재는 후순위)
- [ ] `TODO` pykrx 기반 국내 OHLCV 보강 수집

## 코인 글로벌
- [ ] `TODO` Binance 시장 데이터 수집

## 매크로 (필수 3종)
- [ ] `TODO` FRED 매크로 시계열 수집
- [ ] `TODO` ECOS 매크로 시계열 수집
- [ ] `TODO` IMF 매크로 시계열 수집

## 공시 (RAG 입력)
- [ ] `TODO` DART 공시 수집 및 문서 정규화
- [ ] `TODO` EDGAR 공시 수집 및 문서 정규화

## 뉴스 및 감성
- [ ] `TODO` Finnhub 뉴스 수집 및 감성 파이프라인

## 팩터 기준선
- [ ] `TODO` Kenneth French Data Library(FF Data Library) 팩터 데이터 동기화

## 백테스트 엔진
- [ ] `TODO` VectorBT 백테스트 엔진 연동
- [ ] `TODO` Backtrader 백테스트 엔진 연동

## 추가 소스
- [ ] `TODO` GDELT 이벤트/뉴스 데이터 수집
- [ ] `TODO` Reddit 게시글/댓글 데이터 수집
- [ ] `TODO` FMP 데이터 수집

## 실행 규칙
- 작업 시작 시 `TODO -> IN_PROGRESS`로 변경한다.
- 작업 완료 시 `DONE (YYYY-MM-DD, evidence: module/tests)` 형식으로 기록한다.
- 블로커가 있으면 항목 바로 아래에 `Blocker:` 한 줄을 추가한다.
