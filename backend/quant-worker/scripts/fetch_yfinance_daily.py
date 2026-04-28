from __future__ import annotations

import argparse
from datetime import date, datetime
from pathlib import Path

from src.collectors.yfinance_daily_collector import DailyCollectRequest, YFinanceDailyCollector


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Fetch daily OHLCV from yfinance (default: 2010-01-01 to today)."
    )
    parser.add_argument("--symbol", required=True, help="Ticker symbol (e.g., AAPL, MSFT)")
    parser.add_argument("--start", default="2010-01-01", help="Start date (YYYY-MM-DD)")
    parser.add_argument(
        "--end",
        default=datetime.now().date().isoformat(),
        help="End date (YYYY-MM-DD)",
    )
    parser.add_argument(
        "--output",
        default=None,
        help="Output CSV path. Default: data/yfinance/<symbol>_daily.csv",
    )
    parser.add_argument(
        "--auto-adjust",
        action="store_true",
        help="Use adjusted prices from yfinance.",
    )
    return parser.parse_args()


def run() -> int:
    args = parse_args()

    start_date = date.fromisoformat(args.start)
    end_date = date.fromisoformat(args.end)

    request = DailyCollectRequest(
        symbol=args.symbol.upper(),
        start_date=start_date,
        end_date=end_date,
        auto_adjust=args.auto_adjust,
    )

    collector = YFinanceDailyCollector()
    dataset = collector.fetch(request)

    output_path = args.output or f"data/yfinance/{request.symbol}_daily.csv"
    saved = collector.save_csv(dataset, Path(output_path))

    print(f"rows={len(dataset)}")
    print(f"saved={saved.resolve()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(run())
