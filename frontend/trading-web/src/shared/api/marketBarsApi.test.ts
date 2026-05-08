import { describe, expect, it, vi } from "vitest";
import { fetchJson } from "./index";
import { fetchMarketBars } from "./marketBarsApi";

vi.mock("./index", () => ({
  fetchJson: vi.fn(),
}));

const mockFetchJson = vi.mocked(fetchJson);

describe("marketBarsApi", () => {
  it("calls market bars endpoint with symbol, interval and limit", async () => {
    mockFetchJson.mockResolvedValueOnce([]);

    await fetchMarketBars({ symbol: "005930", interval: "5m", limit: 200 });

    expect(mockFetchJson).toHaveBeenCalledWith("/api/market/bars/005930?interval=5m&limit=200");
  });

  it("encodes symbol path and applies query params", async () => {
    mockFetchJson.mockResolvedValueOnce([]);

    await fetchMarketBars({ symbol: "KRW/BTC", interval: "10m", limit: 50 });

    expect(mockFetchJson).toHaveBeenCalledWith("/api/market/bars/KRW%2FBTC?interval=10m&limit=50");
  });
});
