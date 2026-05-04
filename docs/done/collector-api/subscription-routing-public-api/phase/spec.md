# subscription-routing-public-api Spec

## 1) Goal
`front/subscription-routing-ui` 차단 해제를 위해 `collector-api`의 외부 구독 라우팅 API 계약을 고정한다.

고정 대상:
- favorites CRUD
- strategy-priority symbol CRUD
- routing status 조회

## 2) Boundary and Ownership
외부(운영 UI)용:
- `/api/subscriptions/favorites`
- `/api/subscriptions/strategy-symbols`
- `/api/subscriptions/routing-status`
- `/api/subscriptions/status` (모니터링 전용, 기존 유지)

기존 외부 수동 선택용(현재 존재):
- `/api/kis/symbols/subscriptions` (manual add/remove)

내부 서비스 전용(외부 금지):
- `/api/internal/subscriptions/{ticker}`

경계 원칙:
- 운영자 수동 선택은 `/api/kis/symbols/subscriptions`.
- 정책 기반 목록(즐겨찾기/전략우선)과 정책 반영 결과 조회는 `/api/subscriptions/*`.
- `/api/internal/*`는 trading-api 연동 내부 호출만 허용.

## 3) Normalization and Validation Rules
### mode
- 허용값: `paper`, `live`
- 입력 정규화: trim + lowercase 후 검증
- 실패 시: `invalid_mode`

### channel
- 허용값: `ws`, `rest`
- 입력 정규화: trim + lowercase 후 검증
- 실패 시: `invalid_channel`

### symbol
- 입력 정규화: trim + uppercase
- 공백/빈값 금지
- 패턴: `^[0-9A-Z._-]{1,20}$`
- 실패 시: `invalid_symbol`

## 4) Error and Idempotency Contract
모든 write API는 HTTP 200 + `status` 필드 기반 도메인 결과를 반환한다(기존 collector-api 패턴 유지).

공통 status:
- `added`: 신규 추가
- `removed`: 삭제 성공
- `already_exists`: 이미 존재(멱등 성공)
- `not_found`: 삭제 대상 없음(멱등 성공)
- `invalid_input`: 입력 검증 실패
- `invalid_mode`
- `invalid_channel`
- `invalid_symbol`
- `internal_error`: 비정상 내부 오류

멱등성:
- 같은 리소스 중복 POST는 `already_exists`.
- 존재하지 않는 리소스 DELETE는 `not_found`.
- 멱등 결과도 성공 처리하며 클라이언트 재시도 가능.

## 5) API Spec

### 5.1 Favorites

#### GET `/api/subscriptions/favorites?mode={mode}&channel={channel}`
Response 200:
```json
{
  "mode": "paper",
  "channel": "ws",
  "items": ["005930", "000660"],
  "returnedCount": 2,
  "status": "ok"
}
```

#### POST `/api/subscriptions/favorites`
Request:
```json
{
  "mode": "paper",
  "channel": "ws",
  "symbol": "005930"
}
```
Response 200:
```json
{
  "status": "added",
  "mode": "paper",
  "channel": "ws",
  "symbol": "005930",
  "totalSelected": 5
}
```

#### DELETE `/api/subscriptions/favorites`
Request:
```json
{
  "mode": "paper",
  "channel": "ws",
  "symbol": "005930"
}
```
Response 200:
```json
{
  "status": "removed",
  "mode": "paper",
  "channel": "ws",
  "symbol": "005930",
  "totalSelected": 4
}
```

### 5.2 Strategy Priority Symbols

#### GET `/api/subscriptions/strategy-symbols?mode={mode}`
Response 200:
```json
{
  "mode": "paper",
  "items": ["005930", "035420"],
  "returnedCount": 2,
  "status": "ok"
}
```

#### POST `/api/subscriptions/strategy-symbols`
Request:
```json
{
  "mode": "paper",
  "symbol": "035420"
}
```
Response 200:
```json
{
  "status": "added",
  "mode": "paper",
  "symbol": "035420",
  "totalSelected": 3
}
```

#### DELETE `/api/subscriptions/strategy-symbols`
Request:
```json
{
  "mode": "paper",
  "symbol": "035420"
}
```
Response 200:
```json
{
  "status": "removed",
  "mode": "paper",
  "symbol": "035420",
  "totalSelected": 2
}
```

### 5.3 Routing Status

#### GET `/api/subscriptions/routing-status?mode={mode}`
Response 200:
```json
{
  "generatedAt": "2026-05-05T10:15:30Z",
  "mode": "paper",
  "ws": {
    "slotUsed": 38,
    "slotMax": 40,
    "symbols": ["005930", "000660"]
  },
  "rest": {
    "symbols": ["035420", "251340"]
  },
  "sources": {
    "manual": ["005930"],
    "favorites": ["000660"],
    "strategyPriority": ["035420"]
  },
  "status": "ok"
}
```

필드 목적:
- `sources`: 운영 UI가 정책 반영 경로(수동/즐겨찾기/전략우선)를 검증하는 최소 데이터.
- `ws/rest`: 실제 배치 결과 검증 데이터.

## 6) Compatibility Notes
- 기존 `/api/subscriptions/status`는 모니터링 대시보드(`subscription-monitor-ui`) 전용으로 유지한다.
- 신규 `/api/subscriptions/routing-status`는 write-ops 검증용 최소 contract를 제공한다.
- 기존 `/api/kis/symbols/subscriptions` 수동 제어 API는 유지하되, favorites/strategy 정책 API와 역할 중복을 만들지 않는다.

## 7) Done Criteria for Unblocking Front
`front/subscription-routing-ui` 차단 해제 조건:
1. 본 문서의 9개 엔드포인트(3개 리소스 x GET/POST/DELETE 또는 GET 전용) 계약 고정
2. status code 문자열과 입력 검증 규칙 고정
3. `routing-status`에 `sources` + `ws/rest` 배치 결과 포함
4. internal API(`/api/internal/*`)와 외부 API 경계 고정

본 조건 충족 시 front step-2는 blocked-safe가 아닌 full-scope 구현으로 진행 가능.
