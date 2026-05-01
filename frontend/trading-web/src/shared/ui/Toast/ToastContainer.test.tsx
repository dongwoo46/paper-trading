import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ToastContainer } from './ToastContainer';
import type { Toast } from './types';
import type { ExecutionEvent } from '../../../entities/execution/model/types';

function sampleToast(ticker: string, id: string): Toast {
  const event: ExecutionEvent = {
    executionId: 1,
    orderId: 1,
    ticker,
    tickerName: null,
    side: 'BUY',
    quantity: '10',
    price: '75000',
    fee: '150',
    currency: 'KRW',
    executedAt: '2026-04-30T08:30:00Z',
  };
  return { id, event };
}

describe('ToastContainer', () => {
  it('renders empty when no toasts', () => {
    const { container } = render(<ToastContainer toasts={[]} onDismiss={vi.fn()} />);
    const toastContainer = container.querySelector('.toast-container');
    expect(toastContainer).toBeTruthy();
    expect(toastContainer!.children).toHaveLength(0);
  });

  it('renders one item per toast', () => {
    const toasts = [
      sampleToast('TICK1', 'id-1'),
      sampleToast('TICK2', 'id-2'),
      sampleToast('TICK3', 'id-3'),
    ];

    render(<ToastContainer toasts={toasts} onDismiss={vi.fn()} />);

    // Each toast renders a toast-label "매수 체결"
    const labels = screen.getAllByText('매수 체결');
    expect(labels).toHaveLength(3);
  });
});
