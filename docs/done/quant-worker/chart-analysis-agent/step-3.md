# Step 3: 인프라 계산 모듈 구현 (Infrastructure Calculations)

Assigned agent: quant-dev

## Working Directory
.worktrees/quant-worker-chart-analysis-agent

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CLAUDE.md
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/CLAUDE.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/quant-worker/chart-analysis-agent/spec.md (§3 Alpha Factors, §11 DDD Model)
- C:/Users/dw/Desktop/paper-trading/docs/phase/quant-worker/chart-analysis-agent/decisions.md (§4, §16)
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/src/chart_analysis/domain/ (Step 2 산출물)

## Open Questions
없음.

## Confirmed Design Choices
- `pandas-ta` 위임 — 자체 보조지표 구현 금지 (decisions §4)
- 지지/저항: `scipy.signal.find_peaks` 기반, 클러스터 폭 = ATR×0.5 (decisions §4)
- 캔들 패턴 6종 (Doji/Hammer/Inverted Hammer/Bullish Engulfing/Bearish Engulfing/Morning Star + Evening Star) (decisions §4)
- 추세: MA 정배열(MA20 vs MA60) + ADX 강도 + HH/LL 구조 (decisions §4)
- Confidence: 룰 기반 가중치 합산 → 0.0~1.0 정규화 (decisions §4)
- 자체 구현 항목(지지/저항, 패턴, 추세, confidence)은 **골든 fixture 필수** (decisions §16)
- 디렉토리: `backend/quant-worker/src/chart_analysis/infrastructure/`
- 모든 수치 입출력 `Decimal` (financial safety)

## Tasks

### Substep 3-0: 의존성 + 골든 fixture 디렉토리 셋업
1. `backend/quant-worker/requirements.txt` 에 추가: `pandas-ta==0.3.14b0`, `scipy==1.13.1` (이미 있으면 확인)
2. `tests/fixtures/chart_analysis/` 디렉토리 생성:
   - `support_resistance/sample_uptrend_3m.json` (입력 candles + expected supports/resistances)
   - `support_resistance/sample_sideways_6m.json`
   - `patterns/hammer_01.json`, `doji_01.json`, `engulfing_bull_01.json`, `engulfing_bear_01.json`, `morning_star_01.json`, `evening_star_01.json`, `inverted_hammer_01.json`
   - `trend/uptrend_ma_aligned.json`, `downtrend_ma_inverted.json`, `sideways_low_adx.json`
3. fixture JSON 포맷: `{ "input_candles": [...], "expected": {...} }` — Decimal은 string으로 저장
4. fixture 헬퍼: `tests/conftest.py` 또는 `tests/fixtures/chart_analysis/loader.py` (path → dict 로드)

### Substep 3-1: `IndicatorCalculator` (`pandas-ta` 래퍼)
1. (TEST FIRST) `tests/unit/chart_analysis/infrastructure/test_indicator_calculator.py`
   - 골든 fixture가 아닌 **호출 검증만**: 입력 N봉 → 출력 `IndicatorSet`에 14개 필드 모두 채워짐
   - `Decimal` 타입 변환 검증
   - 입력 행 수 < MA120 한도 → 해당 필드 `None` 허용 (또는 NaN 거부 → ValueError)
2. `src/chart_analysis/infrastructure/indicator_calculator.py`
   - 클래스: `PandasTaIndicatorCalculator(IndicatorCalculator)`
   - 메서드: `compute(candles: list[Candle]) -> IndicatorSet`
   - pandas DataFrame 변환 → `ta.sma`, `ta.rsi`, `ta.macd`, `ta.bbands`, `ta.atr`, `ta.adx`, `ta.stoch`, `ta.obv` 호출
   - 결과 float → `Decimal(str(value))` 변환 (직접 `Decimal(float)` 금지)

### Substep 3-2: `SupportResistanceFinder`
1. (TEST FIRST) `tests/unit/chart_analysis/infrastructure/test_support_resistance_finder.py`
   - 각 골든 fixture 입력 → expected supports/resistances 와 일치 (±ATR×0.1 허용 오차)
   - 빈 입력 → 빈 리스트
2. `src/chart_analysis/infrastructure/support_resistance_finder.py`
   - 클래스: `ScipyPeakSupportResistanceFinder(SupportResistanceFinder)`
   - 메서드: `find(candles: list[Candle], atr: Decimal) -> LevelSet`
   - 절차:
     1. high 시계열에서 `scipy.signal.find_peaks` → 저항 후보
     2. -low 시계열에서 `find_peaks` → 지지 후보
     3. `prominence`/`distance` 파라미터: distance=5, prominence=atr×0.3
     4. ATR×0.5 거리 내 후보 그룹화(평균) → 클러스터링
     5. 최근 우선순위(시간 가중치), 최대 5개 반환

