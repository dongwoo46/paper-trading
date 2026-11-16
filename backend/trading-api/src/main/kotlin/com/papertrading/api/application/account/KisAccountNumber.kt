package com.papertrading.api.application.account

import com.papertrading.api.common.exception.KisAccountNotLinkedException

/** KIS 계좌번호 XXXXXXXX-XX 검사, 비었거나 형식이 틀리면 미연결 (F-4), 값은 바꾸지 않고 그대로 돌려줌 */
internal fun linkedKisAccountNumber(externalAccountId: String?): String {
    val value = externalAccountId ?: throw KisAccountNotLinkedException()
    val parts = value.split("-")
    if (parts.size != 2 || parts.any { it.isBlank() }) throw KisAccountNotLinkedException()
    return value
}
