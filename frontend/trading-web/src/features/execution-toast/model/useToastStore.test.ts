import { describe, it, expect, beforeEach } from 'vitest';
import { useToastStore } from './useToastStore';
import type { ExecutionEvent } from '../../../entities/execution/model/types';

function sampleEvent(ticker = '005930'): ExecutionEvent {
  return {
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
}

describe('useToastStore', () => {
  beforeEach(() => {
    useToastStore.setState({ toasts: [] });
  });

  it('addToast — toast appears in list', () => {
    useToastStore.getState().addToast(sampleEvent());
    const toasts = useToastStore.getState().toasts;
    expect(toasts).toHaveLength(1);
    expect(toasts[0].event.ticker).toBe('005930');
  });

  it('removeToast — toast removed by id', () => {
    useToastStore.getState().addToast(sampleEvent());
    const id = useToastStore.getState().toasts[0].id;
    useToastStore.getState().removeToast(id);
    expect(useToastStore.getState().toasts).toHaveLength(0);
  });

  it('addToast — capped at 5 toasts, oldest dropped', () => {
    // Add 6 toasts with distinct tickers
    for (let i = 0; i < 6; i++) {
      useToastStore.getState().addToast(sampleEvent(`TICK${i}`));
    }
    const toasts = useToastStore.getState().toasts;
    expect(toasts).toHaveLength(5);
    // Oldest (TICK0) should have been dropped; newest (TICK5) should be first
    const tickers = toasts.map((t) => t.event.ticker);
    expect(tickers).not.toContain('TICK0');
    expect(tickers[0]).toBe('TICK5');
  });

  it('addToast — newest toast is first in list', () => {
    useToastStore.getState().addToast(sampleEvent('FIRST'));
    useToastStore.getState().addToast(sampleEvent('SECOND'));
    const toasts = useToastStore.getState().toasts;
    expect(toasts[0].event.ticker).toBe('SECOND');
    expect(toasts[1].event.ticker).toBe('FIRST');
  });
});
