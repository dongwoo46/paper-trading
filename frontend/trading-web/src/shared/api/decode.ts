import { normalizeDecimal, type DecimalString } from "@/shared/lib/decimal"
import {
  asRateFraction,
  asRatePercent,
  type RateFraction,
  type RatePercent,
} from "@/shared/lib/rate"
import { parseSafeInteger } from "@/shared/lib/safe-integer"

/** 값 검사와 타입 부여, 불일치는 DecodeError(path = 오류 위치 표시용) */
export type Decoder<T> = (value: unknown, path: string) => T

function describeType(value: unknown): string {
  if (value === null) return "null"
  if (value === undefined) return "없음"
  if (Array.isArray(value)) return "배열"
  return typeof value
}

export class DecodeError extends Error {
  readonly path: string
  readonly expected: string

  constructor(path: string, expected: string, actual: unknown) {
    // 값 비노출, 계좌번호·토큰의 오류 문구 유출 방지(RULE-11)
    super(`${path}: ${expected} 필요 (받은 형: ${describeType(actual)})`)
    this.name = "DecodeError"
    this.path = path
    this.expected = expected
  }
}

/** 파서가 숫자 토큰도 문자열로 넘기므로 JS number 는 파서 우회 값(이미 double)이라 거부 */
export const decodeString: Decoder<string> = (value, path) => {
  if (typeof value !== "string") throw new DecodeError(path, "문자열", value)
  return value
}

export const decodeBoolean: Decoder<boolean> = (value, path) => {
  if (typeof value !== "boolean") throw new DecodeError(path, "불리언", value)
  return value
}

/** id·개수: 안전 정수 원문 → number */
export const decodeSafeInteger: Decoder<number> = (value, path) => {
  const parsed = typeof value === "string" ? parseSafeInteger(value) : null
  if (parsed === null) throw new DecodeError(path, "안전 정수", value)
  return parsed
}

/** 금액·수량·가격: 정규화한 10진 문자열 */
export const decodeDecimal: Decoder<DecimalString> = (value, path) => {
  const parsed = typeof value === "string" ? normalizeDecimal(value) : null
  if (parsed === null) throw new DecodeError(path, "10진수 문자열", value)
  return parsed
}

/** 소수 비율(0.1234 = 12.34%) */
export const decodeRateFraction: Decoder<RateFraction> = (value, path) =>
  asRateFraction(decodeDecimal(value, path))

/** 퍼센트 값(12.34 = 12.34%) */
export const decodeRatePercent: Decoder<RatePercent> = (value, path) =>
  asRatePercent(decodeDecimal(value, path))

export const decodeRecord: Decoder<Record<string, unknown>> = (value, path) => {
  if (typeof value !== "object" || value === null || Array.isArray(value)) {
    throw new DecodeError(path, "객체", value)
  }
  return value as Record<string, unknown>
}

export function decodeArray<T>(item: Decoder<T>): Decoder<T[]> {
  return (value, path) => {
    if (!Array.isArray(value)) throw new DecodeError(path, "배열", value)
    return value.map((element, index) => item(element, `${path}[${index}]`))
  }
}

/** null·필드 없음 → null */
export function decodeNullable<T>(inner: Decoder<T>): Decoder<T | null> {
  return (value, path) =>
    value === null || value === undefined ? null : inner(value, path)
}

export function decodeOneOf<T extends string>(allowed: readonly T[]): Decoder<T> {
  return (value, path) => {
    if (typeof value !== "string" || !allowed.includes(value as T)) {
      throw new DecodeError(path, `${allowed.join("·")} 중 하나`, value)
    }
    return value as T
  }
}

/** 선언 필드만 추출·서버 추가 필드 무시·없는 필드는 undefined 로 디코더 전달 */
export function decodeObject<T>(
  shape: { [K in keyof T]: Decoder<T[K]> }
): Decoder<T> {
  return (value, path) => {
    const record = decodeRecord(value, path)
    const result: Partial<T> = {}
    for (const key of Object.keys(shape) as (keyof T & string)[]) {
      const field = Object.hasOwn(record, key) ? record[key] : undefined
      result[key] = shape[key](field, `${path}.${key}`)
    }
    return result as T
  }
}
