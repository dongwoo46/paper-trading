import type { BuildReturnSeriesInput, ReturnSeriesPoint } from "../../../entities/portfolio/model/types";

function roundTo2(value: number): number {
  return Math.round(value * 100) / 100;
}

function dedupeByDateLatest<T extends { date: string }>(items: T[]): T[] {
  const byDate = new Map<string, T>();
  for (const item of items) {
    byDate.set(item.date, item);
  }
  return [...byDate.values()].sort((a, b) => a.date.localeCompare(b.date));
}

function calculateReturnPct(current: number, base: number): number {
  if (!Number.isFinite(base) || base === 0) {
    return 0;
  }
  const value = ((current - base) / base) * 100;
  if (!Number.isFinite(value)) {
    return 0;
  }
  return roundTo2(value);
}

export function buildReturnSeries(input: BuildReturnSeriesInput): ReturnSeriesPoint[] {
  const sortedBalances = dedupeByDateLatest(input.balances);
  const sortedBenchmark = dedupeByDateLatest(input.benchmark);
  const benchmarkByDate = new Map(sortedBenchmark.map((item) => [item.date, item.close]));
  const hasBenchmark = sortedBenchmark.length > 0;

  if (sortedBalances.length === 0) {
    return [];
  }

  let baseBalance: number | null = null;
  let baseBenchmark: number | null = null;
  const series: ReturnSeriesPoint[] = [];

  for (const point of sortedBalances) {
    const benchmarkClose = benchmarkByDate.get(point.date) ?? null;
    if (hasBenchmark && benchmarkClose === null) {
      continue;
    }

    if (baseBalance === null || baseBenchmark === null) {
      baseBalance = point.evaluationAmount;
      baseBenchmark = benchmarkClose ?? 0;
      series.push({
        date: point.date,
        evaluationAmount: point.evaluationAmount,
        portfolioReturnPct: 0,
        benchmarkReturnPct: 0,
      });
      continue;
    }

    series.push({
      date: point.date,
      evaluationAmount: point.evaluationAmount,
      portfolioReturnPct: calculateReturnPct(point.evaluationAmount, baseBalance),
      benchmarkReturnPct: hasBenchmark ? calculateReturnPct(benchmarkClose ?? 0, baseBenchmark) : 0,
    });
  }

  return series;
}
