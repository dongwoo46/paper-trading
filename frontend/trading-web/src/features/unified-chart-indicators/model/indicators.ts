export type OhlcvInput = {
  time: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
};

export type BollingerPoint = {
  time: string;
  upper: number;
  middle: number;
  lower: number;
};

export type RsiPoint = {
  time: string;
  value: number;
};

export type MacdPoint = {
  time: string;
  macd: number;
  signal: number;
  histogram: number;
};

export type MaPoint = {
  time: string;
  value: number;
};

export function calcMA(bars: OhlcvInput[], period: number): MaPoint[] {
  const closes = bars.map((b) => b.close);
  const result: MaPoint[] = [];
  for (let i = period - 1; i < bars.length; i++) {
    let sum = 0;
    for (let j = i - period + 1; j <= i; j++) sum += closes[j];
    result.push({ time: bars[i].time, value: sum / period });
  }
  return result;
}

function calcSMA(closes: number[], period: number): number[] {
  return closes.map((_, i) => {
    if (i < period - 1) return NaN;
    let sum = 0;
    for (let j = i - period + 1; j <= i; j++) sum += closes[j];
    return sum / period;
  });
}

function calcEMA(closes: number[], period: number): number[] {
  const result: number[] = new Array(closes.length).fill(NaN);
  const multiplier = 2 / (period + 1);

  const sma = calcSMA(closes, period);
  const seedIndex = period - 1;
  if (seedIndex >= closes.length) return result;

  result[seedIndex] = sma[seedIndex];
  for (let i = seedIndex + 1; i < closes.length; i++) {
    result[i] = closes[i] * multiplier + result[i - 1] * (1 - multiplier);
  }
  return result;
}

export function calcBollingerBands(
  bars: OhlcvInput[],
  period = 20,
  stdMultiplier = 2,
): BollingerPoint[] {
  const closes = bars.map((b) => b.close);
  const sma = calcSMA(closes, period);
  const result: BollingerPoint[] = [];

  for (let i = period - 1; i < bars.length; i++) {
    const middle = sma[i];
    if (isNaN(middle)) continue;

    let variance = 0;
    for (let j = i - period + 1; j <= i; j++) {
      variance += (closes[j] - middle) ** 2;
    }
    const std = Math.sqrt(variance / period);

    result.push({
      time: bars[i].time,
      upper: middle + stdMultiplier * std,
      middle,
      lower: middle - stdMultiplier * std,
    });
  }
  return result;
}

export function calcRsi(bars: OhlcvInput[], period = 14): RsiPoint[] {
  if (bars.length < period) return [];

  const closes = bars.map((b) => b.close);
  const gains: number[] = [];
  const losses: number[] = [];

  for (let i = 1; i < closes.length; i++) {
    const diff = closes[i] - closes[i - 1];
    gains.push(diff > 0 ? diff : 0);
    losses.push(diff < 0 ? -diff : 0);
  }

  if (gains.length < period) return [];

  let avgGain = gains.slice(0, period).reduce((s, v) => s + v, 0) / period;
  let avgLoss = losses.slice(0, period).reduce((s, v) => s + v, 0) / period;

  const result: RsiPoint[] = [];

  const firstRsi = avgLoss === 0 ? 100 : 100 - 100 / (1 + avgGain / avgLoss);
  result.push({ time: bars[period].time, value: firstRsi });

  for (let i = period; i < gains.length; i++) {
    avgGain = (avgGain * (period - 1) + gains[i]) / period;
    avgLoss = (avgLoss * (period - 1) + losses[i]) / period;
    const rsi = avgLoss === 0 ? 100 : 100 - 100 / (1 + avgGain / avgLoss);
    result.push({ time: bars[i + 1].time, value: rsi });
  }
  return result;
}

export function calcMacd(
  bars: OhlcvInput[],
  fast = 12,
  slow = 26,
  signal = 9,
): MacdPoint[] {
  if (bars.length < slow) return [];

  const closes = bars.map((b) => b.close);
  const fastEma = calcEMA(closes, fast);
  const slowEma = calcEMA(closes, slow);

  const macdLine: number[] = closes.map((_, i) => {
    if (isNaN(fastEma[i]) || isNaN(slowEma[i])) return NaN;
    return fastEma[i] - slowEma[i];
  });

  const validMacd = macdLine.filter((v) => !isNaN(v));
  if (validMacd.length < signal) return [];

  const signalEma = calcEMA(validMacd, signal);

  const result: MacdPoint[] = [];
  let validIndex = 0;

  for (let i = 0; i < bars.length; i++) {
    if (isNaN(macdLine[i])) continue;
    const sig = signalEma[validIndex];
    if (!isNaN(sig)) {
      const macdVal = macdLine[i];
      result.push({
        time: bars[i].time,
        macd: macdVal,
        signal: sig,
        histogram: macdVal - sig,
      });
    }
    validIndex++;
  }
  return result;
}