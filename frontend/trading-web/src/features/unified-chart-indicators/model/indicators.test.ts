import { describe, it, expect } from 'vitest';
import {
  calcBollingerBands,
  calcRsi,
  calcMacd,
  type OhlcvInput,
} from './indicators';

function makeBar(close: number, i: number): OhlcvInput {
  return {
    time: `2024-01-${String(i + 1).padStart(2, '0')}`,
    open: close,
    high: close + 1,
    low: close - 1,
    close,
    volume: 1000,
  };
}

function makeBars(closes: number[]): OhlcvInput[] {
  return closes.map((c, i) => makeBar(c, i));
}

describe('calcBollingerBands', () => {
  it('20개 데이터 입력 시 1개 결과 반환', () => {
    const bars = makeBars(Array.from({ length: 20 }, (_, i) => 100 + i));
    const result = calcBollingerBands(bars);
    expect(result).toHaveLength(1);
  });

  it('upper > middle > lower 부등식 항상 성립', () => {
    const bars = makeBars(Array.from({ length: 30 }, (_, i) => 100 + Math.sin(i) * 10));
    const result = calcBollingerBands(bars);
    for (const p of result) {
      expect(p.upper).toBeGreaterThan(p.middle);
      expect(p.middle).toBeGreaterThan(p.lower);
    }
  });

  it('middle = 종가 20일 평균 (오차 1e-9 이내)', () => {
    const closes = Array.from({ length: 20 }, (_, i) => 100 + i);
    const bars = makeBars(closes);
    const result = calcBollingerBands(bars);
    const expected = closes.reduce((s, v) => s + v, 0) / 20;
    expect(Math.abs(result[0].middle - expected)).toBeLessThan(1e-9);
  });

  it('19개 데이터(기간 미달) → 빈 배열', () => {
    const bars = makeBars(Array.from({ length: 19 }, (_, i) => 100 + i));
    const result = calcBollingerBands(bars);
    expect(result).toHaveLength(0);
  });
});

describe('calcRsi', () => {
  it('14일 RSI 값이 0 이상 100 이하', () => {
    const bars = makeBars(Array.from({ length: 20 }, (_, i) => 100 + Math.sin(i) * 5));
    const result = calcRsi(bars);
    for (const p of result) {
      expect(p.value).toBeGreaterThanOrEqual(0);
      expect(p.value).toBeLessThanOrEqual(100);
    }
  });

  it('모든 종가 상승 시 RSI > 50', () => {
    const bars = makeBars(Array.from({ length: 20 }, (_, i) => 100 + i));
    const result = calcRsi(bars);
    expect(result.length).toBeGreaterThan(0);
    for (const p of result) {
      expect(p.value).toBeGreaterThan(50);
    }
  });

  it('모든 종가 하락 시 RSI < 50', () => {
    const bars = makeBars(Array.from({ length: 20 }, (_, i) => 200 - i));
    const result = calcRsi(bars);
    expect(result.length).toBeGreaterThan(0);
    for (const p of result) {
      expect(p.value).toBeLessThan(50);
    }
  });

  it('14개 미만 데이터 → 빈 배열', () => {
    const bars = makeBars(Array.from({ length: 13 }, (_, i) => 100 + i));
    const result = calcRsi(bars);
    expect(result).toHaveLength(0);
  });
});

describe('calcMacd', () => {
  it('slow(26) 이상 데이터가 있어야 macd 배열 반환', () => {
    const bars = makeBars(Array.from({ length: 40 }, (_, i) => 100 + i * 0.5));
    const result = calcMacd(bars);
    expect(result.length).toBeGreaterThan(0);
  });

  it('histogram = macd - signal (오차 1e-9 이내)', () => {
    const bars = makeBars(Array.from({ length: 50 }, (_, i) => 100 + Math.sin(i * 0.3) * 10));
    const result = calcMacd(bars);
    for (const p of result) {
      expect(Math.abs(p.histogram - (p.macd - p.signal))).toBeLessThan(1e-9);
    }
  });

  it('25개 데이터 → 빈 배열 (slow=26 미달)', () => {
    const bars = makeBars(Array.from({ length: 25 }, (_, i) => 100 + i));
    const result = calcMacd(bars);
    expect(result).toHaveLength(0);
  });
});
