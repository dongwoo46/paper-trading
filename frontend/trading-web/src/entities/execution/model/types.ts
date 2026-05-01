export type ExecutionSide = 'BUY' | 'SELL';

export interface ExecutionEvent {
  executionId: number;
  orderId: number;
  ticker: string;
  tickerName: string | null;
  side: ExecutionSide;
  quantity: string;
  price: string;
  fee: string;
  currency: string;
  executedAt: string;
}
