package com.papertrading.api.infrastructure.kis

enum class KisErrorCode {
    KIS_ORDER_REJECTED,
    KIS_AUTH_FAILED,
    KIS_RATE_LIMITED,
    KIS_TEMPORARY_UNAVAILABLE,
    KIS_INVALID_ACCOUNT,
    KIS_DUPLICATE_OR_ALREADY_PROCESSED,
}

class KisApiException(
    val code: KisErrorCode,
    val kisRtCode: String?,
    kisMessage: String?,
) : RuntimeException("KIS API error code=$code rtCd=$kisRtCode message=${kisMessage.orEmpty()}")

fun mapKisErrorCode(rtCd: String?, message: String?): KisErrorCode {
    val normalized = message.orEmpty().lowercase()
    return when {
        normalized.contains("account") || normalized.contains("계좌") -> KisErrorCode.KIS_INVALID_ACCOUNT
        normalized.contains("auth") || normalized.contains("token") || normalized.contains("인증") -> KisErrorCode.KIS_AUTH_FAILED
        normalized.contains("rate") || normalized.contains("limit") || normalized.contains("초과") -> KisErrorCode.KIS_RATE_LIMITED
        normalized.contains("duplicate") || normalized.contains("already") || normalized.contains("중복") -> KisErrorCode.KIS_DUPLICATE_OR_ALREADY_PROCESSED
        rtCd == null -> KisErrorCode.KIS_TEMPORARY_UNAVAILABLE
        else -> KisErrorCode.KIS_ORDER_REJECTED
    }
}
