import { describe, expect, it, vi } from "vitest";
import { fetchJson } from "./index";
import { fetchSubscriptionStatus } from "./subscriptionStatusApi";

vi.mock("./index", () => ({
  fetchJson: vi.fn()
}));

const mockFetchJson = vi.mocked(fetchJson);

describe("subscriptionStatusApi", () => {
  it("calls /api/subscriptions/status and returns mapped payload", async () => {
    const payload = {
      generatedAt: "2026-05-05T00:00:00Z",
      totalWsSlotUsed: 3,
      totalWsSlotMax: 50,
      modes: [
        {
          mode: "paper",
          connectionStatus: "CONNECTED",
          lastConnectedAt: "2026-05-05T00:00:00Z",
          reconnectAttempts: 1,
          wsSymbols: ["005930"],
          restSymbols: ["000660"],
          wsSlotUsed: 1,
          wsSlotMax: 25
        }
      ]
    };
    mockFetchJson.mockResolvedValueOnce(payload);

    const result = await fetchSubscriptionStatus();

    expect(mockFetchJson).toHaveBeenCalledWith("/api/subscriptions/status");
    expect(result).toEqual(payload);
  });
});
