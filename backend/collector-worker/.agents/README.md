# .agents 운영 가이드 — collector-worker

## 목적
이 폴더는 기능 보고서, 재발 방지 규칙을 관리한다.

## 구조
- `AGENTS.md`: 서비스별 규칙 (항상 먼저 읽기)
- `.agents/feature/`: 기능 정의/변경 보고서
- `.agents/rule/`: 버그/장애 재발 방지 규칙

## 핵심 운영 원칙
- 기능 개발 후 반드시 py_compile 또는 uvicorn 실행으로 검증한다.
- API 추가/수정 시 `.agents/feature/README.md`를 즉시 갱신한다.

---

## 개발 이정표

### 완료된 기능

- [x] pykrx 일별 OHLCV 수집 (2010~현재)
- [x] yfinance 일별 OHLCV 수집 (2010~현재)
- [x] catalog 기반 증분 수집 (watermark 기준)
- [x] collector-api 요청 기반 수집 트리거 (HTTP API)
- [x] DB upsert (ON CONFLICT DO UPDATE)
- [x] Redis 의존성 제거 (일별 수집에서 불필요)

### 다음 작업

- [ ] 수집 실패 종목 재시도 로직 강화
- [ ] 수집 이력 로그 DB 적재
- [ ] trading-api 연동 시 필요한 추가 데이터 포맷 지원