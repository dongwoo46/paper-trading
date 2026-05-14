# Chart Analysis Agent — 기술 명세 (Spec)

> 작성: Quant Planner Pass B (2026-05-11)
> 단일 진실 소스: `decisions.md` (Pass A 확정). 본 spec.md는 구현 가이드.
> Worktree: `.worktrees/quant-worker-chart-analysis-agent`

---

## 1. Strategy Overview (전략 목표)

종목 코드만 입력받아 **사전 계산된 7개 윈도우의 차트 분석 결과**를 즉시 반환하고, 인기 종목(TOP 300)에 한해 LLM 자연어 리포트를 함께 제공하는 차트 분석 AI 엔드포인트.

핵심 가치:
- **DB hit 즉시 응답**: 수치 분석은 사전 배치로 PostgreSQL에 적재 → 사용자 요청 시 즉시 응답.
- **LLM 리포트 분리**: SSE 비동기 채널로 별도 노출 (룰 템플릿 폴백 포함).
- **비용 통제**: 콘텐트 해시 비교로 무변동 윈도우 LLM 재호출 스킵, 인기 종목 TOP 300만 자동 LLM 호출.
- **금융 안전**: 모든 가격/confidence 값 `Decimal` 처리, DB는 `NUMERIC`.

대상 사용자: 초보 투자자(어조: 한국어 평문 ~한다 어조, 단정 금지).

배포 대상: Railway MVP (decisions §15).

---

## 2. Multi-window Analysis Spec (7 윈도우)

| # | window | interval | 봉 수 (대략) | 용도 |
|---|--------|----------|--------------|------|
| 1 | 1M     | D        | 22           | 단기 진입/이탈 신호 |
| 2 | 3M     | D        | 65           | 단기 추세 + 패턴 |
| 3 | 6M     | D        | 130          | 중기 추세 + 지지/저항 |
| 4 | 1Y     | D        | 250          | 중장기 흐름 |
| 5 | 1Y     | W        | 52           | 주봉 단기 |
| 6 | 2Y     | W        | 104          | 주봉 중기 |
| 7 | MAX    | W        | 전체         | 장기 구조 |

- 7개 윈도우 모두에 대해 동일한 분석 파이프라인을 실행 → 각 윈도우별로 한 row를 `chart_analysis_result`에 저장.
- 응답 JSON은 `analyses: [...]` 배열에 7개 객체 포함 (decisions §2).
- 데이터 소스: 일봉은 `market_daily_ohlcv`, 주봉은 `market_weekly_ohlcv` (기존 repository 활용 — decisions §6).
- 주봉 정합: 별도 수집(`pykrx_weekly_collector`, `yfinance_weekly_collector`) 이미 존재 → 그대로 사용. 리샘플링 불필요.

---

## 3. Alpha Factors / Indicators 사용

### 3-1. 보조지표 (`pandas-ta` 위임)
- 이동평균: MA20, MA60, MA120
- 모멘텀: RSI(14), MACD(12,26,9), Stochastic(14,3,3)
- 변동성: Bollinger Band(20,2), ATR(14)
- 추세 강도: ADX(14)
- 거래량: OBV, Volume MA20

→ 모두 `pandas-ta` 함수에 위임 (자체 구현 금지).

### 3-2. 자체 구현 알고리즘 (골든 fixture 필수)
- **지지/저항 (`SupportResistanceFinder`)**: `scipy.signal.find_peaks` 기반, 최소 2회 터치 + ATR×0.5 클러스터링.
- **캔들 패턴 (`PatternDetector`, 6종)**: Doji, Hammer, Inverted Hammer, Bullish Engulfing, Bearish Engulfing, Morning/Evening Star.
- **추세 분류 (`TrendClassifier`)**: MA20 vs MA60 정배열 + ADX(>25 추세 / <20 횡보 / 20-25 약추세) + 직전 N봉 HH/LL 구조.
- **Confidence 산정 (`ConfidenceScorer`)**: 룰 기반 가중치 합산 → 0.0~1.0 정규화. 환경변수 임계값 (0.7 강신호 / 0.4 약신호).

