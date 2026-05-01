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
    <div className={`toast-item ${isBuy ? 'toast-buy' : 'toast-sell'}`}>
      <div className="toast-icon">
        {isBuy ? (
          <TrendingUp size={18} className="toast-icon--buy" />
        ) : (
          <TrendingDown size={18} className="toast-icon--sell" />
        )}
      </div>
      <div className="toast-content">
        <div className="toast-label">{label}</div>
        <div className="toast-main">
          {tickerDisplay} {quantity}주 @ {priceDisplay}
        </div>
        <div className="toast-sub">주문 #{event.orderId}</div>
      </div>
      <button
        className="toast-dismiss"
        onClick={() => onDismiss(toast.id)}
        aria-label="닫기"
      >
        <X size={14} />
      </button>
    </div>
  );
}
