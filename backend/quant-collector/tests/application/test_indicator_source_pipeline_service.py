from unittest.mock import patch

from src.application.indicator_source_pipeline_service import IndicatorPipelineOptions, execute


def test_service_routes_to_job_with_provider():
    with patch("src.application.indicator_source_pipeline_service.IndicatorSourcePipelineJob") as mjob, patch(
        "src.application.indicator_source_pipeline_service.build_default_collectors"
    ) as mcollectors, patch("src.application.indicator_source_pipeline_service.build_default_repositories") as mrepos:
        mjob.return_value.run.return_value = {"provider": "yfinance", "rows": 0}
        out = execute(IndicatorPipelineOptions(provider="yfinance", start="2026-01-01", end="2026-01-02"))
    assert out["provider"] == "yfinance"
