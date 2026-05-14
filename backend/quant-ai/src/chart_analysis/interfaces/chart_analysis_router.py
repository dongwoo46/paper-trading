"""차트 분석 API 라우터.

엔드포인트:
- GET  /chart-analysis/notifications           — LLM 완료 알림 SSE 스트림
- POST /chart-analysis/request-llm-report      — 비인기 종목 LLM 요청 큐 등록
- POST /chart-analysis/{symbol}/report         — LLM 리포트 백그라운드 생성 요청
"""
from __future__ import annotations

import asyncio
import json
import logging

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException
from fastapi.responses import JSONResponse, StreamingResponse

from src.chart_analysis.application.generate_report_service import GenerateReportService
from src.chart_analysis.interfaces.dto import (
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

def get_generate_report_service() -> GenerateReportService:
    """GenerateReportService 의존성 팩토리."""
    from src.chart_analysis.infrastructure.chart_analysis_repository import (
        PostgresChartAnalysisRepository,
    )
    from src.chart_analysis.infrastructure.redis_job_store import RedisJobStore
    from src.chart_analysis.infrastructure.rule_template_report_generator import (
        RuleTemplateReportGenerator,
    )
    from src.chart_analysis.infrastructure.slack_notifier import SlackWebhookNotifier
    from src.infrastructure.db import load_db_config_from_env
    from src.infrastructure.db import connect

    db = load_db_config_from_env()

    def _connect():
        return connect(db)

    chart_repo = PostgresChartAnalysisRepository(connect_fn=_connect)
    redis_store = RedisJobStore()
    try:
        from src.chart_analysis.infrastructure.langchain_ollama_report_generator import (
            LangChainOllamaReportGenerator,
        )
        llm = LangChainOllamaReportGenerator()
    except ModuleNotFoundError:
        logger.warning("chart_analysis:langchain_ollama_missing fallback=rule_template")
        llm = RuleTemplateReportGenerator()
    slack = SlackWebhookNotifier()

    def _is_popular(symbol: str) -> bool:
        return True

    def _symbol_name(symbol: str) -> str | None:
        """pykrx → yfinance 순서로 카탈로그에서 종목명을 조회한다."""
        try:
            with _connect() as conn:
                with conn.cursor() as cur:
                    cur.execute(
                        "SELECT name FROM pykrx_symbol_catalog WHERE symbol = %s LIMIT 1",
                        (symbol,),
                    )
                    row = cur.fetchone()
                    if row:
                        return row[0]
                    cur.execute(
                        "SELECT name FROM yfinance_symbol_catalog WHERE ticker = %s LIMIT 1",
                        (symbol,),
                    )
                    row = cur.fetchone()
                    if row:
                        return row[0]
        except Exception as exc:  # noqa: BLE001
            logger.warning("symbol_name_lookup:error symbol=%s error=%s", symbol, exc)
        return None

    return GenerateReportService(
        chart_analysis_repository=chart_repo,
        llm_report_generator=llm,
        redis_job_store=redis_store,
        popular_symbols_fn=_is_popular,
        slack_notifier=slack,
        symbol_name_fn=_symbol_name,
    )


def get_queue_repository():
    """AnalysisRequestQueueRepository 의존성 팩토리."""
    from src.chart_analysis.infrastructure.analysis_request_queue_repository import (
        PostgresAnalysisRequestQueueRepository,
    )
    from src.infrastructure.db import load_db_config_from_env
    from src.infrastructure.db import connect

    db = load_db_config_from_env()

    def _connect():
        return connect(db)

    return PostgresAnalysisRequestQueueRepository(connect_fn=_connect)


# ---------------------------------------------------------------------------
# 엔드포인트
# ---------------------------------------------------------------------------

@chart_analysis_router.get("/notifications")
async def notification_stream() -> StreamingResponse:
    """LLM 완료 알림 SSE 스트림.

    연결 후 LLM 분석이 완료될 때마다 llm_done 이벤트를 수신한다.
    30초마다 keepalive 코멘트를 전송해 연결을 유지한다.
    """
    from src.chart_analysis.infrastructure.notification_hub import subscribe, unsubscribe

    q = subscribe()

    async def _stream():
        try:
            while True:
                try:
                    event = await asyncio.wait_for(q.get(), timeout=30)
                    yield f"event: {event['type']}\ndata: {json.dumps(event, ensure_ascii=False)}\n\n"
                except asyncio.TimeoutError:
                    yield ": keepalive\n\n"
        except asyncio.CancelledError:
            pass
        finally:
            unsubscribe(q)

    return StreamingResponse(
        _stream(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


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

    return JSONResponse(content=response_data.model_dump(), status_code=status_code)


# ---------------------------------------------------------------------------
# 동적 경로 엔드포인트 (정적 경로 이후에 선언해야 함)
# ---------------------------------------------------------------------------

@chart_analysis_router.post("/{symbol}/report")
async def trigger_llm_report(
    symbol: str,
    window: str = "1M",
    interval: str = "D",
    background_tasks: BackgroundTasks = None,
    svc: GenerateReportService = Depends(get_generate_report_service),
) -> JSONResponse:
    """LLM 리포트 백그라운드 생성을 요청한다.

    즉시 {"status": ...} JSON을 반환하고, LLM 생성은 서버 백그라운드에서 진행된다.
    완료 시 DB에 저장 + Slack 알림 전송.

    status 값:
    - queued       : 백그라운드 생성 시작됨
    - pending      : 이미 처리 중 (Redis 락 획득 실패)
    - already_done : LLM 리포트가 이미 존재함
    - not_found    : 분석 결과 없음 (먼저 분석 실행 필요)
    - not_popular  : 비인기 종목
    """
    clean_symbol = symbol.strip().upper()
    clean_window = window.strip().upper()
    clean_interval = interval.strip().upper()

    if clean_window not in _VALID_WINDOWS:
        raise HTTPException(status_code=400, detail=f"Invalid window: {clean_window!r}")
    if clean_interval not in _VALID_INTERVALS:
        raise HTTPException(status_code=400, detail=f"Invalid interval: {clean_interval!r}")

    prepared = await svc.prepare(clean_symbol, clean_window, clean_interval)

    if prepared["runnable"]:
        background_tasks.add_task(
            svc.run_background,
            clean_symbol, clean_window, clean_interval, prepared["snapshot_hash"],
        )
        logger.info(
            "trigger_llm_report:queued symbol=%s window=%s interval=%s",
            clean_symbol, clean_window, clean_interval,
        )

    status_code = 202 if prepared["runnable"] else 200
    return JSONResponse({"status": prepared["status"]}, status_code=status_code)
