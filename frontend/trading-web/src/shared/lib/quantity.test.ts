import { describe, expect, it } from "vitest"

import { normalizeDecimal, type DecimalString } from "./decimal"
import { fillProgressPercent, remainingQuantity } from "./quantity"

const d = (raw: string): DecimalString => {
  const value = normalizeDecimal(raw)
  if (value === null) throw new Error(`시험 입력이 10진수가 아닙니다: ${raw}`)
  return value
}

describe("remainingQuantity (D6, 남은 수량 = 주문수량 − 체결수량)", () => {
  it.each([
    ["10", "3", "7"],
    ["10.5", "3", "7.5"],
    ["10", "10", "0"],
    ["10.00", "10", "0.00"],
    ["0.00000010", "0.00000001", "0.00000009"],
    ["0.3", "0.1", "0.2"],
    ["12345678901234567890", "1", "12345678901234567889"],
    ["1", "3", "-2"],
  ])("%s − %s = %s", (quantity, filled, expected) => {
    expect(remainingQuantity(d(quantity), d(filled))).toBe(expected)
  })
})

describe("fillProgressPercent (D6, 진행 폭 정수 %)", () => {
  it.each([
    ["10", "3", 30],
    ["3", "1", 33],
    ["10", "10", 100],
    ["1000", "999", 99],
    ["0.00000003", "0.00000001", 33],
    ["10", "0", 0],
    ["0", "0", 0],
    ["10", "11", 100],
    ["-5", "1", 0],
    ["10", "-1", 0],
  ])("주문 %s 체결 %s 는 %i%%", (quantity, filled, expected) => {
    expect(fillProgressPercent(d(quantity), d(filled))).toBe(expected)
  })

  it("정수로만 돌려준다", () => {
    expect(Number.isInteger(fillProgressPercent(d("7"), d("2")))).toBe(true)
  })
})
