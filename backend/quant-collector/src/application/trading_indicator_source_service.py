from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class CollectResult:
    provider: str
    rows_inserted: int


class TradingIndicatorSourceService:
    def __init__(
        self,
        yfinance_collector,
        pykrx_collector,
        microstructure_repository,
        session_ohlcv_repository,
        relative_strength_repository,
        alternative_flow_repository,
        metadata_repository,
    ) -> None:
        self._collectors = {
            "yfinance": yfinance_collector,
            "pykrx": pykrx_collector,
        }
        self._microstructure_repository = microstructure_repository
        self._session_ohlcv_repository = session_ohlcv_repository
        self._relative_strength_repository = relative_strength_repository
        self._alternative_flow_repository = alternative_flow_repository
        self._metadata_repository = metadata_repository

    def collect(self, provider: str = "all") -> dict[str, object]:
        providers = ["yfinance", "pykrx"] if provider == "all" else [provider]
        total = 0
        for item in providers:
            collector = self._collectors[item]
            payload = collector.collect()
            total += self._microstructure_repository.upsert_rows(payload["microstructure"], provider=item)
            total += self._session_ohlcv_repository.upsert_rows(payload["session_ohlcv"], provider=item)
            total += self._relative_strength_repository.upsert_rows(payload["relative_strength"], provider=item)
            total += self._alternative_flow_repository.upsert_rows(payload["alternative_flow"], provider=item)
            total += self._metadata_repository.upsert_rows(payload["metadata"], provider=item)
        return {"provider": provider, "rows_inserted": total}
