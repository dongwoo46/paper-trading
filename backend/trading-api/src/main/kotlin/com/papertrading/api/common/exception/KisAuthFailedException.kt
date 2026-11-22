package com.papertrading.api.common.exception

/**
 * J — KIS 인증 실패 (토큰 만료·무효, 자격증명 오류)
 * KIS 원문과 계좌번호는 cause 에만 두고 메시지에 싣지 않음
 */
class KisAuthFailedException(cause: Throwable? = null) :
    ServiceUnavailableException(
        "KIS_AUTH_FAILED",
        "KIS 인증에 실패했습니다.",
        cause,
    )
