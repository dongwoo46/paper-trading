# Trading Journal UI

## Core Feature
계좌별 거래 일지를 ticker/기간 기준으로 조회하고, 항목 상세에서 title/content/sentiment를 수정 저장할 수 있는 운영 UI를 제공한다.

## Considerations
- 기존 trading-web 아키텍처(entities/features/pages, React Query)를 유지해 변경 범위를 최소화했다.
- 거래 일지 생성은 서버 책임으로 두고, UI는 조회/수정 플로우에 집중했다.
- 리뷰에서 발견된 API 계약 불일치를 해소하고 테스트로 고정했다.

## Trade-offs
- 목록 인라인 수정 대신 상세 패널 수정 방식을 채택해 폼 상태와 오류 처리를 단순화했다.
- 초기 구현 속도보다 스펙 정합성(엔드포인트/필드/에러 전파) 우선으로 재작업을 수행했다.

## Implementation Approach
- Trading Journal 전용 entity 타입 및 API client를 추가했다.
- Filter/List/Detail 패널을 분리해 조회/선택/수정 책임을 명확히 했다.
- 라우트(`/trading-journals`)와 사이드바 메뉴를 연결해 접근 경로를 통합했다.
- 저장 성공 시 상세/목록 쿼리 무효화를 통해 화면 상태를 동기화했다.

## Workflow
진입 -> 필터 기반 목록 조회 -> 항목 선택 상세 조회 -> title/content/sentiment 수정 -> 저장 성공 시 목록/상세 재조회.

## Key APIs
- GET `/api/trading-journals?accountId=...&ticker=...&from=...&to=...&page=...&size=...` — 목록 조회
- GET `/api/trading-journals/{journalId}` — 상세 조회
- PATCH `/api/trading-journals/{journalId}` — title/content/sentiment 수정

## DB
- 신규 DB 스키마 변경 없음 (`trading_journals` 기존 테이블 사용).

## Completed / PR
2026-05-04 / #TBD
