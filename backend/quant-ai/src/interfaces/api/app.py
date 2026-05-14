from __future__ import annotations

import logging
import os

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

logger = logging.getLogger(__name__)


app = FastAPI(title="Quant AI Service", version="1.0.0")

from src.chart_analysis.interfaces.chart_analysis_router import chart_analysis_router  # noqa: E402
app.include_router(chart_analysis_router)


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

    logging.getLogger("src").setLevel(logging.INFO)
    logging.getLogger("src.interfaces.api.app").setLevel(logging.INFO)


_configure_logging()
