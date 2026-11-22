package com.papertrading.api.infrastructure.kis

import com.papertrading.api.common.exception.KisAccountNotLinkedException

/** KIS 계좌번호 XXXXXXXX-XX 를 CANO·상품코드로 나눔, 없거나 형식이 틀리면 KIS 호출 전에 409 (잔고·주문 어댑터 공용) */
internal fun kisAccountNumberParts(externalAccountId: String?): Pair<String, String> {
    val parts = externalAccountId?.split("-").orEmpty()
    if (!(parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank())) {
        throw KisAccountNotLinkedException()
    }
    return parts[0] to parts[1]
}
