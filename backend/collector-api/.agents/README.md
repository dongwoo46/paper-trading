# .agents 운영 가이드 — collector-api

## 목적
이 폴더는 코딩 규칙, 기능 보고서, 재발 방지 규칙을 관리한다.

## 구조
- `AGENTS.md`: 전체 공통 규칙 (항상 먼저 읽기)
- `.agents/feature/`: 기능 정의/변경 보고서
- `.agents/rule/`: 버그/장애 재발 방지 규칙
- `.agents/feature/README.md`: 현재 기준 API 인덱스 + 웹 연결 상태

## 핵심 운영 원칙
- 기능 개발 후 검증(컴파일/빌드) 성공을 확인한다.
- API 추가/수정 또는 웹 API 연결 변경이 있으면 `.agents/feature/README.md`를 즉시 갱신한다.
- `.agents/feature/README.md`는 최신 상태만 유지한다.

---

## 개발 이정표

### 완료된 기능

- [x] KIS WebSocket 연동 (구독/해제/실시간 수신)
- [x] KIS REST 관심종목 관리
- [x] Upbit 마켓 카탈로그 + 구독
- [x] pykrx 종목 카탈로그 + 수집 상태 관리
- [x] yfinance 종목 카탈로그 + 수집 상태 관리
- [x] FRED 시리즈 카탈로그 + 구독 + 관측값 스케줄 수집
- [x] 일별 OHLCV 조회 API (pykrx, yfinance)
- [x] trading-web UI 전체 API 연결 완료

### 다음 작업

- [ ] trading-api와의 시세 연동 인터페이스 정의
- [ ] Redis 최신 시세 키 스키마 표준화 (trading-api가 읽을 수 있도록)
- [ ] WebSocket 재연결 안정성 강화