### 3-3. Recommendation 매핑 (decisions §3)
- `STRONG_BUY` / `BUY` / `HOLD` / `SELL` / `STRONG_SELL` (5단계 enum)
- 임계값 환경변수화: `CHART_ANALYSIS_CONFIDENCE_BUY_THRESHOLD`, `CHART_ANALYSIS_CONFIDENCE_STRONG_THRESHOLD`.

---

## 4. Trade-offs (decisions 발췌 요약)

| 항목 | 선택 | 대안 (기각) | 사유 |
|------|------|-------------|------|
| 보조지표 라이브러리 | `pandas-ta` | TA-Lib / 자체 | 설치 간편, 충분한 커버리지 (§4) |
| 지지/저항 | `scipy.signal.find_peaks` | ZigZag / Fractal | 결정론적, 골든 fixture 작성 용이 (§4) |
| LLM provider | LangChain Ollama 어댑터 + 자체 포트 | OpenAI 직접 호출 | Provider 교체 가능, 도메인 LangChain 무지 (§5) |
| LLM 폴백 | 룰 기반 템플릿 1회 | 1.5b 모델 폴백 | 모델 폴백 비결정성 + 보존 차원 (§5) |
| Recommendation 단계 | 5단계 enum | 3단계 / 회귀값 | UX 명확성 (§3) |
| 갱신 트리거 | 데이터 수집 완료 후 chain | 시각 기반 cron | 데이터 일관성 (§6) |
| 무변동 처리 | 콘텐트 해시 비교 스킵 | 무조건 재계산 | LLM 비용 대폭 절감 (§7) |
| 비인기 종목 LLM | 사용자 요청 큐 + 운영자 수동 | 자동 LLM 호출 | 비용 통제 (§9) |
| 사용자 응답 | DB hit 즉시 + 프론트 가짜 진행 | 백엔드 인공 지연 | 백엔드 단순화 (§11) |
| Numeric 안전 | `Decimal` 전체 적용 | `float` | 금융 안전 규칙 (§18) |

---

## 5. Output Schema (계층 응답 JSON)

### 5-1. `POST /chart-analysis/{symbol}` 응답 (수치 분석 7 윈도우)
```
{
  "symbol": "005930",
  "analyses": [
    {
      "window": "1M",
      "interval": "D",
      "computed_at": "2026-05-10T08:00:00Z",
      "snapshot_hash": "sha256...",
      "summary": {
        "recommendation": "BUY",
        "confidence": "0.612"
      },
      "levels": {
        "supports": ["68500", "67200"],
        "resistances": ["71200", "72500"],
        "entry": "69400",
        "stop_loss": "67100",
        "target": "72500",
        "risk_reward": "1.35"
      },
      "technical": {
        "trend": {
          "direction": "uptrend",
          "strength": "medium",
          "ma_alignment": "MA20>MA60",
          "adx": "23.4",
          "hh_ll_structure": "HH"
        },
        "patterns": [
          { "type": "Hammer", "index": 18, "date": "2026-05-09" }
        ],
        "indicator_signals": [
          { "name": "RSI", "value": "58.2", "interpretation": "neutral_bullish" },
          { "name": "MACD", "value": "120.4", "interpretation": "golden_cross_3d" }
        ]
      },
      "volume": {
        "trend": "increasing",
        "spike_detected": true,
        "avg_ratio": "1.42"
      },
      "report": { "status": "available", "narrative": null }
    },
    /* 6 more windows: 3M-D, 6M-D, 1Y-D, 1Y-W, 2Y-W, MAX-W */
  ]
}
```
- 모든 수치 값은 string(Decimal 직렬화) — float 사용 금지.
- `report.narrative`는 항상 null (별도 SSE 엔드포인트에서 조회).
- `report.status`: `available` (LLM 또는 룰 템플릿 존재) / `none` (없음) / `pending` (큐 등록됨).

### 5-2. `POST /chart-analysis/{symbol}/report` 응답 (SSE 스트림)
- Media type: `text/event-stream`
- 이벤트:
  - `event: status\ndata: {"window":"3M","interval":"D","stage":"pending|running|done|failed"}`
  - `event: report\ndata: {"window":"3M","interval":"D","narrative":{...5섹션...},"source":"llm_primary|rule_template"}`
  - `event: end\ndata: {}`

