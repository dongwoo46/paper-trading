"""결과 조회 라우터 — 차트 분석 결과 조회 및 수동 트리거 엔드포인트.

엔드포인트:
- GET  /research/results/{symbol}  — 종목 분석 결과 조회
- GET  /research/symbols           — 분석 완료 종목 목록
- POST /research/run/{symbol}      — 단일 종목 분석 수동 트리거
"""
from __future__ import annotations

import logging
from typing import Any

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel

logger = logging.getLogger(__name__)

results_router = APIRouter(prefix="/research", tags=["research_results"])


# ---------------------------------------------------------------------------
# 응답 DTO
# ---------------------------------------------------------------------------

class ChartAnalysisResultDTO(BaseModel):
    symbol: str
    window: str
    interval: str
    snapshot_hash: str
    computed_at: str
    summary: dict[str, Any]
    levels: dict[str, Any]
    technical: dict[str, Any]
    volume: dict[str, Any]
    report: dict[str, Any]


# ---------------------------------------------------------------------------
# 의존성 팩토리
# ---------------------------------------------------------------------------

def get_chart_analysis_repo():
    """PostgresChartAnalysisRepository 의존성 팩토리."""
    from src.chart_analysis.infrastructure.chart_analysis_repository import (
        PostgresChartAnalysisRepository,
    )
    from src.infrastructure.db import load_db_config_from_env, connect

    db = load_db_config_from_env()

    def _connect():
        return connect(db)

    return PostgresChartAnalysisRepository(_connect)


def _get_connect_fn():
    """psycopg 연결 callable 반환 (symbols 엔드포인트 전용)."""
    from src.infrastructure.db import load_db_config_from_env, connect

    db = load_db_config_from_env()

    def _connect():
        return connect(db)

    return _connect


# ---------------------------------------------------------------------------
# 엔드포인트
# ---------------------------------------------------------------------------

@results_router.get("/results/{symbol}")
def get_results_by_symbol(
    symbol: str,
    chart_analysis_repo=Depends(get_chart_analysis_repo),
) -> dict:
    """종목 코드로 모든 윈도우 분석 결과를 반환한다.

    결과가 없으면 빈 목록을 반환한다 (404 아님).
    """
    clean_symbol = symbol.strip().upper()
    try:
        results = chart_analysis_repo.find_by_symbol(clean_symbol)
    except Exception as exc:  # noqa: BLE001
        logger.error("results_router:find_by_symbol_failed symbol=%s error=%s", clean_symbol, str(exc))
        raise HTTPException(status_code=500, detail=str(exc)) from exc

    analyses = [_result_to_dto(r) for r in results]
    return {"symbol": clean_symbol, "analyses": analyses}


@results_router.get("/symbols")
def get_symbols() -> dict:
    """차트 분석이 완료된 종목 목록과 분석 통계를 반환한다."""
    connect_fn = _get_connect_fn()
    try:
        with connect_fn() as conn:
            with conn.cursor() as cur:
                cur.execute(
                    "SELECT symbol, COUNT(*) AS windows, MAX(numeric_computed_at) AS last_analyzed "
                    "FROM chart_analysis_result "
                    "GROUP BY symbol "
                    "ORDER BY symbol ASC"
                )
                rows = cur.fetchall()
    except Exception as exc:  # noqa: BLE001
        logger.error("results_router:get_symbols_failed error=%s", str(exc))
        raise HTTPException(status_code=500, detail=str(exc)) from exc

    return {
        "symbols": [
            {
                "symbol": r[0],
                "windows": r[1],
                "last_analyzed": r[2].isoformat() if r[2] else None,
            }
            for r in rows
        ]
    }


@results_router.post("/run/{symbol}")
async def run_analysis(symbol: str) -> dict:
    """단일 종목 차트 분석 파이프라인을 수동으로 트리거한다 (개발/디버그용).

    symbol이 비어 있으면 422 반환.
    """
    from src.chart_analysis.application.market_pipeline_triggers import _get_pipeline_service

    clean_symbol = symbol.strip().upper()
    if not clean_symbol:
        raise HTTPException(status_code=422, detail="symbol must not be blank")

    try:
        service = _get_pipeline_service()
        result = await service.run_for_symbol(clean_symbol, is_popular=True)
    except Exception as exc:  # noqa: BLE001
        logger.error("results_router:run_analysis_failed symbol=%s error=%s", clean_symbol, str(exc))
        raise HTTPException(status_code=500, detail=str(exc)) from exc

    return {"symbol": clean_symbol, **result}


# ---------------------------------------------------------------------------
# 내부 헬퍼
# ---------------------------------------------------------------------------

def _result_to_dto(result: Any) -> dict:
    """ChartAnalysisResult → dict (to_response_dict 위임)."""
    return result.to_response_dict()
