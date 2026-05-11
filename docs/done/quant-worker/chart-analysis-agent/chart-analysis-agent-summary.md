# chart-analysis-agent 구현 완료 요약

> 완료일: 2026-05-12
> Branch: feature/quant-worker-chart-analysis-agent
> 테스트: 283/283 통과 | Coverage: 88%

---

## 기능 개요

종목 코드 하나로 **7개 윈도우의 차트 분석 결과**를 즉시 반환하고, 인기 종목(TOP 300)에 한해 LLM 자연어 리포트를 SSE 스트림으로 제공하는 차트 분석 AI 에이전트.

---

## 엔드포인트 3개

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/chart-analysis/{symbol}` | 7 윈도우 수치 분석 즉시 반환 (DB hit) |
| POST | `/chart-analysis/{symbol}/report` | LLM 자연어 리포트 SSE 스트림 |
| POST | `/chart-analysis/request-llm-report` | 비인기 종목 LLM 요청 큐 등록 |

---

## 분석 윈도우 7개

| # | window | interval | 용도 |
|---|--------|----------|------|
| 1 | 1M | D | 단기 진입 신호 |
| 2 | 3M | D | 단기 추세 + 패턴 |
| 3 | 6M | D | 중기 추세 + 지지/저항 |
| 4 | 1Y | D | 중장기 흐름 |
| 5 | 1Y | W | 주봉 단기 |
| 6 | 2Y | W | 주봉 중기 |
| 7 | MAX | W | 장기 구조 |

---

## DB 테이블 3개 (V2 마이그레이션)

- `chart_analysis_result` — PK(symbol, window, interval), 수치+LLM 결과 JSONB
- `analysis_request_queue` — 비인기 종목 LLM 요청 큐
- `popular_symbols` — TOP 300 인기 종목 (주 1회 갱신)

---

## 인프라 계산 모듈 5개

| 클래스 | 역할 |
|--------|------|
| `IndicatorCalculator` | pandas-ta 래퍼 (MA/RSI/MACD/BB/ATR/ADX/Stoch/OBV) |
| `SupportResistanceFinder` | scipy.find_peaks 기반 지지/저항 |
| `PatternDetector` | 캔들 패턴 6종 (Doji/Hammer/Engulfing/Star) |
| `TrendClassifier` | MA 정배열 + ADX + HH/LL 구조 |
| `ConfidenceScorer` | 룰 기반 가중치 합산 → Recommendation |

---

## LLM 통합

- **메인**: `LangChainOllamaReportGenerator` (qwen2.5:7b, 13초 타임아웃, 1회 재시도)
- **폴백**: `RuleTemplateReportGenerator` (한국어 5섹션 결정론적 템플릿)
- **출력**: 5섹션 한국어 리포트 (trend/levels/entry/signal/risk, 500-800자)

---

## Slack 알림

| 트리거 | 메서드 |
|--------|--------|
| LLM 리포트 생성 성공 | `notify_analysis_success(symbol, window, source)` |
| LLM 리포트 생성 실패 | `notify_analysis_failure(symbol, window, error)` |
| 배치 파이프라인 완료 | `notify_batch_completed(market, success, failed)` |

환경변수: `SLACK_WEBHOOK_URL`, `SLACK_NOTIFICATIONS_ENABLED`

---

## APScheduler 잡 4개

1. KRX 일봉 수집 완료 → KRX 차트 분석 파이프라인
2. yfinance 일봉 수집 완료 → 미장 차트 분석 파이프라인
3. 주봉 수집 완료 → 주봉 윈도우 분석
4. 월요일 04:00 KST → 인기 종목 TOP 300 갱신

---

## 환경변수

```
OLLAMA_BASE_URL                          = http://ollama:11434
CHART_ANALYSIS_LLM_MODEL                 = qwen2.5:7b
CHART_ANALYSIS_LLM_TIMEOUT_S             = 25
CHART_ANALYSIS_LLM_INNER_TIMEOUT_S       = 13
CHART_ANALYSIS_CACHE_TTL_S               = 3600
CHART_ANALYSIS_JOB_TTL_S                 = 600
CHART_ANALYSIS_POPULAR_TOP_N             = 300
CHART_ANALYSIS_CONFIDENCE_BUY_THRESHOLD  = 0.4
CHART_ANALYSIS_CONFIDENCE_STRONG_THRESHOLD = 0.7
REDIS_HOST                               = redis
REDIS_PORT                               = 6379
SLACK_WEBHOOK_URL                        = (기존 공유값)
SLACK_NOTIFICATIONS_ENABLED              = true
```

---

## 운영 매뉴얼

### 비인기 종목 LLM 큐 처리 (수동)
```bash
cd backend/quant-worker
python -m scripts.process_llm_request_queue --limit 50
```

### DB 마이그레이션 적용
```bash
docker exec postgres psql -U <user> -d <db> -f src/migrations/V2__create_chart_analysis_tables.sql
```

### FastAPI 서버 실행
```bash
cd backend/quant-worker
uvicorn src.interfaces.api.app:app --host 0.0.0.0 --port 8082
```

---

## 알려진 제한

- TOP 300 외 종목은 LLM 리포트 자동 생성 안 됨 (큐 등록 후 운영자 수동 처리)
- 프론트엔드 가짜 진행 UX는 trading-web 별도 작업
- 통합 테스트(`@pytest.mark.integration`)는 실 PostgreSQL + Ollama 필요 — CI skip 처리
- 주봉 `_is_popular()` 라우터 팩토리에서 현재 항상 `True` 반환 (popular_symbols 테이블 연동 추후 개선)
