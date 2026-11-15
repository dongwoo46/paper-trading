import { parse } from "lossless-json"

/** D4: 숫자 토큰을 원문 문자열로 보존 — 기본 파싱은 double 변환으로 금액·수량 자릿수 손실(RISK-01) */
export function parseJsonLossless(text: string): unknown {
  return parse(text, undefined, { parseNumber: (token) => token })
}
