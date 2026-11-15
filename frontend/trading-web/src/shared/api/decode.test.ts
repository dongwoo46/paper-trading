import { describe, expect, it } from "vitest"

import {
  DecodeError,
  decodeArray,
  decodeBoolean,
  decodeDecimal,
  decodeNullable,
  decodeObject,
  decodeOneOf,
  decodeRateFraction,
  decodeRatePercent,
  decodeRecord,
  decodeSafeInteger,
  decodeString,
} from "./decode"

const failure = (run: () => unknown): DecodeError => {
  try {
    run()
  } catch (error) {
    if (error instanceof DecodeError) return error
    throw error
  }
  throw new Error("DecodeError 가 던져지지 않았습니다")
}

describe("decodeDecimal (계획 §6 4행 golden)", () => {
  it.each([
    ["1000000", "1000000"],
    ["1000000.0000", "1000000.0000"],
    ["0.1", "0.1"],
    ["-1234.5", "-1234.5"],
    ["1E-8", "0.00000001"],
    ["12345678901234567890.12", "12345678901234567890.12"],
  ])("원문 토큰 %s 는 %s 로 평문화·보존한다", (raw, expected) => {
    expect(decodeDecimal(raw, "v")).toBe(expected)
  })

  it("JS number 는 받지 않는다(파서를 우회한 값, 이미 부동소수점)", () => {
    const error = failure(() => decodeDecimal(0.1, "deposit"))
    expect(error.path).toBe("deposit")
  })

  it.each([null, undefined, true, {}, [], "abc", ""])("%j 는 거부한다", (value) => {
    expect(() => decodeDecimal(value, "v")).toThrow(DecodeError)
  })
})

describe("decodeSafeInteger", () => {
  it("안전 정수 원문 토큰을 number 로 바꾼다", () => {
    expect(decodeSafeInteger("123", "id")).toBe(123)
    expect(decodeSafeInteger("-5", "id")).toBe(-5)
    expect(decodeSafeInteger("9007199254740991", "id")).toBe(9007199254740991)
  })

  it.each(["9007199254740992", "1.5", "1E3", "", "abc"])("%j 는 거부한다", (raw) => {
    expect(() => decodeSafeInteger(raw, "id")).toThrow(DecodeError)
  })

  it("JS number·null 은 거부한다", () => {
    expect(() => decodeSafeInteger(5, "id")).toThrow(DecodeError)
    expect(() => decodeSafeInteger(null, "id")).toThrow(DecodeError)
  })
})

describe("decodeRateFraction · decodeRatePercent", () => {
  it("정규화한 10진 문자열을 돌려준다(값은 그대로, 단위는 타입으로 구분)", () => {
    expect(decodeRateFraction("0.1234", "r")).toBe("0.1234")
    expect(decodeRatePercent("1E-1", "r")).toBe("0.1")
  })

  it("10진수가 아니면 거부한다", () => {
    expect(() => decodeRateFraction("12%", "r")).toThrow(DecodeError)
    expect(() => decodeRatePercent(0.5, "r")).toThrow(DecodeError)
  })
})

describe("decodeString · decodeBoolean · decodeOneOf", () => {
  it("형이 맞으면 통과한다", () => {
    expect(decodeString("a", "s")).toBe("a")
    expect(decodeBoolean(false, "b")).toBe(false)
    expect(decodeOneOf(["LOCAL", "KIS_PAPER"] as const)("LOCAL", "mode")).toBe("LOCAL")
  })

  it("형이 다르면 거부한다", () => {
    expect(() => decodeString(1, "s")).toThrow(DecodeError)
    expect(() => decodeBoolean("true", "b")).toThrow(DecodeError)
    expect(() => decodeOneOf(["LOCAL"] as const)("KIS_LIVE", "mode")).toThrow(DecodeError)
  })
})

describe("decodeNullable", () => {
  it("null·undefined 는 null 로, 값이 있으면 안쪽 디코더를 거친다", () => {
    const decode = decodeNullable(decodeDecimal)
    expect(decode(null, "p")).toBeNull()
    expect(decode(undefined, "p")).toBeNull()
    expect(decode("1E-8", "p")).toBe("0.00000001")
  })

  it("값이 있는데 안쪽 디코더가 거부하면 거부한다", () => {
    expect(() => decodeNullable(decodeDecimal)("abc", "p")).toThrow(DecodeError)
  })
})

describe("decodeRecord · decodeArray · decodeObject", () => {
  it("record 는 배열·null 을 거부한다", () => {
    expect(decodeRecord({ a: 1 }, "r")).toEqual({ a: 1 })
    expect(() => decodeRecord([], "r")).toThrow(DecodeError)
    expect(() => decodeRecord(null, "r")).toThrow(DecodeError)
  })

  it("배열 오류 경로에 인덱스가 붙는다", () => {
    const error = failure(() => decodeArray(decodeString)(["a", 2], "items"))
    expect(error.path).toBe("items[1]")
  })

  it("객체 디코더는 선언한 필드만 꺼내고 서버가 더 준 필드는 버린다", () => {
    const decodeAccount = decodeObject({
      id: decodeSafeInteger,
      deposit: decodeDecimal,
      note: decodeNullable(decodeString),
    })
    expect(
      decodeAccount({ id: "7", deposit: "1000000.0000", extra: "x" }, "account")
    ).toEqual({ id: 7, deposit: "1000000.0000", note: null })
  })

  it("중첩 오류 경로를 점으로 잇는다", () => {
    const decodeResponse = decodeObject({
      items: decodeArray(decodeObject({ price: decodeDecimal })),
    })
    const error = failure(() =>
      decodeResponse({ items: [{ price: "1" }, { price: "x" }] }, "response")
    )
    expect(error.path).toBe("response.items[1].price")
  })

  it("없는 필드는 거부하고 경로에 필드 이름이 들어간다", () => {
    const error = failure(() =>
      decodeObject({ deposit: decodeDecimal })({}, "account")
    )
    expect(error.path).toBe("account.deposit")
  })
})

describe("DecodeError", () => {
  it("메시지에 값 자체를 싣지 않는다(계좌번호·토큰 노출 방지)", () => {
    const error = failure(() => decodeDecimal("12345678-01", "account"))
    expect(error.message).not.toContain("12345678-01")
    expect(error.message).toContain("account")
  })
})
