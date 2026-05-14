from __future__ import annotations

from dataclasses import dataclass
from datetime import date

import pandas as pd

from src.collectors.indicator_source_collectors import (
    AlternativeFlowCollector,
    MetadataCollector,
    MicrostructureCollector,
    RelativeStrengthCollector,
    SessionOhlcvCollector,
)
from src.repositories.indicator_source_repositories import RepositoryBundle


@dataclass
class CollectorBundle:
    microstructure: object
    session_ohlcv: object
    relative_strength: object
    alternative_flow: object
    metadata: object

    def collect_microstructure(self, provider: str, start_date: date, end_date: date) -> pd.DataFrame:
        return pd.DataFrame()

    def collect_session_ohlcv(self, provider: str, start_date: date, end_date: date) -> pd.DataFrame:
        return pd.DataFrame()

    def collect_relative_strength(self, provider: str, start_date: date, end_date: date) -> pd.DataFrame:
        return pd.DataFrame()

    def collect_alternative_flow(self, provider: str, start_date: date, end_date: date) -> pd.DataFrame:
        return pd.DataFrame()

    def collect_metadata(self, provider: str, start_date: date, end_date: date) -> pd.DataFrame:
        return pd.DataFrame()


class IndicatorSourcePipelineJob:
    def __init__(self, collectors: CollectorBundle, repositories: RepositoryBundle) -> None:
        self._collectors = collectors
        self._repos = repositories

    def run(self, provider: str, start_date: date, end_date: date) -> dict[str, object]:
        frames = {
            "microstructure": self._collectors.collect_microstructure(provider, start_date, end_date),
            "session_ohlcv": self._collectors.collect_session_ohlcv(provider, start_date, end_date),
            "relative_strength": self._collectors.collect_relative_strength(provider, start_date, end_date),
            "alternative_flow": self._collectors.collect_alternative_flow(provider, start_date, end_date),
            "metadata": self._collectors.collect_metadata(provider, start_date, end_date),
        }
        rows = 0
        rows += self._repos.microstructure.upsert(frames["microstructure"])
        rows += self._repos.session_ohlcv.upsert(frames["session_ohlcv"])
        rows += self._repos.relative_strength.upsert(frames["relative_strength"])
        rows += self._repos.alternative_flow.upsert(frames["alternative_flow"])
        rows += self._repos.metadata.upsert(frames["metadata"])
        return {"provider": provider, "rows": rows, "start": start_date.isoformat(), "end": end_date.isoformat()}
