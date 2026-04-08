# .agents — collector-api

## 구조
- `feature/README.md` — 현재 기준 API 인덱스
- `feature/{기능명}.md` — 기능 보고서
- `rule/{이슈}.md` — 재발 방지 기록

---

## 개발 이정표

### 완료
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
- [ ] Redis 최신 시세 키 스키마 표준화
- [ ] WebSocket 재연결 안정성 강화