### 5-3. `POST /chart-analysis/request-llm-report` 요청/응답
```
Request:  { "symbol": "035720", "window": "3M", "interval": "D" }
Response: { "status": "queued", "queue_position": 12, "estimated_wait": "24h" }
```

---

## 6. LLM Integration Spec

### 6-1. 도메인 포트 (LangChain 무지)
- `LlmReportGenerator` (Protocol):
  - `def generate(snapshot: ChartSnapshot, result: ChartAnalysisResult) -> NarrativeReport`
- 도메인은 `langchain*` import 금지.

### 6-2. 어댑터 구현
- `LangChainOllamaReportGenerator` (`infrastructure/`)
  - `langchain_ollama.ChatOllama(model=os.getenv("CHART_ANALYSIS_LLM_MODEL", "qwen2.5:7b"), base_url=..., timeout=13s)`
  - `langchain_core.output_parsers.PydanticOutputParser(pydantic_object=NarrativeReportSchema)`
  - 프롬프트: 시스템(분석 가이드 + 톤) + 사용자(수치 분석 JSON dump)
  - 스키마 위반 → 1회 재시도 → 실패 시 룰 템플릿으로 위임.
- `RuleTemplateReportGenerator` (폴백)
  - 5섹션 한국어 템플릿 문자열에 수치 값 치환.
  - 결정론적, 외부 호출 없음.

### 6-3. NarrativeReport 스키마 (5섹션)
| 필드 | 길이(자) | 내용 |
|------|---------|------|
| `trend_section` | 100-180 | 추세 분석 + 근거 (HH/LL, MA 정배열, ADX) |
| `support_resistance_section` | 80-150 | 핵심 레벨 + 왜 이 가격대인지 |
| `entry_plan_section` | 80-160 | 진입/손절/목표가 + 이유 |
| `signal_evidence_section` | 100-180 | 각 신호별 근거 (RSI/거래량/패턴) |
| `risk_section` | 60-120 | 리스크 요인 |

총합 500-800자 (한국어).

### 6-4. 타임아웃 정책
- 외부(엔드포인트 전체): 25초 — `CHART_ANALYSIS_LLM_TIMEOUT_S`
- 내부(LLM 호출): 13초 — `CHART_ANALYSIS_LLM_INNER_TIMEOUT_S`
- 초과 시: 룰 템플릿 폴백 후 정상 응답.

### 6-5. 캐시
- Redis 결과 캐시 TTL: `CHART_ANALYSIS_CACHE_TTL_S=3600` (1시간)
- Redis Job 저장 TTL: `CHART_ANALYSIS_JOB_TTL_S=600` (10분, SSE 진행 상태)

---

## 7. Precompute Pipeline Spec

### 7-1. 트리거 체인 (decisions §6)
```
[국장 일봉 수집 잡 완료] → 국장 수치 분석 배치 → [국장 TOP N 종목 LLM 배치]
[미장 일봉 수집 잡 완료] → 미장 수치 분석 배치 → [미장 TOP N 종목 LLM 배치]
[주봉 수집 잡 완료] → 주봉 윈도우 분석 배치
```
- 기존 `src/jobs/batch_schedule.py` APScheduler에 후속 잡으로 chain.
- 잡 의존성: APScheduler의 `EVENT_JOB_EXECUTED` 리스너 또는 명시적 `next_run_time` 트리거.

### 7-2. 콘텐트 해시 갱신 (decisions §7)
- 입력 해시 = sha256(JSON.dumps({"last_n_candles_ohlcv": [...], "key_indicators": {...}})).
- N = max(window의 봉 수, 60) (충분한 검출 범위).
- 절차:
  1. 윈도우별 OHLCV + 핵심 보조지표 조회.
  2. 해시 계산 → 기존 row `snapshot_hash`와 비교.
  3. 동일 → `numeric_computed_at` 만 갱신, LLM 호출 스킵.
  4. 다름 → 수치 분석 재실행 + (TOP 300 포함 시) LLM 재호출.

### 7-3. 인기 종목 TOP 300 (decisions §8)
- 주 1회 (월요일 04:00 KST) 갱신 잡: `popular_symbols_refresh_job`.
- 선정 기준: `score = 0.5 * z(market_cap) + 0.5 * z(60d_avg_amount)`.
- 미장/국장 통합 → score 상위 300.
- 결과를 `popular_symbols` 테이블에 upsert (이전 row 전체 교체 — TRUNCATE+INSERT 트랜잭션).

