import { describe, expect, it } from "vitest"

import { cn } from "./utils"

describe("cn", () => {
  it("거짓 값은 빼고 클래스를 이어 붙인다", () => {
    expect(cn("a", false, undefined, "b")).toBe("a b")
  })

  it("충돌하는 Tailwind 클래스는 뒤의 것만 남긴다", () => {
    expect(cn("p-2", "p-4")).toBe("p-4")
  })
})
