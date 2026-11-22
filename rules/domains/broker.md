# Broker 도메인 — 토스증권 연동 (paper-trading)
<!-- devkit:pointer
path: ^backend/(trading-api|collector-api)/src/main/.+(Toss|Broker)\w*\.kt$
why: 토스증권 등 외부 증권사 연동(토큰·호출 한도·웹소켓·주문 채널) Kotlin 코드
-->

> 아래 줄은 옛 문서에서 원문 그대로 옮겼다(2026-11-21 규칙 이사). 절 제목이 출처다.
> 근거 자료: [토스증권 Open API 조사](../../docs/reference/2026-11-15-toss-open-api.md).

## 옮겨 온 줄 — docs/research/2026-11-15-toss-open-api.md

- 토큰 만료, 재발급, 허용 IP 이탈이면 `auth-revoked` 후 연결이 끊긴다. 재연결 뒤에는 `GET /orders`와 `GET /conditional-orders`로 상태를 다시 맞춰야 한다.

- 시세 `MARKET_DATA` 15, 차트 `MARKET_DATA_CHART` 20, 종목 `STOCK` 5, 전체 종목 `STOCK_ALL` 1, 수급 `STOCK_TRADING_TREND` 10, 장 정보 `MARKET_INFO` 3, 랭킹 5, 시장 지표 5~10, 섹터 5, 계좌 `ACCOUNT` 1, 자산 5, 주문 `ORDER` 10, 주문 조회 5, 거래 가능 정보 6(09:00~09:10에는 3), 조건주문 5, 조건주문 조회 10, 토큰 5. 한도는 사전 공지 없이 바뀔 수 있고 `X-RateLimit-Limit` 헤더로 확인한다. 429는 `Retry-After`만큼 대기한다.

- 토큰이 클라이언트당 1개라서, 같은 키를 쓰는 프로세스가 둘이면 서로 토큰을 무효화한다. 토큰 발급 지점을 `collector-api`나 `trading-api` 중 하나로 모아야 한다.
- 실거래 주문 채널은 trading-api LOCAL 모의 체결과의 라우팅 경계를 설계·승인한 뒤에만 붙인다.

- `marketCountry`·`currency` enum은 명세가 «모르는 값을 허용하도록 구현»하라고 적었다. 모르는 값에서 역직렬화가 터지지 않게 한다.
