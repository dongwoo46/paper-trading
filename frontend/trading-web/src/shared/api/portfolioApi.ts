import type { BenchmarkPoint, DailyBalancePoint } from "../../entities/portfolio/model/types";
import { fetchJson } from "./index";

type DailyBalanceResponse = {
  date: string;
  evaluationAmount: string;
  deposit: string;
};

type BenchmarkResponse = {
  date: string;
  close: string;
};

function parseNumeric(value: string, field: string): number {
  const parsed = Number(value);
  if (Number.isNaN(parsed) || !Number.isFinite(parsed)) {
    throw new Error(`Invalid numeric value for ${field}: ${value}`);
  }
  return parsed;
}

export async function fetchDailyBalances(accountId: number, from: string, to: string): Promise<DailyBalancePoint[]> {
  const result = await fetchJson<DailyBalanceResponse[]>(
    `/api/v1/accounts/${accountId}/daily-balances?from=${from}&to=${to}`
  );

  return result.map((item) => ({
    date: item.date,
    evaluationAmount: parseNumeric(item.evaluationAmount, "evaluationAmount"),
    deposit: parseNumeric(item.deposit, "deposit"),
  }));
}

export async function fetchKospiBenchmark(from: string, to: string): Promise<BenchmarkPoint[]> {
  const result = await fetchJson<BenchmarkResponse[]>(`/api/v1/benchmarks/kospi?from=${from}&to=${to}`);

  return result.map((item) => ({
    date: item.date,
    close: parseNumeric(item.close, "close"),
  }));
}

