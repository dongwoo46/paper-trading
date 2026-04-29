import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { OrderTable } from './OrderTable';
import type { OrderResponse } from '../../../entities/order/model/types';

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

describe('OrderTable', () => {
  it('renders empty state when orders array is empty', () => {
    render(<OrderTable orders={[]} onCancel={vi.fn()} isCancelling={false} />);
    expect(screen.getByText(/주문 내역이 없습니다/)).toBeInTheDocument();
    expect(document.querySelector('tbody')).not.toBeInTheDocument();
  });

  it('shows enabled cancel button for PENDING order', () => {
    render(<OrderTable orders={[makeOrder({ orderStatus: 'PENDING' })]} onCancel={vi.fn()} isCancelling={false} />);
    expect(screen.getByRole('button', { name: '취소' })).not.toBeDisabled();
  });

  it('shows disabled cancel button for FILLED order', () => {
    render(<OrderTable orders={[makeOrder({ orderStatus: 'FILLED' })]} onCancel={vi.fn()} isCancelling={false} />);
    expect(screen.getByRole('button', { name: '취소' })).toBeDisabled();
  });

  it('shows disabled cancel button for CANCELLED order', () => {
    render(<OrderTable orders={[makeOrder({ orderStatus: 'CANCELLED' })]} onCancel={vi.fn()} isCancelling={false} />);
    expect(screen.getByRole('button', { name: '취소' })).toBeDisabled();
  });

  describe('cancel button click', () => {
    // OrderTable calls onCancel prop directly; window.confirm is handled by the parent (OrderTablePanel).
    it('calls onCancel with correct orderId when cancel is clicked on PENDING order', async () => {
      const user = userEvent.setup();
      const onCancel = vi.fn();
      render(<OrderTable orders={[makeOrder({ orderId: 42, orderStatus: 'PENDING' })]} onCancel={onCancel} isCancelling={false} />);
      await user.click(screen.getByRole('button', { name: '취소' }));
      expect(onCancel).toHaveBeenCalledWith(42);
    });

    it('does NOT fire onCancel for disabled button (FILLED order)', async () => {
      const user = userEvent.setup();
      const onCancel = vi.fn();
      render(<OrderTable orders={[makeOrder({ orderId: 42, orderStatus: 'FILLED' })]} onCancel={onCancel} isCancelling={false} />);
      // button is disabled — userEvent skips click on disabled elements
      const btn = screen.getByRole('button', { name: '취소' });
      expect(btn).toBeDisabled();
      await user.click(btn);
      expect(onCancel).not.toHaveBeenCalled();
    });
  });

  describe('orderSide color', () => {
    it('BUY orderSide renders with blue-related background color', () => {
      render(<OrderTable orders={[makeOrder({ orderSide: 'BUY' })]} onCancel={vi.fn()} isCancelling={false} />);
      const buyBadge = screen.getByText('BUY');
      // #2563eb is the blue color used for BUY
      expect(buyBadge).toHaveStyle({ background: '#2563eb' });
    });

    it('SELL orderSide renders with red-related background color', () => {
      render(<OrderTable orders={[makeOrder({ orderSide: 'SELL' })]} onCancel={vi.fn()} isCancelling={false} />);
      const sellBadge = screen.getByText('SELL');
      // #dc2626 is the red color used for SELL
      expect(sellBadge).toHaveStyle({ background: '#dc2626' });
    });
  });
});
