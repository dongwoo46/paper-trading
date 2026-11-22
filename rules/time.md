# Time — 퀀트 Python 의 날짜 타입 (paper-trading)
<!-- devkit:pointer
path: ^backend/quant-[a-z]+/.+\.py$
signal: \bdatetime\b|timedelta|\.date\(\)
why: 퀀트 수집·분석 Python 코드의 date·datetime 다루기
-->
<!-- devkit:scope repo-only -->
<!-- devkit:form v2 -->

이 파일은 퀀트 Python 코드의 날짜 타입 다루기를 정한다. yfinance 수집에서 난 날짜 타입 사고의 재발을 막는다.

## 조항

<!-- gt:PTI1 -->
- `datetime.date` 객체에 `.date()` 를 다시 부르지 않고, 날짜 계산은 `datetime.timedelta` 로 하며 변수에 `date`·`datetime` 타입을 밝힌다.
  Why: `date` 에는 `.date()` 가 없어 이미 날짜인 값에 부르면 수집 배치가 멈춘다(collector-worker 재발 방지 규칙 2, yfinance 사고 · `docs/adr/market-data-collector-worker-incidents.md`). — enforce: review
