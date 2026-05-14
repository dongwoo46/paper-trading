from __future__ import annotations

import os
from dataclasses import dataclass
from datetime import date

from src.catalog.postgres_symbol_catalog import DbConfig
from src.collectors.indicator_source_collectors import (
    AlternativeFlowCollector,
    MetadataCollector,
    MicrostructureCollector,
    RelativeStrengthCollector,
    SessionOhlcvCollector,
)
from src.jobs.indicator_source_pipeline_job import CollectorBundle, IndicatorSourcePipelineJob
from src.repositories.indicator_source_repositories import (
    AlternativeFlowSourceRepository,
    MetadataSourceRepository,
    MicrostructureSourceRepository,
    RelativeStrengthSourceRepository,
    RepositoryBundle,
    SessionOhlcvSourceRepository,
)


@dataclass(frozen=True)
class IndicatorPipelineOptions:
    provider: str = "all"
    start: str = "2020-01-01"
    end: str = "2030-01-01"


def load_db_config_from_env() -> DbConfig:
    return DbConfig(
        host=os.getenv("PG_HOST", "localhost"),
        port=int(os.getenv("PG_PORT", "5432")),
        database=os.getenv("PG_DATABASE", "paper"),
        user=os.getenv("PG_USER", "paper"),
        password=os.getenv("PG_PASSWORD", "paper"),
    )


def build_default_collectors() -> CollectorBundle:
    return CollectorBundle(
        microstructure=MicrostructureCollector(),
        session_ohlcv=SessionOhlcvCollector(),
        relative_strength=RelativeStrengthCollector(),
        alternative_flow=AlternativeFlowCollector(),
        metadata=MetadataCollector(),
    )


def build_default_repositories(config: DbConfig) -> RepositoryBundle:
    return RepositoryBundle(
        microstructure=MicrostructureSourceRepository(config),
        session_ohlcv=SessionOhlcvSourceRepository(config),
        relative_strength=RelativeStrengthSourceRepository(config),
        alternative_flow=AlternativeFlowSourceRepository(config),
        metadata=MetadataSourceRepository(config),
    )


def execute(options: IndicatorPipelineOptions) -> dict[str, object]:
    start_date = date.fromisoformat(options.start)
    end_date = date.fromisoformat(options.end)
    if start_date > end_date:
        raise ValueError("start date must be <= end date")

    job = IndicatorSourcePipelineJob(
        collectors=build_default_collectors(),
        repositories=build_default_repositories(load_db_config_from_env()),
    )
    return job.run(provider=options.provider, start_date=start_date, end_date=end_date)
