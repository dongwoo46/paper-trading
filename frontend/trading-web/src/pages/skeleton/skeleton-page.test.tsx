import { afterEach, describe, expect, it } from "vitest"

import { cleanup, render } from "@/test/render"

import { SkeletonPage } from "./skeleton-page"

afterEach(cleanup)

describe("SkeletonPage", () => {
  it("앱 이름 제목과 골격 안내 문구를 보인다", async () => {
    const container = await render(<SkeletonPage />)

    expect(container.querySelector("h1")?.textContent).toBe("trading-web")
    expect(container.textContent).toContain("교체 골격입니다")
  })

  it("본문은 main 영역 하나다", async () => {
    const container = await render(<SkeletonPage />)

    expect(container.querySelectorAll("main")).toHaveLength(1)
  })
})
