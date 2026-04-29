import { fetchJson } from '../../../shared/api';
import type { AccountResponse, OrderResponse, PlaceOrderRequest } from '../model/types';

export function fetchListAccounts(): Promise<AccountResponse[]> {
  return fetchJson<AccountResponse[]>('/api/v1/accounts');
}

export function fetchPlaceOrder(accountId: number, req: PlaceOrderRequest): Promise<OrderResponse> {
  return fetchJson<OrderResponse>(`/api/v1/accounts/${accountId}/orders`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export function fetchListOrders(accountId: number): Promise<OrderResponse[]> {
  return fetchJson<OrderResponse[]>(`/api/v1/accounts/${accountId}/orders`);
}

// DELETE returns 204 No Content — fetchJson expects JSON body, so we use the underlying fetch.
// This is the only place direct fetch is used, justified by the 204 No Content contract.
export async function fetchCancelOrder(accountId: number, orderId: number): Promise<void> {
  const apiBase = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '';
  const response = await fetch(`${apiBase}/api/v1/accounts/${accountId}/orders/${orderId}`, {
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json' },
  });
  if (!response.ok) {
    const message = await response.text();
    throw new Error(`${response.status} ${response.statusText}: ${message}`);
  }
}
