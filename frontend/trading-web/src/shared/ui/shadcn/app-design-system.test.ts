import { readdirSync, readFileSync } from "node:fs"
import { extname, join, relative } from "node:path"
import { describe, expect, it } from "vitest"

const sourceRoot = join(process.cwd(), "src")

function collectProductionTsxFiles(directory: string): string[] {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const path = join(directory, entry.name)

    if (entry.isDirectory()) {
      if (path.includes(join("shared", "ui", "shadcn"))) return []
      return collectProductionTsxFiles(path)
    }

    if (extname(entry.name) !== ".tsx" || entry.name.includes(".test.")) return []
    return [path]
  })
}

const forbiddenPatterns = [
  {
    label: "raw interactive/table element",
    pattern: /<(?:button|input|select|textarea|table|thead|tbody|tfoot|tr|th|td)(?:\s|>)/g,
  },
  {
    label: "legacy design token class",
    pattern: /\b(?:bg-bg-|text-text-|border-border-|bg-brand-|text-brand-|border-brand-|bg-status-|text-status-|border-status-)/g,
  },
  {
    label: "hard-coded Tailwind palette class",
    pattern: /\b(?:bg|text|border|ring|from|via|to)-(?:black|white|gray|slate|zinc|neutral|stone|red|orange|amber|yellow|lime|green|emerald|teal|cyan|sky|blue|indigo|violet|purple|fuchsia|pink|rose)(?:-|\/|\b)/g,
  },
  {
    label: "hard-coded color literal",
    pattern: /(?:#[0-9a-fA-F]{3,8}\b|rgba?\()/g,
  },
]

describe("application-wide shadcn design-system contract", () => {
  it("uses shadcn primitives and semantic tokens in every production screen", () => {
    const violations = collectProductionTsxFiles(sourceRoot).flatMap((path) => {
      const source = readFileSync(path, "utf8")

      return forbiddenPatterns.flatMap(({ label, pattern }) => {
        pattern.lastIndex = 0
        return pattern.test(source)
          ? [`${relative(sourceRoot, path)}: ${label}`]
          : []
      })
    })

    expect(violations).toEqual([])
  })
})
