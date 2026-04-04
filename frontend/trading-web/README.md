# trading-web

`paper-trading` 통합 프론트엔드입니다.  
현재 `Market Collector` 운영 화면(KIS WS/REST 구독, 심볼 검색)이 포함되어 있습니다.

## 실행

```bash
npm install
npm run dev
```

기본 주소:
- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080` (Vite proxy `/api`)

## 주요 기능

- KIS WebSocket 구독 목록 조회/추가/삭제
- KIS REST watchlist 조회/추가/삭제
- `paper/live` 모드 분리 표시
- 국내 주식 심볼 검색 (`/api/symbols/kr`)

## 환경변수

백엔드가 다른 주소면:

```bash
VITE_API_BASE_URL=http://your-host:8080
```

