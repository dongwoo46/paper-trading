from __future__ import annotations

import asyncio
import contextlib
import logging
import os
import time
from datetime import datetime
from typing import Literal

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from src.application.daily_fetch_service import DailyFetchOptions, execute


class CollectDailyRequest(BaseModel):
    provider: Literal["yfinance", "pykrx", "all"] = "all"
    start: str = "2010-01-01"
    end: str = Field(default_factory=lambda: datetime.now().date().isoformat())
    only_default: bool = False
    auto_adjust: bool = False
    adjusted: bool = False


class CollectDailyResponse(BaseModel):
    provider: str
    symbols: int
    success_symbols: int
    failed_symbols: int
    total_rows_inserted: int
    start: str
    end: str


app = FastAPI(title="Collector Worker API", version="1.0.0")
_periodic_task: asyncio.Task[None] | None = None
_periodic_lock = asyncio.Lock()
logger = logging.getLogger(__name__)


class _ColorFormatter(logging.Formatter):
    RESET = "\x1b[0m"
    COLORS = {
        logging.DEBUG: "\x1b[36m",     # cyan
        logging.INFO: "\x1b[32m",      # green
        logging.WARNING: "\x1b[33m",   # yellow
        logging.ERROR: "\x1b[31m",     # red
        logging.CRITICAL: "\x1b[35m",  # magenta
    }

    def format(self, record: logging.LogRecord) -> str:
        color = self.COLORS.get(record.levelno, self.RESET)
        original_levelname = record.levelname
        record.levelname = f"{color}{original_levelname}{self.RESET}"
        try:
            return super().format(record)
        finally:
            record.levelname = original_levelname


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/collect/daily", response_model=CollectDailyResponse)
def collect_daily(request: CollectDailyRequest) -> CollectDailyResponse:
    try:
        result = execute(
            DailyFetchOptions(
                provider=request.provider,
                start=request.start,
                end=request.end,
                only_default=request.only_default,
                auto_adjust=request.auto_adjust,
                adjusted=request.adjusted,
            )
        )
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=str(exc)) from exc

    return CollectDailyResponse(**result)


@app.on_event("startup")
async def startup_event() -> None:
    _configure_logging()
    global _periodic_task
    if _is_periodic_enabled():
        logger.info(
            "auto_collect:start enabled=true interval_seconds=%d provider=%s only_default=%s",
            _periodic_interval_seconds(),
            _periodic_provider(),
            _periodic_only_default(),
        )
        _periodic_task = asyncio.create_task(_run_periodic_collect_loop())
    else:
        logger.info("auto_collect:start enabled=false")


@app.on_event("shutdown")
async def shutdown_event() -> None:
    global _periodic_task
    if _periodic_task is not None:
        _periodic_task.cancel()
        with contextlib.suppress(asyncio.CancelledError):
            await _periodic_task
        _periodic_task = None
        logger.info("auto_collect:stopped")


def _is_periodic_enabled() -> bool:
    return os.getenv("AUTO_COLLECT_ENABLED", "false").lower() in ("1", "true", "yes", "on")


def _periodic_interval_seconds() -> int:
    return int(os.getenv("AUTO_COLLECT_INTERVAL_SECONDS", "900"))


def _periodic_provider() -> str:
    return os.getenv("AUTO_COLLECT_PROVIDER", "all")


def _periodic_only_default() -> bool:
    return os.getenv("AUTO_COLLECT_ONLY_DEFAULT", "true").lower() in ("1", "true", "yes", "on")


async def _run_periodic_collect_loop() -> None:
    interval = _periodic_interval_seconds()
    while True:
        started_at = time.perf_counter()
        try:
            async with _periodic_lock:
                result = execute(
                    DailyFetchOptions(
                        provider=_periodic_provider(),
                        only_default=_periodic_only_default(),
                    )
                )
                elapsed_ms = int((time.perf_counter() - started_at) * 1000)
                logger.info(
                    "auto_collect:tick done provider=%s symbols=%s success=%s failed=%s elapsed_ms=%d",
                    result["provider"],
                    result["symbols"],
                    result["success_symbols"],
                    result["failed_symbols"],
                    elapsed_ms,
                )
        except Exception as exc:  # noqa: BLE001
            elapsed_ms = int((time.perf_counter() - started_at) * 1000)
            logger.exception("auto_collect:tick error elapsed_ms=%d error=%s", elapsed_ms, exc)
        await asyncio.sleep(interval)


def _configure_logging() -> None:
    root = logging.getLogger()
    if not root.handlers:
        logging.basicConfig(
            level=logging.WARNING,
            format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
        )
    else:
        root.setLevel(logging.WARNING)

    if os.getenv("NO_COLOR", "").lower() not in ("1", "true", "yes", "on"):
        formatter = _ColorFormatter("%(asctime)s %(levelname)s [%(name)s] %(message)s")
        for handler in root.handlers:
            handler.setFormatter(formatter)

    # Keep app/service logs visible without enabling noisy/broken third-party INFO logs.
    logging.getLogger("src").setLevel(logging.INFO)
    logging.getLogger("src.interfaces.api.app").setLevel(logging.INFO)
    logging.getLogger("src.application.daily_fetch_service").setLevel(logging.INFO)
