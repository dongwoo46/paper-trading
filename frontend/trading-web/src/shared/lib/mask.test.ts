import { describe, expect, it } from "vitest"

import { maskAccountNumber } from "./mask"

describe("maskAccountNumber (AS-01, RULE-11)", () => {
  it("XXXXXXXX-XX 의 앞 8자리 중 가운데 4자리를 가린다", () => {
    expect(maskAccountNumber("12345678-01")).toBe("12****78-01")
    expect(maskAccountNumber("50123456-22")).toBe("50****56-22")
  })

  it.each([
    "1234567801",
    "12345678-0",
    "12345678-012",
    "1234567-01",
    "abcdefgh-01",
    "12345678-0a",
    "12345678-01\n",
    " 12345678-01",
    "12345678–01",
    " ",
    "계좌",
  ])("형식이 이상한 %j 는 전체를 가린다", (raw) => {
    expect(maskAccountNumber(raw)).toBe("********")
  })

  it("형식이 이상한 값의 어떤 글자도 결과에 남지 않는다", () => {
    const raw = "9988776655"
    const masked = maskAccountNumber(raw)
    expect(masked).not.toMatch(/\d/)
  })

  it("값이 없으면 «-» 이다", () => {
    expect(maskAccountNumber(null)).toBe("-")
    expect(maskAccountNumber(undefined)).toBe("-")
    expect(maskAccountNumber("")).toBe("-")
  })
})
