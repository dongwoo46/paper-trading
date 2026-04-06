export type Mode = "paper" | "live";

export type KrSymbol = {
  symbol: string;
  name: string;
  market: string;
};

export type ChangeResponse = {
  status: string;
  mode: string;
  symbol: string;
  totalRegistrations: number;
  maxRegistrations: number;
  subscriptions: Record<string, string[]>;
};

export type SymbolCatalogItem = {
  symbol?: string;
  ticker?: string;
  name: string;
  market: string;
  enabled: boolean;
  isDefault: boolean;
};

export type FredCatalogItem = {
  seriesId: string;
  title: string;
  category: string;
  frequency: string;
  units: string;
  enabled: boolean;
  isDefault: boolean;
};

export type UpbitCatalogItem = {
  market: string;
  name?: string;
  koreanName?: string;
  englishName?: string;
  marketGroup: string;
  enabled: boolean;
  isDefault: boolean;
};

export type CatalogResponse<T> = {
  items: T[];
  returnedCount: number;
  totalCatalogCount: number;
  totalSubscribedCount: number;
};

export type OhlcvSymbolSummary = {
  symbol: string;
  market: string;
  latestTradeDate: string;
  totalBars: number;
};

export type OhlcvDailyBar = {
  source: string;
  symbol: string;
  market: string;
  tradeDate: string;
  openPrice: number;
  highPrice: number;
  lowPrice: number;
  closePrice: number;
  volume: number;
  adjClosePrice?: number | null;
  provider: string;
  interval: string;
  isAdjusted: boolean;
  collectedAt: string;
};
