# Chart Analysis Agent — 확정 설계 결정

> Pass A Q&A 완료 시점: 2026-05-11
> 본 문서는 Planner Pass B 입력. spec.md 및 step-2..N.md 생성의 단일 진실 소스.

## 1. 입출력 흐름

- 입력: `POST /chart-analysis/{symbol}` — 종목코드만 (보조지표·OHLCV는 백엔드가 DB에서 사전 계산된 값 조회)
- 응답: 7 윈도우 (일봉 1M/3M/6M/1Y + 주봉 1Y/2Y/MAX) 수치 분석 결과를 DB hit로 즉시 반환
- LLM 리포트: 별도 `POST /chart-analysis/{symbol}/report` — SSE 비동기
- 비인기 종목 LLM 부재 시: 사용자가 큐 등록 → 운영자(개발자)가 수동 처리

## 2. 응답 JSON 구조 (수치 분석)

계층 구조:
```
{
  "symbol": "005930",
  "analyses": [
    {
      "window": "3M",
      "interval": "D",
      "computed_at": "...",
      "snapshot_hash": "...",
      "summary": { "recommendation": "BUY|...", "confidence": 0.0~1.0 },
      "levels": { "supports": [...], "resistances": [...], "entry": ..., "stop_loss": ..., "target": ..., "risk_reward": ... },
      "technical": {
        "trend": { "direction": "uptrend|...", "strength": "weak|medium|strong",
                   "ma_alignment": "...", "adx": ..., "hh_ll_structure": "..." },
        "patterns": [ { "type": "Hammer|...", "index": ..., "date": "..." } ],
        "indicator_signals": [ { "name": "RSI", "value": ..., "interpretation": "..." } ]
      },
      "volume": { "trend": "...", "spike_detected": true|false, "avg_ratio": ... },
      "report": { "status": "available|none|pending", "narrative": null  // lazy }
    },
    ...
  ]
}
```

## 3. recommendation 등급 + confidence 매핑

- 5단계: `STRONG_BUY`, `BUY`, `HOLD`, `SELL`, `STRONG_SELL`
- confidence: 0.0~1.0 (룰 기반 가중치 합산 후 정규화)
- 매핑:
  - long signal + confidence > 0.7 → STRONG_BUY
  - long signal + 0.4 < confidence ≤ 0.7 → BUY
  - |confidence| ≤ 0.3 → HOLD
  - short signal + 0.4 < confidence ≤ 0.7 → SELL
  - short signal + confidence > 0.7 → STRONG_SELL
- 임계값(0.7/0.4)은 환경변수 또는 yaml로 외부화, 백테스트로 추후 튜닝

## 4. 분석 로직

- 보조지표 라이브러리: `pandas-ta`
- 캔들 패턴 (6개): 도지, 망치, 역망치, 강세 엔걸핑, 약세 엔걸핑, 모닝스타/이브닝스타
- 추세 판단: MA 정배열(MA20 vs MA60) + ADX 강도(>25 추세, <20 횡보) + HH/LL 가격 구조
- 지지·저항선: `scipy.signal.find_peaks` 자체 구현
- confidence 산정: 룰 기반 가중치 합산 (결정론적, 외부 yaml/상수 튜닝 가능)

## 5. LLM 통합

- Provider 추상화: 자체 `LlmReportGenerator` 포트 + LangChain Ollama 어댑터 (도메인은 LangChain 무지)
- 메인 모델: `qwen2.5:7b` (CUDA 가속, RTX 4060 Ti로 ~2-3초)
- 폴백: 룰 기반 템플릿 1회 (모델 폴백 미사용 — 1.5b 모델은 보존하되 본 phase에선 미사용)
- 출력 스키마 (Pydantic 강제, 5섹션):
  - trend_section: 추세 분석 + 근거 (HH/LL, MA, ADX)
  - support_resistance_section: 레벨 + 왜 이 가격대인지
  - entry_plan_section: 진입가/손절가/목표가 + 이유
  - signal_evidence_section: 각 신호별 근거 (RSI/거래량/패턴 등)
  - risk_section: 리스크 요인
- 톤 가이드: 한국어 평문 (~한다 어조), 단정 금지(~로 판단된다/~할 가능성), 초보자 친화(전문 용어 옆 괄호 설명), 500~800자
- LLM 타임아웃: 13초 (전체 25초 예산 내)
- LangChain `PydanticOutputParser` + 스키마 위반 1회 재시도 후 룰 템플릿 폴백

## 6. 사전 계산 파이프라인

- **트리거: 데이터 수집 완료 → 수치 분석 → LLM 분석 (체인)**
- cron 시각 단일이 아닌, 데이터 수집 잡 완료 의존성 기반
- 국장 파이프라인: pykrx 일봉/주봉 → chart_analysis_result 업데이트 → TOP N 국내 종목 LLM 호출
- 미장 파이프라인: yfinance 일봉/주봉 → chart_analysis_result 업데이트 → TOP N 미국 종목 LLM 호출
- 주봉 데이터 정합: 일봉 수집 후 주봉 리샘플링 또는 별도 수집 (Planner Pass B에서 구체화)

