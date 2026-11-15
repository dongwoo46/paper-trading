# 토스증권 Open API 조사 (2026-11-15)

KIS와 토스증권을 함께 쓰기 위해 토스증권 공식 문서로 가능한 일을 정리한다.

## 출처와 범위

- 읽은 문서: [공식 개요 문서](https://openapi.tossinvest.com/openapi-docs/overview.md) 전문(엔드포인트 목록, 시작하기, 호출 한도, 에러 코드, 웹소켓 연동)
- 개별 API 스키마: 2026-11-15 추가 확인. 공식 명세 `https://openapi.tossinvest.com/openapi-docs/latest/openapi.json`(OpenAPI, 버전 1.2.28)에 있다. 처음 조사 때는 다른 경로를 시도해 403·404였다. 요약은 아래 «개별 API 스키마(step 5~7 범위)» 절.
- 읽지 못한 문서: 소개 페이지, 이용약관, FAQ, 보도자료. 본문을 자바스크립트로 그려서 서버에서 받으면 제목만 온다.
- 공식 MCP: 개요 문서와 소개 페이지에 언급이 없다. 커뮤니티 서버만 있다(tossinvest-mcp, JeongSeongMok/tossinvest-openapi-mcp, scjang01/tossinvest-mcp, kcw2034/toss-invest-mcp).
- 아래 표의 항목은 개요 문서에 적힌 것만 옮겼다. 필드 단위 스펙은 개별 문서를 직접 읽고 확정해야 한다.

## 인증과 접속 조건

| 항목 | 내용 |
|---|---|
| 토큰 | `POST /oauth2/token`, Client Credentials, `Authorization: Bearer` |
| 키 발급 | 토스증권 WTS 설정 > Open API 메뉴에서 `client_id`, `client_secret` 발급 |
| 허용 IP | 설정 메뉴에서 등록한 IP만 호출 가능, 그 밖은 403 `ip-not-allowed` (웹소켓도 동일) |
| 토큰 수 | 클라이언트당 1개만 유효, 새로 발급하면 이전 토큰은 `token-revoked` |
| 계좌 헤더 | 계좌·자산, 주문, 조건주문은 `X-Tossinvest-Account: {accountSeq}` 추가 필요 |
| 샌드박스 | 개요 문서에 없음(커뮤니티 서버 문서는 없다고 명시), 주문은 실거래로 취급 |

## 할 수 있는 일

### 시세와 종목 정보 (토큰만 필요, 한도 초당 1~20회, 그룹별)

| 하고 싶은 일 | 엔드포인트 |
|---|---|
| 현재가, 호가, 최근 체결 | `GET /api/v1/prices`, `/orderbook`, `/trades` |
| 상하한가 | `GET /api/v1/price-limits` |
| 캔들 (1분봉, 일봉) | `GET /api/v1/candles` |
| 종목 마스터 | `GET /api/v1/stocks` (종목명, 시장, 통화, 상장 상태, 발행주식수) |
| 매수 유의사항 | `GET /api/v1/stocks/{symbol}/warnings` (정리매매, 단기과열, 투자경고, VI, 신주인수권) |
| 국내 수급 동향 | `investor-trading`, `program-trades`, `short-selling`, `credit-trades`, `securities-lending` (모두 `/api/v1/stocks/{symbol}/` 아래, 일별) |
| 환율 | `GET /api/v1/exchange-rate` (KRW, USD) |
| 장 운영 시간 | `GET /api/v1/market-calendar/KR` (KRX, NXT 세션), `/US` (데이마켓, 프리, 정규, 애프터) |
| 랭킹 | `GET /api/v1/rankings` (거래대금, 거래량, 등락률), `/rankings/investor-trading` (외국인, 기관, 개인 순매수, 순매도) |
| 시장 지표 | `GET /api/v1/market-indicators/prices` (국내 지수, 국채), `/{symbol}/candles`, `/{symbol}/investor-trading` (코스피, 코스닥) |
| 섹터 | `GET /api/v1/sectors`, `/{sectorId}`, `/stocks`, `/etfs`, `/sectors/rankings` |

### 계좌, 주문, 조건주문 (토큰과 계좌 헤더 필요)

| 하고 싶은 일 | 엔드포인트 |
|---|---|
| 계좌 목록, 보유 종목과 평가손익 | `GET /api/v1/accounts`, `/holdings` |
| 주문 생성, 정정, 취소 | `POST /api/v1/orders`, `/orders/{orderId}/modify`, `/orders/{orderId}/cancel` (지정가, 시장가, KR, US) |
| 주문 조회 | `GET /api/v1/orders` (대기, 종료), `/orders/{orderId}` |
| 거래 가능 정보 | `GET /api/v1/buying-power`, `/sellable-quantity`, `/commissions` |
| 조건주문 | `POST /api/v1/conditional-orders` (SINGLE, OCO, OTO, 지정가, 시장가), `modify`, `DELETE`, 목록, 상세 |

주문 관련 규칙(에러 코드 표 기준):
- `clientOrderId`로 멱등성을 지원한다. 같은 ID로 진행 중이면 409 `request-in-progress`, 내용이 다르면 422 `idempotency-key-conflict`.
- 주문 금액이 1억원 이상이면 `confirmHighValueOrder: true`가 필요하고, 30억원을 넘으면 거부된다.
- 같은 종목에 반대 방향 대기 주문이 있으면 409 `opposite-pending-order-exists`.
- OCO, OTO 조건주문은 종목당 1개, SINGLE은 제한이 없다.
- 금액 주문(US)은 정규장에만 가능하다.

### 웹소켓 `wss://openapi-ws.tossinvest.com/ws/v1`

| type | 내용 | 전달 보장 |
|---|---|---|
| `trade:{us,kr}` | 실시간 체결 | LOSSY (밀리면 유실, 유실 감지용 sequence 없음) |
| `orderbook:{us,kr}` | 실시간 호가 | LOSSY |
| `personal:order` | 본인 계좌 주문 이벤트 | LOSSLESS (세션 내) |
| `personal:conditional-order` | 본인 계좌 조건주문 이벤트 | LOSSLESS (세션 내) |

- 국내(`kr`)는 통합 시세(KRX+NXT)만 제공한다.
- 한도: 계정당 동시 연결 2개(초과하면 가장 오래된 연결이 끊김), 연결당 구독 100건, 구독 선언 초당 5회.
- 구독 선언은 JSON 배열 1개가 전체 구독을 대체한다(full-replace, 빈 배열은 전체 해제).
- 클라이언트 수신이 180초 없으면 종료되므로 60초마다 `PING`을 보낸다.
- 토큰 만료, 재발급, 허용 IP 이탈이면 `auth-revoked` 후 연결이 끊긴다. 재연결 뒤에는 `GET /orders`와 `GET /conditional-orders`로 상태를 다시 맞춰야 한다.

## 호출 한도 (초당, 클라이언트와 API 그룹 단위)

시세 `MARKET_DATA` 15, 차트 `MARKET_DATA_CHART` 20, 종목 `STOCK` 5, 전체 종목 `STOCK_ALL` 1, 수급 `STOCK_TRADING_TREND` 10, 장 정보 `MARKET_INFO` 3, 랭킹 5, 시장 지표 5~10, 섹터 5, 계좌 `ACCOUNT` 1, 자산 5, 주문 `ORDER` 10, 주문 조회 5, 거래 가능 정보 6(09:00~09:10에는 3), 조건주문 5, 조건주문 조회 10, 토큰 5. 한도는 사전 공지 없이 바뀔 수 있고 `X-RateLimit-Limit` 헤더로 확인한다. 429는 `Retry-After`만큼 대기한다.

## KIS와 함께 쓸 때 확인할 점

- 🟠 KIS 쪽 사양은 이번 조사 범위 밖이다. 아래 항목은 토스 문서에서 읽은 제약이며, KIS와의 우열은 판단하지 않았다.
- 토큰이 클라이언트당 1개라서, 같은 키를 쓰는 프로세스가 둘이면 서로 토큰을 무효화한다. 토큰 발급 지점을 `collector-api`나 `trading-api` 중 하나로 모아야 한다.
- 웹소켓 동시 연결이 2개뿐이고 구독이 연결당 100건이다. 전 종목 시세 수집에는 맞지 않고, 관심 종목 단위 구독에 맞는다.
- 시세 채널이 LOSSY라서 체결 틱을 하나도 빠뜨리면 안 되는 용도에는 쓰지 못한다. 현재가 갱신과 호가 표시 정도에 맞는다.
- 국내 시세가 통합(KRX+NXT)만 나와서 거래소별 호가가 필요하면 쓸 수 없다.
- 개요 문서에는 계좌, 자산, 주문 조회 중심의 API만 보인다. 체결 내역 전용 조회, 일별 손익, 입출금 같은 API는 목록에 없다.
- 허용 IP 등록이 필수라서 로컬 개발과 서버 환경의 IP를 각각 등록해야 하고, 회선이 바뀌면 403이 난다.
- 주문은 실거래라서 `trading-api`의 LOCAL 모의 체결과 섞이지 않게 라우팅 경계를 먼저 설계해야 한다(설계 변경이므로 사용자 승인 필요).

## 개별 API 스키마(step 5~7 범위)

출처: 공식 명세 `openapi.json` 1.2.28(2026-11-15 확인). 필드 전체는 명세가 원본이고, 여기는 이 기능이 쓰는 것만 줄였다. `*`는 필수.

| API | 요청 | 응답(성공 시 `result` 아래) |
|---|---|---|
| `POST /oauth2/token` | `application/x-www-form-urlencoded`: `grant_type*`(`client_credentials`만), `client_id*`, `client_secret*` | 최상위(봉투 없음): `access_token*`(JWT), `token_type*`(`Bearer`), `expires_in*`(int64, 만료까지 남은 초. 값의 크기는 명세에 없음) |
| `GET /api/v1/accounts` | 토큰만 | `result[]`: `accountNo*`, `accountSeq*`(int64, 계좌 헤더에 넣는 값), `accountType*`(`BROKERAGE` 등, 현재 `BROKERAGE`만 지원) |
| `GET /api/v1/holdings` | 헤더 `X-Tossinvest-Account*`(accountSeq), 선택 `symbol` | `result`: 합계 `totalPurchaseAmount`·`marketValue.amount`·`marketValue.amountAfterCost`·`profitLoss.amount`·`profitLoss.amountAfterCost`·`dailyProfitLoss.amount`는 통화별 `{krw*, usd(없으면 null)}`, 손익률 `rate`는 소수비율 문자열(0.1516 = 15.16%). `items[]`: `symbol*`, `name*`, `marketCountry*`(KR\|US), `currency*`(KRW\|USD), `quantity*`, `lastPrice*`, `averagePurchasePrice*`, `marketValue{purchaseAmount, amount, amountAfterCost}`, `profitLoss{amount, amountAfterCost, rate, rateAfterCost}`, `dailyProfitLoss{amount, rate}`, `cost{commission*, tax(없으면 null)}` |
| `GET /api/v1/prices` | `symbols*`(콤마 구분, 최대 200) | `result[]`: `symbol*`, `timestamp`(date-time, 없으면 null), `lastPrice*`, `currency*` |

이 기능에 바로 걸리는 사실:
- 금액·수량·가격·비율은 모두 **decimal 문자열**이다. `BigDecimal(String)`으로 바로 읽는다(FLAG-06 확정, 부동소수 거치지 않음).
- `marketCountry`·`currency` enum은 명세가 «모르는 값을 허용하도록 구현»하라고 적었다. 모르는 값에서 역직렬화가 터지지 않게 한다.
- 토큰 응답만 `result` 봉투가 없다. 나머지는 성공 시 `result`, 실패 시 `error{requestId, code, message, data}` 봉투.
- `expires_in` 크기는 명세에 없어 실제 응답으로 확인한다(FLAG-16의 TTL 계산이 이 값에 걸림).

## 다음에 필요한 일

1. (2026-11-15 끝) 개별 API 스키마는 공식 `openapi.json`에서 확인했다. 남은 것은 실제 응답으로 `expires_in` 크기를 보는 것뿐이다.
2. 소개 페이지, 이용약관, FAQ를 브라우저로 읽어 신청 조건과 이용 제한을 확인한다.
3. 토스를 시세 소스나 주문 채널로 붙일지 설계를 정해 승인받는다. 그 전에는 코드를 바꾸지 않는다.
