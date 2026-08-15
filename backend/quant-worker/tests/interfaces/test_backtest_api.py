from __future__ import annotations

from datetime import datetime
from decimal import Decimal
from uuid import UUID

from fastapi import FastAPI
from fastapi.testclient import TestClient

from src.backtest.api import (
    backtest_router,
    get_backtest_artifact_reader,
    get_backtest_execution_dispatcher,
    get_backtest_run_service,
)
from src.backtest.artifacts import BacktestLogs
from src.backtest.domain import BacktestSummary, RunStatus
from src.backtest.repository import InMemoryBacktestRunRepository
from src.backtest.service import BacktestRunService
from tests.backtest.test_domain import run_request_payload

RUN_ID = UUID("00000000-0000-0000-0000-000000000002")
NOW = datetime(2024, 1, 1, 9, 0)  # noqa: DTZ001 - project contract is KST-naive
app = FastAPI()
app.include_router(backtest_router)


class RecordingDispatcher:
    def __init__(self) -> None:
        self.run_ids: list[UUID] = []

    def dispatch(self, run_id: UUID) -> None:
        self.run_ids.append(run_id)


class FakeArtifactReader:
    def read_details(self, run: object) -> dict[str, object]:
        return {"TotalPerformance": {"PortfolioStatistics": {"EndEquity": "110.50"}}}

    def read_logs(self, run: object) -> BacktestLogs:
        return BacktestLogs(stdout="captured stdout", stderr="captured stderr")


def make_client() -> tuple[TestClient, BacktestRunService, RecordingDispatcher]:
    service = BacktestRunService(
        InMemoryBacktestRunRepository(),
        id_factory=lambda: RUN_ID,
        clock=lambda: NOW,
    )
    dispatcher = RecordingDispatcher()
    app.dependency_overrides[get_backtest_run_service] = lambda: service
    app.dependency_overrides[get_backtest_execution_dispatcher] = lambda: dispatcher
    app.dependency_overrides[get_backtest_artifact_reader] = FakeArtifactReader
    return TestClient(app), service, dispatcher


def clear_overrides() -> None:
    app.dependency_overrides.clear()


def test_create_and_lookup_run_returns_string_money_and_pending_status() -> None:
    client, _, dispatcher = make_client()
    try:
        create_response = client.post("/backtest-runs", json=run_request_payload())
        lookup_response = client.get(f"/backtest-runs/{RUN_ID}")
    finally:
        clear_overrides()

    assert create_response.status_code == 202
    assert create_response.json() == {
        "runId": str(RUN_ID),
        "status": "PENDING",
        "costProfile": "KR_DEFAULT_V1",
    }
    assert dispatcher.run_ids == [RUN_ID]
    assert lookup_response.status_code == 200
    body = lookup_response.json()
    assert body["currency"] == "KRW"
    assert body["initialCash"] == "100000000"
    assert body["costProfile"] == "KR_DEFAULT_V1"
    assert body["strategy"]["name"] == "price and technical strategy"
    assert body["createdAt"] == "2024-01-01T09:00:00"
    assert not isinstance(body["initialCash"], float)


def test_create_rejects_missing_unknown_and_market_mismatched_cost_profile() -> None:
    client, _, _ = make_client()
    missing = run_request_payload()
    del missing["costProfile"]
    unknown = run_request_payload()
    unknown["costProfile"] = "UNKNOWN"
    mismatch = run_request_payload()
    mismatch["costProfile"] = "US_DEFAULT_V1"
    try:
        responses = [
            client.post("/backtest-runs", json=missing),
            client.post("/backtest-runs", json=unknown),
            client.post("/backtest-runs", json=mismatch),
        ]
    finally:
        clear_overrides()

    assert [response.status_code for response in responses] == [422, 422, 422]
    assert [response.json()["detail"][0]["type"] for response in responses] == [
        "unknown_cost_profile",
        "unknown_cost_profile",
        "cost_profile_market_mismatch",
    ]


