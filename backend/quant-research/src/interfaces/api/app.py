"""quant-research FastAPI 앱 진입점."""
from __future__ import annotations

from contextlib import asynccontextmanager

from fastapi import FastAPI

from src.infrastructure.db import connect, load_db_config_from_env
from src.infrastructure.migration_runner import run_migrations
from src.interfaces.api.results_router import results_router
from src.jobs.chart_analysis_schedule import (
    start_chart_analysis_scheduler,
    stop_chart_analysis_scheduler,
)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """앱 시작 시 DB 마이그레이션 실행 후 스케줄러를 시작한다."""
    connect_fn = lambda: connect(load_db_config_from_env())  # noqa: E731
    run_migrations(connect_fn)
    scheduler = start_chart_analysis_scheduler()
    yield
    stop_chart_analysis_scheduler(scheduler)


app = FastAPI(title="Quant Research Service", version="1.0.0", lifespan=lifespan)
app.include_router(results_router)


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}
