import { useEffect } from 'react';
import { TrendingDown, TrendingUp, X } from 'lucide-react';
import type { Toast } from './types';

interface ToastItemProps {
  toast: Toast;
  onDismiss: (id: string) => void;
}

function formatPrice(price: string, currency: string): string {
  if (currency === 'KRW') {
    const num = parseInt(price, 10);
    return isNaN(num) ? price : `${num.toLocaleString('ko-KR')}원`;
  }
  const num = parseFloat(price);
  return isNaN(num) ? price : `$${num.toFixed(2)}`;
}

export function ToastItem({ toast, onDismiss }: ToastItemProps) {
  const { event } = toast;
  const isBuy = event.side === 'BUY';
  const label = isBuy ? '매수 체결' : '매도 체결';
  const tickerDisplay = event.tickerName ?? event.ticker;
  const quantity = event.quantity;
  const priceDisplay = formatPrice(event.price, event.currency);

  useEffect(() => {
    const timer = setTimeout(() => {
      onDismiss(toast.id);
    }, 4500);
    return () => clearTimeout(timer);
  }, [toast.id, onDismiss]);

  return (
    <div
      className={`pointer-events-auto flex items-start gap-2.5 px-4 py-3 rounded-lg min-w-[280px] max-w-[360px] bg-bg-card text-text-primary shadow-[0_4px_16px_rgba(0,0,0,0.4)] border-l-4 animate-toast-in ${isBuy ? 'border-status-success' : 'border-status-error'}`}
    >
      <div className="shrink-0 mt-0.5">
        {isBuy ? (
          <TrendingUp size={18} className="text-status-success" />
        ) : (
          <TrendingDown size={18} className="text-status-error" />
        )}
      </div>
      <div className="flex-1 min-w-0">
        <div className="text-[11px] font-semibold tracking-[0.04em] uppercase opacity-70 mb-0.5">{label}</div>
        <div className="text-sm font-medium whitespace-nowrap overflow-hidden text-ellipsis">
          {tickerDisplay} {quantity}주 @ {priceDisplay}
        </div>
        <div className="text-xs opacity-60 mt-0.5">주문 #{event.orderId}</div>
      </div>
      <button
        className="shrink-0 p-0.5 text-text-secondary opacity-60 hover:opacity-100 transition-opacity mt-0.5"
        onClick={() => onDismiss(toast.id)}
        aria-label="닫기"
      >
        <X size={14} />
      </button>
    </div>
  );
}
