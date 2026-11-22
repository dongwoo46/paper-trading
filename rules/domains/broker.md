# Broker 도메인 — 토스증권 연동 (paper-trading)
<!-- devkit:pointer
path: ^backend/(trading-api|collector-api)/src/main/.+(Toss|Broker)\w*\.kt$
why: 토스증권 등 외부 증권사 연동(토큰·호출 한도·웹소켓·주문 채널) Kotlin 코드
-->
<!-- devkit:scope repo-only -->
<!-- devkit:form v2 -->

이 파일은 토스증권 Open API 를 붙이는 Kotlin 코드의 토큰, 호출 한도, 웹소켓 재연결, 주문 채널, 응답 enum 기준을 정한다. 토스 명세가 정한 제약을 어겨 연결이 끊기거나 실거래 주문이 모의 체결과 섞이는 것을 막는다. 근거 자료는 [토스증권 Open API 조사](../../docs/reference/2026-11-15-toss-open-api.md)다.

## 조항

<!-- gt:PTK1 -->
- 웹소켓이 `auth-revoked` 로 끊기면(토큰 만료, 재발급, 허용 IP 이탈) 재연결 뒤 `GET /orders` 와 `GET /conditional-orders` 로 주문 상태를 다시 맞춘다.
  Why: 토스는 이 세 경우에 `auth-revoked` 를 보내고 연결을 끊어, 끊긴 동안의 주문 이벤트를 받지 못한다(토스증권 Open API 조사 2026-11-15 웹소켓 절, aa6097e). — enforce: review
<!-- gt:PTK2 -->
- 토스 API 호출 한도는 응답 `X-RateLimit-Limit` 헤더로 확인하고, 429 를 받으면 `Retry-After` 만큼 기다린다. 기다린 뒤 다시 부를지는 devkit LA12 가 정한다. 2026-11-15 공지 한도(초당): 시세 `MARKET_DATA` 15, 차트 `MARKET_DATA_CHART` 20, 종목 `STOCK` 5, 전체 종목 `STOCK_ALL` 1, 수급 `STOCK_TRADING_TREND` 10, 장 정보 `MARKET_INFO` 3, 랭킹 5, 시장 지표 5~10, 섹터 5, 계좌 `ACCOUNT` 1, 자산 5, 주문 `ORDER` 10, 주문 조회 5, 거래 가능 정보 6(09:00~09:10에는 3), 조건주문 5, 조건주문 조회 10, 토큰 5.
  Why: 토스 한도는 클라이언트와 API 그룹 단위이고 사전 공지 없이 바뀔 수 있어, 박아 둔 숫자보다 헤더가 맞다(토스증권 Open API 조사 2026-11-15 호출 한도 절, aa6097e). — enforce: review
<!-- gt:PTK3 -->
- 토스 토큰 발급 지점은 `collector-api` 와 `trading-api` 중 한 곳으로 모으고, 같은 키로 두 프로세스가 따로 발급하지 않는다.
  Why: 토큰은 클라이언트당 1개만 유효해 새로 발급하면 이전 토큰이 `token-revoked` 되므로, 두 프로세스가 서로의 토큰을 무효화한다(토스증권 Open API 조사 2026-11-15 인증 절, aa6097e). — enforce: review
<!-- gt:PTK4 -->
- 증권사(토스·KIS 등) 실거래 주문 채널은 trading-api LOCAL 모의 체결과의 라우팅 경계를 설계하고 사람 승인을 받은 뒤에만 붙인다.
  Why: 경계 없이 붙이면 모의 주문이 실계좌로 나간다. 토스는 개요 문서에 샌드박스가 없어 주문을 실거래로 취급한다(토스증권 Open API 조사 2026-11-15, aa6097e «설계 변경이므로 사용자 승인 필요»). — enforce: review
<!-- gt:PTK5 -->
- 토스 응답의 `marketCountry`·`currency` enum 은 모르는 값이 와도 역직렬화가 실패하지 않게 받는다.
  Why: 토스 명세가 이 enum 에 «모르는 값을 허용하도록 구현»하라고 적어, 값이 늘면 응답 전체를 못 읽게 된다(토스증권 Open API 조사 2026-11-15 개별 API 스키마 절, ad2c033). — enforce: review
