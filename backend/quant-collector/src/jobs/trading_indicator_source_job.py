from __future__ import annotations

import os

from src.application.daily_fetch_service import load_db_config_from_env
from src.application.trading_indicator_source_service import TradingIndicatorSourceService
from src.collectors.trading_indicator_collectors import (
    PykrxTradingIndicatorCollector,
    YFinanceTradingIndicatorCollector,
)
from src.repositories.market_alternative_flow_repository import MarketAlternativeFlowRepository
from src.repositories.market_metadata_repository import MarketMetadataRepository
from src.repositories.market_microstructure_repository import MarketMicrostructureRepository
from src.repositories.market_relative_strength_repository import MarketRelativeStrengthRepository
from src.repositories.market_session_ohlcv_repository import MarketSessionOhlcvRepository


def execute() -> dict[str, object]:
    provider = os.getenv("INDICATOR_PROVIDER", "all")
    config = load_db_config_from_env()
    service = TradingIndicatorSourceService(
        yfinance_collector=YFinanceTradingIndicatorCollector(),
        pykrx_collector=PykrxTradingIndicatorCollector(),
        microstructure_repository=MarketMicrostructureRepository(config),
        session_ohlcv_repository=MarketSessionOhlcvRepository(config),
        relative_strength_repository=MarketRelativeStrengthRepository(config),
        alternative_flow_repository=MarketAlternativeFlowRepository(config),
        metadata_repository=MarketMetadataRepository(config),
    )
    return service.collect(provider=provider)
