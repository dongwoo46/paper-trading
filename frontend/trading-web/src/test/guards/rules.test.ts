// @vitest-environment node
import { describe, expect, it } from "vitest"

import {
  findForbiddenCssVariables,
  findForbiddenNumberConversions,
  findTestImports,
  gitBlobSha1,
} from "./rules"

describe("gitBlobSha1", () => {
  it("git hash-object 와 같은 값을 낸다", () => {
    // `printf 'hello\n' | git hash-object --stdin` 의 결과
    expect(gitBlobSha1(Buffer.from("hello\n"))).toBe(
      "ce013625030ba8dba906f756967f9e9ca394464a"
    )
  })

  it("빈 파일은 git 의 빈 blob 해시다", () => {
    expect(gitBlobSha1(Buffer.alloc(0))).toBe(
      "e69de29bb2d1d6434b8b29ae775ad8c2e48c5391"
    )
  })
})

describe("findForbiddenCssVariables", () => {
  const forbidden = new Set(["--bg-main", "--color-order-buy"])

  it("금지 이름을 선언하거나 쓰면 잡는다", () => {
    const css =
      ":root { --bg-main: red; }\n.a { color: var(--color-order-buy); }"
    expect(findForbiddenCssVariables(css, forbidden)).toEqual([
      "--bg-main",
      "--color-order-buy",
    ])
  })

  it("이름이 더 긴 새 변수는 잡지 않는다", () => {
    expect(
      findForbiddenCssVariables(
        "--bg-main-new: 1; --color-order-buy2: 2",
        forbidden
      )
    ).toEqual([])
  })

  it("금지 이름이 없으면 빈 목록이다", () => {
    expect(findForbiddenCssVariables("--tone-rise: 1", forbidden)).toEqual([])
  })
})

describe("findForbiddenNumberConversions", () => {
  it("parseFloat, parseInt, Number(, Math. 를 잡는다", () => {
    const source = [
      "const a = parseFloat(x)",
      "const b = parseInt(x, 10)",
      "const c = Number(x)",
      "const d = Math.round(x)",
      "const e = Number.parseFloat(x)",
    ].join("\n")
    expect(findForbiddenNumberConversions(source)).toEqual([
      "parseFloat",
      "parseInt",
      "Number(",
      "Math.",
      "parseFloat",
    ])
  })

  it("Number 를 타입으로 쓰거나 비슷한 이름은 잡지 않는다", () => {
    const source = "type N = Number\nconst isNumberLike = 1\nconst MathLike = 2"
    expect(findForbiddenNumberConversions(source)).toEqual([])
  })
})

describe("findTestImports", () => {
  it("src/test 를 가리키는 import 를 잡는다", () => {
    const source = [
      'import a from "@/test/fixtures/x.json"',
      'import b from "../test/guards/rules"',
      'const c = await import("./test/fixtures/y")',
      'import ok from "@/shared/lib/utils"',
    ].join("\n")
    expect(findTestImports(source)).toEqual([
      "@/test/fixtures/x.json",
      "../test/guards/rules",
      "./test/fixtures/y",
    ])
  })

  it("test 로 끝나는 다른 이름 경로는 잡지 않는다", () => {
    expect(findTestImports('import a from "@/shared/latest/x"')).toEqual([])
  })
})
