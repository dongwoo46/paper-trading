import { describe, expect, it } from "vitest"

import { parseSafeInteger } from "./safe-integer"

describe("parseSafeInteger (id·개수 전용)", () => {
  it.each([
    ["0", 0],
    ["12", 12],
    ["-5", -5],
    ["007", 7],
    ["-0", 0],
    ["9007199254740991", 9007199254740991],
    ["-9007199254740991", -9007199254740991],
  ])("%s 는 %i", (text, expected) => {
    expect(parseSafeInteger(text)).toBe(expected)
  })

  it("-0 은 음의 0 이 아니라 0 이다", () => {
    expect(Object.is(parseSafeInteger("-0"), 0)).toBe(true)
  })

  it.each([
    "9007199254740992",
    "-9007199254740992",
    "12345678901234567890",
    "1.5",
    "1.0",
    "1E3",
    "",
    " 1",
    "1 ",
    "abc",
    "+1",
    "--1",
  ])("안전 정수가 아닌 %j 는 null 이다", (text) => {
    expect(parseSafeInteger(text)).toBeNull()
  })
})
