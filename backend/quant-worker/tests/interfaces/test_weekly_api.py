from __future__ import annotations

from datetime import date
from fastapi.testclient import TestClient
from unittest.mock import patch

from src.interfaces.api.app import app


def test_collect_weekly_route_exists():
    client = TestClient(app)
    with patch("src.interfaces.api.app.execute_weekly") as mock_execute:
        mock_execute.return_value = {
            "provider": "all",
            "symbols": 0,
            "success_symbols": 0,
            "failed_symbols": 0,
            "total_rows_inserted": 0,
            "start": "2024-01-01",
            "end": "2024-01-31",
        }
        response = client.post("/collect/weekly", json={"provider": "all", "start": "2024-01-01", "end": "2024-01-31"})
    assert response.status_code == 200
    assert response.json()["provider"] == "all"


def test_market_weekly_symbol_route_exists():
    client = TestClient(app)
    with patch("src.interfaces.api.app.fetch_market_weekly_bars") as mock_fetch:
        mock_fetch.return_value = []
        response = client.get("/market/weekly/AAPL")
    assert response.status_code == 200
    assert response.json() == []


def test_collect_weekly_rejects_invalid_date_window():
    client = TestClient(app)
    response = client.post("/collect/weekly", json={"provider": "pykrx", "start": "2024-02-01", "end": "2024-01-01"})
    assert response.status_code == 400


def test_market_weekly_rejects_blank_symbol():
    client = TestClient(app)
    response = client.get("/market/weekly/%20%20")
    assert response.status_code == 400
    assert response.json()["detail"] == "symbol must not be blank"


def test_market_weekly_rejects_from_gt_to():
    client = TestClient(app)
    response = client.get("/market/weekly/AAPL?from=2024-02-01&to=2024-01-01")
    assert response.status_code == 400
    assert response.json()["detail"] == "from must be <= to"


def test_market_weekly_clamps_limit_bounds():
    client = TestClient(app)
    with patch("src.interfaces.api.app.fetch_market_weekly_bars") as mock_fetch:
        mock_fetch.return_value = []
        response_low = client.get("/market/weekly/AAPL?limit=-5")
        response_high = client.get("/market/weekly/AAPL?limit=9999")

    assert response_low.status_code == 200
    assert response_high.status_code == 200
    first_call = mock_fetch.call_args_list[0].kwargs
    second_call = mock_fetch.call_args_list[1].kwargs
    assert first_call["limit"] == 1
    assert second_call["limit"] == 520
    assert isinstance(first_call["from_date"], date)
    assert isinstance(first_call["to_date"], date)
