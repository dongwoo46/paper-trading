export type JournalSentiment = "BULLISH" | "BEARISH" | "NEUTRAL" | "REFLECTIVE";

export interface TradingJournalListItem {
  journalId: number;
  accountId: number;
  orderId: number;
  ticker: string;
  journalType: string;
  sentiment: JournalSentiment;
  title: string;
  summary: string;
  createdAt: string;
  updatedAt: string;
}

export interface TradingJournalListResponse {
  items: TradingJournalListItem[];
  page: number;
  size: number;
  total: number;
}

export interface TradingJournalDetailResponse {
  journalId: number;
  accountId: number;
  orderId: number;
  ticker: string;
  journalType: string;
  sentiment: JournalSentiment;
  title: string;
  content: string;
  summary: string;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateTradingJournalRequest {
  title: string;
  content: string;
  sentiment: JournalSentiment;
}
