const QUANT_API_BASE = import.meta.env.VITE_QUANT_API_BASE_URL ?? "http://localhost:8000";

async function fetchQuantJson<T>(path: string): Promise<T> {
  const response = await fetch(`${QUANT_API_BASE}${path}`);
  if (!response.ok) {
    const message = await response.text();
    throw new Error(`${response.status} ${response.statusText}: ${message}`);
  }
  return (await response.json()) as T;
}

type InvestorFlowApiRecord = {
  trade_date: string;
  symbol: string;
  market: string;
  foreign_net_volume: number;
  institution_net_volume: number;
  individual_net_volume: number;
};

export type InvestorFlowPoint = {
  time: string;
  foreign: number;
  institution: number;
  individual: number;
};

export async function fetchInvestorFlow(
  symbol: string,
  from?: string,
  to?: string,
  limit = 250
): Promise<InvestorFlowPoint[]> {
  const encoded = encodeURIComponent(symbol.trim());
  const params = new URLSearchParams();
  if (from !== undefined) params.set("from", from);
  if (to !== undefined) params.set("to", to);
  params.set("limit", String(limit));
  const path = `/investor-flow/${encoded}?${params.toString()}`;
  const records = await fetchQuantJson<InvestorFlowApiRecord[]>(path);
  return records.map((r) => ({
    time: r.trade_date,
    foreign: r.foreign_net_volume,
    institution: r.institution_net_volume,
    individual: r.individual_net_volume,
  }));
}
