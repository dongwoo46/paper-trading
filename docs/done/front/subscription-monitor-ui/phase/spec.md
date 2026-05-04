# subscription-monitor-ui Spec

## 1. Goal and Scope
- Goal: Add a read-only monitoring dashboard for collector subscription health in `RealtimePage > KisPanel` using `GET /api/subscriptions/status`.
- In scope: status fetch/display, mode summary, slot usage, connection/reconnect visibility, ws/rest symbol lists, refresh behavior, loading/error/empty UI.
- Out of scope: any subscription control action (add/remove/toggle), routing policy writes, favorite/strategy writes.

## 2. Boundary with Existing KIS Control UI
- Existing control remains in `KIS 구독 제어` and related catalog/price cards.
- New monitor section is visually separated and labeled `구독 상태 모니터링 (읽기 전용)`.
- No buttons that call POST/DELETE/PATCH for subscriptions are allowed in monitor section.
- Reuse current page layout (`feature-grid` + `SectionCard`) but place monitor cards in independent block after control block.

## 3. API Contract
- Endpoint: `GET /api/subscriptions/status`
- Response shape:
  - `generatedAt: string`
  - `totalWsSlotUsed: number`
  - `totalWsSlotMax: number`
  - `modes: Array<{ mode, connectionStatus, lastConnectedAt, reconnectAttempts, wsSymbols, restSymbols, wsSlotUsed, wsSlotMax }>`

## 4. UI Data Mapping
| API field | UI 위치 | 표시 형식 | 비고 |
|---|---|---|---|
| `generatedAt` | 모니터링 헤더 | `생성 시각` 텍스트 | ISO 문자열 그대로, 추후 포맷 가능 |
| `totalWsSlotUsed` | 전역 요약 카드 | 숫자 | `totalWsSlotMax`와 함께 사용 |
| `totalWsSlotMax` | 전역 요약 카드 | `used / max` | 전역 WS 슬롯 점유율 |
| `modes[].mode` | 모드별 카드 제목 | `paper`/`live` | 고정 2개 가정 금지 |
| `modes[].connectionStatus` | 모드별 상태 배지 | 상태 텍스트 | `CONNECTED` 등 원문 표기 |
| `modes[].lastConnectedAt` | 모드 메타 | 시간 텍스트/`-` | null 허용 |
| `modes[].reconnectAttempts` | 모드 메타 | 정수 | 재연결 누적 횟수 |
| `modes[].wsSlotUsed` | 모드 슬롯 | 정수 | `wsSlotMax`와 함께 |
| `modes[].wsSlotMax` | 모드 슬롯 | `used / max` | 모드별 WS 슬롯 |
| `modes[].wsSymbols` | 모드 상세 표 | 심볼 리스트 + count | 길면 스크롤 |
| `modes[].restSymbols` | 모드 상세 표 | 심볼 리스트 + count | 길면 스크롤 |

## 5. UX/State Rules
- Query key: `['kis','subscription-status']` (or equivalent isolated key).
- Auto refresh: default 10s polling.
- Manual refresh button allowed (read-only behavior 유지).
- Loading: skeleton or `로딩 중` 상태.
- Error: fetch error message + retry CTA.
- Empty: `modes` 비어있으면 안내 문구 표시.

## 6. Test Expectations (for next steps)
- API client parsing test for response fields.
- Monitor component render test for summary/mode cards.
- Read-only boundary test: monitor section contains no mutation trigger.
- Regression smoke: existing KIS control actions still operate.