### 7-4. 큐 처리 스크립트 (decisions §9)
- 경로: `backend/quant-worker/scripts/process_llm_request_queue.py`
- 실행: `python -m scripts.process_llm_request_queue [--limit N]`
- 동작:
  1. `analysis_request_queue` 에서 `status='pending'` row를 `requested_at ASC` 정렬로 N개 fetch.
  2. row 별 LLM 호출 → 성공 시 `chart_analysis_result.llm_report` 업데이트 + `status='completed'`.
  3. 실패 시 `status='failed'`, `processed_at` 기록.

---

## 8. DB Schema (decisions §12)

### 8-1. `chart_analysis_result`
- PK: `(symbol, window, interval)`
- 컬럼: symbol VARCHAR(20), window VARCHAR(10), interval VARCHAR(5), snapshot_hash VARCHAR(64), recommendation VARCHAR(15), confidence NUMERIC(4,3), levels JSONB, trend JSONB, patterns JSONB, indicator_signals JSONB, volume_analysis JSONB, llm_report JSONB nullable, llm_report_source VARCHAR(20), numeric_computed_at TIMESTAMPTZ, llm_computed_at TIMESTAMPTZ nullable, created_at, updated_at.
- 인덱스: `(symbol)`, `(numeric_computed_at)`.

### 8-2. `analysis_request_queue`
- PK: `id BIGSERIAL`
- 컬럼: symbol, window, interval, status VARCHAR(20), requested_count INT default 1, requested_at TIMESTAMPTZ, processed_at TIMESTAMPTZ nullable.
- 부분 유니크: `UNIQUE (symbol, window, interval) WHERE status IN ('pending','processing')`.
- 인덱스: `(status, requested_at)`.

### 8-3. `popular_symbols`
- PK: `symbol VARCHAR(20)`
- 컬럼: market VARCHAR(10) ('KRX'|'US'), rank INT, market_cap NUMERIC(24,0), avg_volume NUMERIC(24,0), score NUMERIC(10,6), updated_at TIMESTAMPTZ.

### 8-4. 마이그레이션
- 파일: `backend/quant-worker/src/migrations/V2__create_chart_analysis_tables.sql`
- 모든 컬럼 NOT NULL 명시(가능한 경우). `Decimal` 매핑 컬럼은 NUMERIC.

---

## 9. SSE 비동기 리포트 흐름 (decisions §10)

```
Client                              Server (FastAPI)             Redis      DB
  | POST .../report                    |                            |          |
  |----------------------------------->|                            |          |
  |                                    | GET cache by snapshot_hash |          |
  |                                    |--------------------------->|          |
  |                                    |<--cache miss---------------|          |
  |                                    | SELECT chart_analysis_result          |
  |                                    |-------------------------------------->|
  |                                    |<--row found (llm_report exists)-------|
  |   event: status (running)          |                            |          |
  |<-----------------------------------|                            |          |
  |   event: report (narrative)        |                            |          |
  |<-----------------------------------|                            |          |
  |   event: end                       |                            |          |
  |<-----------------------------------|                            |          |
```

- LLM 리포트 부재 시: `event: status (none)` → `event: end`. 사용자는 별도 `/request-llm-report`로 큐 등록.
- 동일 snapshot_hash 동시 요청: Redis Job lock으로 dedup (`SETNX key TTL=600`).

---

## 10. Fake Real-time UX (decisions §11)

- 백엔드 변경 없음. 프론트엔드(`trading-web`) 책임.
- 본 phase 범위 외이지만 API 계약(즉시 응답)을 보장하여 프론트 구현을 가능하게 함.

---

## 11. DDD Model Summary

### 11-1. Aggregate Roots
| 이름 | 책임 | 식별자 |
|------|------|--------|
| `ChartSnapshot` | 분석 입력 단위 (OHLCV + 지표 + 윈도우 메타) | (symbol, window, interval, snapshot_hash) |
| `ChartAnalysisResult` | 분석 결과 단위 (trend/levels/recommendation/report) | (symbol, window, interval) |

