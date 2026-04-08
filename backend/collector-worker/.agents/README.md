# .agents — collector-worker

## 구조
- `feature/README.md` — 현재 기준 API 인덱스
- `feature/{기능명}.md` — 기능 보고서
- `rule/{이슈}.md` — 재발 방지 기록

---

## 개발 이정표

### 완료
- [x] pykrx 일별 OHLCV 수집 (2010~현재)
- [x] yfinance 일별 OHLCV 수집 (2010~현재)
- [x] catalog 기반 증분 수집 (watermark 기준)
- [x] collector-api 요청 기반 수집 트리거 (HTTP API)
- [x] DB upsert (ON CONFLICT DO UPDATE)
- [x] Redis 의존성 제거

### 다음 작업
- [ ] 수집 실패 종목 재시도 로직 강화
- [ ] 수집 이력 로그 DB 적재
- [ ] trading-api 연동 시 추가 데이터 포맷 지원
