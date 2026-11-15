import { describe, expect, it } from "vitest"

import { parseJsonLossless } from "./parse-json"

describe("parseJsonLossless (D4, 계획 §6 4행 golden)", () => {
  it("JSON 숫자 토큰을 원문 문자열로 보존한다", () => {
    const parsed = parseJsonLossless(
      '{"a":1000000,"b":1000000.0000,"c":0.1,"d":-1234.5,"e":1E-8,"f":12345678901234567890.12}'
    )
    expect(parsed).toEqual({
      a: "1000000",
      b: "1000000.0000",
      c: "0.1",
      d: "-1234.5",
      e: "1E-8",
      f: "12345678901234567890.12",
    })
  })

  it("2^53 을 넘는 정수 토큰도 잃지 않는다", () => {
    expect(parseJsonLossless("9007199254740993")).toBe("9007199254740993")
  })

  it("음의 0·지수 부호를 그대로 둔다", () => {
    expect(parseJsonLossless("[-0, 1e+3, 2.50E-2]")).toEqual(["-0", "1e+3", "2.50E-2"])
  })

  it("JSON 문자열·불리언·null 은 그대로, 배열·객체는 재귀로 처리한다", () => {
    expect(
      parseJsonLossless('{"s":"1.50","t":true,"f":false,"n":null,"list":[1,{"x":2.0}]}')
    ).toEqual({ s: "1.50", t: true, f: false, n: null, list: ["1", { x: "2.0" }] })
  })

  it("같은 키가 두 번 나오면 한쪽으로 덮지 않고 던진다(금액 필드 모호성 방지)", () => {
    expect(() => parseJsonLossless('{"price":1,"price":2}')).toThrow(SyntaxError)
  })

  it("JSON 이 아니면 던진다", () => {
    expect(() => parseJsonLossless("{not json")).toThrow(SyntaxError)
    expect(() => parseJsonLossless("")).toThrow(SyntaxError)
  })
})
