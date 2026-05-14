from datetime import date
from unittest.mock import MagicMock

from src.jobs.indicator_source_pipeline_job import IndicatorSourcePipelineJob


def test_job_runs_provider_specific_paths():
    c = MagicMock()
    r = MagicMock()
    c.collect_microstructure.return_value = []
    c.collect_session_ohlcv.return_value = []
    c.collect_relative_strength.return_value = []
    c.collect_alternative_flow.return_value = []
    c.collect_metadata.return_value = []
    for name in ["microstructure", "session_ohlcv", "relative_strength", "alternative_flow", "metadata"]:
        getattr(r, name).upsert.return_value = 0

    job = IndicatorSourcePipelineJob(c, r)
    out = job.run(provider="pykrx", start_date=date(2026, 1, 1), end_date=date(2026, 1, 2))
    assert out["provider"] == "pykrx"
    assert c.collect_microstructure.called
