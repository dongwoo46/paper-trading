import { describe, expect, it } from "vitest"

import { normalizeDecimal, type DecimalString } from "./decimal"
import { toChartCoordinate } from "./chart-coordinate"

const d = (raw: string): DecimalString => {
  const value = normalizeDecimal(raw)
  if (value === null) throw new Error(`시험 입력이 10진수가 아닙니다: ${raw}`)
  return value
}

describe("toChartCoordinate (DEC-03, 좌표 전용 Number 변환)", () => {
  it("10진 문자열을 좌표 숫자로 바꾼다", () => {
    expect(toChartCoordinate(d("1234.5"))).toBe(1234.5)
    expect(toChartCoordinate(d("-0.00000001"))).toBe(-1e-8)
    expect(toChartCoordinate(d("0"))).toBe(0)
  })

  it("20자리 정수도 유한한 숫자로 바뀐다(정밀도는 좌표에만 영향)", () => {
    const value = toChartCoordinate(d("12345678901234567890.12"))
    expect(Number.isFinite(value)).toBe(true)
  })

  it("유한하지 않은 결과는 던진다(좌표에 NaN·Infinity 를 넘기지 않는다)", () => {
    const huge = `1${"0".repeat(400)}` as DecimalString
    expect(() => toChartCoordinate(huge)).toThrow(RangeError)
  })
})
