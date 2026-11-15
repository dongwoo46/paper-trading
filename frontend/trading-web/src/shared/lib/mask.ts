// AS-01: XXXXXXXX-XX 의 앞 8자리 중 가운데 4자리
const ACCOUNT_NUMBER = /^(\d{2})\d{4}(\d{2})-(\d{2})$/
// 형식 이상 값은 길이·구조 비노출을 위해 고정 길이
const FULLY_MASKED = "********"

/** RULE-11 계좌번호 마스킹, 값 없음 «-» */
export function maskAccountNumber(raw: string | null | undefined): string {
  if (raw == null || raw === "") return "-"
  const match = ACCOUNT_NUMBER.exec(raw)
  if (!match) return FULLY_MASKED
  return `${match[1]}****${match[2]}-${match[3]}`
}
