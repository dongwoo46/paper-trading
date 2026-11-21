# trading-web

모의투자·알고리즘 트레이딩 운영 대시보드(React · TypeScript · Vite · shadcn/ui Base Nova). 지침은 `CLAUDE.md`, UI 규칙은 `docs/UI_GUIDE.md` 입니다.

## 실행

```bash
npm install
npm run dev          # 개발 서버
npm run build        # 타입 검사 + 번들
npm run lint
npm test -- --run
```

Node 20 이상(CI 는 20).

## 개발 서버 프록시

| 접두사 | 대상 | 환경 변수(기본값) |
|---|---|---|
| `/api/v1` · `/api/accounts` · `/api/kis/account` · `/api/positions` · `/api/trading-journals` · `/api/operations` · `/api/tax-summaries` | trading-api | `VITE_TRADING_API_TARGET` (`http://127.0.0.1:8080`) |
| `/api/kis/ws` · `/api/kis/rest` · `/api/kis/symbols` · `/api/upbit` · `/api/fred` · `/api/pykrx` · `/api/yfinance` · `/api/subscriptions` · `/api/symbols` · `/api/market` · `/api/internal/kis` · `/api/internal/subscriptions` | collector-api | `VITE_COLLECTOR_API_TARGET` (`http://127.0.0.1:8081`) |
| `/api-quant/*` (접두사를 떼고 전달) | quant-worker | `VITE_QUANT_API_TARGET` (`http://127.0.0.1:8082`) |

collector-api 를 docker compose 가 아니라 직접 실행하면 `server.port` 설정이 없어 기본 8080 으로 떠서 trading-api 와 겹칩니다. 그때는 다른 포트로 띄우고 `VITE_COLLECTOR_API_TARGET` 을 맞춥니다.