### 11-2. Value Objects (불변)
- `Candle(date, open, high, low, close, volume)` — 모든 가격 `Decimal`.
- `IndicatorSet(ma20, ma60, ma120, rsi14, macd, macd_signal, macd_hist, bb_upper, bb_lower, atr14, adx14, stoch_k, stoch_d, obv, volume_ma20)` — 모든 값 `Decimal`.
- `TrendAnalysis(direction: Direction enum, strength: Strength enum, ma_alignment: str, adx: Decimal, hh_ll_structure: str)`
- `LevelSet(supports: list[Decimal], resistances: list[Decimal])`
- `TradePlan(entry: Decimal, stop_loss: Decimal, target: Decimal, risk_reward: Decimal)`
- `CandlePattern(type: PatternType enum, index: int, date: date)`
- `IndicatorSignal(name: str, value: Decimal, interpretation: str)`
- `VolumeAnalysis(trend: str, spike_detected: bool, avg_ratio: Decimal)`
- `Recommendation(grade: Grade enum, confidence: Decimal)`
- `NarrativeReport(trend_section, support_resistance_section, entry_plan_section, signal_evidence_section, risk_section, source: ReportSource)`

### 11-3. Domain Ports (Protocol)
- `OhlcvRepository.find_window(symbol, window, interval) -> list[Candle]`
- `ChartAnalysisRepository.upsert(result: ChartAnalysisResult)`, `.find(symbol) -> list[ChartAnalysisResult]`
- `AnalysisRequestQueueRepository.enqueue(...)`, `.fetch_pending(limit) -> list[QueueItem]`, `.mark_processed(id, status)`
- `LlmReportGenerator.generate(snapshot, result) -> NarrativeReport`
- `IndicatorCalculator`, `SupportResistanceFinder`, `PatternDetector`, `TrendClassifier`, `ConfidenceScorer` — 도메인 서비스 또는 인프라 (분리 시 인프라).

### 11-4. Application Services
- `AnalyzeChartService` — 단일 종목 7 윈도우 분석 수행/조회 코디네이션 (Read-Through 캐시).
- `GenerateReportService` — SSE 스트림 + Redis Job + LLM 어댑터 호출.
- `PrecomputePipelineService` — 시장별 배치 (수치 + LLM 체인).
- `ProcessRequestQueueService` — 큐 워커.

---

## 12. Test Strategy (decisions §16)

| 영역 | 방식 | 위치 |
|------|------|------|
| 보조지표 | pandas-ta 신뢰 — 호출만 확인 | `tests/unit/chart_analysis/test_indicator_calculator.py` |
| 지지/저항 | 골든 fixture | `tests/fixtures/chart_analysis/support_resistance/*.json` + `tests/unit/...` |
| 캔들 패턴 | 골든 fixture (패턴별 input/expected) | `tests/fixtures/chart_analysis/patterns/*.json` |
| 추세 분류 | 골든 fixture | `tests/fixtures/chart_analysis/trend/*.json` |
| Confidence | 단위 (룰 가중치 표) | `tests/unit/.../test_confidence_scorer.py` |
| LLM 어댑터 | `LlmReportGenerator` fake DI | `tests/unit/.../test_generate_report_service.py` |
| 룰 템플릿 폴백 | 단위 | `tests/unit/.../test_rule_template_report_generator.py` |
| 큐 등록/조회 | 단위 (repository inmem fake) | `tests/unit/.../test_request_queue.py` |
| SSE 엔드포인트 | `httpx.AsyncClient + ASGI transport` | `tests/integration/test_chart_analysis_sse.py` (마커 `integration`) |
| 실제 Ollama 호출 | `@pytest.mark.integration` (CI skip) | `tests/integration/test_langchain_ollama_smoke.py` |
| 콘텐트 해시 | 단위 (해시 결정성) | `tests/unit/.../test_snapshot_hash.py` |

TDD 순서: Domain → Infrastructure → Application → API.

---

## 13-A. Slack 알림 (신규 추가)

