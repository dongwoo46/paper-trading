import { describe, expect, it, vi } from "vitest";
import { fetchDailyBalances, fetchKospiBenchmark } from "./portfolioApi";
import { fetchJson } from "./index";

vi.mock("./index", () => ({
  fetchJson: vi.fn(),
}));

const mockFetchJson = vi.mocked(fetchJson);

describe("portfolioApi", () => {
  it("parses daily balance numeric strings safely", async () => {
    mockFetchJson.mockResolvedValueOnce([
      { date: "2026-04-01", evaluationAmount: "1000000.5", deposit: "900000" },
    ]);

    const result = await fetchDailyBalances(1, "2026-04-01", "2026-04-30");

    expect(result).toEqual([
      { date: "2026-04-01", evaluationAmount: 1000000.5, deposit: 900000 },
    ]);
  });

  it("throws when daily balance contains invalid number", async () => {
    mockFetchJson.mockResolvedValueOnce([
      { date: "2026-04-01", evaluationAmount: "abc", deposit: "900000" },
    ]);

    await expect(fetchDailyBalances(1, "2026-04-01", "2026-04-30")).rejects.toThrow(
      "Invalid numeric value"
    );
  });

  it("parses benchmark numeric strings safely", async () => {
    mockFetchJson.mockResolvedValueOnce([{ date: "2026-04-01", close: "2500.25" }]);

    const result = await fetchKospiBenchmark("2026-04-01", "2026-04-30");

    expect(result).toEqual([{ date: "2026-04-01", close: 2500.25 }]);
  });
});

