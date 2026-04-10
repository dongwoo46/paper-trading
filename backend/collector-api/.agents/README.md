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

### 다음 작업 (Phase 4 선행 — trading-api 주문 API 위해 필수)
- [ ] Redis Pub/Sub 시세 발행 구현: realtime-quote-pubsub.md
  - RawEventPipeline.publish() 구현 (현재 TODO log.debug 상태)
  - KisRawEventParser: H0STCNT0(체결) 파싱 → QuoteEvent
  - QuoteRedisPublisher: Hash 저장(quote:{ticker}) + Pub/Sub 발행
  - 동적 채널: quote:{ticker} (구독 종목별 자동 생성)
- [ ] WebSocket 재연결 안정성 강화
