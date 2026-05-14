from __future__ import annotations

from dotenv import load_dotenv
load_dotenv()

from src.interfaces.api.app import app

__all__ = ["app"]