### Substep 3-3: `PatternDetector` (캔들 패턴 6종)
1. (TEST FIRST) `tests/unit/chart_analysis/infrastructure/test_pattern_detector.py`
   - 6개 fixture (`hammer_01`, `doji_01`, `engulfing_bull_01`, `engulfing_bear_01`, `morning_star_01`, `evening_star_01`, `inverted_hammer_01`) 입력 → 정확히 해당 패턴이 검출됨
   - 비대상 패턴 케이스 → 검출되지 않음
2. `src/chart_analysis/infrastructure/pattern_detector.py`
   - 클래스: `RuleBasedPatternDetector(PatternDetector)`
   - 메서드: `detect(candles: list[Candle]) -> list[CandlePattern]`
   - 6개 룰 함수 (private): `_is_doji`, `_is_hammer`, `_is_inverted_hammer`, `_is_bullish_engulfing`, `_is_bearish_engulfing`, `_is_morning_star_or_evening_star`
   - 룰 정의 명시 (예: Doji = |close-open| / (high-low) < 0.1)

### Substep 3-4: `TrendClassifier`
1. (TEST FIRST) `tests/unit/chart_analysis/infrastructure/test_trend_classifier.py`
   - 3개 fixture (uptrend / downtrend / sideways) 입력 → expected `Direction`, `Strength`, `ma_alignment`, `hh_ll_structure` 와 일치
2. `src/chart_analysis/infrastructure/trend_classifier.py`
   - 클래스: `MaAdxTrendClassifier(TrendClassifier)`
   - 메서드: `classify(candles: list[Candle], indicators: IndicatorSet) -> TrendAnalysis`
   - 로직:
     - MA 정배열: MA20 > MA60 → "MA20>MA60", 반대 → "MA20<MA60", 교차/근접 → "FLAT"
     - ADX: >25 STRONG, 20-25 MEDIUM, <20 WEAK
     - HH/LL: 직전 20봉 고점/저점 시퀀스로 "HH" / "LL" / "RANGING" 판정
     - direction: 정배열 + ADX strong → UPTREND, 역배열 + ADX strong → DOWNTREND, 그 외 → SIDEWAYS

### Substep 3-5: `ConfidenceScorer`
1. (TEST FIRST) `tests/unit/chart_analysis/infrastructure/test_confidence_scorer.py`
   - 신호 dict 입력 → confidence Decimal in [0.0, 1.0]
   - 임계값 환경변수 적용 검증 (monkeypatch)
   - long/short 양방향 신호 → 정규화 후 적절한 부호 매핑
2. `src/chart_analysis/infrastructure/confidence_scorer.py`
   - 클래스: `WeightedRuleConfidenceScorer(ConfidenceScorer)`
   - 메서드: `score(trend: TrendAnalysis, patterns, indicator_signals, volume_analysis) -> Recommendation`
   - 가중치 표(코드 내 상수, yaml 외부화 가능):
     - MA 정배열 + ADX strong: +0.25
     - 최근 패턴 강세형 (Hammer/Bullish Engulfing/Morning Star): +0.15
     - RSI < 30 (반등 신호): +0.10
     - 거래량 spike + 양봉: +0.10
     - 패턴 약세형: -0.15
     - RSI > 70: -0.10
   - 합산 → tanh 또는 clamping → [0.0, 1.0] 정규화 → grade 매핑 (spec §3-3)

### Substep 3-6: 모듈 초기화 + 검증
1. `src/chart_analysis/infrastructure/__init__.py` (public re-export)
2. 모든 새 파일 `python -m py_compile` 통과
3. 단위 테스트 100% 통과

## Acceptance Criteria
- 5개 인프라 클래스 + 6개 골든 fixture 완성
- 단위 테스트 통과 (TDD 순서 commit 이력 존재)
- 보조지표 계산은 `pandas-ta` 위임 확인 (자체 보조지표 수식 0건)
- 지지/저항/패턴/추세 모두 fixture 일치 검증
- 모든 메서드 입출력 `Decimal` 일관성 (float 노출 0건)
- 커밋 메시지 한국어

## Agent Return Protocol
---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <한두 문장>
- Files modified: <목록>
- Test result: <pytest 결과>
- Blockers: <none | description>
---
