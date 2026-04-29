import { describe, it, expectTypeOf } from 'vitest';
import type {
  PlaceOrderRequest,
  OrderStatus,
} from './types';

describe('PlaceOrderRequest type', () => {
  it('accepts a valid request with limitPrice=null', () => {
    const req: PlaceOrderRequest = {
      ticker: 'AAPL',
      marketType: 'NASDAQ',
      orderType: 'MARKET',
      orderSide: 'BUY',
      orderCondition: 'DAY',
      quantity: '10',
      limitPrice: null,
      expireAt: null,
      idempotencyKey: '550e8400-e29b-41d4-a716-446655440000',
    };
    expectTypeOf(req).toMatchTypeOf<PlaceOrderRequest>();
  });

  it('accepts a fully populated request', () => {
    const req: PlaceOrderRequest = {
      ticker: '005930',
      marketType: 'KOSPI',
      orderType: 'LIMIT',
      orderSide: 'SELL',
      orderCondition: 'GTD',
      quantity: '5',
      limitPrice: '75000',
      expireAt: '2025-12-31T23:59:59.000Z',
      idempotencyKey: '550e8400-e29b-41d4-a716-446655440001',
    };
    expectTypeOf(req).toMatchTypeOf<PlaceOrderRequest>();
  });
});

describe('OrderStatus type', () => {
  it('covers all 5 values', () => {
    const statuses: OrderStatus[] = [
      'PENDING',
      'PARTIAL',
      'FILLED',
      'CANCELLED',
      'REJECTED',
    ];
    // compile-time check — runtime value just confirms all literals are listed
    expect(statuses).toHaveLength(5);
  });
});
