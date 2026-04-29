export type MarketType = 'KOSPI' | 'KOSDAQ' | 'NASDAQ' | 'NYSE' | 'CRYPTO';
export type OrderType = 'MARKET' | 'LIMIT';
export type OrderSide = 'BUY' | 'SELL';
export type OrderCondition = 'DAY' | 'GTC' | 'IOC' | 'FOK' | 'GTD';
export type OrderStatus = 'PENDING' | 'PARTIAL' | 'FILLED' | 'CANCELLED' | 'REJECTED';
export type TradingMode = 'LOCAL' | 'KIS_PAPER' | 'KIS_LIVE' | 'UPBIT_LIVE';

export interface PlaceOrderRequest {
  ticker: string;
  marketType: MarketType;
  orderType: OrderType;
  orderSide: OrderSide;
  orderCondition: OrderCondition;
  quantity: string;         // BigDecimal → string
  limitPrice: string | null;
  expireAt: string | null;  // ISO 8601
  idempotencyKey: string;   // crypto.randomUUID()
}

export interface OrderResponse {
  orderId: number;
  ticker: string;
  marketType: MarketType;
  orderType: OrderType;
  orderSide: OrderSide;
  orderCondition: OrderCondition;
  orderStatus: OrderStatus;
  quantity: string;           // BigDecimal → string
  filledQuantity: string;
  limitPrice: string | null;
  avgFilledPrice: string | null;
  fee: string;
  createdAt: string;          // ISO 8601
}

export interface AccountResponse {
  id: number;
  accountName: string;
  tradingMode: TradingMode;
  deposit: string;
  availableDeposit: string;
  isActive: boolean;
}
