import { fetchJson } from "./index";

export type MarketSource = "pykrx" | "yfinance";
export type UnifiedInterval = "1m" | "5m" | "10m" | "1d" | "1w";

export type SymbolSummary = {
  symbol: string;
  name: string;
};

export type OhlcvPoint = {
  startedAt: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
  tradeValue: number;
};

type DailyApiItem = {
  tradeDate: string;
  open: string;
  high: string;
  low: string;
  close: string;
  volume: string;
  tradeValue: string;
};

type WeeklyApiItem = {
  tradeDate: string;
  open: string;
  high: string;
  low: string;
  close: string;
  volume: string;
  tradeValue: string;
};

type MinuteApiItem = {
  startedAt: string;
  open: string;
  high: string;
  low: string;
  close: string;
  volume: string;
  tradeValue: string;
};

type MinuteApiResponse = {
  symbol: string;
  interval: string;
  bars: MinuteApiItem[];
};

function toNumber(value: string): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function toPoint(item: DailyApiItem | WeeklyApiItem | MinuteApiItem, startedAt: string): OhlcvPoint {
  return {
    startedAt,
    open: toNumber(item.open),
    high: toNumber(item.high),
    low: toNumber(item.low),
    close: toNumber(item.close),
    volume: toNumber(item.volume),
    tradeValue: toNumber(item.tradeValue),
  };
}

export async function fetchMarketSymbols(source: MarketSource, limit = 300): Promise<SymbolSummary[]> {
  const path = `/api/${source}/ohlcv/symbols?limit=${limit}`;
  return fetchJson<SymbolSummary[]>(path);
}

export async function fetchDailyBars(source: MarketSource, symbol: string): Promise<OhlcvPoint[]> {
  const encoded = encodeURIComponent(symbol.trim());
  const path = `/api/${source}/ohlcv/daily?symbol=${encoded}`;
  const items = await fetchJson<DailyApiItem[]>(path);
  return items.map((item) => toPoint(item, item.tradeDate));
}

export async function fetchWeeklyBars(source: MarketSource, symbol: string, limit = 130): Promise<OhlcvPoint[]> {
  const encoded = encodeURIComponent(symbol.trim());
  const path = `/api/market/weekly/${encoded}?source=${source}&limit=${limit}`;
  const items = await fetchJson<WeeklyApiItem[]>(path);
  return items.map((item) => toPoint(item, item.tradeDate));
}

export async function fetchMinuteBars(symbol: string, interval: "1m" | "5m" | "10m", limit = 100): Promise<OhlcvPoint[]> {
  const encoded = encodeURIComponent(symbol.trim());
  const path = `/api/market/bars/${encoded}?interval=${interval}&limit=${limit}`;
  const response = await fetchJson<MinuteApiResponse>(path);
  return (response.bars ?? []).map((item) => toPoint(item, item.startedAt));
}