def test_invalid_dsl_is_422_with_stable_planned_factor_error() -> None:
    client, _, _ = make_client()
    payload = run_request_payload()
    payload["strategy"]["factors"][0]["category"] = "flow"  # type: ignore[index]
    try:
        response = client.post("/backtest-runs", json=payload)
    finally:
        clear_overrides()

    assert response.status_code == 422
    assert any(error["type"] == "unsupported_factor_category" for error in response.json()["detail"])


def test_unknown_run_returns_stable_404() -> None:
    client, _, _ = make_client()
    try:
        response = client.get("/backtest-runs/00000000-0000-0000-0000-000000000099")
    finally:
        clear_overrides()

    assert response.status_code == 404
    assert response.json()["detail"]["code"] == "BACKTEST_RUN_NOT_FOUND"


def test_pending_result_and_logs_return_stable_not_ready_errors() -> None:
    client, _, _ = make_client()
    try:
        client.post("/backtest-runs", json=run_request_payload())
        result_response = client.get(f"/backtest-runs/{RUN_ID}/result")
        logs_response = client.get(f"/backtest-runs/{RUN_ID}/logs")
    finally:
        clear_overrides()

    assert result_response.status_code == 409
    assert result_response.json()["detail"]["code"] == "BACKTEST_RESULT_NOT_READY"
    assert logs_response.status_code == 409
    assert logs_response.json()["detail"]["code"] == "BACKTEST_LOGS_NOT_READY"


def test_completed_result_serializes_all_metrics_as_strings() -> None:
    client, service, _ = make_client()
    try:
        client.post("/backtest-runs", json=run_request_payload())
        service.update_status(
            RUN_ID,
            RunStatus.RUNNING,
            artifact_path=f"/configured/runs/{RUN_ID}",
        )
        service.store_summary(
            RUN_ID,
            BacktestSummary(
                total_return=Decimal("0.10"),
                max_drawdown=Decimal("0.04"),
                annualized_return=Decimal("0.08"),
                sharpe=Decimal("1.25"),
                calmar=Decimal("2.00"),
                win_rate=Decimal("0.60"),
                total_trades=12,
            ),
        )
        response = client.get(f"/backtest-runs/{RUN_ID}/result")
    finally:
        clear_overrides()

    assert response.status_code == 200
    metrics = response.json()["summary"]
    assert metrics["totalReturn"] == "0.10"
    assert metrics["sharpe"] == "1.25"
    assert metrics["totalTrades"] == 12
    assert not any(isinstance(value, float) for value in metrics.values())
    assert response.json()["details"] == {
        "TotalPerformance": {"PortfolioStatistics": {"EndEquity": "110.50"}}
    }


def test_logs_endpoint_returns_captured_logs_for_failed_run() -> None:
    client, service, _ = make_client()
    try:
        client.post("/backtest-runs", json=run_request_payload())
        service.update_status(
            RUN_ID,
            RunStatus.FAILED,
            error_message="LEAN exited with code 17",
            artifact_path=f"/configured/runs/{RUN_ID}",
        )
        status_response = client.get(f"/backtest-runs/{RUN_ID}")
        result_response = client.get(f"/backtest-runs/{RUN_ID}/result")
        response = client.get(f"/backtest-runs/{RUN_ID}/logs")
    finally:
        clear_overrides()

    assert status_response.status_code == 200
    assert status_response.json()["status"] == "FAILED"
    assert status_response.json()["errorMessage"] == "LEAN exited with code 17"
    assert result_response.status_code == 409
    assert result_response.json()["detail"]["code"] == "BACKTEST_RUN_FAILED"
    assert response.status_code == 200
    assert response.json() == {
        "runId": str(RUN_ID),
        "status": "FAILED",
        "artifactPath": f"/configured/runs/{RUN_ID}",
        "stdout": "captured stdout",
        "stderr": "captured stderr",
    }
