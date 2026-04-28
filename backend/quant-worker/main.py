from __future__ import annotations

from scripts.fetch_daily_from_catalog import run as run_catalog_job


def main() -> int:
    """Run full daily collection job (default provider: all)."""
    return run_catalog_job()


if __name__ == "__main__":
    raise SystemExit(main())
