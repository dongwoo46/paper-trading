import { describe, expect, it } from "vitest"

import { normalizeDecimal, type DecimalString } from "./decimal"
import {
  asRateFraction,
  asRatePercent,
  formatRateFraction,
  formatRatePercent,
  fractionToPercent,
  percentToFraction,
} from "./rate"

const d = (raw: string): DecimalString => {
  const value = normalizeDecimal(raw)
  if (value === null) throw new Error(`시험 입력이 10진수가 아닙니다: ${raw}`)
  return value
}

describe("단위 변환 (D5)", () => {
  it("소수 비율을 퍼센트로 옮긴다(소수점 2칸)", () => {
    expect(fractionToPercent(asRateFraction(d("0.1234")))).toBe("12.34")
    expect(fractionToPercent(asRateFraction(d("-0.05")))).toBe("-5")
  })

  it("퍼센트를 소수 비율로 옮긴다", () => {
    expect(percentToFraction(asRatePercent(d("12.34")))).toBe("0.1234")
  })

  it("두 단위는 타입으로 구분된다(컴파일 시험 — 빌드의 tsc 가 검사)", () => {
    const fraction = asRateFraction(d("0.1"))
    const percent = asRatePercent(d("10"))
    // @ts-expect-error 소수 비율을 퍼센트 표시 함수에 넣을 수 없다
    formatRatePercent(fraction)
    // @ts-expect-error 퍼센트를 소수 비율 표시 함수에 넣을 수 없다
    formatRateFraction(percent)
    expect(true).toBe(true)
  })
})

describe("비율 표시 소수 2자리 half-up (AS-03)", () => {
  it.each([
    ["0.12345", "12.35%"],
    ["0.12344", "12.34%"],
    ["0.0005", "0.05%"],
    ["-0.12345", "-12.35%"],
    ["0.00004", "0.00%"],
    ["-0.00004", "0.00%"],
    ["0", "0.00%"],
    ["1", "100.00%"],
    ["1E-8", "0.00%"],
  ])("소수 비율 %s 는 %s", (raw, expected) => {
    expect(formatRateFraction(asRateFraction(d(raw)))).toBe(expected)
  })

  it.each([
    ["1.005", "1.01%"],
    ["12.3", "12.30%"],
    ["-0.004", "0.00%"],
    ["100", "100.00%"],
  ])("퍼센트 %s 는 %s", (raw, expected) => {
    expect(formatRatePercent(asRatePercent(d(raw)))).toBe(expected)
  })

  it("null·undefined 는 «-» 이다", () => {
    expect(formatRateFraction(null)).toBe("-")
    expect(formatRatePercent(undefined)).toBe("-")
  })

  it("signed 는 양수에 +, 음수에 -, 0 에는 부호가 없다", () => {
    expect(formatRateFraction(asRateFraction(d("0.05")), { signed: true })).toBe("+5.00%")
    expect(formatRateFraction(asRateFraction(d("-0.05")), { signed: true })).toBe("-5.00%")
    expect(formatRateFraction(asRateFraction(d("0.00004")), { signed: true })).toBe("0.00%")
    expect(formatRatePercent(asRatePercent(d("2.5")), { signed: true })).toBe("+2.50%")
  })
})
