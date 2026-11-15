# 토스 fixture

공식 명세 `openapi.json` 1.2.28 의 응답 예시를 옮긴 가짜 값입니다. 실제 토큰·키·계좌·금액이 아닙니다.

- `error-ip-not-allowed.json`, `error-edge-blocked.json`: 명세에 예시가 없어 개요 문서의 에러 코드 표와 에러 봉투 모양으로 만들었고 `message` 문구는 임의입니다.
- `success-holdings-mixed.json`, `success-holdings-krw-only.json`(usd null), `success-holdings-empty.json`, `success-buying-power-krw.json`, `success-buying-power-usd.json`, `success-accounts.json`, `success-accounts-empty.json`: 명세의 응답 예시(withHoldings, filteredBySymbol, emptyHoldings, krw, usd, brokerageAccount, emptyAccounts) 모양을 옮긴 가짜 값입니다. `success-accounts.json` 의 둘째 계좌는 `accountSeq` 가 int32 를 넘는 값(9876543210)과 두 번째 계좌 유형을 시험하려고 임의로 더했습니다.
- `token-success.json`: `access_token` 은 시험 전용 문자열입니다(명세 예시는 말줄임표로 잘린 JWT).
