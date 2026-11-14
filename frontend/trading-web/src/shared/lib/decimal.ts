import { parseSafeInteger } from "./safe-integer"

/** 정규화 10진 문자열(지수·앞자리 0 없음, 소수부 끝 0 은 서버 자릿수라 보존), normalizeDecimal 경유 값만 — 산술 없음(FLAG-04) */
export type DecimalString = string & { readonly __brand: "DecimalString" }

export type DecimalParts = {
  negative: boolean
  integer: string
  fraction: string
}

export type DecimalTone = "rise" | "fall" | "flat"

export type FormatDecimalOptions = {
  /** 표시 소수 자리, half-up(0 에서 먼 쪽)·모자란 자리는 0 채움 */
  fractionDigits?: number
  /** RULE-05 부호 병기, 0 은 부호 없음 */
  signed?: boolean
}

const RAW_PATTERN = /^([+-]?)(\d+)(?:\.(\d+))?(?:[eE]([+-]?\d+))?$/
const NORMALIZED_PATTERN = /^(-?)(\d+)(?:\.(\d+))?$/
// 1E9999 입력의 수천 자 문자열 생성 방지 상한, BigDecimal 실사용 자릿수(1E-8)보다 훨씬 큼
const MAX_EXPONENT = 1000

function isAllZero(digits: string): boolean {
  return /^0*$/.test(digits)
}

/** pointPos = 소수점 앞 자릿수, digits 범위 밖이면 0 채움 */
function build(negative: boolean, digits: string, pointPos: number): DecimalString {
  let integer: string
  let fraction: string
  if (pointPos <= 0) {
    integer = "0"
    fraction = "0".repeat(-pointPos) + digits
  } else if (pointPos >= digits.length) {
    integer = digits + "0".repeat(pointPos - digits.length)
    fraction = ""
  } else {
    integer = digits.slice(0, pointPos)
    fraction = digits.slice(pointPos)
  }
  integer = integer.replace(/^0+(?=\d)/, "")
  // 음의 0 방지
  const sign = negative && !isAllZero(integer + fraction) ? "-" : ""
  return `${sign}${integer}${fraction ? `.${fraction}` : ""}` as DecimalString
}

export function normalizeDecimal(raw: string): DecimalString | null {
  const match = RAW_PATTERN.exec(raw)
  if (!match) return null
  const [, sign, integer, fraction = "", exponentText = "0"] = match
  const exponent = parseSafeInteger(exponentText.replace(/^\+/, ""))
  if (exponent === null || exponent > MAX_EXPONENT || exponent < -MAX_EXPONENT) {
    return null
  }
  return build(sign === "-", integer + fraction, integer.length + exponent)
}

/** 내부 불변식 전용, 위반은 버그 */
export function requireDecimal(raw: string): DecimalString {
  const value = normalizeDecimal(raw)
  if (value === null) throw new Error("10진수가 아닌 값이 들어왔습니다.")
  return value
}

export function splitDecimal(value: DecimalString): DecimalParts {
  const match = NORMALIZED_PATTERN.exec(value)
  if (!match) throw new Error("정규화되지 않은 10진 문자열입니다.")
  return {
    negative: match[1] === "-",
    integer: match[2],
    fraction: match[3] ?? "",
  }
}

export function decimalSign(value: DecimalString): -1 | 0 | 1 {
  const { negative, integer, fraction } = splitDecimal(value)
  if (isAllZero(integer + fraction)) return 0
  return negative ? -1 : 1
}

export function decimalTone(value: DecimalString | null | undefined): DecimalTone {
  if (value == null) return "flat"
  const sign = decimalSign(value)
  return sign > 0 ? "rise" : sign < 0 ? "fall" : "flat"
}

export function compareDecimal(a: DecimalString, b: DecimalString): -1 | 0 | 1 {
  const signA = decimalSign(a)
  const signB = decimalSign(b)
  if (signA !== signB) return signA < signB ? -1 : 1
  if (signA === 0) return 0
  const left = splitDecimal(a)
  const right = splitDecimal(b)
  const magnitude = compareMagnitude(left, right)
  return signA < 0 ? (-magnitude as -1 | 0 | 1) : magnitude
}

function compareMagnitude(a: DecimalParts, b: DecimalParts): -1 | 0 | 1 {
  if (a.integer.length !== b.integer.length) {
    return a.integer.length < b.integer.length ? -1 : 1
  }
  if (a.integer !== b.integer) return a.integer < b.integer ? -1 : 1
  const width = a.fraction.length > b.fraction.length ? a.fraction.length : b.fraction.length
  const left = a.fraction.padEnd(width, "0")
  const right = b.fraction.padEnd(width, "0")
  if (left === right) return 0
  return left < right ? -1 : 1
}

/** 비율 단위 변환 전용 표기 이동(양수 = 오른쪽), 산술 아님 */
export function shiftDecimalPoint(value: DecimalString, places: number): DecimalString {
  const { negative, integer, fraction } = splitDecimal(value)
  return build(negative, integer + fraction, integer.length + places)
}

/** half-up(0 에서 먼 쪽), 반올림 결과 0 은 부호 없음 */
export function roundDecimal(value: DecimalString, fractionDigits: number): DecimalString {
  const { negative, integer, fraction } = splitDecimal(value)
  if (fraction.length <= fractionDigits) {
    return build(negative, integer + fraction.padEnd(fractionDigits, "0"), integer.length)
  }
  const kept = integer + fraction.slice(0, fractionDigits)
  const roundUp = fraction.charAt(fractionDigits) >= "5"
  const rounded = (BigInt(kept) + (roundUp ? 1n : 0n)).toString()
  // 자리 올림으로 자릿수가 늘어도 소수점은 끝에서 fractionDigits 칸
  const padded = rounded.padStart(fractionDigits + 1, "0")
  return build(negative, padded, padded.length - fractionDigits)
}

function groupThousands(integer: string): string {
  return integer.replace(/\B(?=(\d{3})+(?!\d))/g, ",")
}

export function formatDecimal(
  value: DecimalString | null | undefined,
  options: FormatDecimalOptions = {}
): string {
  if (value == null) return "-"
  const target =
    options.fractionDigits === undefined
      ? value
      : roundDecimal(value, options.fractionDigits)
  const { negative, integer, fraction } = splitDecimal(target)
  const zero = decimalSign(target) === 0
  const sign = negative ? "-" : options.signed && !zero ? "+" : ""
  return `${sign}${groupThousands(integer)}${fraction ? `.${fraction}` : ""}`
}

/** AS-02: 소수부 전부 0 이면 정수, 아니면 그대로(반올림 없음) */
export function formatWon(value: DecimalString | null | undefined): string {
  if (value == null) return "-"
  const { negative, integer, fraction } = splitDecimal(value)
  const shownFraction = isAllZero(fraction) ? "" : `.${fraction}`
  return `${negative ? "-" : ""}${groupThousands(integer)}${shownFraction}`
}
