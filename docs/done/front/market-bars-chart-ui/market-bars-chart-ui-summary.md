# market-bars-chart-ui Summary

## 구현 기능 요약
- `Market Bars Chart` 화면을 추가해 심볼별 분봉 히스토리 조회/시각화를 지원했다.
- `interval(1m/5m/10m)` 전환과 `limit` 제어를 UI에서 제공한다.
- 로딩/빈 데이터/오류 상태를 분리해 사용자 피드백을 명확히 했다.

## 신규/수정 파일 목록
- frontend/trading-web/src/shared/api/marketBarsApi.ts
- frontend/trading-web/src/shared/api/marketBarsApi.test.ts
- frontend/trading-web/src/pages/market-bars/ui/MarketBarsChartPage.tsx
- frontend/trading-web/src/pages/market-bars/ui/MarketBarsChartPage.test.tsx
- frontend/trading-web/src/pages/market-bars/ui/MarketBarsChartPage.css
- frontend/trading-web/src/App.tsx
- frontend/trading-web/src/App.test.tsx
- frontend/trading-web/src/shared/ui/Sidebar.tsx
- frontend/trading-web/src/shared/ui/Sidebar.test.tsx
- frontend/trading-web/package-lock.json

## API 연동 요약
- 연동 API: `GET /api/market/bars/{symbol}?interval=...&limit=...`
- 클라이언트에서 심볼/간격/조회개수 파라미터를 조합해 호출하도록 구현했다.
- 응답 데이터를 차트 렌더링 모델로 변환해 표시한다.

## 오류 처리 UX 요약 (400/404/5xx)
- `400`: 잘못된 입력(간격/limit/심볼) 안내 메시지를 표시한다.
- `404`: 조회 가능한 바 데이터가 없음을 빈 상태 메시지로 표시한다.
- `5xx`: 서버 오류 안내와 재시도 유도 메시지를 표시한다.
- 오류/빈 상태가 동시에 보이지 않도록 상태 분기 우선순위를 정리했다.

## 테스트 결과 및 알려진 제한
- 최종 검증:
  - `npm run build` PASS
  - `npm test -- --run` PASS (`111/111`)
- 알려진 제한:
  - 차트 데이터는 API 응답 품질(갭/지연/종목별 데이터 가용성)에 의존한다.
