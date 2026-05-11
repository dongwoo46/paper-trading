"""차트 분석 API 라우터.

엔드포인트:
- POST /chart-analysis/{symbol}               — 7 윈도우 수치 분석 즉시 반환
- POST /chart-analysis/{symbol}/report         — SSE LLM 리포트 스트림
- POST /chart-analysis/request-llm-report      — 비인기 종목 LLM 요청 큐 등록
"""
from __future__ import annotations

import json
import logging
from typing import AsyncIterator

from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import StreamingResponse

from chart_analysis.application.analyze_chart_service import AnalyzeChartService
from chart_analysis.application.generate_report_service import GenerateReportService
from chart_analysis.interfaces.dto import (
    ChartAnalysisResponseDTO,
    RequestLlmReportRequestDTO,
    RequestLlmReportResponseDTO,
)

logger = logging.getLogger(__name__)

chart_analysis_router = APIRouter(prefix="/chart-analysis", tags=["chart_analysis"])

# ---------------------------------------------------------------------------
# 유효성 검증 상수
# ---------------------------------------------------------------------------

_VALID_WINDOWS = {"1M", "3M", "6M", "1Y", "2Y", "MAX"}
_VALID_INTERVALS = {"D", "W"}


# ---------------------------------------------------------------------------
# 의존성 팩토리 (테스트에서 override 가능)
# ---------------------------------------------------------------------------

def get_analyze_chart_service() -> AnalyzeChartService:
    """AnalyzeChartService 의존성 팩토리.

    프로덕션에서는 환경변수 기반 DB/Redis 연결,
    테스트에서는 app.dependency_overrides 로 교체.
    """
    from chart_analysis.infrastructure.chart_analysis_repository import (
        PostgresChartAnalysisRepository,
    )
    from chart_analysis.infrastructure.analysis_request_queue_repository import (
        PostgresAnalysisRequestQueueRepository,
    )
    from src.application.daily_fetch_service import load_db_config_from_env
    from src.catalog.postgres_symbol_catalog import connect

    db = load_db_config_from_env()

    def _connect():
        return connect(db)

    chart_repo = PostgresChartAnalysisRepository(connect_fn=_connect)
    queue_repo = PostgresAnalysisRequestQueueRepository(connect_fn=_connect)
    return AnalyzeChartService(
        chart_analysis_repository=chart_repo,
        analysis_request_queue_repository=queue_repo,
    )


def get_generate_report_service() -> GenerateReportService:
    """GenerateReportService 의존성 팩토리."""
    from chart_analysis.infrastructure.chart_analysis_repository import (
        PostgresChartAnalysisRepository,
    )
    from chart_analysis.infrastructure.redis_job_store import RedisJobStore
    from chart_analysis.infrastructure.langchain_ollama_report_generator import (
        LangChainOllamaReportGenerator,
    )
    from chart_analysis.infrastructure.slack_notifier import SlackWebhookNotifier
    from src.application.daily_fetch_service import load_db_config_from_env
    from src.catalog.postgres_symbol_catalog import connect

    db = load_db_config_from_env()

    def _connect():
        return connect(db)

    chart_repo = PostgresChartAnalysisRepository(connect_fn=_connect)
    redis_store = RedisJobStore()
    llm = LangChainOllamaReportGenerator()
    slack = SlackWebhookNotifier()

    def _is_popular(symbol: str) -> bool:
        # 실제 구현에서는 popular_symbols 테이블 조회
        # 여기서는 간단히 True를 반환 (배치 파이프라인 Step 6에서 구체화)
        return True

    return GenerateReportService(
        chart_analysis_repository=chart_repo,
        llm_report_generator=llm,
        redis_job_store=redis_store,
        popular_symbols_fn=_is_popular,
        slack_notifier=slack,
    )


def get_queue_repository():
    """AnalysisRequestQueueRepository 의존성 팩토리."""
    from chart_analysis.infrastructure.analysis_request_queue_repository import (
        PostgresAnalysisRequestQueueRepository,
    )
    from src.application.daily_fetch_service import load_db_config_from_env
    from src.catalog.postgres_symbol_catalog import connect

    db = load_db_config_from_env()

    def _connect():
        return connect(db)

    return PostgresAnalysisRequestQueueRepository(connect_fn=_connect)


# ---------------------------------------------------------------------------
# 엔드포인트
# ---------------------------------------------------------------------------


@chart_analysis_router.post("/request-llm-report")
async def request_llm_report(
    request: RequestLlmReportRequestDTO,
    queue_repo=Depends(get_queue_repository),
):
    """비인기 종목 LLM 리포트 생성 요청을 큐에 등록한다."""
    if request.window not in _VALID_WINDOWS:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid window: {request.window!r}. Valid: {sorted(_VALID_WINDOWS)}",
        )
    if request.interval not in _VALID_INTERVALS:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid interval: {request.interval!r}. Valid: {sorted(_VALID_INTERVALS)}",
        )

    pending_before = queue_repo.fetch_pending(limit=10000)
    is_duplicate = any(
        (isinstance(item, dict) and item.get("symbol") == request.symbol
         and item.get("window") == request.window
         and item.get("interval") == request.interval)
        or (not isinstance(item, dict)
            and getattr(item, "symbol", None) == request.symbol
            and getattr(item, "window", None) == request.window
            and getattr(item, "interval", None) == request.interval)
        for item in pending_before
    )

    queue_repo.enqueue(request.symbol, request.window, request.interval)

    pending_after = queue_repo.fetch_pending(limit=10000)
    queue_position = len(pending_after)

    status_code = 200 if is_duplicate else 201
    response_data = RequestLlmReportResponseDTO(
        status="queued",
        queue_position=queue_position,
        estimated_wait="24h",
    )

    from fastapi.responses import JSONResponse
    return JSONResponse(content=response_data.model_dump(), status_code=status_code)


# ---------------------------------------------------------------------------
# 동적 경로 엔드포인트 (정적 경로 이후에 선언해야 함)
# ---------------------------------------------------------------------------


@chart_analysis_router.post("/{symbol}", response_model=ChartAnalysisResponseDTO)
async def analyze_chart(
    symbol: str,
    svc: AnalyzeChartService = Depends(get_analyze_chart_service),
) -> ChartAnalysisResponseDTO:
    """종목 코드로 7 윈도우 수치 분석 결과를 즉시 반환한다."""
    try:
        return svc.get_analyses(symbol.strip().upper())
    except Exception as exc:  # noqa: BLE001
        logger.error("analyze_chart:error symbol=%s error=%s", symbol, str(exc))
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@chart_analysis_router.post("/{symbol}/report")
async def report_chart(
    symbol: str,
    svc: GenerateReportService = Depends(get_generate_report_service),
) -> StreamingResponse:
    """LLM 자연어 리포트를 SSE 스트림으로 반환한다."""
    clean_symbol = symbol.strip().upper()

    async def _event_generator() -> AsyncIterator[str]:
        async for event in svc.stream(clean_symbol, "1M", "D"):
            yield _format_sse(event)

    return StreamingResponse(
        _event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
        },
    )


# ---------------------------------------------------------------------------
# SSE 포매터
# ---------------------------------------------------------------------------


def _format_sse(event: dict) -> str:
    """SSE 이벤트 딕셔너리를 text/event-stream 포맷으로 변환한다."""
    event_type = event.get("event", "message")
    data = event.get("data", {})
    return f"event: {event_type}\ndata: {json.dumps(data, ensure_ascii=False)}\n\n"