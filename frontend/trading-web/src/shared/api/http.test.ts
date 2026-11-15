import { afterEach, describe, expect, it, vi } from "vitest"

import { ApiError } from "./error"
import {
  decodeArray,
  decodeDecimal,
  decodeObject,
  decodeSafeInteger,
  decodeString,
} from "./decode"
import { requestJson } from "./http"

// text() 만 가진 가짜 응답, response.json() 우회(double 변환) 검출용
const fakeResponse = (status: number, body: string) => ({
  ok: status >= 200 && status < 300,
  status,
  text: async () => body,
})

// 응답 = text() 를 가진 객체, 그 밖(Error·DOMException)은 fetch 가 던질 값
// jsdom 의 DOMException 은 Node 의 Error 를 상속하지 않아 instanceof Error 판별 불가
const stubFetch = (outcome: ReturnType<typeof fakeResponse> | Error | DOMException) => {
  const fetchMock = vi.fn(async () => {
    if (!("text" in outcome)) throw outcome
    return outcome
  })
  vi.stubGlobal("fetch", fetchMock)
  return fetchMock
}

const caught = async (run: () => Promise<unknown>): Promise<unknown> => {
  try {
    await run()
  } catch (error) {
    return error
  }
  throw new Error("거부되지 않았습니다")
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe("requestJson — 성공", () => {
  it("본문을 무손실로 읽어 디코더로 넘긴다(금액 20자리·지수 표기 보존)", async () => {
    stubFetch(
      fakeResponse(
        200,
        '{"id":12,"deposit":12345678901234567890.12,"quantity":1E-8,"orders":[{"id":1,"price":1000000.0000}]}'
      )
    )
    const decode = decodeObject({
      id: decodeSafeInteger,
      deposit: decodeDecimal,
      quantity: decodeDecimal,
      orders: decodeArray(decodeObject({ id: decodeSafeInteger, price: decodeDecimal })),
    })
    await expect(requestJson("/api/v1/accounts/12", decode)).resolves.toEqual({
      id: 12,
      deposit: "12345678901234567890.12",
      quantity: "0.00000001",
      orders: [{ id: 1, price: "1000000.0000" }],
    })
  })

  it("GET 은 Accept 만 보내고 본문·Content-Type 이 없다", async () => {
    const fetchMock = stubFetch(fakeResponse(200, '"ok"'))
    await requestJson("/api/x", decodeString)
    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit]
    expect(url).toBe("/api/x")
    expect(init.method).toBe("GET")
    expect(init.body).toBeUndefined()
    expect(new Headers(init.headers).get("accept")).toBe("application/json")
    expect(new Headers(init.headers).has("content-type")).toBe(false)
  })

  it("body 가 있으면 JSON 문자열로 보내고 Content-Type 을 붙인다", async () => {
    const fetchMock = stubFetch(fakeResponse(200, '"ok"'))
    await requestJson("/api/orders", decodeString, {
      method: "POST",
      body: { quantity: "0.00000001", idempotencyKey: "k-1" },
    })
    const [, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit]
    expect(init.method).toBe("POST")
    expect(init.body).toBe('{"quantity":"0.00000001","idempotencyKey":"k-1"}')
    expect(new Headers(init.headers).get("content-type")).toBe("application/json")
  })

  it("빈 본문(204)은 디코더에 null 로 넘긴다", async () => {
    stubFetch(fakeResponse(204, ""))
    await expect(requestJson("/api/x", (value) => value)).resolves.toBeNull()
  })

  it("signal 을 fetch 에 그대로 전달한다", async () => {
    const fetchMock = stubFetch(fakeResponse(200, '"ok"'))
    const controller = new AbortController()
    await requestJson("/api/x", decodeString, { signal: controller.signal })
    const [, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit]
    expect(init.signal).toBe(controller.signal)
  })
})

describe("requestJson — 실패는 모두 ApiError (D14)", () => {
  it("4xx JSON 오류 본문을 정규화한다", async () => {
    stubFetch(
      fakeResponse(404, '{"status":404,"code":"ORDER_NOT_FOUND","message":"주문 없음"}')
    )
    const error = await caught(() => requestJson("/api/x", decodeString))
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      kind: "http",
      status: 404,
      code: "ORDER_NOT_FOUND",
      service: "trading-api",
      userMessage: "요청한 대상을 찾을 수 없습니다.",
    })
  })

  it("JSON 이 아닌 오류 본문(프록시 HTML)도 ApiError 로 만든다", async () => {
    stubFetch(fakeResponse(502, "<html>Bad Gateway</html>"))
    const error = await caught(() => requestJson("/api/x", decodeString))
    expect(error).toMatchObject({
      kind: "http",
      status: 502,
      service: "unknown",
      rawMessage: "<html>Bad Gateway</html>",
    })
  })

  it("fetch 자체가 실패하면 kind network 이다", async () => {
    stubFetch(new TypeError("Failed to fetch"))
    const error = await caught(() => requestJson("/api/x", decodeString))
    expect(error).toMatchObject({ kind: "network", status: 0 })
  })

  it("취소(AbortError)는 감싸지 않고 그대로 던진다", async () => {
    stubFetch(new DOMException("The operation was aborted.", "AbortError"))
    const error = await caught(() => requestJson("/api/x", decodeString))
    expect(error).not.toBeInstanceOf(ApiError)
    expect((error as Error).name).toBe("AbortError")
  })

  it("성공 응답이 JSON 이 아니면 kind decode 이다", async () => {
    stubFetch(fakeResponse(200, "<html>not json</html>"))
    const error = await caught(() => requestJson("/api/x", decodeString))
    expect(error).toMatchObject({ kind: "decode", status: 200 })
  })

  it("디코더가 거부하면 kind decode 이고 경로가 원문 자리에 남는다", async () => {
    stubFetch(fakeResponse(200, '{"deposit":"abc"}'))
    const error = await caught(() =>
      requestJson("/api/x", decodeObject({ deposit: decodeDecimal }))
    )
    expect(error).toMatchObject({ kind: "decode", status: 200 })
    expect((error as ApiError).rawMessage).toContain("response.deposit")
  })

  it("디코더가 DecodeError 가 아닌 예외를 던지면 그대로 올린다(버그를 가리지 않는다)", async () => {
    stubFetch(fakeResponse(200, '"ok"'))
    const error = await caught(() =>
      requestJson("/api/x", () => {
        throw new RangeError("디코더 버그")
      })
    )
    expect(error).toBeInstanceOf(RangeError)
  })
})
