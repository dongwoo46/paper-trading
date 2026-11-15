const toHex = (byte: number) => byte.toString(16).padStart(2, "0")

/** RULE-04 주문 멱등키, UUID v4(서버 컬럼 한도 100자 이내) — randomUUID 는 보안 컨텍스트 전용이라 사내망 http 용 getRandomValues 경로 */
export function createIdempotencyKey(): string {
  const cryptoApi = globalThis.crypto
  if (typeof cryptoApi?.randomUUID === "function") return cryptoApi.randomUUID()
  if (typeof cryptoApi?.getRandomValues === "function") {
    const bytes = cryptoApi.getRandomValues(new Uint8Array(16))
    bytes[6] = (bytes[6] & 0x0f) | 0x40
    bytes[8] = (bytes[8] & 0x3f) | 0x80
    const hex = Array.from(bytes, toHex).join("")
    return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
  }
  // 예측 가능한 난수로의 조용한 대체 금지
  throw new Error("안전한 난수원(crypto)이 없어 멱등키를 만들 수 없습니다.")
}
