import { createHash } from "node:crypto"
import { readdirSync } from "node:fs"
import { join } from "node:path"

const SKIPPED_DIRECTORIES = new Set([
  "node_modules",
  "dist",
  "coverage",
  ".git",
])

/** git 이 파일 내용에 매기는 blob 해시(sha1 of "blob <크기>\0<내용>"). */
export function gitBlobSha1(content: Buffer): string {
  return createHash("sha1")
    .update(`blob ${content.length}\0`)
    .update(content)
    .digest("hex")
}

/** 프로젝트 루트 기준 상대 경로(/ 구분) 목록. node_modules·dist·coverage·.git 은 건너뛴다. */
export function listProjectFiles(root: string, relativeDir = ""): string[] {
  const entries = readdirSync(join(root, relativeDir), { withFileTypes: true })
  return entries
    .flatMap((entry) => {
      const relative = relativeDir ? `${relativeDir}/${entry.name}` : entry.name
      if (entry.isDirectory()) {
        return SKIPPED_DIRECTORIES.has(entry.name)
          ? []
          : listProjectFiles(root, relative)
      }
      return entry.isFile() ? [relative] : []
    })
    .sort()
}

/** 시험 전용 폴더(src/test)이거나 시험 파일(*.test.*, *.spec.*)이면 true. */
export function isTestFile(relativePath: string): boolean {
  return (
    relativePath.startsWith("src/test/") ||
    /\.(test|spec)\.(ts|tsx)$/.test(relativePath)
  )
}

/** text 에 나온 CSS 사용자 정의 변수 중 forbidden 에 있는 이름(처음 나온 순서, 중복 제거). */
export function findForbiddenCssVariables(
  text: string,
  forbidden: ReadonlySet<string>
): string[] {
  const found = new Set<string>()
  for (const match of text.matchAll(/--[A-Za-z0-9_-]+/g)) {
    if (forbidden.has(match[0])) found.add(match[0])
  }
  return [...found]
}

const NUMBER_CONVERSION =
  /\bparseFloat\b|\bparseInt\b|\bNumber\s*\(|\bMath\s*\./g

/** RULE-03: 부동소수점 변환 사용 위치(나온 순서, 중복 유지). */
export function findForbiddenNumberConversions(source: string): string[] {
  return [...source.matchAll(NUMBER_CONVERSION)].map((match) =>
    match[0].startsWith("Number")
      ? "Number("
      : match[0].startsWith("Math")
        ? "Math."
        : match[0]
  )
}

const IMPORT_SPECIFIER =
  /(?:\bfrom\s*|\bimport\s*\(\s*|\bimport\s+|\brequire\s*\(\s*)["']([^"']+)["']/g

/** import·require 대상 중 src/test(시험 전용 폴더)를 가리키는 경로. */
export function findTestImports(source: string): string[] {
  return [...source.matchAll(IMPORT_SPECIFIER)]
    .map((match) => match[1])
    .filter((specifier) => /(^|\/)test(\/|$)/.test(specifier))
}
