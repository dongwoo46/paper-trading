import { afterEach, describe, expect, it } from "vitest"

import { cleanup, render } from "@/test/render"

import { App } from "./App"

afterEach(cleanup)

describe("App", () => {
  it("골격 화면을 그린다", async () => {
    const container = await render(<App />)

    expect(container.querySelector("h1")?.textContent).toBe("trading-web")
  })
})
