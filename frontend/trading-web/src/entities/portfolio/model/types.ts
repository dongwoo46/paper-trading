export interface DailyBalancePoint {
  date: string;
  evaluationAmount: number;
  deposit: number;
}

export interface BenchmarkPoint {
  date: string;
  close: number;
}

export interface ReturnSeriesPoint {
  date: string;
  evaluationAmount: number;
  portfolioReturnPct: number;
  benchmarkReturnPct: number;
}

export interface BuildReturnSeriesInput {
  balances: DailyBalancePoint[];
  benchmark: BenchmarkPoint[];
}

