// @vitest-environment node
import { readFileSync } from "node:fs"
import { resolve } from "node:path"
import { describe, expect, it } from "vitest"

import {
  findForbiddenCssVariables,
  findForbiddenNumberConversions,
  findTestImports,
  gitBlobSha1,
  listProjectFiles,
  isTestFile,
} from "./rules"

const projectRoot = resolve(import.meta.dirname, "../../..")
const srcRoot = resolve(projectRoot, "src")

type OldBlobs = { count: number; files: { blob: string; path: string }[] }
type OldCssNames = { names: string[]; generatorStandardNames: string[] }

const readJson = <T>(name: string): T =>
  JSON.parse(readFileSync(resolve(srcRoot, "test/fixtures", name), "utf8")) as T

const oldBlobs = readJson<OldBlobs>("old-app-tracked-blobs.json")
const oldCss = readJson<OldCssNames>("old-css-custom-properties.json")

const projectFiles = listProjectFiles(projectRoot)
// 앱 소스: src 아래, 시험 파일·시험 전용 폴더 제외
const appSourceFiles = projectFiles.filter(
  (file) => file.startsWith("src/") && !isTestFile(file)
)
const read = (relative: string) => readFileSync(resolve(projectRoot, relative))

describe("D15 고정 자료", () => {
  it("옛 추적 파일 blob 해시가 146개 있다", () => {
    expect(oldBlobs.count).toBe(146)
    expect(oldBlobs.files).toHaveLength(146)
  })

  it("옛 CSS 변수 이름 목록이 비어 있지 않다", () => {
    expect(oldCss.names.length).toBeGreaterThan(0)
  })

  it("생성기 표준 이름은 옛 이름 목록의 부분집합이다", () => {
    const all = new Set(oldCss.names)
    expect(oldCss.generatorStandardNames.every((name) => all.has(name))).toBe(
      true
    )
  })
})

describe("D15 (a) 옛 파일 이식 없음", () => {
  it("새 src 파일의 blob 해시가 옛 추적 파일과 같은 것이 0건이다", () => {
    const oldHashes = new Map(oldBlobs.files.map((f) => [f.blob, f.path]))
    // 범위는 src/ 아래 전체(시험 폴더 포함, D15 «새 src»). 루트 설정 파일은 대상이 아니다.
    const matches = projectFiles
      .filter((file) => file.startsWith("src/"))
      .map((file) => ({ file, blob: gitBlobSha1(read(file)) }))
      .filter(({ blob }) => oldHashes.has(blob))
      .map(({ file, blob }) => `${file} == ${oldHashes.get(blob)}`)
    expect(matches).toEqual([])
  })
})

describe("D15 (b) 옛 CSS 변수 이름 사용 없음", () => {
  it("앱 소스와 index.html 이 옛 사용자 정의 변수 이름을 쓰지 않는다", () => {
    const standard = new Set(oldCss.generatorStandardNames)
    const forbidden = new Set(oldCss.names.filter((n) => !standard.has(n)))
    expect(forbidden.size).toBeGreaterThan(0)

    const hits = [...appSourceFiles, "index.html"].flatMap((file) =>
      findForbiddenCssVariables(read(file).toString("utf8"), forbidden).map(
        (name) => `${file}: ${name}`
      )
    )
    expect(hits).toEqual([])
  })
})

describe("D11 (c) 시험 전용 폴더 import 없음", () => {
  it("시험 파일 밖에서 src/test 를 import 하지 않는다", () => {
    const hits = appSourceFiles
      .filter((file) => /\.(ts|tsx)$/.test(file))
      .flatMap((file) =>
        findTestImports(read(file).toString("utf8")).map(
          (target) => `${file}: ${target}`
        )
      )
    expect(hits).toEqual([])
  })
})

describe("D11 (d) 부동소수점 변환 없음", () => {
  // 허용 목록은 비어 있다. 차트 좌표 함수(DEC-03)가 생기는 step 에서 사람 확인 뒤에만 채운다.
  const ALLOWED_FILES: string[] = []

  it("앱 소스에 parseFloat·Number(·parseInt(·Math. 가 0건이다", () => {
    const hits = appSourceFiles
      .filter((file) => /\.(ts|tsx)$/.test(file))
      .filter((file) => !ALLOWED_FILES.includes(file))
      .flatMap((file) =>
        findForbiddenNumberConversions(read(file).toString("utf8")).map(
          (token) => `${file}: ${token}`
        )
      )
    expect(hits).toEqual([])
  })
})
