from __future__ import annotations

import argparse
from datetime import date, datetime
from pathlib import Path

from src.collectors.pykrx_daily_collector import DailyCollectRequest, PykrxDailyCollector


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Fetch daily OHLCV from pykrx (default: 2010-01-01 to today)."
    )
    parser.add_argument("--symbol", required=True, help="KRX ticker (e.g., 005930)")
    parser.add_argument("--start", default="2010-01-01", help="Start date (YYYY-MM-DD)")
    parser.add_argument(
        "--end",
        default=datetime.now().date().isoformat(),
        help="End date (YYYY-MM-DD)",
    )
    parser.add_argument(
        "--output",
        default=None,
        help="Output CSV path. Default: data/pykrx/<symbol>_daily.csv",
    )
    parser.add_argument(
        "--adjusted",
        action="store_true",
        help="Request adjusted price data if supported by installed pykrx version.",
    )
    return parser.parse_args()


def run() -> int:
    args = parse_args()

    request = DailyCollectRequest(
        symbol=args.symbol,
        start_date=date.fromisoformat(args.start),
        end_date=date.fromisoformat(args.end),
        adjusted=args.adjusted,
    )

    collector = PykrxDailyCollector()
    dataset = collector.fetch(request)

    output_path = args.output or f"data/pykrx/{request.symbol}_daily.csv"
    saved = collector.save_csv(dataset, Path(output_path))

    print(f"rows={len(dataset)}")
    print(f"saved={saved.resolve()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(run())
