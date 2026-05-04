import { fetchJson } from "./index";

export type SubscriptionModeStatus = {
  mode: string;
  connectionStatus: string;
  lastConnectedAt: string | null;
  reconnectAttempts: number;
  wsSymbols: string[];
  restSymbols: string[];
  wsSlotUsed: number;
  wsSlotMax: number;
};

export type SubscriptionStatusResponse = {
  generatedAt: string;
  totalWsSlotUsed: number;
  totalWsSlotMax: number;
  modes: SubscriptionModeStatus[];
};

export async function fetchSubscriptionStatus(): Promise<SubscriptionStatusResponse> {
  return fetchJson<SubscriptionStatusResponse>("/api/subscriptions/status");
}
