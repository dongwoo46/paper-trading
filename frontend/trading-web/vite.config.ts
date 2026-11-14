import { resolve } from "node:path"
import tailwindcss from "@tailwindcss/vite"
import react from "@vitejs/plugin-react"
import { loadEnv } from "vite"
import { defineConfig } from "vitest/config"

import { buildProxyTable } from "./proxy.config.ts"

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const proxy = buildProxyTable(loadEnv(mode, process.cwd(), "VITE_"))

  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: {
        "@": resolve(import.meta.dirname, "./src"),
      },
    },
    server: { proxy },
    preview: { proxy },
    test: {
      environment: "jsdom",
      include: ["src/**/*.test.{ts,tsx}"],
    },
  }
})
