# Step 8: 코드 리뷰 (Security + Quant + DDD)

Assigned agent: code-reviewer

## Working Directory
.worktrees/quant-worker-chart-analysis-agent

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CLAUDE.md
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/CLAUDE.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/quant-worker/chart-analysis-agent/spec.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/quant-worker/chart-analysis-agent/decisions.md
- Step 2-6의 모든 구현 파일 (`backend/quant-worker/src/chart_analysis/**`, 마이그레이션, 스크립트, 라우터)

## Open Questions
없음.

## Confirmed Design Choices
- 리뷰 범위: **현재 phase 변경 파일만** (memory `feedback_test_scope.md`)
- 리뷰 기준: 코드 품질, 보안, 금융 안전(Decimal), DDD 경계, LLM 어댑터 안정성

## Tasks

### Substep 8-1: 보안 검토
1. 환경변수 하드코딩 검사 — `OLLAMA_BASE_URL`, `REDIS_*`, `PG_*` 등 모두 환경변수 또는 기본값
2. API 키/시크릿 로그 출력 없음 확인
3. SQL 인젝션 가드 — 모든 쿼리 파라미터 바인딩 사용 (psycopg `%s`)
4. JSON 직렬화 시 Decimal → string (float 노출 0건)
5. Redis 키 네임스페이스 충돌 가능성 검토

### Substep 8-2: 금융 안전 검토
1. 모든 가격/confidence 필드 `Decimal` 사용 확인 (검색: `: float` 또는 `float(` 사용처)
2. `Decimal(float_value)` 직접 변환 금지 (반드시 `Decimal(str(...))`)
3. DB 컬럼 타입 NUMERIC 확인 (V2 마이그레이션 검토)
4. pandas-ta float 반환 → Decimal 변환 누락 검사
5. JSONB 저장 시 Decimal 직렬화 정확성

### Substep 8-3: DDD 경계 검토
1. 도메인 계층(`src/chart_analysis/domain/`) 에서 `langchain`, `sqlalchemy`, `pandas`, `pandas_ta`, `scipy`, `redis`, `httpx` import 0건 확인
2. Application 계층은 도메인 Port만 의존, 구체 어댑터 직접 import 금지
3. Infrastructure가 도메인 Port를 구현 (Protocol 또는 abstract base)
4. Aggregate boundary 명확성 — `ChartSnapshot`, `ChartAnalysisResult`의 책임 분리

### Substep 8-4: LLM 어댑터 + SSE 안정성
1. LLM 타임아웃 13초 적용 확인
2. Pydantic 스키마 위반 → 1회 재시도 → 폴백 동작 흐름 검토
3. SSE 스트림 생성기에서 예외 처리 (`finally`로 락 해제, 클라이언트 disconnect 처리)
4. Redis 락 TTL = 600s, snapshot_hash 키 충돌 가능성 검토
5. 룰 템플릿 폴백의 결정성 + 단정 어조("확실히/100%") 부재 검증

### Substep 8-5: 콘텐트 해시 + 큐 무결성
1. 해시 입력 정규화 (Decimal → str, 순서 안정성) 확인
2. 부분 UNIQUE 인덱스 `analysis_request_queue` 동작 (status 변경 후 재요청 가능)
3. 큐 처리 스크립트의 status 전이 원자성

### Substep 8-6: 리뷰 결과 정리
1. must-fix / nice-to-have / 정보 분류
2. 각 must-fix는 파일:라인 + 수정 제안 명시
3. 본 단계에서는 코드 수정 금지 — 발견 사항을 보고서로 정리

## Acceptance Criteria
- 보안 must-fix 0건 (있으면 BLOCKED 처리)
- 금융 안전 must-fix 0건
- DDD 경계 위반 0건
- LLM/SSE 안정성 must-fix 0건
- 리뷰 결과 보고서 제출 (must-fix 있으면 quant-dev 재호출 필요)
- 커밋 없음

## Agent Return Protocol
---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <한두 문장 — "must-fix N건, nice-to-have N건.">
- Files modified: <목록 (보통 none)>
- Test result: N/A
- Blockers: <none | must-fix 항목 요약>
---
