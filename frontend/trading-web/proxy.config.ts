import type { ProxyOptions } from "vite"

/**
 * 개발 서버 프록시 표 (D10). `server.proxy` 와 `preview.proxy` 가 같은 표를 쓴다.
 * 브라우저는 백엔드를 직접 부르지 못한다(CORS 는 체결 SSE 컨트롤러 하나뿐).
 */
export type ProxyEnv = Partial<
  Record<
    | "VITE_TRADING_API_TARGET"
    | "VITE_COLLECTOR_API_TARGET"
    | "VITE_QUANT_API_TARGET",
    string
  >
>

export const DEFAULT_TARGETS = {
  trading: "http://127.0.0.1:8080",
  collector: "http://127.0.0.1:8081",
  quant: "http://127.0.0.1:8082",
} as const

export const TRADING_API_PREFIXES = [
  "/api/v1",
  "/api/accounts",
  "/api/kis/account",
  "/api/positions",
  "/api/trading-journals",
  "/api/operations",
  "/api/tax-summaries",
] as const

export const COLLECTOR_API_PREFIXES = [
  "/api/kis/ws",
  "/api/kis/rest",
  "/api/kis/symbols",
  "/api/upbit",
  "/api/fred",
  "/api/pykrx",
  "/api/yfinance",
  "/api/subscriptions",
  "/api/symbols",
  "/api/market",
  "/api/internal/kis",
  "/api/internal/subscriptions",
] as const

/** quant-worker 는 접두사를 떼고 보낸다(`/api-quant/collect/daily` → `/collect/daily`). */
export const QUANT_API_PREFIX = "/api-quant"

export function buildProxyTable(env: ProxyEnv): Record<string, ProxyOptions> {
  const trading = env.VITE_TRADING_API_TARGET || DEFAULT_TARGETS.trading
  const collector = env.VITE_COLLECTOR_API_TARGET || DEFAULT_TARGETS.collector
  const quant = env.VITE_QUANT_API_TARGET || DEFAULT_TARGETS.quant

  const table: Record<string, ProxyOptions> = {}
  for (const prefix of TRADING_API_PREFIXES) {
    table[prefix] = { target: trading, changeOrigin: true }
  }
  for (const prefix of COLLECTOR_API_PREFIXES) {
    table[prefix] = { target: collector, changeOrigin: true }
  }
  table[QUANT_API_PREFIX] = {
    target: quant,
    changeOrigin: true,
    rewrite: (path) => path.slice(QUANT_API_PREFIX.length),
  }
  return table
}
