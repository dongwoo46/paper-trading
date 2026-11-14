// @vitest-environment node
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
