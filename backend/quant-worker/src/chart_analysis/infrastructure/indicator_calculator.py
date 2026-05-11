"""PandasTaIndicatorCalculator — pandas-ta 위임 보조지표 계산기.

금융 안전: 모든 float 결과는 Decimal(str(value)) 로 변환한다.
직접 Decimal(float_value) 사용 금지.
"""
from __future__ import annotations

import math
from decimal import Decimal
from typing import Optional

import pandas as pd
import pandas_ta as ta

from chart_analysis.domain.value_objects import Candle, IndicatorSet

# 최소 필요 봉 수 — MA120 계산을 위해 120봉 필요
_MIN_CANDLES = 120


def _to_decimal(value: Optional[float]) -> Decimal:
    """float 또는 NaN 값을 Decimal로 변환한다.

    NaN → ValueError 발생 (호출자가 처리).
    """
    if value is None or (isinstance(value, float) and math.isnan(value)):
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

        # --- Simple Moving Averages ---
        ma20_s = ta.sma(close, length=20)
        ma60_s = ta.sma(close, length=60)
        ma120_s = ta.sma(close, length=120)

        # --- RSI ---
        rsi14_s = ta.rsi(close, length=14)

        # --- MACD (12/26/9) ---
        macd_df = ta.macd(close, fast=12, slow=26, signal=9)
        # cols: MACD_12_26_9, MACDh_12_26_9, MACDs_12_26_9
        macd_col = "MACD_12_26_9"
        macd_hist_col = "MACDh_12_26_9"
        macd_signal_col = "MACDs_12_26_9"

        # --- Bollinger Bands (20, 2.0) ---
        bb_df = ta.bbands(close, length=20)
        # cols: BBL_20_2.0_2.0, BBM_20_2.0_2.0, BBU_20_2.0_2.0, ...
        bb_lower_col = "BBL_20_2.0_2.0"
        bb_upper_col = "BBU_20_2.0_2.0"

        # --- ATR (14) ---
        atr14_s = ta.atr(high, low, close, length=14)

        # --- ADX (14) ---
        adx_df = ta.adx(high, low, close, length=14)
        adx_col = "ADX_14"

        # --- Stochastic (14, 3, 3) ---
        stoch_df = ta.stoch(high, low, close, k=14, d=3, smooth_k=3)
        stoch_k_col = "STOCHk_14_3_3"
        stoch_d_col = "STOCHd_14_3_3"

        # --- OBV ---
        obv_s = ta.obv(close, volume)

        # --- Volume MA20 ---
        volume_ma20_s = ta.sma(volume, length=20)

        # 마지막 봉 값 추출 (가장 최근 값)
        last = -1

        try:
            ma20 = _to_decimal(ma20_s.iloc[last])
            ma60 = _to_decimal(ma60_s.iloc[last])
            ma120 = _to_decimal(ma120_s.iloc[last])
            rsi14 = _to_decimal(rsi14_s.iloc[last])
            macd_val = _to_decimal(macd_df[macd_col].iloc[last])
            macd_signal_val = _to_decimal(macd_df[macd_signal_col].iloc[last])
            macd_hist_val = _to_decimal(macd_df[macd_hist_col].iloc[last])
            bb_upper = _to_decimal(bb_df[bb_upper_col].iloc[last])
            bb_lower = _to_decimal(bb_df[bb_lower_col].iloc[last])
            atr14 = _to_decimal(atr14_s.iloc[last])
            adx14 = _to_decimal(adx_df[adx_col].iloc[last])
            stoch_k = _to_decimal(stoch_df[stoch_k_col].iloc[last])
            stoch_d = _to_decimal(stoch_df[stoch_d_col].iloc[last])
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