import { fetchJson } from "./index";

export type MarketBarsInterval = "1m" | "5m" | "10m";

export interface MarketBar {
  startedAt: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
  tradeValue: number;
  vwap: number;
  tickCount: number;
}

interface FetchMarketBarsParams {
  symbol: string;
  interval: MarketBarsInterval;
  limit: number;
}

export async function fetchMarketBars(params: FetchMarketBarsParams): Promise<MarketBar[]> {
  const encodedSymbol = encodeURIComponent(params.symbol);
  const query = `interval=${params.interval}&limit=${params.limit}`;
  return fetchJson<MarketBar[]>(`/api/market/bars/${encodedSymbol}?${query}`);
}
