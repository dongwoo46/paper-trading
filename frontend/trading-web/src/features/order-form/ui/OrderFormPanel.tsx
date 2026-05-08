import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { fetchPlaceOrder } from '../../../entities/order/api/orderApi';
import type { MarketType, OrderCondition, OrderSide, OrderType } from '../../../entities/order/model/types';

interface OrderFormPanelProps {
  accountId: number;
  onSuccess: () => void;
}

interface FormState {
  ticker: string;
  marketType: MarketType;
  orderType: OrderType;
  orderSide: OrderSide;
  orderCondition: OrderCondition;
  quantity: string;
  limitPrice: string;
  expireAt: string;
}

const DEFAULT_FORM: FormState = {
  ticker: '',
  marketType: 'KOSPI',
  orderType: 'MARKET',
  orderSide: 'BUY',
  orderCondition: 'DAY',
  quantity: '',
  limitPrice: '',
  expireAt: '',
};

const INPUT_CLS = "bg-bg-input border border-white/12 text-text-primary px-4 py-3 rounded-xl outline-none transition-all focus:border-brand-primary focus:shadow-[0_0_0_4px_rgba(96,165,250,0.25)] focus:bg-bg-card";
const SELECT_CLS = INPUT_CLS;
const LABEL_CLS = "min-w-[80px] text-text-secondary text-[13px]";
const BTN_PRIMARY = "inline-flex items-center justify-center gap-2.5 px-5 py-3 rounded-xl font-semibold text-sm cursor-pointer transition-all border border-transparent bg-gradient-to-br from-blue-500 to-emerald-500 text-white shadow hover:-translate-y-0.5 hover:brightness-110 whitespace-nowrap disabled:opacity-50 disabled:cursor-not-allowed";

export function OrderFormPanel({ accountId, onSuccess }: OrderFormPanelProps) {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<FormState>(DEFAULT_FORM);
  const [validationError, setValidationError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: fetchPlaceOrder.bind(null, accountId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['orders', accountId] });
      setForm(DEFAULT_FORM);
      setValidationError(null);
      onSuccess();
    },
  });

  const validate = (): string | null => {
    if (!form.ticker.trim()) return '종목코드를 입력해 주세요.';
    const qty = parseFloat(form.quantity);
    if (isNaN(qty) || qty <= 0) return '수량은 0보다 커야 합니다.';
    if (form.orderType === 'LIMIT') {
      const price = parseFloat(form.limitPrice);
      if (isNaN(price) || price <= 0) return '지정가는 0보다 커야 합니다.';
    }
    if (form.orderCondition === 'GTD') {
      if (!form.expireAt) return '만료일시를 입력해 주세요.';
      if (new Date(form.expireAt) <= new Date()) return '만료일시는 현재 시각 이후여야 합니다.';
    }
    return null;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const error = validate();
    if (error) {
      setValidationError(error);
      return;
    }
    setValidationError(null);

    mutation.mutate({
      ticker: form.ticker.trim().toUpperCase(),
      marketType: form.marketType,
      orderType: form.orderType,
      orderSide: form.orderSide,
      orderCondition: form.orderCondition,
      quantity: form.quantity,
      limitPrice: form.orderType === 'LIMIT' ? form.limitPrice : null,
      expireAt: form.orderCondition === 'GTD' ? new Date(form.expireAt).toISOString() : null,
      idempotencyKey: crypto.randomUUID(),
    });
  };

  const set = <K extends keyof FormState>(key: K) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
      setForm((prev) => ({ ...prev, [key]: e.target.value }));
    };

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-3 py-4">
      <div className="flex gap-3 flex-wrap">
        <label className={LABEL_CLS}>종목코드</label>
        <input
          className={INPUT_CLS}
          style={{ width: '160px' }}
          value={form.ticker}
          onChange={set('ticker')}
          placeholder="예: 005930"
          required
        />
      </div>

      <div className="flex gap-3 flex-wrap">
        <label className={LABEL_CLS}>시장</label>
        <select className={SELECT_CLS} style={{ width: '160px' }} value={form.marketType} onChange={set('marketType')}>
          <option value="KOSPI">KOSPI</option>
          <option value="KOSDAQ">KOSDAQ</option>
          <option value="NASDAQ">NASDAQ</option>
          <option value="NYSE">NYSE</option>
          <option value="CRYPTO">CRYPTO</option>
        </select>
      </div>

      <div className="flex gap-3 flex-wrap">
        <label className={LABEL_CLS}>주문 유형</label>
        <select className={SELECT_CLS} style={{ width: '160px' }} value={form.orderType} onChange={set('orderType')}>
          <option value="MARKET">MARKET (시장가)</option>
          <option value="LIMIT">LIMIT (지정가)</option>
        </select>
      </div>

      <div className="flex gap-3 flex-wrap">
        <label className={LABEL_CLS}>매매 방향</label>
        <select className={SELECT_CLS} style={{ width: '160px' }} value={form.orderSide} onChange={set('orderSide')}>
          <option value="BUY">BUY (매수)</option>
          <option value="SELL">SELL (매도)</option>
        </select>
      </div>

      <div className="flex gap-3 flex-wrap">
        <label className={LABEL_CLS}>주문 조건</label>
        <select className={SELECT_CLS} style={{ width: '160px' }} value={form.orderCondition} onChange={set('orderCondition')}>
          <option value="DAY">DAY</option>
          <option value="GTC">GTC</option>
          <option value="IOC">IOC</option>
          <option value="FOK">FOK</option>
          <option value="GTD">GTD</option>
        </select>
      </div>

      <div className="flex gap-3 flex-wrap">
        <label className={LABEL_CLS}>수량</label>
        <input
          type="number"
          className={INPUT_CLS}
          style={{ width: '160px' }}
          value={form.quantity}
          onChange={set('quantity')}
          placeholder="예: 10"
          min="0"
          step="any"
          required
        />
      </div>

      {form.orderType === 'LIMIT' && (
        <div className="flex gap-3 flex-wrap">
          <label className={LABEL_CLS}>지정가</label>
          <input
            type="number"
            className={INPUT_CLS}
            style={{ width: '160px' }}
            value={form.limitPrice}
            onChange={set('limitPrice')}
            placeholder="예: 75000"
            min="0"
            step="any"
          />
        </div>
      )}

      {form.orderCondition === 'GTD' && (
        <div className="flex gap-3 flex-wrap">
          <label className={LABEL_CLS}>만료일시</label>
          <input
            type="datetime-local"
            className={INPUT_CLS}
            style={{ width: '220px' }}
            value={form.expireAt}
            onChange={set('expireAt')}
          />
        </div>
      )}

      {(validationError ?? mutation.error) && (
        <p className="text-status-error text-[13px] m-0">
          {validationError ?? (mutation.error instanceof Error ? mutation.error.message : '주문 실패')}
        </p>
      )}

      <div className="flex gap-3 flex-wrap">
        <button
          type="submit"
          className={BTN_PRIMARY}
          disabled={mutation.isPending}
          style={{ minWidth: '120px' }}
        >
          {mutation.isPending ? '주문 중...' : '주문 제출'}
        </button>
      </div>
    </form>
  );
}
