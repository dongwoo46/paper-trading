import { describe, expect, it } from "vitest";
import { buildReturnSeries } from "./normalizeSeries";

describe("buildReturnSeries", () => {
  it("sets base-day returns to 0 and builds cumulative returns", () => {
    const result = buildReturnSeries({
      balances: [
        { date: "2026-04-01", evaluationAmount: 1000, deposit: 800 },
        { date: "2026-04-02", evaluationAmount: 1100, deposit: 800 },
      ],
      benchmark: [
        { date: "2026-04-01", close: 2000 },
        { date: "2026-04-02", close: 2100 },
      ],
    });

    expect(result).toEqual([
      { date: "2026-04-01", evaluationAmount: 1000, portfolioReturnPct: 0, benchmarkReturnPct: 0 },
      { date: "2026-04-02", evaluationAmount: 1100, portfolioReturnPct: 10, benchmarkReturnPct: 5 },
    ]);
  });

  it("skips date points missing benchmark data", () => {
    const result = buildReturnSeries({
      balances: [
        { date: "2026-04-01", evaluationAmount: 1000, deposit: 800 },
        { date: "2026-04-02", evaluationAmount: 1100, deposit: 800 },
      ],
      benchmark: [{ date: "2026-04-01", close: 2000 }],
    });

    expect(result).toEqual([
      { date: "2026-04-01", evaluationAmount: 1000, portfolioReturnPct: 0, benchmarkReturnPct: 0 },
    ]);
  });

  it("builds portfolio-only series when benchmark is entirely missing", () => {
    const result = buildReturnSeries({
      balances: [
        { date: "2026-04-01", evaluationAmount: 1000, deposit: 800 },
        { date: "2026-04-02", evaluationAmount: 1100, deposit: 800 },
      ],
      benchmark: [],
    });

    expect(result).toEqual([
      { date: "2026-04-01", evaluationAmount: 1000, portfolioReturnPct: 0, benchmarkReturnPct: 0 },
      { date: "2026-04-02", evaluationAmount: 1100, portfolioReturnPct: 10, benchmarkReturnPct: 0 },
    ]);
  });

  it("guards division by zero on base values", () => {
    const result = buildReturnSeries({
      balances: [
        { date: "2026-04-01", evaluationAmount: 0, deposit: 0 },
        { date: "2026-04-02", evaluationAmount: 100, deposit: 0 },
      ],
      benchmark: [
        { date: "2026-04-01", close: 0 },
        { date: "2026-04-02", close: 100 },
      ],
    });

    expect(result).toEqual([
      { date: "2026-04-01", evaluationAmount: 0, portfolioReturnPct: 0, benchmarkReturnPct: 0 },
      { date: "2026-04-02", evaluationAmount: 100, portfolioReturnPct: 0, benchmarkReturnPct: 0 },
    ]);
  });

  it("deduplicates same-date points by keeping the latest entry", () => {
    const result = buildReturnSeries({
      balances: [
        { date: "2026-04-01", evaluationAmount: 1000, deposit: 800 },
        { date: "2026-04-01", evaluationAmount: 1300, deposit: 800 },
        { date: "2026-04-02", evaluationAmount: 1430, deposit: 800 },
      ],
      benchmark: [
        { date: "2026-04-01", close: 2000 },
        { date: "2026-04-01", close: 2500 },
        { date: "2026-04-02", close: 2750 },
      ],
    });

    expect(result).toEqual([
      { date: "2026-04-01", evaluationAmount: 1300, portfolioReturnPct: 0, benchmarkReturnPct: 0 },
      { date: "2026-04-02", evaluationAmount: 1430, portfolioReturnPct: 10, benchmarkReturnPct: 10 },
    ]);
  });
});