## 7. 콘텐트 해시 기반 갱신

- 각 (symbol, window, interval) 조합마다 입력 데이터 해시 계산 (예: 최근 N봉 + 핵심 보조지표 sha256)
- DB의 기존 snapshot_hash와 비교:
  - 동일 → 수치 분석 + LLM 모두 스킵 (computed_at만 갱신)
  - 다름 → 수치 분석 재실행 + (TOP 300이면) LLM 재호출
- 효과: 장기 윈도우는 거의 변동 없어 자원 대폭 절감

## 8. 인기 종목 TOP 300

- 미장 + 국장 통합 TOP 300
- 선정 기준: 시가총액 + 거래대금 종합 (Planner Pass B에서 가중치 결정)
- 미장:국장 비율: 동적 (실제 데이터 기반)
- 갱신 주기: 주 1회 (월요일 새벽)

## 9. 사용자 요청 큐 (비인기 종목)

- 테이블: `analysis_request_queue`
  - id, symbol, window, interval, status(pending/processing/completed/failed), requested_count, requested_at, processed_at
- API: `POST /chart-analysis/request-llm-report` (사용자가 호출, 큐에 insert)
- 우선순위: FIFO (요청 시간순)
- 중복 요청: requested_count 증가, 새 row 생성 X
- 처리: 운영자(개발자) 별도 스크립트 수동 실행 — `python scripts/process_llm_request_queue.py`
- 사용자 응답: "분석 대기열에 등록되었습니다 (예상 대기 시간: 24시간 내)"

## 10. SSE 비동기 LLM 리포트

- `POST /chart-analysis/{symbol}/report` → `text/event-stream`
- 흐름: 요청 즉시 SSE 연결 → DB에 LLM 리포트 있으면 즉시 push → 없으면 진행 상태 push (pending → running → done/failed)
- Job 저장소: Redis (TTL 10분)
- 결과 캐시: Redis (TTL 1시간) + DB 영구 저장
- 동시 중복 요청: 같은 snapshot_hash면 캐시 반환 (dedup)

## 11. 가짜 실시간 UX

- 백엔드: DB hit 시 즉시 응답
- **프론트엔드가 가짜 진행 상황 표시 책임**: 1.5~3.5초 랜덤 지연 + 단계 progress (`데이터 로드 → 보조지표 계산 → 패턴 인식 → LLM 추론`)
- 백엔드 API는 변경 없음 (즉시 응답)

## 12. 저장 모델

### 12-1. chart_analysis_result (메인)
```
PRIMARY KEY (symbol, window, interval)
- symbol VARCHAR(20)
- window VARCHAR(10) -- '1M', '3M', '6M', '1Y', '2Y', 'MAX'
- interval VARCHAR(5) -- 'D', 'W'
- snapshot_hash VARCHAR(64)
- recommendation VARCHAR(15)
- confidence NUMERIC(4,3)
- levels JSONB -- supports, resistances, entry, stop_loss, target, risk_reward
- trend JSONB -- direction, strength, ma_alignment, adx, hh_ll
- patterns JSONB -- list
- indicator_signals JSONB -- list
- volume_analysis JSONB
- llm_report JSONB -- 5섹션 또는 null
- llm_report_source VARCHAR(20) -- 'llm_primary' | 'rule_template' | 'none'
- numeric_computed_at TIMESTAMP
- llm_computed_at TIMESTAMP (nullable)
- created_at, updated_at
INDEX (symbol)
INDEX (numeric_computed_at)
```

### 12-2. analysis_request_queue
```
- id BIGSERIAL PRIMARY KEY
- symbol, window, interval
- status VARCHAR(20)
- requested_count INT
- requested_at, processed_at
UNIQUE (symbol, window, interval) WHERE status IN ('pending', 'processing')
INDEX (status, requested_at)
```

### 12-3. popular_symbols (인기 TOP 300, 주 1회 갱신)
```
- symbol PRIMARY KEY
- market VARCHAR(10) -- 'KRX' | 'US'
- rank INT
- market_cap NUMERIC
- avg_volume NUMERIC
- updated_at
```

## 13. DDD 모델

### Aggregate Roots
- `ChartSnapshot` (입력): symbol, interval, window, candles, indicators
- `ChartAnalysisResult` (출력): snapshot_ref, trend, levels, trade_plan, patterns, indicator_signals, volume_analysis, recommendation, report

