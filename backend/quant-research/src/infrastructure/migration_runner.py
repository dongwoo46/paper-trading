"""Migration runner — quant-research DB 스키마 자동 생성."""
from __future__ import annotations

import logging
from pathlib import Path

logger = logging.getLogger(__name__)

_MIGRATIONS_DIR = Path(__file__).parent.parent.parent.parent / "migrations"


def run_migrations(connect_fn) -> None:
    """migrations/ 디렉터리의 *.sql 파일을 파일명 오름차순으로 실행한다.

    각 SQL 파일 실행 후 commit한다.
    """
    sql_files = sorted(_MIGRATIONS_DIR.glob("*.sql"))
    if not sql_files:
        logger.warning("No migration files found in %s", _MIGRATIONS_DIR)
        return

    conn = connect_fn()
    try:
        for sql_file in sql_files:
            try:
                sql = sql_file.read_text(encoding="utf-8")
                conn.execute(sql)
                conn.commit()
                logger.info("Migration applied: %s", sql_file.name)
            except Exception as exc:
                logger.error("Migration failed: %s — %s", sql_file.name, exc)
                raise
    finally:
        conn.close()
