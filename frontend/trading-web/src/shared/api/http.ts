import { DecodeError, type Decoder } from "./decode"
import {
  decodeApiError,
  networkApiError,
  normalizeApiError,
  type ApiError,
} from "./error"
import { parseJsonLossless } from "./parse-json"

export type RequestOptions = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE"
  /** JSON 직렬화, 금액·수량은 문자열로 전달 */
  body?: unknown
  headers?: HeadersInit
  signal?: AbortSignal
}

const ROOT_PATH = "response"

// 취소는 오류 상태로 바꾸지 않고 호출자(TanStack Query 등)에게 그대로 전달
const isAbort = (error: unknown): boolean =>
  typeof error === "object" &&
  error !== null &&
  "name" in error &&
  error.name === "AbortError"

function errorFromResponse(status: number, text: string): ApiError {
  try {
    return normalizeApiError(status, text.trim() === "" ? null : parseJsonLossless(text))
  } catch (error) {
    if (!(error instanceof SyntaxError)) throw error
    // 프록시 HTML 등 JSON 아닌 본문은 원문 그대로 정규화(rawMessage 로 노출)
    return normalizeApiError(status, text)
  }
}

async function fetchText(
  url: string,
  options: RequestOptions
): Promise<{ status: number; ok: boolean; text: string }> {
  const headers = new Headers(options.headers)
  headers.set("Accept", "application/json")
  if (options.body !== undefined) headers.set("Content-Type", "application/json")
  try {
    const response = await fetch(url, {
      method: options.method ?? "GET",
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: options.signal,
    })
    // response.json() 은 double 변환이라 text 만 사용(D4)
    return { status: response.status, ok: response.ok, text: await response.text() }
  } catch (error) {
    if (isAbort(error)) throw error
    throw networkApiError()
  }
}

/** D7 API 호출 단일 경로, 취소 외 실패는 전부 ApiError(D14) */
export async function requestJson<T>(
  url: string,
  decode: Decoder<T>,
  options: RequestOptions = {}
): Promise<T> {
  const { status, ok, text } = await fetchText(url, options)
  if (!ok) throw errorFromResponse(status, text)

  try {
    // 빈 본문(204 등)은 null
    const value = text.trim() === "" ? null : parseJsonLossless(text)
    return decode(value, ROOT_PATH)
  } catch (error) {
    if (error instanceof DecodeError) throw decodeApiError(status, error)
    if (error instanceof SyntaxError) {
      throw decodeApiError(status, new DecodeError(ROOT_PATH, "JSON 본문", text))
    }
    throw error
  }
}
