import { describe, expect, it } from "vitest"

import {
  compareDecimal,
  decimalSign,
  decimalTone,
  formatDecimal,
  formatWon,
  normalizeDecimal,
  roundDecimal,
  shiftDecimalPoint,
  splitDecimal,
  type DecimalString,
} from "./decimal"

// 시험 전용: 올바른 입력만 넘기므로 null 이면 시험이 잘못된 것
const d = (raw: string): DecimalString => {
  const value = normalizeDecimal(raw)
  if (value === null) throw new Error(`시험 입력이 10진수가 아닙니다: ${raw}`)
  return value
}

describe("normalizeDecimal (계획 §6 4행 golden)", () => {
  it.each([
    ["1000000", "1000000"],
    ["1000000.0000", "1000000.0000"],
    ["0.1", "0.1"],
    ["-1234.5", "-1234.5"],
    ["1E-8", "0.00000001"],
    ["12345678901234567890.12", "12345678901234567890.12"],
  ])("%s 는 %s 로 평문화·원문 보존한다", (raw, expected) => {
    expect(normalizeDecimal(raw)).toBe(expected)
  })

  it.each([
    ["1E+3", "1000"],
    ["1e3", "1000"],
    ["1.5E3", "1500"],
    ["1.50E1", "15.0"],
    ["12.5E-3", "0.0125"],
    ["-2.5E-4", "-0.00025"],
    ["+5", "5"],
    ["007.50", "7.50"],
    ["0.0", "0.0"],
    ["-0.0", "0.0"],
    ["-0", "0"],
  ])("%s 는 %s 로 정규화한다", (raw, expected) => {
    expect(normalizeDecimal(raw)).toBe(expected)
  })

  it.each(["", " 1", "1 ", "abc", "1e", "1.", ".5", "--1", "NaN", "Infinity", "1,000"])(
    "10진수가 아닌 %j 는 null 이다",
    (raw) => {
      expect(normalizeDecimal(raw)).toBeNull()
    }
  )

  it("지수가 비정상적으로 크면 null 이다(메모리 폭증 방지)", () => {
    expect(normalizeDecimal("1E9999")).toBeNull()
    expect(normalizeDecimal("1E-9999")).toBeNull()
  })
})

describe("splitDecimal", () => {
  it("부호·정수부·소수부로 나눈다", () => {
    expect(splitDecimal(d("-1234.50"))).toEqual({
      negative: true,
      integer: "1234",
      fraction: "50",
    })
    expect(splitDecimal(d("7"))).toEqual({
      negative: false,
      integer: "7",
      fraction: "",
    })
  })
})

describe("formatDecimal", () => {
  it.each([
    ["1000000", "1,000,000"],
    ["1000000.0000", "1,000,000.0000"],
    ["0.1", "0.1"],
    ["-1234.5", "-1,234.5"],
    ["1E-8", "0.00000001"],
    ["12345678901234567890.12", "12,345,678,901,234,567,890.12"],
    ["999", "999"],
    ["1000", "1,000"],
  ])("%s 는 %s 로 자리 구분해 보인다", (raw, expected) => {
    expect(formatDecimal(d(raw))).toBe(expected)
  })

  it("null·undefined 는 «-» 이다", () => {
    expect(formatDecimal(null)).toBe("-")
    expect(formatDecimal(undefined)).toBe("-")
  })

  it("fractionDigits 는 half-up 으로 맞추고 모자란 자리는 0 으로 채운다", () => {
    expect(formatDecimal(d("1234.565"), { fractionDigits: 2 })).toBe("1,234.57")
    expect(formatDecimal(d("1234.564"), { fractionDigits: 2 })).toBe("1,234.56")
    expect(formatDecimal(d("1"), { fractionDigits: 2 })).toBe("1.00")
    expect(formatDecimal(d("0.005"), { fractionDigits: 2 })).toBe("0.01")
    expect(formatDecimal(d("999.995"), { fractionDigits: 2 })).toBe("1,000.00")
    expect(formatDecimal(d("1234.5"), { fractionDigits: 0 })).toBe("1,235")
  })

  it("음수 half-up 은 0 에서 먼 쪽으로 올린다", () => {
    expect(formatDecimal(d("-1234.565"), { fractionDigits: 2 })).toBe("-1,234.57")
  })

  it("반올림 결과가 0 이면 음수 부호를 지운다", () => {
    expect(formatDecimal(d("-0.004"), { fractionDigits: 2 })).toBe("0.00")
  })

  it("signed 는 양수에 +, 음수에 -, 0 에는 부호가 없다", () => {
    expect(formatDecimal(d("1500"), { signed: true })).toBe("+1,500")
    expect(formatDecimal(d("-1500"), { signed: true })).toBe("-1,500")
    expect(formatDecimal(d("0"), { signed: true })).toBe("0")
    expect(formatDecimal(d("0.001"), { signed: true, fractionDigits: 2 })).toBe("0.00")
  })
})

