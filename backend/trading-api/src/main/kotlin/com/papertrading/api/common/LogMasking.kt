package com.papertrading.api.common

private val KIS_ACCOUNT_NUMBER = Regex("""(\d+)(\d{2})(-\d+)""")

/** KIS 계좌번호(CANO-상품코드) 앞자리를 가림 — 체결 id·계좌 범위 로그용, 끝 2자리와 상품코드만 남김 */
fun maskKisAccountNumber(text: String): String = KIS_ACCOUNT_NUMBER.replace(text) { match ->
    val (hiddenDigits, lastDigits, productCode) = match.destructured
    "*".repeat(hiddenDigits.length) + lastDigits + productCode
}
