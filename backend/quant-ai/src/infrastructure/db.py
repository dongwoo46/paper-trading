from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class DbConfig:
    host: str
    port: int
    database: str
    user: str
    password: str


def load_db_config_from_env() -> DbConfig:
    return DbConfig(
        host=os.getenv("PG_HOST", "localhost"),
        port=int(os.getenv("PG_PORT", "5432")),
        database=os.getenv("PG_DATABASE", "paper"),
        user=os.getenv("PG_USER", "paper"),
        password=os.getenv("PG_PASSWORD", "paper"),
    )


def connect(config: DbConfig):
    """Open a psycopg connection from a DbConfig."""
    try:
        import psycopg
    except ImportError as exc:
        raise RuntimeError(
            "psycopg is required. Install with `pip install psycopg[binary]`."
        ) from exc
    return psycopg.connect(
        host=config.host,
        port=config.port,
        dbname=config.database,
        user=config.user,
        password=config.password,
    )
