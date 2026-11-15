import { act } from "react"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { stubLocalStorage } from "@/test/memory-storage"
import { cleanup, render } from "@/test/render"

import { ThemeProvider, useTheme } from "./theme-provider"

function stubSystemTheme(prefersDark: boolean) {
  vi.stubGlobal("matchMedia", (query: string) => ({
    matches: prefersDark,
    media: query,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
  }))
}

function ThemeProbe() {
  const { theme } = useTheme()
  return <span data-testid="theme">{theme}</span>
}

beforeEach(() => {
  stubLocalStorage()
  document.documentElement.className = ""
  stubSystemTheme(false)
})

afterEach(async () => {
  await cleanup()
  vi.unstubAllGlobals()
})

describe("ThemeProvider", () => {
  it("저장된 값이 없으면 시스템 설정(라이트)을 따른다", async () => {
    await render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>
    )

    expect(document.documentElement.classList.contains("light")).toBe(true)
  })

  it("시스템이 다크면 다크 클래스를 붙인다", async () => {
    stubSystemTheme(true)
    await render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>
    )

    expect(document.documentElement.classList.contains("dark")).toBe(true)
  })

  it("저장된 테마를 우선한다", async () => {
    localStorage.setItem("theme", "dark")
    const container = await render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>
    )

    expect(container.textContent).toBe("dark")
    expect(document.documentElement.classList.contains("dark")).toBe(true)
  })

  it("d 키로 라이트·다크를 바꾸고 저장한다", async () => {
    localStorage.setItem("theme", "light")
    await render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>
    )

    await act(async () => {
      window.dispatchEvent(new KeyboardEvent("keydown", { key: "d" }))
    })

    expect(document.documentElement.classList.contains("dark")).toBe(true)
    expect(localStorage.getItem("theme")).toBe("dark")
  })

  it("Provider 밖에서 useTheme 을 쓰면 오류다", async () => {
    const spy = vi.spyOn(console, "error").mockImplementation(() => undefined)

    await expect(render(<ThemeProbe />)).rejects.toThrow(
      "useTheme must be used within a ThemeProvider"
    )

    spy.mockRestore()
  })
})
