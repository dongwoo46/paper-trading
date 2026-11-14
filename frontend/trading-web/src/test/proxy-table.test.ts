// @vitest-environment node
import { readFileSync, readdirSync, statSync } from "node:fs"
import { join, resolve } from "node:path"
import { describe, expect, it } from "vitest"

import {
  COLLECTOR_API_PREFIXES,
  DEFAULT_TARGETS,
  QUANT_API_PREFIX,
  TRADING_API_PREFIXES,
  buildProxyTable,
} from "../../proxy.config"

describe("개발 프록시 표 (D10)", () => {
  it("환경 변수가 없으면 서비스별 기본 포트로 보낸다", () => {
    const table = buildProxyTable({})

    for (const prefix of TRADING_API_PREFIXES) {
      expect(table[prefix]?.target).toBe(DEFAULT_TARGETS.trading)
    }
    for (const prefix of COLLECTOR_API_PREFIXES) {
      expect(table[prefix]?.target).toBe(DEFAULT_TARGETS.collector)
    }
    expect(table[QUANT_API_PREFIX]?.target).toBe(DEFAULT_TARGETS.quant)
  })

  it("기본 대상은 trading-api 8080, collector-api 8081, quant-worker 8082 이다", () => {
    expect(DEFAULT_TARGETS).toEqual({
      trading: "http://127.0.0.1:8080",
      collector: "http://127.0.0.1:8081",
      quant: "http://127.0.0.1:8082",
    })
  })

  it("환경 변수로 서비스별 대상을 바꾼다", () => {
    const table = buildProxyTable({
      VITE_TRADING_API_TARGET: "http://trading.test:1",
      VITE_COLLECTOR_API_TARGET: "http://collector.test:2",
      VITE_QUANT_API_TARGET: "http://quant.test:3",
    })

    expect(table[TRADING_API_PREFIXES[0]]?.target).toBe("http://trading.test:1")
    expect(table[COLLECTOR_API_PREFIXES[0]]?.target).toBe(
      "http://collector.test:2"
    )
    expect(table[QUANT_API_PREFIX]?.target).toBe("http://quant.test:3")
  })

  it("trading-api 와 collector-api 접두사가 겹치지 않는다", () => {
    const collector: readonly string[] = COLLECTOR_API_PREFIXES
    const overlap = TRADING_API_PREFIXES.filter((prefix) =>
      collector.includes(prefix)
    )
    expect(overlap).toEqual([])
  })

  it("quant-worker 접두사는 대상으로 보내기 전에 떼어 낸다", () => {
    const rewrite = buildProxyTable({})[QUANT_API_PREFIX]?.rewrite
    expect(rewrite?.("/api-quant/collect/daily")).toBe("/collect/daily")
  })

  it("각 접두사는 한 서비스에만 연결된다", () => {
    const table = buildProxyTable({})
    const expected =
      TRADING_API_PREFIXES.length + COLLECTOR_API_PREFIXES.length + 1
    expect(Object.keys(table)).toHaveLength(expected)
  })
})

/**
 * 프록시 표가 백엔드 컨트롤러가 실제로 여는 경로를 다 덮는지 본다.
 * 표끼리만 맞춰 보면(위 시험들) 표에 빠진 경로를 못 잡는다 — 컨트롤러 소스를 읽어 대조한다.
 * 클래스 수준 `@RequestMapping` 이 `/api` 처럼 넓은 컨트롤러는 메서드 수준 경로까지 붙여 본다.
 */
const repoRoot = resolve(import.meta.dirname, "../../../..")

function listKotlinFiles(dir: string): string[] {
  return readdirSync(dir).flatMap((name) => {
    const path = join(dir, name)
    return statSync(path).isDirectory()
      ? listKotlinFiles(path)
      : path.endsWith(".kt")
        ? [path]
        : []
  })
}

/** 컨트롤러 한 파일이 여는 경로 전부(클래스 경로 + 메서드 경로). 못 읽는 모양이면 던진다. */
function readControllerRoutes(source: string, file: string): string[] {
  const classMappings = [...source.matchAll(/^@RequestMapping\("([^"]*)"\)/gm)]
  if (classMappings.length !== 1) {
    throw new Error(`${file}: 클래스 수준 @RequestMapping("...") 가 하나가 아니다`)
  }
  const base = classMappings[0][1]

  const annotations = [
    ...source.matchAll(/^[ \t]+@(?:Get|Post|Put|Delete|Patch)Mapping\b(\([^)]*\))?/gm),
  ]
  const looseCount = [
    ...source.matchAll(/^[ \t]+@(?:Get|Post|Put|Delete|Patch|Request)Mapping\b/gm),
  ].length
  if (annotations.length !== looseCount) {
    throw new Error(`${file}: 읽지 못하는 메서드 매핑 모양이 있다`)
  }

  const routes = annotations.map((match) => {
    const args = match[1]
    if (!args) return base
    const literal = args.match(/^\(\s*(?:(?:value|path)\s*=\s*)?"([^"]*)"/)
    if (literal) return base + literal[1]
    if (/\b(?:value|path)\s*=/.test(args) || /^\(\s*\[/.test(args)) {
      throw new Error(`${file}: 읽지 못하는 메서드 경로 모양 ${args}`)
    }
    return base
  })
  return routes.length > 0 ? routes : [base]
}

function covers(prefix: string, route: string): boolean {
  return route === prefix || route.startsWith(`${prefix}/`)
}

function routesOf(service: "trading-api" | "collector-api"): string[] {
  const dir = join(repoRoot, "backend", service, "src/main")
  const controllers = listKotlinFiles(dir).filter((file) =>
    /^@(?:RestController|Controller)\b/m.test(readFileSync(file, "utf8"))
  )
  expect(controllers.length).toBeGreaterThan(0)
  return controllers.flatMap((file) =>
    readControllerRoutes(readFileSync(file, "utf8"), file)
  )
}

describe("프록시 표 ↔ 백엔드 컨트롤러 경로", () => {
  it("trading-api 컨트롤러 경로는 모두 trading-api 접두사로만 덮인다", () => {
    const uncovered = routesOf("trading-api").filter(
      (route) =>
        !TRADING_API_PREFIXES.some((prefix) => covers(prefix, route)) ||
        COLLECTOR_API_PREFIXES.some((prefix) => covers(prefix, route))
    )
    expect(uncovered).toEqual([])
  })

  it("collector-api 컨트롤러 경로는 모두 collector-api 접두사로만 덮인다", () => {
    const uncovered = routesOf("collector-api").filter(
      (route) =>
        !COLLECTOR_API_PREFIXES.some((prefix) => covers(prefix, route)) ||
        TRADING_API_PREFIXES.some((prefix) => covers(prefix, route))
    )
    expect(uncovered).toEqual([])
  })

  it("매핑 읽기 도우미: 넓은 클래스 경로는 메서드 경로까지 붙이고 못 읽는 모양은 던진다", () => {
    const wide = [
      '@RequestMapping("/api")',
      "class A {",
      '  @GetMapping("/x/{id}")',
      '  @PostMapping(value = "/y", consumes = ["a"])',
      "  @GetMapping",
      "}",
    ].join("\n")
    expect(readControllerRoutes(wide, "A.kt")).toEqual([
      "/api/x/{id}",
      "/api/y",
      "/api",
    ])
    expect(() =>
      readControllerRoutes(
        '@RequestMapping("/api")\nclass A {\n  @GetMapping(value = ["/z"])\n}',
        "B.kt"
      )
    ).toThrow()
    expect(() => readControllerRoutes("class C", "C.kt")).toThrow()
  })
})
