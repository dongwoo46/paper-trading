import { afterEach, describe, expect, it, vi } from "vitest"

import { createIdempotencyKey } from "./idempotency"

const UUID_V4 =
  /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/

afterEach(() => {
  vi.unstubAllGlobals()
})

describe("createIdempotencyKey (RULE-04)", () => {
  it("UUID v4 형식의 문자열이다(서버 한도 100자 이내)", () => {
    const key = createIdempotencyKey()
    expect(key).toMatch(UUID_V4)
    expect(key.length).toBeLessThanOrEqual(100)
  })

  it("호출마다 다르다", () => {
    const keys = new Set(Array.from({ length: 1000 }, createIdempotencyKey))
    expect(keys.size).toBe(1000)
  })

  it("randomUUID 가 없는 환경(비보안 컨텍스트)에서도 getRandomValues 로 만든다", () => {
    vi.stubGlobal("crypto", {
      getRandomValues: (bytes: Uint8Array) => {
        bytes.fill(255)
        return bytes
      },
    })
    expect(createIdempotencyKey()).toMatch(UUID_V4)
  })

  it("난수원이 전혀 없으면 조용히 대체하지 않고 던진다", () => {
    vi.stubGlobal("crypto", {})
    expect(() => createIdempotencyKey()).toThrow()
  })
})
