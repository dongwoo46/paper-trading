import {
  formatDecimal,
  shiftDecimalPoint,
  type DecimalString,
} from "./decimal"

/** 소수 비율(0.1234 = 12.34%) — 포지션 수익률·일별 손익률·비중 */
export type RateFraction = DecimalString & { readonly __unit: "fraction" }
/** 퍼센트 값(12.34 = 12.34%) — KIS 잔고 수익률 */
export type RatePercent = DecimalString & { readonly __unit: "percent" }

export type FormatRateOptions = {
  /** RULE-05 부호 병기, 0 은 부호 없음 */
  signed?: boolean
}

export const asRateFraction = (value: DecimalString): RateFraction =>
  value as RateFraction

export const asRatePercent = (value: DecimalString): RatePercent =>
  value as RatePercent

export const fractionToPercent = (rate: RateFraction): RatePercent =>
  asRatePercent(shiftDecimalPoint(rate, 2))

export const percentToFraction = (rate: RatePercent): RateFraction =>
  asRateFraction(shiftDecimalPoint(rate, -2))

/** AS-03: 소수 2자리 half-up, 값 없음 «-» */
export function formatRatePercent(
  rate: RatePercent | null | undefined,
  options: FormatRateOptions = {}
): string {
  if (rate == null) return "-"
  return `${formatDecimal(rate, { fractionDigits: 2, signed: options.signed })}%`
}

export function formatRateFraction(
  rate: RateFraction | null | undefined,
  options: FormatRateOptions = {}
): string {
  if (rate == null) return "-"
  return formatRatePercent(fractionToPercent(rate), options)
}
