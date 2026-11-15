import { act, type ReactElement } from "react"
import { createRoot, type Root } from "react-dom/client"

// React 가 act() 안에서 갱신이 일어난다고 믿게 하는 표시(시험 환경 전용)
;(
  globalThis as { IS_REACT_ACT_ENVIRONMENT?: boolean }
).IS_REACT_ACT_ENVIRONMENT = true

const mounted: { root: Root; container: HTMLElement }[] = []

/** 요소를 문서에 그려 컨테이너를 돌려준다. 시험이 끝나면 cleanup() 을 부른다. */
export async function render(element: ReactElement): Promise<HTMLElement> {
  const container = document.createElement("div")
  document.body.appendChild(container)
  const root = createRoot(container)
  mounted.push({ root, container })
  await act(async () => {
    root.render(element)
  })
  return container
}

export async function cleanup(): Promise<void> {
  for (const { root, container } of mounted.splice(0)) {
    await act(async () => {
      root.unmount()
    })
    container.remove()
  }
}
