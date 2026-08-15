# Step 1: Backtesting Engine Planning
Assigned agent: Quant Planner

## Working Directory
.worktrees/quant-worker-backtest-engine

## Feature
백테스팅 엔진 기본 구조

**Goal**: Python 기반 백테스팅 엔진의 도메인/API/데이터 계약/검증 기준을 설계하고, 사용자 확인을 거친 뒤 `spec.md`와 후속 implementation/test/review step 파일을 생성한다.

## Files to Read
- CODEX.md
- docs/TODO.md (`quant-worker / backtest-engine` 항목과 관련 선행/후속 항목)
- backend/quant-worker/README.md
- backend/quant-worker/pyproject.toml
- backend/quant-worker/src
- backend/quant-worker/tests
- docs/phase/quant-worker/backtest-engine/step-1.md

## Open Questions
(Planner Pass A가 코드베이스를 읽은 뒤 질문 목록과 추천 옵션을 생성한다. 여기에 미리 확정하지 않음.)

## Confirmed Design Choices
- Backtest engine: QuantConnect LEAN only, executed through Docker (`quantconnect/lean:latest`).
- Strategy input: JSON DSL. Local AI generates structured strategy JSON; project code validates it and converts it to LEAN algorithm/config. AI-generated raw LEAN Python code is out of scope for MVP.
- LEAN project generation: fixed LEAN template project plus run-specific config/strategy injection. At execution time, copy the template into `runs/{runId}` for isolation.
- Data supply: export local PostgreSQL OHLCV data into LEAN local data format before execution. Source tables are `market_daily_ohlcv` and `market_weekly_ohlcv`; MVP prioritizes daily bars while keeping weekly export extensible.
- Target markets: support both KR and US from the MVP. `market` is required; KR uses KRW/Korean fee and tax defaults, US uses USD/US defaults.
- Execution API: asynchronous run API (`POST /backtest-runs`, status/result/log retrieval). Synchronous `POST /backtest` is out of scope for MVP.
- Result persistence: store run metadata and summary metrics in DB; store detailed LEAN raw results, equity curve, trades, and logs as artifacts.
- First DSL scope: design a multi-factor JSON DSL for AI-generated strategies. Supported factor categories include price, technical, flow, fundamental, macro, news sentiment, disclosure, paper factors, and events. MVP execution supports price/technical factors first; other categories must be represented in the schema as planned/unsupported until their data pipelines and LEAN custom-data adapters are implemented.

## Tasks
1. 현재 `quant-worker` 구조, 테스트 방식, 데이터 저장소/모델, 기존 차트/수급/지표 파이프라인을 파악한다.
2. 백테스팅 엔진의 핵심 설계 결정을 Planner Pass A로 사용자에게 제시한다. 각 결정은 다음을 반드시 포함한다:
   - 기능/개념 설명: 이 선택이 백테스트 정확도, 확장성, 전략 개발 흐름에 어떤 영향을 주는지
   - 최소 3개 이상의 선택지: 단순/균형/확장 또는 보수적/중간/고급 방향
   - 각 선택지의 장점, 단점, 구현 난이도, 검증 방법
   - planner의 추천안 1개와 추천 이유
   - 최종 결정은 사용자가 선택한다는 명시
3. 다음 결정 영역을 반드시 다룬다:
   - 엔진 방식: vectorbt 사용 vs 자체 벡터화 엔진 vs 이벤트 기반 엔진
   - 입력 데이터 계약: 일봉 OHLCV만 우선 vs OHLCV+수급 vs OHLCV+수급+팩터 확장 계약
   - 전략 표현: 고정 built-in 전략 vs 파라미터 DSL vs Python strategy interface
   - 비용/슬리피지/체결 정책: 단순 비율 vs 종목/시장별 정책 vs 이벤트 기반 체결 모델
   - 성과 지표 범위: MVP 지표 vs 리스크 확장 지표 vs factor/IC 연동
   - FastAPI 노출 범위: 내부 서비스만 vs `POST /backtest` MVP vs 저장/조회 포함 API
   - 테스트/검증 기준: synthetic fixtures, golden metrics, edge cases, regression coverage
4. 사용자 답변 후 확정된 선택만 반영해 `spec.md`를 작성한다.
5. 후속 step 파일을 생성한다. 기본 초안은 다음 흐름을 고려하되, Pass A/B 결과에 맞게 조정한다:
   - `step-2.md`: backtest domain model, engine, metrics TDD 구현
   - `step-3.md`: data adapter/API integration TDD 구현
   - `step-4.md`: targeted QA verification
   - `step-5.md`: code and quant review
   - `step-6.md`: cleanup summary and PR prep

## Acceptance Criteria
- Planner Pass A는 사용자 선택 가능한 대안을 보존해서 출력한다.
- `spec.md`는 사용자 확인이 끝난 결정만 포함한다.
- `step-2.md` 이후 파일은 fresh subagent가 읽고 실행할 수 있게 Working Directory, Files to Read, Open Questions, Confirmed Design Choices, Tasks, Acceptance Criteria, Agent Return Protocol을 포함한다.
- 금융 계산은 float 기반 금액 산출로 설계하지 않는다. 금액/수량/수익률 계산의 정밀도 정책을 명시한다.
- 백테스트 결과 지표에는 최소 Sharpe, MDD, annualized return, Calmar, win rate가 포함되도록 설계한다.
- 모든 docs 경로는 main repo root 기준이다.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences>
- Files modified: <paths relative to worktree root>
- Test result: N/A (planner step)
- Blockers: <none | description>
---
