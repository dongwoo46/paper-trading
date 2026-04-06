from __future__ import annotations

import argparse
from datetime import datetime

from src.application.daily_fetch_service import DailyFetchOptions, execute


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Read enabled symbols from PostgreSQL catalog and fetch daily OHLCV data. "
            "Default window: 2010-01-01 to today."
        )
    )
    parser.add_argument(
        "--provider",
        choices=["yfinance", "pykrx", "all"],
        default="all",
        help="Provider to run.",
    )
    parser.add_argument("--start", default="2010-01-01", help="Start date (YYYY-MM-DD)")
    parser.add_argument(
        "--end",
        default=datetime.now().date().isoformat(),
        help="End date (YYYY-MM-DD). Omit to use latest date.",
    )
    parser.add_argument(
        "--output-root",
        default="data",
        help="Root output directory. CSVs are saved per provider and symbol.",
    )
    parser.add_argument(
        "--only-default",
        action="store_true",
        help="Fetch only `is_default = true` symbols.",
    )
    parser.add_argument(
        "--auto-adjust",
        action="store_true",
        help="yfinance option for adjusted prices.",
    )
    parser.add_argument(
        "--adjusted",
        action="store_true",
        help="pykrx option for adjusted prices.",
    )
    return parser.parse_args()


def run() -> int:
    args = parse_args()
    result = execute(
        DailyFetchOptions(
            provider=args.provider,
            start=args.start,
            end=args.end,
            output_root=args.output_root,
            only_default=args.only_default,
            auto_adjust=args.auto_adjust,
            adjusted=args.adjusted,
        )
    )
    print(f"symbols={result['symbols']}")
    print(f"success_symbols={result['success_symbols']}")
    print(f"failed_symbols={result['failed_symbols']}")
    print(f"summary={result['summary_path']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(run())
