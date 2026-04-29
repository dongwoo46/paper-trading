import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { OrderFormPanel } from './OrderFormPanel';

// Mock the entire orderApi module
vi.mock('../../../entities/order/api/orderApi', () => ({
  fetchPlaceOrder: vi.fn(),
  fetchListAccounts: vi.fn(),
  fetchListOrders: vi.fn(),
  fetchCancelOrder: vi.fn(),
}));

import { fetchPlaceOrder } from '../../../entities/order/api/orderApi';

const mockedFetchPlaceOrder = fetchPlaceOrder as ReturnType<typeof vi.fn>;

function renderPanel(onSuccess = vi.fn()) {
  const qc = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return {
    onSuccess,
    ...render(
      <QueryClientProvider client={qc}>
        <OrderFormPanel accountId={1} onSuccess={onSuccess} />
      </QueryClientProvider>
    ),
  };
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('OrderFormPanel', () => {
  it('renders required form fields on initial render', () => {
    renderPanel();
    // ticker input — identified by placeholder
    expect(screen.getByPlaceholderText(/005930/)).toBeInTheDocument();
    // selects: marketType, orderType, orderSide, orderCondition
    expect(screen.getByDisplayValue('KOSPI')).toBeInTheDocument();
    expect(screen.getByDisplayValue(/MARKET/)).toBeInTheDocument();
    expect(screen.getByDisplayValue(/BUY/)).toBeInTheDocument();
    expect(screen.getByDisplayValue('DAY')).toBeInTheDocument();
    // quantity input
    expect(screen.getByPlaceholderText(/10/)).toBeInTheDocument();
  });

  it('does NOT show limitPrice input when orderType is MARKET', () => {
    renderPanel();
    expect(screen.queryByPlaceholderText(/75000/)).not.toBeInTheDocument();
  });

  it('shows limitPrice input when orderType is LIMIT', async () => {
    const user = userEvent.setup();
    renderPanel();
    await user.selectOptions(screen.getByDisplayValue(/MARKET/), 'LIMIT');
    expect(screen.getByPlaceholderText(/75000/)).toBeInTheDocument();
  });

  it('does NOT show expireAt input when orderCondition is DAY', () => {
    renderPanel();
    expect(screen.queryByDisplayValue('datetime-local')).not.toBeInTheDocument();
    // check by type attribute
    expect(document.querySelector('input[type="datetime-local"]')).not.toBeInTheDocument();
  });

  it('shows expireAt input when orderCondition is GTD', async () => {
    const user = userEvent.setup();
    renderPanel();
    await user.selectOptions(screen.getByDisplayValue('DAY'), 'GTD');
    expect(document.querySelector('input[type="datetime-local"]')).toBeInTheDocument();
  });

  it('shows error and does NOT call fetchPlaceOrder when ticker is empty', async () => {
    // The ticker input has `required` which causes jsdom to block the native submit event.
    // Use fireEvent.submit to bypass HTML5 constraint validation and invoke the React onSubmit handler.
    const { container } = renderPanel();
    const form = container.querySelector('form')!;
    fireEvent.submit(form);
    expect(await screen.findByText(/종목코드를 입력해 주세요/)).toBeInTheDocument();
    expect(mockedFetchPlaceOrder).not.toHaveBeenCalled();
  });

  it('shows error and does NOT call fetchPlaceOrder when quantity is 0', async () => {
    const user = userEvent.setup();
    const { container } = renderPanel();
    await user.type(screen.getByPlaceholderText(/005930/), 'AAPL');
    // quantity is empty by default — enter 0
    await user.type(screen.getByPlaceholderText(/10/), '0');
    // Use fireEvent.submit to bypass HTML5 constraint validation
    const form = container.querySelector('form')!;
    fireEvent.submit(form);
    expect(await screen.findByText(/수량은 0보다 커야 합니다/)).toBeInTheDocument();
    expect(mockedFetchPlaceOrder).not.toHaveBeenCalled();
  });

  it('calls fetchPlaceOrder with idempotencyKey matching UUID pattern for valid MARKET order', async () => {
    const user = userEvent.setup();
    const orderResponse = {
      orderId: 1,
      ticker: 'AAPL',
      marketType: 'NASDAQ',
      orderType: 'MARKET',
      orderSide: 'BUY',
      orderCondition: 'DAY',
      orderStatus: 'PENDING',
      quantity: '10',
      filledQuantity: '0',
      limitPrice: null,
      avgFilledPrice: null,
      fee: '0',
      createdAt: new Date().toISOString(),
    };
    mockedFetchPlaceOrder.mockResolvedValueOnce(orderResponse);

    renderPanel();
    await user.type(screen.getByPlaceholderText(/005930/), 'AAPL');
    await user.type(screen.getByPlaceholderText(/10/), '10');
    await user.click(screen.getByRole('button', { name: /주문 제출/ }));

    await waitFor(() => {
      expect(mockedFetchPlaceOrder).toHaveBeenCalledTimes(1);
    });

    const [, req] = mockedFetchPlaceOrder.mock.calls[0] as [number, Record<string, unknown>];
    const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
    expect(req.idempotencyKey).toMatch(uuidPattern);
  });

  it('calls onSuccess and resets form (ticker empty) after successful submit', async () => {
    const user = userEvent.setup();
    const onSuccess = vi.fn();
    const orderResponse = {
      orderId: 2,
      ticker: 'TSLA',
      marketType: 'NASDAQ',
      orderType: 'MARKET',
      orderSide: 'BUY',
      orderCondition: 'DAY',
      orderStatus: 'PENDING',
      quantity: '5',
      filledQuantity: '0',
      limitPrice: null,
      avgFilledPrice: null,
      fee: '0',
      createdAt: new Date().toISOString(),
    };
    mockedFetchPlaceOrder.mockResolvedValueOnce(orderResponse);

    renderPanel(onSuccess);
    const tickerInput = screen.getByPlaceholderText(/005930/);
    await user.type(tickerInput, 'TSLA');
    await user.type(screen.getByPlaceholderText(/10/), '5');
    await user.click(screen.getByRole('button', { name: /주문 제출/ }));

    await waitFor(() => {
      expect(onSuccess).toHaveBeenCalledTimes(1);
    });

    // form reset: ticker should be empty
    expect(tickerInput).toHaveValue('');
  });
});
