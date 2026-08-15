import { useEffect } from 'react';
import { TrendingDown, TrendingUp, X } from 'lucide-react';
import type { Toast } from './types';
import { Button } from '../shadcn/button';
import { Card, CardContent } from '../shadcn/card';

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
    <Card
      className={`pointer-events-auto min-w-70 max-w-90 animate-toast-in gap-0 border-l-4 py-0 shadow-lg ${isBuy ? 'border-order-buy' : 'border-order-sell'}`}
    >
      <CardContent className="flex items-start gap-2.5 px-4 py-3">
      <div className="mt-0.5 shrink-0">
        {isBuy ? (
          <TrendingUp size={18} className="text-order-buy" />
        ) : (
          <TrendingDown size={18} className="text-order-sell" />
        )}
      </div>
      <div className="flex-1 min-w-0">
        <div className="text-[11px] font-semibold tracking-[0.04em] uppercase opacity-70 mb-0.5">{label}</div>
        <div className="text-sm font-medium whitespace-nowrap overflow-hidden text-ellipsis">
          {tickerDisplay} {quantity}주 @ {priceDisplay}
        </div>
        <div className="text-xs opacity-60 mt-0.5">주문 #{event.orderId}</div>
      </div>
      <Button
        type="button"
        variant="ghost"
        size="icon-xs"
        className="mt-0.5 shrink-0"
        onClick={() => onDismiss(toast.id)}
        aria-label="닫기"
      >
        <X size={14} />
      </Button>
      </CardContent>
    </Card>
  );
}
