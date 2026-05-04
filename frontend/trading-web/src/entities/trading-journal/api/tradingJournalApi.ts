import { fetchJson } from "../../../shared/api";
import type {
  TradingJournalDetailResponse,
  TradingJournalListResponse,
  UpdateTradingJournalRequest,
} from "../model/types";

export function fetchTradingJournalList(params: {
  accountId: number;
  ticker?: string;
  from: string;
  to: string;
  page: number;
  size: number;
}): Promise<TradingJournalListResponse> {
  const query = new URLSearchParams({
    accountId: String(params.accountId),
    from: params.from,
    to: params.to,
    page: String(params.page),
    size: String(params.size),
  });
  if (params.ticker && params.ticker.trim().length > 0) query.set("ticker", params.ticker.trim());
  return fetchJson<TradingJournalListResponse>(`/api/trading-journals?${query.toString()}`);
}

export function fetchTradingJournalDetail(journalId: number): Promise<TradingJournalDetailResponse> {
  return fetchJson<TradingJournalDetailResponse>(`/api/trading-journals/${journalId}`);
}

export function updateTradingJournal(
  journalId: number,
  body: UpdateTradingJournalRequest
): Promise<{ journalId: number; status: "UPDATED" }> {
  return fetchJson<{ journalId: number; status: "UPDATED" }>(`/api/trading-journals/${journalId}`, {
    method: "PATCH",
    body: JSON.stringify(body),
  });
}
