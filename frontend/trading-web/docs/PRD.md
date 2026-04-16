# PRD — trading-web

## 역할
시장 데이터 수집 운영 대시보드. collector-api/trading-api 연동 UI.

## 아키텍처

### 디렉토리 구조
```
src/
├── app/                    전역 초기화, 라우팅, 스타일
├── pages/                  페이지 조합 (로직 최소화)
│   ├── realtime/           KIS / Upbit 실시간 탭
│   ├── history/            pykrx / yfinance 일봉 탭
│   └── macro/              FRED 매크로 페이지
├── features/               도메인별 UI + 로직
│   ├── kis-management/     KisPanel
│   ├── upbit-management/   UpbitPanel
│   ├── catalog-management/ SymbolCatalogPanel
│   └── fred-management/    FredPanel
├── entities/               도메인 타입 정의
│   ├── symbol/
│   └── market/
└── shared/
    ├── api/                API 호출 일원화
    └── ui/                 공통 컴포넌트
```

### 패턴
- FSD (Feature-Sliced Design): pages → features → entities → shared (역방향 금지)
- TanStack React Query: 서버 상태 전담 (useQuery / useMutation)
- 컴포넌트 분리: 큰 패널 → CatalogTable, SelectionChips 등 서브 컴포넌트

### 데이터 흐름
```
사용자 액션
  → feature 컴포넌트 (useMutation)
  → shared/api/ (fetch)
  → collector-api or trading-api
  → React Query 캐시 무효화 → UI 갱신
```

## 핵심 기능 (MVP)
- KIS 구독/관심종목 관리 (WS/REST, paper/live)
- Upbit 구독/카탈로그 관리
- pykrx/yfinance 카탈로그/구독/OHLCV 조회
- FRED 시리즈 구독/관측값 조회
- 소스별 카탈로그 동기화 트리거

## 미구현 (제외)
- trading-api 주문/포지션 UI
- 실시간 차트 (WebSocket 시세 스트리밍)
- 포트폴리오 대시보드
- 전략 등록/모니터링 UI
- 사용자 인증/로그인
- 모바일 최적화
