# Trading Journal UI

## Core Feature
계좌별 거래 일지를 조회하고(ticker/기간 필터), 단건 상세를 확인하며, 제목/내용/심리 태그를 수정할 수 있는 운영 UI를 제공한다.

## Considerations
- 기존 `trading-web` 구조(entities/features/pages + React Query)와 동일한 패턴으로 구현해 유지보수 비용을 낮춘다.
- 거래 일지 생성은 서버(`trading-api`) 책임이며, UI는 조회/수정 중심으로 설계한다.
- 금액/수량 표시는 서버 응답 문자열(BigDecimal 직렬화)을 그대로 노출하고 프론트에서 수치 연산을 추가하지 않는다.
- 모바일에서도 사용 가능하도록 목록/상세 패널 전환이 필요하다.

## Trade-offs
- Option A: 목록에서 인라인 수정
  - 장점: 클릭 수 감소
  - 단점: 폼 상태/검증/취소 흐름이 복잡해지고 오입력 위험 증가
- Option B: 상세 패널에서 수정(선택)
  - 장점: 읽기와 편집 맥락 분리, 검증/에러 처리 단순
  - 단점: 상세 진입 1단계 추가

선택: Option B. 목록은 탐색/필터 중심, 상세에서 수정 저장을 수행한다.

## Implementation Approach
- Page Layer: `TradingJournalPage`에서 account/ticker/date filter와 selected journal 상태를 관리한다.
- Entity Layer: trading-journal API client와 타입(`list/detail/update`)을 정의한다.
- Feature Layer:
  - `trading-journal-list`: 목록 테이블 + 빈 상태/로딩/오류
  - `trading-journal-filter`: ticker/기간 필터 컨트롤
  - `trading-journal-editor`: 상세 조회 + 수정 폼 + 저장 액션
- Shared/UI: 라우팅(`/trading-journals`)과 사이드바 메뉴를 추가한다.

## Workflow
1. 사용자가 거래 일지 화면 진입.
2. 기본 파라미터(`accountId`, 최근 30일, ticker optional)로 목록 조회.
3. 사용자가 ticker/기간 필터 변경 시 목록 재조회.
4. 목록 항목 클릭 시 단건 상세 조회.
5. 사용자가 `title/content/sentiment` 수정 후 저장.
6. 저장 성공 시 상세/목록 쿼리 무효화 및 최신 값 반영.

## API
GET /api/trading-journals?accountId={id}&ticker={ticker?}&from={yyyy-mm-dd}&to={yyyy-mm-dd}&page={n}&size={n} — 목록 조회  
Request: `{ accountId: number, ticker?: string, from: string, to: string, page: number, size: number }`  
Response: `{ items: TradingJournalListItem[], page: number, size: number, total: number }`  
Errors: `400` (기간 역전/size 초과), `404` (account 없음)

GET /api/trading-journals/{journalId} — 단건 상세 조회  
Request: path param `{ journalId: number }`  
Response: `{ journalId, accountId, orderId, ticker, journalType, sentiment, title, content, summary, createdAt, updatedAt }`  
Errors: `404` (journal 없음)

PATCH /api/trading-journals/{journalId} — 일지 수정  
Request: `{ title: string, content: string, sentiment: "BULLISH" | "BEARISH" | "NEUTRAL" | "REFLECTIVE" }`  
Response: `{ journalId: number, status: "UPDATED" }`  
Errors: `400` (validation), `404` (journal 없음), `409` (version 충돌)

## DB
UI phase는 DB 변경 없음.
- 기존 `trading_journals` 테이블/인덱스(`account_id+ticker+created_at`)를 필터 UX 기준으로 활용한다.

## Ambiguities To Confirm
1. 기본 조회 기간을 최근 30일로 고정할지, 최근 90일로 할지?
2. 목록에서 `AUTO_*`/`MANUAL_NOTE` 타입 필터가 이번 phase 범위에 포함되는지?
3. `sentiment`를 필수 입력으로 강제할지(현재 API는 nullable 가능)?