### 알림 트리거
| 이벤트 | 메서드 | 내용 |
|--------|--------|------|
| LLM 리포트 생성 성공 | `notify_analysis_success(symbol, window, source)` | 종목/윈도우/source(llm_primary\|rule_template) |
| LLM 리포트 생성 실패 | `notify_analysis_failure(symbol, window, error)` | 종목/윈도우/에러 메시지 |
| 배치 파이프라인 완료 | `notify_batch_completed(market, success_count, failed_count)` | 시장/성공·실패 종목 수 |

### 구현
- 클래스: `SlackWebhookNotifier` (`infrastructure/slack_notifier.py`)
- 환경변수: `SLACK_WEBHOOK_URL`, `SLACK_NOTIFICATIONS_ENABLED` (기존 quant-worker 패턴)
- 알림 실패 → 로그만, 예외 전파 금지 (메인 플로우 차단 금지)
- `SLACK_NOTIFICATIONS_ENABLED=false` → no-op

---

## 13. Environment Variables (decisions §17)

```
OLLAMA_BASE_URL                              = http://ollama:11434
CHART_ANALYSIS_LLM_MODEL                     = qwen2.5:7b
CHART_ANALYSIS_LLM_TIMEOUT_S                 = 25
CHART_ANALYSIS_LLM_INNER_TIMEOUT_S           = 13
CHART_ANALYSIS_CACHE_TTL_S                   = 3600
CHART_ANALYSIS_JOB_TTL_S                     = 600
REDIS_HOST                                   = redis
REDIS_PORT                                   = 6379
CHART_ANALYSIS_POPULAR_TOP_N                 = 300
CHART_ANALYSIS_CONFIDENCE_BUY_THRESHOLD      = 0.4
CHART_ANALYSIS_CONFIDENCE_STRONG_THRESHOLD   = 0.7
SLACK_WEBHOOK_URL                            = (기존 quant-worker 공유)
SLACK_NOTIFICATIONS_ENABLED                  = true
```

신규 `requirements.txt` 추가:
- `pandas-ta==0.3.14b0`
- `scipy==1.13.1`
- `langchain==0.3.7`
- `langchain-ollama==0.2.0`
- `langchain-core==0.3.15`
- `pydantic==2.9.2`
- `sqlalchemy==2.0.36`
- `alembic==1.13.3` (선택, 본 phase는 V2 raw SQL 마이그레이션 사용)
- `apscheduler==3.10.4`
- `redis==5.1.1`
- `httpx==0.27.2` (테스트용)
- `sse-starlette==2.1.3` (또는 FastAPI `EventSourceResponse` 자체 구현)

---

## 14. Phase Step Breakdown (요약)

| Step | 담당 | 범위 |
|------|------|------|
| 1 | quant-planner | Pass A + Pass B (본 spec.md) |
| 2 | quant-dev | Domain Layer (Aggregates + VO + Ports) |
| 3 | quant-dev | Infrastructure Calculations (5 계산기 + 골든 fixture) |
| 4 | quant-dev | Persistence + LLM 어댑터 + Redis |
| 5 | quant-dev | FastAPI Endpoints + Application Services + SSE |
| 6 | quant-dev | Precompute Pipeline + 큐 처리 + APScheduler |
| 7 | test-engineer | 단위 + 골든 + 통합 마커 검증 |
| 8 | code-reviewer | 코드/보안/도메인/금융 안전 리뷰 |
| 9 | orchestrator | Cleanup + PR (orchestrator 직접) |

Total: 9 steps.

---

## 15. Acceptance Summary (전체 phase 완료 조건)

- `POST /chart-analysis/{symbol}` 가 7 윈도우 결과를 즉시(p95 < 500ms) 반환.
- `POST /chart-analysis/{symbol}/report` SSE가 LLM 또는 룰 템플릿 리포트를 5섹션 Pydantic 스키마로 반환.
- `POST /chart-analysis/request-llm-report` 가 큐에 등록 (중복 시 requested_count 증가).
- 콘텐트 해시 비교로 무변동 윈도우 LLM 재호출 스킵 (로그 검증).
- 골든 fixture 100% 통과 (지지/저항, 패턴, 추세).
- 모든 가격/confidence `Decimal` 사용. float 사용처 0건.
- `python -m py_compile` 통과, 단위 테스트 통과.
- 환경변수 누락 시 기본값으로 부팅 가능.
