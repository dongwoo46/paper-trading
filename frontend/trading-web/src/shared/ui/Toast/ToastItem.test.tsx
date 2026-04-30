import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ToastItem } from './ToastItem';
import type { Toast } from './types';
import type { ExecutionEvent } from '../../../entities/execution/model/types';

function sampleToast(
  overrides: Partial<ExecutionEvent> = {},
  id = 'toast-1',
): Toast {
  const event: ExecutionEvent = {
    executionId: 1,
    orderId: 1,
    ticker: '005930',
    tickerName: null,
    side: 'BUY',
    quantity: '10',
    price: '75000',
    fee: '150',
    currency: 'KRW',
    executedAt: '2026-04-30T08:30:00Z',
    ...overrides,
  };
  return { id, event };
}

describe('ToastItem', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('BUY toast shows 매수 체결 label', () => {
    const toast = sampleToast({ side: 'BUY' });
    render(<ToastItem toast={toast} onDismiss={vi.fn()} />);
    expect(screen.getByText('매수 체결')).toBeDefined();
  });

  it('SELL toast shows 매도 체결 label', () => {
    const toast = sampleToast({ side: 'SELL' });
    render(<ToastItem toast={toast} onDismiss={vi.fn()} />);
    expect(screen.getByText('매도 체결')).toBeDefined();
  });

  it('KRW price formatted with Korean locale (75,000원)', () => {
    const toast = sampleToast({ price: '75000', currency: 'KRW' });
    render(<ToastItem toast={toast} onDismiss={vi.fn()} />);
    // The formatted price "75,000원" should appear in the rendered output
    expect(screen.getByText(/75,000원/)).toBeDefined();
  });

  it('auto-dismiss fires onDismiss after 4500ms', () => {
    vi.useFakeTimers();
    const onDismiss = vi.fn();
    const toast = sampleToast({}, 'timer-toast');
    render(<ToastItem toast={toast} onDismiss={onDismiss} />);

    expect(onDismiss).not.toHaveBeenCalled();

    vi.advanceTimersByTime(4500);

    expect(onDismiss).toHaveBeenCalledOnce();
    expect(onDismiss).toHaveBeenCalledWith('timer-toast');
  });
});
