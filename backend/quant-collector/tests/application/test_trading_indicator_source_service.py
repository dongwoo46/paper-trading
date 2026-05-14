from __future__ import annotations

from src.application.trading_indicator_source_service import TradingIndicatorSourceService


def test_service_collects_for_selected_provider_only():
    calls: list[str] = []

    class _Collector:
        def collect(self):
            return {
                "microstructure": [{"source": "yfinance"}],
                "session_ohlcv": [{"source": "yfinance"}],
                "relative_strength": [{"source": "yfinance"}],
                "alternative_flow": [{"source": "yfinance"}],
                "metadata": [{"source": "yfinance"}],
            }

    class _Repo:
        def __init__(self, name: str):
            self._name = name

        def upsert_rows(self, rows, provider: str):
            calls.append(f"{self._name}:{provider}:{len(rows)}")
            return len(rows)

    service = TradingIndicatorSourceService(
        yfinance_collector=_Collector(),
        pykrx_collector=_Collector(),
        microstructure_repository=_Repo("micro"),
        session_ohlcv_repository=_Repo("session"),
        relative_strength_repository=_Repo("rs"),
        alternative_flow_repository=_Repo("flow"),
        metadata_repository=_Repo("meta"),
    )

    result = service.collect(provider="yfinance")
    assert result["provider"] == "yfinance"
    assert result["rows_inserted"] == 5
    assert len(calls) == 5
    assert all(":yfinance:" in call for call in calls)
