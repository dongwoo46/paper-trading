# Market Bars Chart UI

## Core Feature
`collector-api`의 `GET /api/market/bars/{symbol}?interval=1m|5m|10m&limit={n}`를 사용해 종목별 분봉(1m/5m/10m) 히스토리 차트를 조회/시각화하는 운영 UI.

## Scope
- 심볼 입력(예: `005930`, `AAPL`) 후 분봉 히스토리 조회
- interval 탭 전환(`1m`, `5m`, `10m`)
- limit 제어(모바일/데스크탑 공통 동작)
- 로딩/빈 상태/에러 상태 UX 명시
- 모바일 우선 레이아웃 대응

## API Contract
- Method: `GET`
- Endpoint: `/api/market/bars/{symbol}`
- Query:
  - `interval`: `1m | 5m | 10m` (필수)
  - `limit`: 정수 (필수, 상한은 서버 정책 따름)

응답 바 1건 필드:
- `startedAt` (ISO datetime)
- `open`, `high`, `low`, `close`
- `volume`
- `tradeValue`
- `vwap`
- `tickCount`

## API Usage and Error UX Rules
- `interval`은 UI 탭에서만 변경 가능하도록 고정된 enum 사용(임의 문자열 금지)
- `limit`은 UI에서 숫자 범위 제한 적용 후 요청
- `400`(invalid interval/limit): 사용자 입력 오류로 처리, "조회 조건을 확인해 주세요." 노출
- `404`(데이터 없음): 정상 빈 상태로 처리, "해당 조건의 분봉 데이터가 없습니다." 노출
- `5xx`/네트워크 오류: 재시도 가능한 에러 상태로 처리, "일시적인 오류가 발생했습니다. 다시 시도해 주세요." + 재시도 버튼
- 응답 바 배열이 비어 있어도 `404`와 동일한 빈 상태 UX로 통일

## UI Behavior

### 1. Symbol Input
- 상단에 심볼 입력 필드 + 조회 버튼 제공
- 마지막 성공 조회 심볼을 페이지 상태로 유지
- 빈 문자열/공백 입력 시 API 호출 금지, 인라인 안내 문구 표시

### 2. Interval Tabs (1m/5m/10m)
- 기본값 `1m`
- 탭 클릭 시 즉시 재조회
- 활성 탭 시 시각 강조, 비활성 탭은 버튼 스타일
- 탭 전환 중 이전 데이터는 유지하지 않고 로딩 상태 표시(오해 방지)

### 3. Limit Control
- 기본값 `100`
- 옵션형 컨트롤(예: `50 / 100 / 200 / 500`) 사용
- limit 변경 시 즉시 재조회
- 서버 상한 초과 입력 경로를 UI에서 차단

### 4. Chart and Data Panel
- 차트는 OHLC 바(캔들) 우선, 라이브러리 제약 시 라인 차트 대체 가능
- x축: `startedAt`, y축: 가격(`open/high/low/close`)
- 보조 정보(볼륨/틱수)는 하단 요약 또는 툴팁으로 제공

### 5. States
- Loading: 스켈레톤 또는 로더 + "분봉 데이터를 불러오는 중..."
- Empty: 데이터 없음 메시지 + 현재 심볼/interval/limit 표시
- Error: 오류 메시지 + 재시도 버튼
- Success: 차트 렌더 + 마지막 갱신 시각 표기

## Mobile Layout
- 360px 기준에서도 가로 스크롤 없이 상단 컨트롤 2줄 이내 배치
- 1행: 심볼 입력 + 조회 버튼
- 2행: interval 탭 + limit 컨트롤
- 차트 영역 최소 높이 보장(예: 280px 이상)
- 터치 대상(탭/버튼) 최소 40px 높이

## Query/State Model
- Query key: `["market-bars", symbol, interval, limit]`
- `symbol`이 유효할 때만 fetch enabled
- 동일 조건 재조회는 수동 새로고침 버튼 또는 retry 버튼으로 실행
- 클라이언트에서 OHLC 재계산 금지(서버 데이터 그대로 표시)

## Acceptance Anchors for Implementation
- interval 탭 3종 전환 시 각각 올바른 `interval` 값으로 요청됨
- limit 변경 시 요청 파라미터가 즉시 반영됨
- `400/404/5xx`별 UX가 서로 다르게 동작
- 모바일 폭에서 컨트롤/차트가 깨지지 않음
