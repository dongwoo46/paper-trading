export type AccountType = "PAPER" | "LIVE" | "VIRTUAL";
export type TradingMode = "LOCAL" | "KIS_PAPER" | "KIS_LIVE";
export type MarketType = "KOSPI" | "KOSDAQ" | "NYSE" | "NASDAQ" | string;
export type PriceSource = "REDIS" | "NONE" | string;

export interface AccountResponse {
  id: number;
  accountName: string;
  accountType: AccountType;
  tradingMode: TradingMode;
  deposit: string;
  availableDeposit: string;
  lockedDeposit: string;
  baseCurrency: string;
  externalAccountId: string | null;
  isActive: boolean;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface PositionResponse {
  ticker: string;
  marketType: MarketType;
  quantity: string;
  orderableQuantity: string;
  lockedQuantity: string;
  avgBuyPrice: string;
  currentPrice: string | null;
  evaluationAmount: string | null;
  unrealizedPnl: string | null;
  returnRate: string | null;
  priceSource: PriceSource;
  priceUpdatedAt: string | null;
}
