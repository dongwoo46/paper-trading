"""PandasTaIndicatorCalculator — pandas-ta 위임 보조지표 계산기.

금융 안전: 모든 float 결과는 Decimal(str(value)) 로 변환한다.
직접 Decimal(float_value) 사용 금지.
"""
from __future__ import annotations

import math
from decimal import Decimal
from typing import Optional

import pandas as pd
from src.chart_analysis.domain.value_objects import Candle, IndicatorSet

# 분석 윈도우별로 짧은 구간도 계산할 수 있게 adaptive 지표를 사용한다.
_MIN_CANDLES = 3


def _to_decimal(value: Optional[float]) -> Decimal:
    """float 또는 NaN 값을 Decimal로 변환한다.

    NaN → ValueError 발생 (호출자가 처리).
    """
    if value is None or pd.isna(value) or (isinstance(value, float) and math.isnan(value)):
        raise ValueError("지표 값이 NaN — 입력 데이터 부족")
    return Decimal(str(value))


class PandasTaIndicatorCalculator:
    """pandas-ta 라이브러리를 위임하여 보조지표를 계산한다.

    IndicatorCalculator 포트를 구현한다.
    """

    def calculate(self, candles: list[Candle]) -> IndicatorSet:
        """봉 목록으로 보조지표 세트를 계산한다.

        Args:
            candles: 시간 순으로 정렬된 Candle 리스트.

        Returns:
            IndicatorSet — 모든 필드 Decimal.

        Raises:
            ValueError: 입력 봉 수 부족 또는 NaN 결과.
        """
        if not candles:
            raise ValueError("캔들 목록이 비어 있습니다.")
        if len(candles) < _MIN_CANDLES:
            raise ValueError(
                f"보조지표 계산에 최소 {_MIN_CANDLES}봉이 필요합니다. "
                f"현재: {len(candles)}봉"
            )

        df = self._to_dataframe(candles)
        return self._compute_indicators(df)

    # ------------------------------------------------------------------ #
    # Private helpers
    # ------------------------------------------------------------------ #

    @staticmethod
    def _to_dataframe(candles: list[Candle]) -> pd.DataFrame:
        """Candle 리스트를 pandas DataFrame으로 변환한다."""
        return pd.DataFrame(
            {
                "open": [float(c.open) for c in candles],
                "high": [float(c.high) for c in candles],
                "low": [float(c.low) for c in candles],
                "close": [float(c.close) for c in candles],
                "volume": [float(c.volume) for c in candles],
            }
        )

    def _compute_indicators(self, df: pd.DataFrame) -> IndicatorSet:
        """DataFrame에서 보조지표를 계산하고 IndicatorSet을 반환한다."""
        close = df["close"]
        high = df["high"]
        low = df["low"]
        volume = df["volume"]

        ma20_s = close.rolling(window=min(20, len(df)), min_periods=1).mean()
        ma60_s = close.rolling(window=min(60, len(df)), min_periods=1).mean()
        ma120_s = close.rolling(window=min(120, len(df)), min_periods=1).mean()

        delta = close.diff().fillna(0)
        gain = delta.clip(lower=0).rolling(window=min(14, len(df)), min_periods=1).mean()
        loss = (-delta.clip(upper=0)).rolling(window=min(14, len(df)), min_periods=1).mean()
        rs = gain / loss.replace(0, pd.NA)
        rsi14_s = (100 - (100 / (1 + rs))).infer_objects(copy=False).fillna(100)

        ema_fast = close.ewm(span=min(12, len(df)), adjust=False).mean()
        ema_slow = close.ewm(span=min(26, len(df)), adjust=False).mean()
        macd_s = ema_fast - ema_slow
        macd_signal_s = macd_s.ewm(span=min(9, len(df)), adjust=False).mean()
        macd_hist_s = macd_s - macd_signal_s

        bb_len = min(20, len(df))
        bb_mid = close.rolling(window=bb_len, min_periods=1).mean()
        bb_std = close.rolling(window=bb_len, min_periods=1).std().fillna(0)
        bb_upper_s = bb_mid + (bb_std * 2)
        bb_lower_s = bb_mid - (bb_std * 2)

        prev_close = close.shift(1).fillna(close)
        tr = pd.concat([(high - low), (high - prev_close).abs(), (low - prev_close).abs()], axis=1).max(axis=1)
        atr14_s = tr.rolling(window=min(14, len(df)), min_periods=1).mean()

        up_move = high.diff().fillna(0)
        down_move = -low.diff().fillna(0)
        plus_dm = up_move.where((up_move > down_move) & (up_move > 0), 0)
        minus_dm = down_move.where((down_move > up_move) & (down_move > 0), 0)
        plus_di = 100 * plus_dm.rolling(min(14, len(df)), min_periods=1).sum() / tr.rolling(min(14, len(df)), min_periods=1).sum().replace(0, pd.NA)
        minus_di = 100 * minus_dm.rolling(min(14, len(df)), min_periods=1).sum() / tr.rolling(min(14, len(df)), min_periods=1).sum().replace(0, pd.NA)
        dx = ((plus_di - minus_di).abs() / (plus_di + minus_di).replace(0, pd.NA) * 100).infer_objects(copy=False).fillna(0)
        adx14_s = dx.rolling(window=min(14, len(df)), min_periods=1).mean()

        stoch_low = low.rolling(window=min(14, len(df)), min_periods=1).min()
        stoch_high = high.rolling(window=min(14, len(df)), min_periods=1).max()
        stoch_k_s = ((close - stoch_low) / (stoch_high - stoch_low).replace(0, pd.NA) * 100).infer_objects(copy=False).fillna(50)
        stoch_d_s = stoch_k_s.rolling(window=min(3, len(df)), min_periods=1).mean()

        obv_s = (volume.where(delta >= 0, -volume)).cumsum()
        volume_ma20_s = volume.rolling(window=min(20, len(df)), min_periods=1).mean()

        # 마지막 봉 값 추출 (가장 최근 값)
        last = -1

        try:
            ma20 = _to_decimal(ma20_s.iloc[last])
            ma60 = _to_decimal(ma60_s.iloc[last])
            ma120 = _to_decimal(ma120_s.iloc[last])
            rsi14 = _to_decimal(rsi14_s.iloc[last])
            macd_val = _to_decimal(macd_s.iloc[last])
            macd_signal_val = _to_decimal(macd_signal_s.iloc[last])
            macd_hist_val = _to_decimal(macd_hist_s.iloc[last])
            bb_upper = _to_decimal(bb_upper_s.iloc[last])
            bb_lower = _to_decimal(bb_lower_s.iloc[last])
            atr14 = _to_decimal(atr14_s.iloc[last])
            adx14 = _to_decimal(adx14_s.iloc[last])
            stoch_k = _to_decimal(stoch_k_s.iloc[last])
            stoch_d = _to_decimal(stoch_d_s.iloc[last])
            obv = _to_decimal(obv_s.iloc[last])
            volume_ma20 = _to_decimal(volume_ma20_s.iloc[last])
        except (KeyError, IndexError) as e:
            raise ValueError(f"보조지표 계산 중 오류: {e}") from e

        return IndicatorSet(
            ma20=ma20,
            ma60=ma60,
            ma120=ma120,
            rsi14=rsi14,
            macd=macd_val,
            macd_signal=macd_signal_val,
            macd_hist=macd_hist_val,
            bb_upper=bb_upper,
            bb_lower=bb_lower,
            atr14=atr14,
            adx14=adx14,
            stoch_k=stoch_k,
            stoch_d=stoch_d,
            obv=obv,
            volume_ma20=volume_ma20,
        )
