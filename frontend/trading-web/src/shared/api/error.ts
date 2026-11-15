import type { DecodeError } from "./decode"

export type ApiErrorKind = "http" | "network" | "decode"
export type ApiService = "trading-api" | "collector-api" | "quant-worker" | "unknown"

export type ApiErrorInit = {
  kind: ApiErrorKind
  status: number
  code: string | null
  service: ApiService
  userMessage: string
  rawMessage: string | null
}

// 프록시 HTML 오류 페이지 같은 긴 본문 방지 상한
const RAW_MESSAGE_LIMIT = 500

/** 화면 문구(userMessage)와 접힌 서버 원문(rawMessage)의 분리(D14, EDGE-05) */
export class ApiError extends Error {
  readonly kind: ApiErrorKind
  readonly status: number
  readonly code: string | null
  readonly service: ApiService
  readonly userMessage: string
  readonly rawMessage: string | null

  constructor(init: ApiErrorInit) {
    super(init.userMessage)
    this.name = "ApiError"
    this.kind = init.kind
    this.status = init.status
    this.code = init.code
    this.service = init.service
    this.userMessage = init.userMessage
    this.rawMessage = init.rawMessage
  }
}

function userMessageFor(status: number): string {
  if (status === 400 || status === 422) return "요청 값을 확인해 주세요."
  if (status === 401 || status === 403) return "인증이 필요하거나 권한이 없습니다."
  if (status === 404) return "요청한 대상을 찾을 수 없습니다."
  if (status === 409) return "다른 요청과 충돌했습니다. 새로 고친 뒤 다시 시도해 주세요."
  if (status === 429) return "요청이 너무 많습니다. 잠시 뒤 다시 시도해 주세요."
  if (status >= 500) return "서버에 문제가 생겼습니다. 잠시 뒤 다시 시도해 주세요."
  return "요청을 처리하지 못했습니다."
}

type RawError = { service: ApiService; code: string | null; rawMessage: string | null }

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === "object" && value !== null && !Array.isArray(value)

const asText = (value: unknown): string | null =>
  typeof value === "string" ? value : null

function clip(text: string | null): string | null {
  return text === null ? null : text.slice(0, RAW_MESSAGE_LIMIT)
}

// FastAPI 검증 오류 항목의 input(요청 값) 유출 방지로 msg 만 사용
function fastApiDetail(detail: unknown): string | null {
  if (typeof detail === "string") return detail
  if (!Array.isArray(detail)) return null
  const messages = detail.flatMap((item) =>
    isRecord(item) && typeof item.msg === "string" ? [item.msg] : []
  )
  return messages.length > 0 ? messages.join("; ") : null
}

/** 서비스별 오류 본문 모양: trading-api {status,code,message} · collector {status,error,code,message,path,timestamp} · FastAPI {detail} */
function readRawError(body: unknown): RawError {
  if (typeof body === "string") {
    return { service: "unknown", code: null, rawMessage: clip(body) }
  }
  if (!isRecord(body)) return { service: "unknown", code: null, rawMessage: null }
  if ("detail" in body) {
    return { service: "quant-worker", code: null, rawMessage: clip(fastApiDetail(body.detail)) }
  }
  const code = asText(body.code)
  const rawMessage = clip(asText(body.message))
  if ("error" in body || "path" in body || "timestamp" in body) {
    return { service: "collector-api", code, rawMessage }
  }
  if (code !== null || rawMessage !== null) {
    return { service: "trading-api", code, rawMessage }
  }
  return { service: "unknown", code: null, rawMessage: null }
}

/** HTTP 상태가 본문 status 보다 우선 */
export function normalizeApiError(status: number, body: unknown): ApiError {
  const raw = readRawError(body)
  return new ApiError({
    kind: "http",
    status,
    code: raw.code,
    service: raw.service,
    userMessage: userMessageFor(status),
    rawMessage: raw.rawMessage,
  })
}

export function networkApiError(): ApiError {
  return new ApiError({
    kind: "network",
    status: 0,
    code: null,
    service: "unknown",
    userMessage: "서버에 연결하지 못했습니다. 네트워크를 확인해 주세요.",
    rawMessage: null,
  })
}

/** 응답은 왔으나 기대 모양 아님, 원문 자리에는 오류 위치(path)만 */
export function decodeApiError(status: number, error: DecodeError): ApiError {
  return new ApiError({
    kind: "decode",
    status,
    code: null,
    service: "unknown",
    userMessage: "응답 형식이 예상과 달라 표시하지 못했습니다.",
    rawMessage: error.message,
  })
}
