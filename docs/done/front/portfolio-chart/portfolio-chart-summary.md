# Portfolio Return Chart

## Core Feature
`/portfolio` 페이지에서 계좌 일별 평가금액 추이와 KOSPI 대비 누적 수익률을 함께 조회/비교할 수 있는 차트 기능을 구현했다.

## Considerations
- 기존 API 계약을 크게 바꾸지 않기 위해 수익률 정규화는 프런트에서 수행했다.
- loading/error/empty 상태와 네비게이션 패턴은 기존 account 페이지와 일관되게 유지했다.
- 벤치마크 데이터 누락(특히 404) 시에도 포트폴리오 단독 차트로 degrade 하도록 보완했다.

## Trade-offs
- 백엔드에서 최종 수익률 시리즈를 제공하는 대신 프런트 정규화 방식을 채택해 phase 독립성과 배포 속도를 우선했다.
- 그 대가로 정규화 규칙(기준일 0%, 결측 skip, 중복 날짜 latest 우선, 0 division 방어)을 테스트로 강하게 고정했다.

## Implementation Approach
- `entities/portfolio`에 포트폴리오/벤치마크 타입 정의.
- `shared/api/portfolioApi`에 일별 잔고/벤치마크 조회 API와 응답 숫자 파싱 구현.
- `features/portfolio-chart/model/normalizeSeries`에 수익률 시리즈 정규화 로직 구현.
- `PortfolioChartPanel`과 `/portfolio` 페이지를 추가하고 `App.tsx`, `Sidebar.tsx`에 라우팅/내비게이션 연결.
- 코드리뷰 MUST FIX 3건(benchmark 404 degrade, 0 division, 중복 날짜 규칙) 재작업 반영 완료.

## Workflow
1. `/portfolio` 진입 후 계좌 목록 조회 및 기본 계좌 선택.
2. 기간(from/to) 기준으로 DailyBalance + KOSPI 벤치마크 조회.
3. 시계열 정렬/정규화 후 평가금액과 누적 수익률을 차트로 렌더.
4. 벤치마크 부재 시 경고를 노출하고 포트폴리오 단독 시리즈로 표시.

## Key APIs
- `GET /api/v1/accounts`
- `GET /api/v1/accounts/{accountId}/daily-balances?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /api/v1/benchmarks/kospi?from=YYYY-MM-DD&to=YYYY-MM-DD`

## Test / Review Result
- Step 2: `npm run test -- portfolio-chart`, `npm run test -- PortfolioChartPage.test.tsx`, `npm run build` PASS
- Step 3: `npm run test -- portfolio-chart`, `npm run test -- account`, `npm run build` PASS
- Step 4: 코드리뷰 FAIL (MUST FIX 3건)
- Step 5: MUST FIX 반영 후 `npm run test -- portfolio-chart`, `npm run test -- PortfolioChartPage.test.tsx`, `npm run build` PASS
- Step 6: 2차 코드리뷰 PASS (`npm run build` PASS)

## Residual Risks
- 백엔드 벤치마크 데이터 지연/결측이 잦은 경우 경고 UI 노출 빈도가 높아질 수 있다.
- 대량 기간 조회 시 브라우저 렌더링 성능은 실제 운영 데이터 규모로 추가 모니터링이 필요하다.

## Commit Message (Korean, Proposal)
`feat(front): 포트폴리오 수익률 차트 페이지 및 수익률 정규화 로직 구현`

## PR Draft
Title: `feat(front): 포트폴리오 수익률 차트 추가 및 벤치마크 결측 대응`

Body:
- 배경
  - 운영 대시보드에서 계좌 성과를 일별 추이와 벤치마크 대비로 함께 확인할 수 있도록 포트폴리오 차트 기능이 필요했다.
- 변경점
  - `/portfolio` 페이지 및 사이드바 메뉴 추가
  - DailyBalance/KOSPI 조회 API 모듈 및 타입 추가
  - 누적 수익률 정규화 로직(기준일 0%, 결측 skip, 0 division 방어, 중복 날짜 latest 우선) 구현
  - benchmark 404 시 포트폴리오 단독 degrade + 경고 UI 처리
- 검증
  - `npm run test -- portfolio-chart`
  - `npm run test -- account`
  - `npm run test -- PortfolioChartPage.test.tsx`
  - `npm run build`
- 리스크
  - 벤치마크 소스 결측 빈도에 따라 사용자 경고 노출이 증가할 수 있음

## Completed / PR
2026-05-03 / #TBD
