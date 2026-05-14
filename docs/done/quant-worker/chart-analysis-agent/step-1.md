# Step 1: 차트 분석 AI 에이전트 — 설계 Q&A + Spec 생성
Assigned agent: Quant Planner

## Working Directory
.worktrees/quant-worker-chart-analysis-agent

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CLAUDE.md
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/CLAUDE.md
- C:/Users/dw/Desktop/paper-trading/docs/TODO.md
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/main.py
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/requirements.txt
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/src/  ← 디렉토리 전체 구조 파악
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/scripts/  ← 기존 수집 스크립트 패턴
- C:/Users/dw/Desktop/paper-trading/docker-compose.yml
- C:/Users/dw/Desktop/paper-trading/scripts/pull-models.sh

## Feature Goal
현재 차트 데이터(OHLCV + 보조지표)를 입력받아 지지선·저항선·진입가·손절가·추세를 분석하고,
LLM으로 초보자가 이해할 수 있는 한국어 자연어 리포트를 반환하는 FastAPI 엔드포인트(`POST /chart-analysis`)를 구현한다.
Railway MVP 배포 대상 기능이며, **로컬 Ollama LLM**으로 운영하고 추후 외부 LLM(OpenAI)으로 전환 가능한 추상화가 필요하다.

## Confirmed Design Choices (사용자 사전 확정)

다음 결정은 Q&A로 이미 확정되었다. Pass A에서 이 항목들에 대해 다시 묻지 말고, 나머지 미결 사항만 도출하라.

| 항목 | 확정 |
|---|---|
| 출력 범위 | **C. 풀 분석** — 트레이딩 신호(진입가/손절가/목표가/R:R) + 캔들 패턴 + 보조지표 신호 + 거래량 분석 + 자연어 리포트 |
| LLM 연동 | **LangChain** (`langchain_ollama.ChatOllama` 사용, 추후 다른 Provider 교체 가능한 추상화 필요) |
| LLM 러너 | **Ollama** (docker-compose에 이미 셋업, `http://ollama:11434`) |
| 후보 모델 | docker-compose가 자동 pull하는 `qwen2.5:7b` (메인) / `qwen2.5:1.5b` (빠른 폴백) / `gemma4:e2b` (보조). 기본 모델 선택은 Planner가 추천. |
| 보조지표 라이브러리 | **pandas-ta** (requirements.txt에 추가) |
| FastAPI 서버 | **이번 피처에서 신규 셋업** — `main.py` 또는 신규 `api.py`에 FastAPI 앱 추가. quant-worker는 docker-compose에서 port 8082로 노출됨 |
| 지지선·저항선 알고리즘 | TA-Lib 미제공 항목이므로 **자체 구현** (scipy.signal.find_peaks 등) |
| 데이터 타입 | 금융 수치는 `Decimal` 사용 (financial safety 규칙) |

## Tasks

### Pass A (현재 단계)
1. 위 Files to Read를 읽고 quant-worker 코드 구조/패턴을 파악한다.
2. **Confirmed Design Choices에 명시된 항목은 재질문하지 않는다.**
3. 다음 미결 영역에 대해 추가 설계 질문 + 권고 옵션을 도출한다:
   - **입력 데이터 스키마**: 클라이언트가 OHLCV/보조지표를 직접 POST body로 보내는지, 종목코드+기간만 받고 quant-worker가 DB에서 조회하는지
   - **분석 기간(lookback)**: 일봉 기준 며칠치를 분석할지 (60/120/250?), 분봉 지원 여부
   - **응답 JSON 스키마**: 어떤 필드를 반환할지 (recommendation/confidence/support_levels/resistance_levels/entry_price/stop_loss/target_price/detected_patterns/indicator_signals/volume_analysis/llm_report 등 어떤 조합)
   - **캔들 패턴 인식 범위**: 어떤 패턴 몇 개를 지원할지 (망치/도지/엔걸핑/모닝스타 등)
   - **추세 판단 방법**: MA 기울기 / ADX / 둘 다
   - **신뢰도 점수 산정**: 어떤 룰로 0~1 또는 약/중/강 등급 산정
   - **LLM Provider 추상화 인터페이스**: LangChain `BaseChatModel` 직접 사용 vs 자체 `LLMProvider` 인터페이스
   - **프롬프트 전략**: 어떤 입력을 LLM에 전달해 자연어 리포트를 생성할지 (수치 신호 dict → 한국어 리포트)
   - **에러 처리**: LLM 호출 실패 시 룰 기반 폴백 리포트 제공 여부
   - **응답 시간**: 동기 vs 비동기 (스트리밍 필요 여부), 타임아웃 정책
   - **테스트 전략**: LLM 호출 mocking, 보조지표 계산 검증 방식
   - **DDD 경계**: 도메인 객체 (Aggregate Root, VO 등) 식별 — ChartSnapshot / AnalysisResult / SupportLevel 등 어떻게 모델링?
4. 질문 리스트를 출력하고 "Awaiting user decisions on remaining questions" 메시지로 종료한다.
5. **이 단계에서는 spec.md / step-2..N.md를 작성하지 말 것.** Pass B에서 작성한다.

### Pass B (사용자 결정 후 별도 호출)
1. 확정된 모든 결정으로 spec.md 작성
2. step-2..N.md 생성 (총 5단계)
3. 각 단계의 substeps 정의 (DDD: 1 Aggregate Root = 1 substep)

## Acceptance Criteria
- Pass A: 미결 영역에 대한 구조화된 질문 + 권고 옵션(2-3개)만 출력 (코드 작성 금지)
- Pass B: spec.md 생성, step-2..N.md 생성, 모든 결정 반영, 사용자 승인 완료
- 모든 doc 경로는 main repo root 기준 절대 경로 사용

## Agent Return Protocol
When you finish Pass A, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences — Pass A 완료 시 "Pass A 완료. 미결 영역 N개 질문 도출, 사용자 결정 대기">
- Files modified: <none for Pass A>
- Test result: N/A
- Blockers: <none | description>
---