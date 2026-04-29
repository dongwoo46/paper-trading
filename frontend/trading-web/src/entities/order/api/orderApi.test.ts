import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  fetchListAccounts,
  fetchPlaceOrder,
  fetchListOrders,
  fetchCancelOrder,
} from './orderApi';

// Helper: build a minimal Response-like mock
function mockFetch(body: unknown, status = 200): ReturnType<typeof vi.fn> {
  const json = async () => body;
  const text = async () => (typeof body === 'string' ? body : JSON.stringify(body));
  return vi.fn().mockResolvedValue({ ok: status >= 200 && status < 300, status, statusText: 'OK', json, text });
}

beforeEach(() => {
  vi.restoreAllMocks();
});

describe('fetchListAccounts', () => {
  it('calls GET /api/v1/accounts and returns parsed response', async () => {
    const accounts = [{ id: 1, accountName: '테스트계좌', tradingMode: 'LOCAL', deposit: '1000000', availableDeposit: '900000', isActive: true }];
    vi.stubGlobal('fetch', mockFetch(accounts));

    const result = await fetchListAccounts();

    expect(fetch).toHaveBeenCalledTimes(1);
    const [url] = (fetch as ReturnType<typeof vi.fn>).mock.calls[0] as [string, RequestInit?];
    expect(url).toContain('/api/v1/accounts');
    expect(result).toEqual(accounts);
  });
});

describe('fetchPlaceOrder', () => {
  it('calls POST /api/v1/accounts/1/orders with body including idempotencyKey', async () => {
    const order = { orderId: 10, ticker: 'AAPL', orderStatus: 'PENDING' };
    vi.stubGlobal('fetch', mockFetch(order));

    const req = {
      ticker: 'AAPL',
      marketType: 'NASDAQ' as const,
      orderType: 'MARKET' as const,
      orderSide: 'BUY' as const,
      orderCondition: 'DAY' as const,
      quantity: '10',
      limitPrice: null,
      expireAt: null,
      idempotencyKey: crypto.randomUUID(),
    };

    const result = await fetchPlaceOrder(1, req);

    expect(fetch).toHaveBeenCalledTimes(1);
    const [url, init] = (fetch as ReturnType<typeof vi.fn>).mock.calls[0] as [string, RequestInit];
    expect(url).toContain('/api/v1/accounts/1/orders');
    expect(init.method).toBe('POST');
    const body = JSON.parse(init.body as string) as Record<string, unknown>;
    expect(body.idempotencyKey).toBeDefined();
    expect(typeof body.idempotencyKey).toBe('string');
    expect(result).toEqual(order);
  });
});

describe('fetchListOrders', () => {
  it('calls GET /api/v1/accounts/1/orders', async () => {
    const orders = [{ orderId: 1, ticker: '005930', orderStatus: 'PENDING' }];
    vi.stubGlobal('fetch', mockFetch(orders));

    const result = await fetchListOrders(1);

    expect(fetch).toHaveBeenCalledTimes(1);
    const [url, init] = (fetch as ReturnType<typeof vi.fn>).mock.calls[0] as [string, RequestInit?];
    expect(url).toContain('/api/v1/accounts/1/orders');
    // should NOT be POST
    expect((init?.method ?? 'GET').toUpperCase()).not.toBe('POST');
    expect(result).toEqual(orders);
  });
});

describe('fetchCancelOrder', () => {
  it('calls DELETE /api/v1/accounts/1/orders/42 and resolves void on 204', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 204,
      statusText: 'No Content',
      text: async () => '',
    });
    vi.stubGlobal('fetch', fetchMock);

    const result = await fetchCancelOrder(1, 42);

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toContain('/api/v1/accounts/1/orders/42');
    expect(init.method).toBe('DELETE');
    expect(result).toBeUndefined();
  });
});
