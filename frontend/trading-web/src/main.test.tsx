import { afterEach, describe, expect, it, vi } from "vitest"

import { stubLocalStorage } from "@/test/memory-storage"

afterEach(() => {
  document.body.innerHTML = ""
  vi.resetModules()
  vi.unstubAllGlobals()
})

describe("main", () => {
  it("#root 에 앱을 그린다", async () => {
    stubLocalStorage()
    vi.stubGlobal("matchMedia", (query: string) => ({
      matches: false,
      media: query,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
    }))
    ;(
      globalThis as { IS_REACT_ACT_ENVIRONMENT?: boolean }
    ).IS_REACT_ACT_ENVIRONMENT = false
    document.body.innerHTML = '<div id="root"></div>'

    await import("./main")

    await vi.waitFor(() => {
      expect(document.querySelector("#root h1")?.textContent).toBe(
        "trading-web"
      )
    })
  })
})
