import type { AccountResponse, PositionResponse } from '../model/types';
import { fetchJson } from '../../../shared/api/index';

export function fetchAccounts(): Promise<AccountResponse[]> {
  return fetchJson<AccountResponse[]>("/api/v1/accounts");
}

export function fetchPositions(accountId: number): Promise<PositionResponse[]> {
  return fetchJson<PositionResponse[]>(`/api/v1/accounts/${accountId}/positions`);
}
