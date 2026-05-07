# trading-journal-service

## Core Feature
Trading Journal 생성/수정/조회 API와 애플리케이션 서비스 계층을 구현하고 프론트 연동 가능한 계약을 확정했다.

## Considerations
- DDD 경계를 유지해 account-journal 일관성 검증을 서비스에서 수행.
- API 계약 검증(유효성/404/paging/ticker filter)을 통합테스트로 보강.

## Trade-offs
- 리스트 paging 파라미터에서 silent 보정 대신 명시적 400 반환을 선택해 계약 일관성을 우선했다.

## Implementation Approach
- application: Command/Query service + command/query/result 모델 추가.
- persistence: account/ticker + pageable 조회 메서드 추가.
- presentation: TradingJournalController + DTO + 예외 매핑 연결.
- migration: journal 조회 인덱스(account_id, created_at), (account_id, ticker, created_at) 추가.
- tests: 서비스 테스트/컨트롤러 통합테스트 및 회귀 케이스 추가.

## Workflow
Create/Update/List/Detail 요청 -> Controller DTO 검증 -> Application Service -> Repository 조회/저장 -> Response 매핑.

## Key APIs
- POST `/api/trading-journals`
- PATCH `/api/trading-journals/{journalId}`
- GET `/api/trading-journals`
- GET `/api/trading-journals/{journalId}`

## DB
- migration: `V2__trading_journal_indexes.sql`
- indexes:
  - `(account_id, created_at desc)`
  - `(account_id, ticker, created_at desc)`

## Completed / PR
2026-05-07 / PR 미생성 (draft 준비 완료)