describe("formatWon (AS-02)", () => {
  it("소수부가 모두 0 이면 정수로 보인다", () => {
    expect(formatWon(d("1000000.0000"))).toBe("1,000,000")
    expect(formatWon(d("0.00"))).toBe("0")
  })

  it("소수부가 0 이 아니면 그대로 보이고 반올림하지 않는다", () => {
    expect(formatWon(d("1234.50"))).toBe("1,234.50")
    expect(formatWon(d("0.00000001"))).toBe("0.00000001")
  })

  it("null 은 «-» 이다", () => {
    expect(formatWon(null)).toBe("-")
  })
})

describe("roundDecimal", () => {
  it("half-up 으로 지정 자리까지 맞춘 10진 문자열을 돌려준다", () => {
    expect(roundDecimal(d("12.345"), 2)).toBe("12.35")
    expect(roundDecimal(d("12.3449"), 2)).toBe("12.34")
    expect(roundDecimal(d("12"), 2)).toBe("12.00")
    expect(roundDecimal(d("12.5"), 0)).toBe("13")
    expect(roundDecimal(d("-12.5"), 0)).toBe("-13")
  })
})

describe("compareDecimal", () => {
  it.each([
    ["2", "10", -1],
    ["-2", "-10", 1],
    ["1.0", "1", 0],
    ["0.1", "0.09", 1],
    ["-0.1", "0.1", -1],
    ["0.0", "-0", 0],
    ["12345678901234567890.12", "12345678901234567890.13", -1],
    ["12345678901234567891", "12345678901234567890.99", 1],
  ])("%s 와 %s 를 비교하면 %i", (a, b, expected) => {
    expect(compareDecimal(d(a), d(b))).toBe(expected)
  })

  it("정렬에 쓰면 문자열 길이가 아니라 값 순서가 된다", () => {
    const sorted = ["10", "-3", "2.5", "0.001", "-30"]
      .map(d)
      .sort(compareDecimal)
    expect(sorted).toEqual(["-30", "-3", "0.001", "2.5", "10"])
  })
})

describe("decimalSign · decimalTone (RULE-05)", () => {
  it("부호를 -1·0·1 로 돌려준다", () => {
    expect(decimalSign(d("1.5"))).toBe(1)
    expect(decimalSign(d("-0.00000001"))).toBe(-1)
    expect(decimalSign(d("0.000"))).toBe(0)
  })

  it("양수는 rise, 음수는 fall, 0·값 없음은 flat 이다", () => {
    expect(decimalTone(d("1.5"))).toBe("rise")
    expect(decimalTone(d("-1"))).toBe("fall")
    expect(decimalTone(d("0"))).toBe("flat")
    expect(decimalTone(null)).toBe("flat")
    expect(decimalTone(undefined)).toBe("flat")
  })
})

describe("shiftDecimalPoint", () => {
  it.each([
    ["0.1234", 2, "12.34"],
    ["12.34", -2, "0.1234"],
    ["0.5", 2, "50"],
    ["3", 2, "300"],
    ["-0.0123", 2, "-1.23"],
    ["1.5", -3, "0.0015"],
    ["12345678901234567890.12", 2, "1234567890123456789012"],
    ["0", 2, "0"],
    ["1.50", 2, "150"],
  ])("%s 의 소수점을 %i 칸 옮기면 %s", (raw, places, expected) => {
    expect(shiftDecimalPoint(d(raw), places)).toBe(expected)
  })
})
