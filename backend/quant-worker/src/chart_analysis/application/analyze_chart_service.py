"""AnalyzeChartService — 단일 종목 7 윈도우 분석 결과 조회 Application Service."""
from __future__ import annotations

from chart_analysis.interfaces.dto import (
    AnalysisWindowDTO,
    ChartAnalysisResponseDTO,
    ReportStatusDTO,
)


class AnalyzeChartService:
    """단일 종목의 7 윈도우 수치 분석 결과를 조회한다.

    - DB에서 사전 계산된 ChartAnalysisResult 목록 조회
    - report.status 매핑: llm_report 있으면 'available', 큐 등록됨 'pending', 그 외 'none'
    """

    def __init__(
        self,
        chart_analysis_repository,
        analysis_request_queue_repository,
    ) -> None:
        self._repo = chart_analysis_repository
        self._queue_repo = analysis_request_queue_repository

    def get_analyses(self, symbol: str) -> ChartAnalysisResponseDTO:
        """종목 코드로 7 윈도우 분석 결과를 즉시 반환한다."""
        results = self._repo.find_by_symbol(symbol)
        if not results:
            return ChartAnalysisResponseDTO(symbol=symbol, analyses=[])

        analyses = []
        for result in results:
            dto = AnalysisWindowDTO.from_domain(result)

            # report.status 재매핑: queue 상태 반영
            if result.report is not None:
                status = "available"
            elif self._is_pending_in_queue(symbol, result.window, result.interval):
                status = "pending"
            else:
                status = "none"

            # report 필드를 재구성 (status만 업데이트)
            dto = dto.model_copy(update={"report": ReportStatusDTO(status=status, narrative=None)})
            analyses.append(dto)

        return ChartAnalysisResponseDTO(symbol=symbol, analyses=analyses)

    def _is_pending_in_queue(self, symbol: str, window: str, interval: str) -> bool:
        """분석 요청 큐에 pending 상태 항목이 있는지 확인한다."""
        pending_items = self._queue_repo.fetch_pending(limit=1000)
        for item in pending_items:
            # QueueItem 또는 dict 형태 모두 지원
            if isinstance(item, dict):
                s = item.get("symbol")
                w = item.get("window")
                i = item.get("interval")
            else:
                s = getattr(item, "symbol", None)
                w = getattr(item, "window", None)
                i = getattr(item, "interval", None)
            if s == symbol and w == window and i == interval:
                return True
        return False