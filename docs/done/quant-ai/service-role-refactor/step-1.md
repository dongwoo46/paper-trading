# Step 1: Feature Spec and API Spec
Assigned agent: Service Planner

## Working Directory
.worktrees/quant-ai-service-role-refactor

## Feature
quant-research / quant-ai 서비스 역할 재정립 (service-role-refactor)

**Goal**: quant-ai가 분석 파이프라인까지 오케스트레이션하는 역할 역전 구조를 수정하여,
quant-research는 기술분석 계산 + DB 저장 + 결과 조회를 담당하고,
quant-ai는 LLM 자연어 해석(Ollama) 생성만 담당하도록 역할을 분리한다.

## Files to Read
- CLAUDE.md
- docs/TODO.md  (P0 항목 service-role-refactor 전체 읽기)
- backend/quant-ai/main.py
- backend/quant-ai/src/interfaces/api/app.py
- backend/quant-ai/src/chart_analysis/interfaces/chart_analysis_router.py
- backend/quant-ai/src/chart_analysis/application/precompute_pipeline_service.py
- backend/quant-ai/src/chart_analysis/application/analyze_chart_service.py
- backend/quant-ai/src/chart_analysis/application/generate_report_service.py
- backend/quant-ai/src/chart_analysis/infrastructure/ohlcv_repository.py
- backend/quant-ai/src/chart_analysis/infrastructure/chart_analysis_repository.py
- backend/quant-ai/src/chart_analysis/infrastructure/analysis_request_queue_repository.py
- backend/quant-ai/src/chart_analysis/domain/ports.py
- backend/quant-research/src/interfaces/api/app.py
- backend/quant-research/src/interfaces/api/research_router.py
- backend/quant-research/src/chart_analysis/domain/ports.py

## Open Questions
(Planner가 코드베이스를 읽은 뒤 질문 목록을 생성한다. 여기에 미리 작성하지 않음.)

## Confirmed Design Choices
(Q&A 완료 후 확정된 결정만 기록. Planner가 직접 채운다.)

## Tasks
1. 위 파일들을 읽고 현재 quant-ai / quant-research 구조를 파악한다.
2. 역할 분리에 필요한 불명확한 점을 구조화된 질문 목록으로 출력한다 (Pass A).
3. 사용자 답변을 받은 뒤 확정된 결정으로 spec.md를 작성한다 (Pass B).
4. step-2.md ~ step-4.md를 생성한다 (각 step은 독립 실행 가능하게 Files to Read 명시).

## Acceptance Criteria
- spec.md 생성 (역할 분리 결정 사항, 이전할 파일 목록, 삭제할 엔드포인트, 추가할 엔드포인트)
- step-2.md ~ step-4.md 생성 (각 step에 Files to Read 및 Tasks 포함)
- 각 step 파일 끝에 "## Agent Return Protocol" 포함
- 모든 doc 경로는 main repo root 기준

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