### Value Objects
- `Candle`, `IndicatorSet`
- `TrendAnalysis` (direction + strength + HH/LL)
- `LevelSet` (supports + resistances)
- `TradePlan` (entry + stop_loss + target + risk_reward)
- `CandlePattern`, `IndicatorSignal`, `VolumeAnalysis`
- `Recommendation` (enum + confidence)
- `NarrativeReport` (5섹션)

### Domain Ports
- `OhlcvRepository` (DB OHLCV 조회)
- `ChartAnalysisRepository` (chart_analysis_result CRUD)
- `AnalysisRequestQueueRepository`
- `LlmReportGenerator` (LangChain Ollama 또는 룰 템플릿 어댑터)
- `IndicatorCalculator`, `SupportResistanceFinder`, `PatternDetector`, `TrendClassifier`, `ConfidenceScorer` (도메인 서비스 또는 인프라)

## 14. 디렉토리 구조

```
backend/quant-worker/src/chart_analysis/
├── domain/
│   ├── chart_snapshot.py
│   ├── chart_analysis_result.py
│   ├── value_objects.py
│   └── ports.py
├── application/
│   ├── analyze_chart_service.py
│   ├── generate_report_service.py
│   ├── precompute_pipeline_service.py
│   └── process_request_queue_service.py
├── infrastructure/
│   ├── ohlcv_repository.py
│   ├── chart_analysis_repository.py
│   ├── analysis_request_queue_repository.py
│   ├── indicator_calculator.py
│   ├── support_resistance_finder.py
│   ├── pattern_detector.py
│   ├── trend_classifier.py
│   ├── confidence_scorer.py
│   ├── langchain_ollama_report_generator.py
│   ├── rule_template_report_generator.py
│   └── redis_job_store.py
└── interfaces/
    ├── chart_analysis_router.py
    └── dto.py
```

## 15. FastAPI 신규 셋업

- `backend/quant-worker/api.py` 또는 `main.py`에 FastAPI 앱 생성
- `app.include_router(chart_analysis_router)`
- docker-compose는 quant-worker 8082 포트로 이미 노출
- APScheduler를 FastAPI lifespan에 등록 (배치 잡 트리거)

## 16. 테스트

- LLM mock: `LlmReportGenerator` fake (DI)
- 보조지표: pandas-ta 신뢰, 자체 구현(지지·저항/패턴/confidence)은 골든 fixture (`tests/fixtures/chart_analysis/`)
- 통합 테스트: `@pytest.mark.integration` 마커 (실 Ollama 호출, 평소 CI skip)
- Phase 5단계에서 acceptance:
  - 단위 테스트 통과
  - 골든 fixture 비교 통과
  - 수치 분석 7 윈도우 정상 산출
  - LLM 어댑터 fake로 흐름 검증
  - 큐 등록/조회 검증
  - SSE 엔드포인트 통합 테스트 (httpx asgi)

## 17. 환경변수

```
OLLAMA_BASE_URL              = http://ollama:11434
CHART_ANALYSIS_LLM_MODEL     = qwen2.5:7b
CHART_ANALYSIS_LLM_TIMEOUT_S = 25
CHART_ANALYSIS_LLM_INNER_TIMEOUT_S = 13
CHART_ANALYSIS_CACHE_TTL_S   = 3600
CHART_ANALYSIS_JOB_TTL_S     = 600
REDIS_HOST                   = redis
REDIS_PORT                   = 6379
CHART_ANALYSIS_POPULAR_TOP_N = 300
CHART_ANALYSIS_CONFIDENCE_BUY_THRESHOLD = 0.4
CHART_ANALYSIS_CONFIDENCE_STRONG_THRESHOLD = 0.7
```

## 18. 금융 안전

- 가격·confidence 등 수치 필드는 `Decimal` 사용 (CRITICAL: financial safety)
- DB 저장 시 NUMERIC 타입

## 19. Phase 단계 분할 권고 (Planner Pass B 참고)

- Step 1: Planner (현재) — spec + step 파일
- Step 2: quant-dev — Domain Layer (ChartSnapshot, ChartAnalysisResult, VO, Ports)
- Step 3: quant-dev — Infrastructure (DB Repository, pandas-ta, 지지·저항, 패턴 검출, 추세, confidence)
- Step 4: quant-dev — LLM 어댑터 (LangChain Ollama, 룰 템플릿 폴백, Pydantic 스키마)
- Step 5: quant-dev — FastAPI Router (수치 엔드포인트 + SSE 리포트 + 큐 등록 API)
- Step 6: quant-dev — 배치 파이프라인 + APScheduler + 큐 처리 스크립트
- Step 7: test-engineer — 단위 + 골든 + 통합 마커
- Step 8: code-reviewer — 코드/보안/도메인 리뷰
- Step 9: orchestrator — Cleanup + PR (단, cleanup은 orchestrator 직접 처리, 서브에이전트 X)

→ `total_steps`는 Planner가 최종 결정 (9 권고, 7 가능)
