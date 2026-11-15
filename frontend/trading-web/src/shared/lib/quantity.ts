import { requireDecimal, splitDecimal, type DecimalString } from "./decimal"
import { parseSafeInteger } from "./safe-integer"

// D6: 수량 BigInt 연산은 이 두 함수만 허용, 금액 산술 없음(FLAG-04)

function scaleOf(value: DecimalString): number {
  return splitDecimal(value).fraction.length
}

function toScaledInteger(value: DecimalString, scale: number): bigint {
  const { negative, integer, fraction } = splitDecimal(value)
  const magnitude = BigInt(integer + fraction.padEnd(scale, "0"))
  return negative ? -magnitude : magnitude
}

function fromScaledInteger(scaled: bigint, scale: number): DecimalString {
  const negative = scaled < 0n
  const digits = (negative ? -scaled : scaled).toString().padStart(scale + 1, "0")
  const integer = digits.slice(0, digits.length - scale)
  const fraction = digits.slice(digits.length - scale)
  return requireDecimal(
    `${negative ? "-" : ""}${integer}${scale > 0 ? `.${fraction}` : ""}`
  )
}

/** 주문수량 − 체결수량, 자릿수는 큰 쪽 기준·체결 > 주문이면 음수 그대로(서버 불일치 은폐 금지) */
export function remainingQuantity(
  quantity: DecimalString,
  filledQuantity: DecimalString
): DecimalString {
  const scale = scaleOf(quantity) > scaleOf(filledQuantity) ? scaleOf(quantity) : scaleOf(filledQuantity)
  return fromScaledInteger(
    toScaledInteger(quantity, scale) - toScaledInteger(filledQuantity, scale),
    scale
  )
}

/** 진행 막대 폭 정수 %(0~100), 내림이라 미체결이 남으면 100 불가 */
export function fillProgressPercent(
  quantity: DecimalString,
  filledQuantity: DecimalString
): number {
  const scale = scaleOf(quantity) > scaleOf(filledQuantity) ? scaleOf(quantity) : scaleOf(filledQuantity)
  const total = toScaledInteger(quantity, scale)
  const filled = toScaledInteger(filledQuantity, scale)
  if (total <= 0n || filled <= 0n) return 0
  if (filled >= total) return 100
  const percent = parseSafeInteger(((filled * 100n) / total).toString())
  if (percent === null) throw new Error("진행률 계산 결과가 정수 범위를 벗어났습니다.")
  return percent
}
