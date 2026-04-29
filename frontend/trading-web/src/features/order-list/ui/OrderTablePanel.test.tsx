import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { OrderTablePanel } from './OrderTablePanel';
import type { OrderResponse } from '../../../entities/order/model/types';

vi.mock('../../../entities/order/api/orderApi', () => ({
  fetchListOrders: vi.fn(),
  fetchCancelOrder: vi.fn(),
  fetchPlaceOrder: vi.fn(),
  fetchListAccounts: vi.fn(),
}));

import { fetchListOrders, fetchCancelOrder } from '../../../entities/order/api/orderApi';

const mockedFetchListOrders = fetchListOrders as ReturnType<typeof vi.fn>;
const mockedFetchCancelOrder = fetchCancelOrder as ReturnType<typeof vi.fn>;

function makeOrder(overrides: Partial<OrderResponse> = {}): OrderResponse {
  return {
    orderId: 1,
    ticker: '005930',
    marketType: 'KOSPI',
    orderType: 'LIMIT',
    orderSide: 'BUY',
    orderCondition: 'DAY',
    orderStatus: 'PENDING',
    quantity: '10',
    filledQuantity: '0',
    limitPrice: '70000',
    avgFilledPrice: null,
    fee: '0',
    createdAt: '2024-01-01T00:00:00.000Z',
    ...overrides,
  };
}

function renderPanel(accountId = 1) {
  const qc = new QueryClient({
    defaultOptions: { queries: { retry: false, refetchInterval: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={qc}>
      <OrderTablePanel accountId={accountId} />
    </QueryClientProvider>
  );
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('OrderTablePanel', () => {
  it('calls fetchListOrders with the given accountId', async () => {
    mockedFetchListOrders.mockResolvedValue([]);
    renderPanel(7);
    await waitFor(() => {
      expect(mockedFetchListOrders).toHaveBeenCalledWith(7);
    });
  });

  it('shows loading state while fetching', () => {
    // never resolves — keeps the query in loading state
    mockedFetchListOrders.mockReturnValue(new Promise(() => {}));
    renderPanel();
    expect(screen.getByText(/로딩 중/)).toBeInTheDocument();
  });

  it('shows orders in table after successful load', async () => {
    const orders = [
      makeOrder({ orderId: 10, ticker: 'AAPL', orderStatus: 'PENDING' }),
      makeOrder({ orderId: 11, ticker: 'TSLA', orderStatus: 'FILLED' }),
    ];
    mockedFetchListOrders.mockResolvedValue(orders);
    renderPanel();
    expect(await screen.findByText('AAPL')).toBeInTheDocument();
    expect(screen.getByText('TSLA')).toBeInTheDocument();
  });

  it('hides FILLED orders when PENDING filter is clicked', async () => {
    const orders = [
      makeOrder({ orderId: 10, ticker: 'AAPL', orderStatus: 'PENDING' }),
      makeOrder({ orderId: 11, ticker: 'TSLA', orderStatus: 'FILLED' }),
    ];
    mockedFetchListOrders.mockResolvedValue(orders);
    const user = userEvent.setup();
    renderPanel();
    await screen.findByText('AAPL');

    await user.click(screen.getByRole('button', { name: 'PENDING' }));

    expect(screen.getByText('AAPL')).toBeInTheDocument();
    expect(screen.queryByText('TSLA')).not.toBeInTheDocument();
  });

  it('shows all orders when ALL filter is clicked after PENDING filter', async () => {
    const orders = [
      makeOrder({ orderId: 10, ticker: 'AAPL', orderStatus: 'PENDING' }),
      makeOrder({ orderId: 11, ticker: 'TSLA', orderStatus: 'FILLED' }),
    ];
    mockedFetchListOrders.mockResolvedValue(orders);
    const user = userEvent.setup();
    renderPanel();
    await screen.findByText('AAPL');

    // First apply PENDING filter
    await user.click(screen.getByRole('button', { name: 'PENDING' }));
    expect(screen.queryByText('TSLA')).not.toBeInTheDocument();

    // Then click ALL to restore
    await user.click(screen.getByRole('button', { name: '전체' }));
    expect(screen.getByText('AAPL')).toBeInTheDocument();
    expect(screen.getByText('TSLA')).toBeInTheDocument();
  });
});
