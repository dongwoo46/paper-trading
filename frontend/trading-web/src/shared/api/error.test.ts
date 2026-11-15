import { describe, expect, it } from "vitest"

import {
  ApiError,
  decodeApiError,
  networkApiError,
  normalizeApiError,
} from "./error"
import { DecodeError } from "./decode"

describe("normalizeApiError — 서비스별 오류 모양 (D14)", () => {
  it("trading-api {status, code, message}", () => {
    const error = normalizeApiError(404, {
      status: 404,
      code: "ORDER_NOT_FOUND",
      message: "주문을 찾을 수 없습니다.",
    })
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      kind: "http",
      status: 404,
      code: "ORDER_NOT_FOUND",
      service: "trading-api",
      rawMessage: "주문을 찾을 수 없습니다.",
    })
  })

  it("collector-api {status, error, code, message, path, timestamp}", () => {
    const error = normalizeApiError(400, {
      status: 400,
      error: "Bad Request",
      code: "INVALID_SYMBOL",
      message: "심볼이 올바르지 않습니다.",
      path: "/api/kis/symbols/catalog",
      timestamp: "2026-11-15T10:00:00",
    })
    expect(error).toMatchObject({
      status: 400,
      code: "INVALID_SYMBOL",
      service: "collector-api",
      rawMessage: "심볼이 올바르지 않습니다.",
    })
  })

  it("FastAPI {detail: 문자열}", () => {
    const error = normalizeApiError(404, { detail: "Not Found" })
    expect(error).toMatchObject({
      service: "quant-worker",
      code: null,
      rawMessage: "Not Found",
    })
  })

  it("FastAPI {detail: 검증 오류 배열} 은 msg 만 잇고 입력 값(input)은 싣지 않는다", () => {
    const error = normalizeApiError(422, {
      detail: [
        { loc: ["body", "symbols"], msg: "field required", type: "missing", input: "SECRET-1" },
        { loc: ["body", "start"], msg: "invalid date", type: "value_error", input: "SECRET-2" },
      ],
    })
    expect(error.service).toBe("quant-worker")
    expect(error.rawMessage).toBe("field required; invalid date")
    expect(error.rawMessage).not.toContain("SECRET")
  })

  it("알 수 없는 모양(HTML·빈 본문)은 service unknown 이고 원문은 500자까지만 둔다", () => {
    const html = `<html>${"x".repeat(2000)}</html>`
    const error = normalizeApiError(502, html)
    expect(error.service).toBe("unknown")
    expect(error.code).toBeNull()
    expect(error.rawMessage?.length).toBe(500)
    expect(normalizeApiError(500, null).rawMessage).toBeNull()
  })

  it("HTTP 상태가 본문 status 보다 우선한다", () => {
    const error = normalizeApiError(409, { status: 500, code: "X", message: "m" })
    expect(error.status).toBe(409)
  })
})

describe("userMessage — status 로 고르고 원문을 섞지 않는다 (EDGE-05)", () => {
  it.each([
    [400, "요청 값을 확인해 주세요."],
    [401, "인증이 필요하거나 권한이 없습니다."],
    [403, "인증이 필요하거나 권한이 없습니다."],
    [404, "요청한 대상을 찾을 수 없습니다."],
    [409, "다른 요청과 충돌했습니다. 새로 고친 뒤 다시 시도해 주세요."],
    [422, "요청 값을 확인해 주세요."],
    [429, "요청이 너무 많습니다. 잠시 뒤 다시 시도해 주세요."],
    [418, "요청을 처리하지 못했습니다."],
    [500, "서버에 문제가 생겼습니다. 잠시 뒤 다시 시도해 주세요."],
    [503, "서버에 문제가 생겼습니다. 잠시 뒤 다시 시도해 주세요."],
  ])("%i → %s", (status, expected) => {
    expect(normalizeApiError(status, null).userMessage).toBe(expected)
  })

  it("서버 원문(예외 이름·스택)은 userMessage 에 들어가지 않고 rawMessage 에만 있다", () => {
    const raw = "java.lang.NullPointerException at OrderService.place(OrderService.kt:42)"
    const error = normalizeApiError(500, { status: 500, code: "INTERNAL_SERVER_ERROR", message: raw })
    expect(error.userMessage).not.toContain("NullPointerException")
    expect(error.rawMessage).toBe(raw)
  })
})

describe("networkApiError · decodeApiError", () => {
  it("연결 실패는 status 0, kind network 이다", () => {
    const error = networkApiError()
    expect(error).toMatchObject({ kind: "network", status: 0, code: null })
    expect(error.userMessage).toBe("서버에 연결하지 못했습니다. 네트워크를 확인해 주세요.")
  })

  it("응답 해석 실패는 kind decode 이고 경로만 원문 자리에 둔다", () => {
    const error = decodeApiError(200, new DecodeError("account.deposit", "10진수 문자열", "abc"))
    expect(error).toMatchObject({ kind: "decode", status: 200 })
    expect(error.userMessage).toBe("응답 형식이 예상과 달라 표시하지 못했습니다.")
    expect(error.rawMessage).toContain("account.deposit")
  })
})
