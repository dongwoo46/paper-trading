# 토스 fixture

공식 명세 `openapi.json` 1.2.28 의 응답 예시를 옮긴 가짜 값입니다. 실제 토큰·키·계좌·금액이 아닙니다.

- `error-ip-not-allowed.json`, `error-edge-blocked.json`: 명세에 예시가 없어 개요 문서의 에러 코드 표와 에러 봉투 모양으로 만들었고 `message` 문구는 임의입니다.
- `success-holdings-mixed.json`, `success-holdings-krw-only.json`(usd null), `success-holdings-empty.json`, `success-buying-power-krw.json`, `success-buying-power-usd.json`, `success-accounts.json`, `success-accounts-empty.json`: 명세의 응답 예시(withHoldings, filteredBySymbol, emptyHoldings, krw, usd, brokerageAccount, emptyAccounts) 모양을 옮긴 가짜 값입니다. `success-accounts.json` 의 둘째 계좌는 `accountSeq` 가 int32 를 넘는 값(9876543210)과 두 번째 계좌 유형을 시험하려고 임의로 더했습니다.
- `success-prices-kr.json`, `success-prices-us.json`: 명세의 응답 예시(krStock, usStock) 값을 옮긴 가짜 값입니다.
- `success-prices-empty.json`: 종목이 없는 빈 result 를 시험하려고 만든 값입니다(명세 예시 없음).
- `success-prices-timestamp-null.json`: 명세가 timestamp 를 «체결 미발생 등으로 시각이 없을 경우 null» 로 허용해, krStock 예시의 timestamp 만 null 로 바꾼 값입니다.
- `success-orderbook-kr.json`, `success-orderbook-us.json`: 명세의 응답 예시(krStock, usStock) 값을 옮긴 가짜 값입니다(asks 낮은 가격순, bids 높은 가격순).
- `success-orderbook-empty.json`: 명세 required 인 asks·bids 가 빈 배열이고 timestamp 가 null 인 경우(데이터 미제공)를 시험하려고 만든 값입니다(명세 예시 없음).
- `success-candles-daily-kr.json`, `success-candles-minute-kr.json`, `success-candles-last-page.json`: 명세의 응답 예시(dailyCandles, minuteCandles, lastPage) 값을 옮긴 가짜 값입니다(최신순, 마지막 쪽은 nextBefore null).
- `success-candles-daily-us.json`: 미국 일봉의 `-04:00` 오프셋을 원문 그대로 싣는지 시험하려고 dailyCandles 모양에 임의 값을 넣어 만든 가짜 값입니다(명세 예시 없음, 명세는 미국 일봉이 `-04:00`·`-05:00` 라고만 설명).
- `token-success.json`: `access_token` 은 시험 전용 문자열입니다(명세 예시는 말줄임표로 잘린 JWT).
