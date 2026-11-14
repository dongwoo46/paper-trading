const INTEGER = /^-?\d+$/
// 2^53 - 1, 이보다 큰 정수는 number 변환 시 값 변형
const MAX_SAFE = 9007199254740991n

/** id·개수 전용(금액·수량·가격 사용 금지, RULE-03), 소스 검사 Number 변환 허용 파일 */
export function parseSafeInteger(text: string): number | null {
  if (!INTEGER.test(text)) return null
  const value = BigInt(text)
  if (value > MAX_SAFE || value < -MAX_SAFE) return null
  // -0 방지
  return value === 0n ? 0 : Number(value)
}
