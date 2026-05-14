# chart-analysis-frontend-refactor — 완료 요약

## 완료일
2026-05-14

## 변경 목적
백엔드 서비스 역할 분리(service-role-refactor) 완료에 따른 프론트엔드 API 엔드포인트 정합성 확보 + AI 자연어 해설 SSE 연동.

## 변경 파일

### `frontend/trading-web/src/shared/api/chartAnalysisApi.ts`
| 함수 | 기존 | 변경 후 |
|------|------|---------|
| `fetchChartAnalysis` | POST `/api-ai/chart-analysis/{symbol}` | GET `/api-research/research/results/{symbol}` |
| `runChartAnalysis` | POST `/api-ai/admin/run-analysis/{symbol}` | POST `/api-research/research/run/{symbol}` |
| `fetchAnalyzedSymbols` | GET `/api-ai/admin/symbols` | GET `/api-research/research/symbols` |
| `streamLlmReport` (신규) | 없음 | POST `/api-ai/chart-analysis/{symbol}/report` (SSE) |

신규 타입: `LlmNarrative`, `LlmReportData`

### `frontend/trading-web/src/pages/chart-analysis/ui/ChartAnalysisPage.tsx`
- `llmStage`, `llmReport`, `llmError` 상태 추가
- `sseAbortRef` (AbortController) + cleanup useEffect
- `startLlmStream` 헬퍼: 이전 SSE abort → 새 스트림 시작
- `viewMutation.onSuccess` → `startLlmStream` 자동 연쇄
- `runAnalysisMutation.onSuccess` → `viewMutation.mutate` 자동 연쇄 (분석→조회→SSE 한 버튼으로)
- `AiNarrationCard` 컴포넌트 추가 (stage별 스켈레톤/섹션 렌더링)
- 에러 메시지 "quant-ai" → "quant-research"

## UX 흐름
1. "② 기술 분석 실행" 클릭 → quant-research 분석 실행
2. 완료 → 자동으로 결과 조회 (quant-research)
3. 결과 조회 완료 → 자동으로 AI SSE 스트림 시작 (quant-ai)
4. 결과 패널: 수치 분석 카드 + IndicatorTable + AiNarrationCard(스트리밍)
