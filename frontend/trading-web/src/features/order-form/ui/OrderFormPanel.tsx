import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { fetchPlaceOrder } from '../../../entities/order/api/orderApi';
import type { MarketType, OrderCondition, OrderSide, OrderType } from '../../../entities/order/model/types';
import { Alert, AlertDescription } from '../../../shared/ui/shadcn/alert';
import { Button } from '../../../shared/ui/shadcn/button';
import { Input } from '../../../shared/ui/shadcn/input';
import { Label } from '../../../shared/ui/shadcn/label';
import { NativeSelect, NativeSelectOption } from '../../../shared/ui/shadcn/native-select';

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

const FIELD_ROW = 'grid gap-2 sm:grid-cols-[7rem_minmax(0,14rem)] sm:items-center';

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

  const errorMessage = validationError
    ?? (mutation.error instanceof Error ? mutation.error.message : mutation.error ? '주문 실패' : null);

  return (
    <form onSubmit={handleSubmit} className="flex max-w-xl flex-col gap-4 py-4">
      <div className={FIELD_ROW}>
        <Label htmlFor="order-ticker">종목코드</Label>
        <Input
          id="order-ticker"
          value={form.ticker}
          onChange={set('ticker')}
          placeholder="예: 005930"
          required
        />
      </div>

      <div className={FIELD_ROW}>
        <Label htmlFor="order-market">시장</Label>
        <NativeSelect id="order-market" className="w-full" value={form.marketType} onChange={set('marketType')}>
          <NativeSelectOption value="KOSPI">KOSPI</NativeSelectOption>
          <NativeSelectOption value="KOSDAQ">KOSDAQ</NativeSelectOption>
          <NativeSelectOption value="NASDAQ">NASDAQ</NativeSelectOption>
          <NativeSelectOption value="NYSE">NYSE</NativeSelectOption>
          <NativeSelectOption value="CRYPTO">CRYPTO</NativeSelectOption>
        </NativeSelect>
      </div>

      <div className={FIELD_ROW}>
        <Label htmlFor="order-type">주문 유형</Label>
        <NativeSelect id="order-type" className="w-full" value={form.orderType} onChange={set('orderType')}>
          <NativeSelectOption value="MARKET">MARKET (시장가)</NativeSelectOption>
          <NativeSelectOption value="LIMIT">LIMIT (지정가)</NativeSelectOption>
        </NativeSelect>
      </div>

      <div className={FIELD_ROW}>
        <Label htmlFor="order-side">매매 방향</Label>
        <NativeSelect id="order-side" className="w-full" value={form.orderSide} onChange={set('orderSide')}>
          <NativeSelectOption value="BUY">BUY (매수)</NativeSelectOption>
          <NativeSelectOption value="SELL">SELL (매도)</NativeSelectOption>
        </NativeSelect>
      </div>

      <div className={FIELD_ROW}>
        <Label htmlFor="order-condition">주문 조건</Label>
        <NativeSelect id="order-condition" className="w-full" value={form.orderCondition} onChange={set('orderCondition')}>
          <NativeSelectOption value="DAY">DAY</NativeSelectOption>
          <NativeSelectOption value="GTC">GTC</NativeSelectOption>
          <NativeSelectOption value="IOC">IOC</NativeSelectOption>
          <NativeSelectOption value="FOK">FOK</NativeSelectOption>
          <NativeSelectOption value="GTD">GTD</NativeSelectOption>
        </NativeSelect>
      </div>

      <div className={FIELD_ROW}>
        <Label htmlFor="order-quantity">수량</Label>
        <Input
          id="order-quantity"
          type="number"
          value={form.quantity}
          onChange={set('quantity')}
          placeholder="예: 10"
          min="0"
          step="any"
          required
        />
      </div>

      {form.orderType === 'LIMIT' && (
        <div className={FIELD_ROW}>
          <Label htmlFor="order-limit-price">지정가</Label>
          <Input
            id="order-limit-price"
            type="number"
            value={form.limitPrice}
            onChange={set('limitPrice')}
            placeholder="예: 75000"
            min="0"
            step="any"
          />
        </div>
      )}

      {form.orderCondition === 'GTD' && (
        <div className={FIELD_ROW}>
          <Label htmlFor="order-expire-at">만료일시</Label>
          <Input
            id="order-expire-at"
            type="datetime-local"
            value={form.expireAt}
            onChange={set('expireAt')}
          />
        </div>
      )}

      {errorMessage && (
        <Alert variant="destructive">
          <AlertDescription>{errorMessage}</AlertDescription>
        </Alert>
      )}

      <div className="sm:pl-[7rem]">
        <Button
          type="submit"
          size="lg"
          className="min-w-30"
          disabled={mutation.isPending}
        >
          {mutation.isPending ? '주문 중...' : '주문 제출'}
        </Button>
      </div>
    </form>
  );
}
