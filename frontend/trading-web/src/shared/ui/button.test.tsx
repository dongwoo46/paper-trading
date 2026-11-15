import { afterEach, describe, expect, it } from "vitest"

import { cleanup, render } from "@/test/render"

import { Button } from "./button"

afterEach(cleanup)

describe("Button", () => {
  it("글자를 가진 button 요소로 그린다", async () => {
    const container = await render(<Button>확인</Button>)
    const button = container.querySelector("button")

    expect(button?.textContent).toBe("확인")
  })

  it("variant 로 클래스가 바뀐다", async () => {
    const container = await render(
      <>
        <Button>기본</Button>
        <Button variant="outline">윤곽</Button>
      </>
    )
    const [primary, outline] = Array.from(container.querySelectorAll("button"))

    expect(primary.className).toContain("bg-primary")
    expect(outline.className).not.toContain("bg-primary")
  })

  it("disabled 이면 누를 수 없다", async () => {
    const container = await render(<Button disabled>확인</Button>)

    expect(container.querySelector("button")?.disabled).toBe(true)
  })
})
